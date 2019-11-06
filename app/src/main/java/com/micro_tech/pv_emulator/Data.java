package com.micro_tech.pv_emulator;

import android.content.Context;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.jjoe64.graphview.series.DataPoint;

import static com.micro_tech.pv_emulator.MainActivity.ISC;
import static com.micro_tech.pv_emulator.MainActivity.SIZE;
import static com.micro_tech.pv_emulator.MainActivity.VOC;

public class Data {


    public final static int VENDOR_ID=4660;
    public static UsbManager manager;
    public static UsbInterface usbInterface;
    public static UsbEndpoint outEnd, inEnd;
    public static UsbDevice stmUsb;
    public static int WIDTH=144;
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    public static int[] J={800,1000,1100,1400};
    public static byte[] T={21,25,27,30};

    public static Typeface tp;
    public static boolean idTag=true;
    public static final long timeStamp = 5;
    public static boolean start=true;

    public static final String FILE_NAME="Historique.txt";

    public static String HELP="Solar PV Emulator\n\n" +
            "Developed by MicroTech Lab\n\n" +
            "Website : www.microtech-lab.com\n\n"+
            "Email : info@microtech-lab.com\n\n"+
            "Mobile : (+213) 5 60 09 58 85\n\n"+
            "Phone : (+213) 24 91 20 97\n\n"+
            "Fax : (+213) 24 91 20 97\n\n";


    public static double CurrentFunction(double voltage,double j,double t){
        double i;
        i=(ISC/1000)*j+0.1;
        double expo=(-10+0.038*(t-25)+(10/VOC)*voltage);
        i-=Math.exp(expo);
        return i;

    }
    static double  vFunction(double current,double j,double t){
        double v;

        if (current>ISC/1000*j+0.01) return 0;
        v=(VOC/10)*(Math.log(ISC/1000*j-current+0.01)+10-0.038*(t-25));
        if(v<0)v=0;
        return v;

    }

    public static void identification(DataPoint[] data,Context context){

        double Ic=2,v1,v2,v3,v4;
        double DI=0.005;

        if(WIDTH==36){

            for(int i=0;i<SIZE;i++) {
                v1=vFunction(Ic,J[0],T[0]);
                data[i]= new DataPoint(v1,Ic);
                Ic-=DI;
            }
        }
        else if(WIDTH==72) {

            for(int i=0;i<SIZE;i++) {
                v1=vFunction(Ic,J[0],T[0]);
                v2=vFunction(Ic,J[1],T[1]);
                data[i]= new DataPoint(v1+v2,Ic);
                Ic-=DI;

            }

        } else if(WIDTH==108) {

            //replace radians nd temp
            for(int i=0;i<SIZE;i++) {
                v1=vFunction(Ic,J[0],T[0]);
                v2=vFunction(Ic,J[1],T[1]);
                v3=vFunction(Ic,J[2],T[2]);

                data[i]= new DataPoint(v1+v2+v3,Ic);

                Ic-=DI;
            }
        }

        else if(WIDTH==144) {

            //replace radians nd temp
            for(int i=0;i<SIZE;i++) {
                v1=vFunction(Ic,J[0],T[0]);
                v2=vFunction(Ic,J[1],T[1]);
                v3=vFunction(Ic,J[2],T[2]);
                v4=vFunction(Ic,J[3],T[3]);
                data[i]= new DataPoint(v1+v2+v3+v4,Ic);

                Ic-=DI;
            }
        }

    }

}
