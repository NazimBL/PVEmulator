package com.micro_tech.pv_emulator;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import java.nio.ByteBuffer;


import static com.micro_tech.pv_emulator.Data.J;
import static com.micro_tech.pv_emulator.Data.T;
import static com.micro_tech.pv_emulator.Data.VENDOR_ID;
import static com.micro_tech.pv_emulator.Data.inEnd;
import static com.micro_tech.pv_emulator.Data.manager;
import static com.micro_tech.pv_emulator.Data.outEnd;
import static com.micro_tech.pv_emulator.Data.usbInterface;
import static com.micro_tech.pv_emulator.MainActivity.bytes;
import static com.micro_tech.pv_emulator.MainActivity.code;
import static com.micro_tech.pv_emulator.MainActivity.vp;

public class MyUsb {

    public static byte[] first=new byte[64];
    public static UsbDeviceConnection connection;

    public static UsbInterface findAdbInterface(UsbDevice device) {

        if(device.getVendorId()!=VENDOR_ID)return null;
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {

            UsbInterface intf = device.getInterface(i);
            return intf;

        }
        return null;
    }

    public static void manageEndpoints(){

        outEnd=null;
        inEnd=null;

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {

            UsbEndpoint ep = usbInterface.getEndpoint(i);

            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) outEnd = ep;
            else inEnd = ep;

        }

    }

    public static  void usbCom(UsbDevice device, Context context){


        manageEndpoints();

        if(outEnd!=null && device!=null && usbInterface!=null ) {
            if(device.getVendorId()!=VENDOR_ID)return ;
            //usbConnected=true;
            // Toast.makeText(context,"connected",Toast.LENGTH_SHORT).show();
            PendingIntent pendingIntent=PendingIntent.getBroadcast(context,0,
                    new Intent(UsbManager.EXTRA_PERMISSION_GRANTED),0);
            manager.requestPermission(device,pendingIntent);
            connection=null;
            if(manager.hasPermission(device)) connection=manager.openDevice(device);
            else Toast.makeText(context,"no pemission",Toast.LENGTH_SHORT).show();

            //turn this to write / read methods
            try {
                if (connection != null) {

                    if (connection.claimInterface(usbInterface,true)) {
                        try {

                            int transfer;
                            if(code==0){
                                transfer = connection.bulkTransfer(outEnd,sendTempt(),12,0);
                                Toast.makeText(context,"transfer "+transfer,Toast.LENGTH_SHORT).show();

                            }
                            else if(code==1){

                                bytes ="OK".getBytes();
                                transfer = connection.bulkTransfer(outEnd,bytes,bytes.length,0);
                                int reception1=connection.bulkTransfer(inEnd,first,first.length,0);
                                connection.releaseInterface(usbInterface);
                                connection.close();

                               // Toast.makeText(context,"transfered : "+transfer,Toast.LENGTH_SHORT).show();
                               // Toast.makeText(context,"received 1 "+reception1,Toast.LENGTH_SHORT).show();
                                MainActivity.myToast(" width "+first[0],context);
                                Data.WIDTH=first[0];


                            }else if(code==3){

                                bytes ="LK".getBytes();
                                transfer = connection.bulkTransfer(outEnd,bytes,bytes.length,0);

                                int reception1=connection.bulkTransfer(inEnd,first,first.length,0);
                                connection.releaseInterface(usbInterface);
                                connection.close();
                                vp=readCriticalPoint(first[0],first[1]);

                                //Toast.makeText(context,"transfered : "+transfer,Toast.LENGTH_SHORT).show();
                               //Toast.makeText(context,"received 1 "+reception1,Toast.LENGTH_SHORT).show();
                                Toast.makeText(context,"vp "+vp,Toast.LENGTH_SHORT).show();
                            }

                        }catch (Exception e){
                            Toast.makeText(context,e.toString(),Toast.LENGTH_SHORT).show();
                            connection.releaseInterface(usbInterface);
                            connection.close();
                        }

                    } else {
                        Toast.makeText(context,"couldnt claim interface",Toast.LENGTH_SHORT).show();
                        connection.releaseInterface(usbInterface);
                        connection.close();
                    }

                } else {
                    Toast.makeText(context,"couldnt open connection  ",Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                Toast.makeText(context,e.toString(),Toast.LENGTH_SHORT).show();

            }finally {
                connection.releaseInterface(usbInterface);
                connection.close();
            }

        }else  {

            Toast.makeText(context,"no end point found",Toast.LENGTH_SHORT).show();
        }

    }

    public static double readCriticalPoint(byte low,byte high){
        double vs;
        byte[] rec=new byte[2];

        rec[0]=low;
        rec[1]=high;

        ByteBuffer wrapped = ByteBuffer.wrap(rec);
        vs = (double)wrapped.getShort();

        return vs;
    }
    //this sends JlowB , JhighB , T 4times ( 4x3 =12 )
    static byte[] sendTempt(){

        byte[] ret=new byte[12];
        short r;
        int i=0;

        for(int j=0;j<4;j++) {

            r = (short) J[j];
            ret[i++] = (byte) r;
            ret[i++] = (byte) (r >> 8);
            ret[i++] = T[j];

        }

        return ret;

    }


}
