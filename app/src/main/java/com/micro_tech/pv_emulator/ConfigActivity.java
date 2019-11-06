package com.micro_tech.pv_emulator;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import static com.micro_tech.pv_emulator.Data.ACTION_USB_ATTACHED;
import static com.micro_tech.pv_emulator.Data.ACTION_USB_DETACHED;
import static com.micro_tech.pv_emulator.Data.CurrentFunction;
import static com.micro_tech.pv_emulator.Data.FILE_NAME;
import static com.micro_tech.pv_emulator.Data.J;
import static com.micro_tech.pv_emulator.Data.T;
import static com.micro_tech.pv_emulator.Data.VENDOR_ID;
import static com.micro_tech.pv_emulator.Data.idTag;
import static com.micro_tech.pv_emulator.Data.manager;
import static com.micro_tech.pv_emulator.Data.stmUsb;
import static com.micro_tech.pv_emulator.Data.usbInterface;
import static com.micro_tech.pv_emulator.MainActivity.usbTag;

public class ConfigActivity extends AppCompatActivity implements GestureDetector.OnGestureListener{


    public static final int LENGTH=37;
    public static double[] v=new double[LENGTH];

    private final BroadcastReceiver UsbDetachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            UsbDevice device;

            if (intent.getAction() == ACTION_USB_DETACHED) {

                Toast.makeText(ConfigActivity.this, "USB DETTACHED", Toast.LENGTH_SHORT).show();
                device =  intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == VENDOR_ID) {
                    usbTag=false;
                }

            }else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){

                Toast.makeText(ConfigActivity.this, "USB ATTACHED", Toast.LENGTH_SHORT).show();
                device =  intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == VENDOR_ID) {
                    usbTag=false;
                }
            }
        }

    };


    private TextView text,temp_text,rad_text;
    private SeekBar tBar,rBar;
    private Typeface tp;
    //swip variable
    private byte i=0;

    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 50;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private GraphView graph;
    private LineGraphSeries<DataPoint> serie1, line;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        loadPreferences();

        graph.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

        tBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                temp_text.setText(progressChangedValue+" º");
                T[i]=(byte)progressChangedValue;
                graphsInit(J[i], T[i]);
                idTag=true;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                temp_text.setText(progressChangedValue+" º");
                T[i]=(byte)progressChangedValue;
                graphsInit(J[i],T[i]);
                //send j , t via usb
              if(usbTag) usbTask((byte)0);
                //config changed enable id
                Data.idTag=true;
            }
        });

        rBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                J[i]= (short) progressChangedValue;
                graphsInit(J[i],T[i]);
                rad_text.setText(progressChangedValue+" w/m²");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                rad_text.setText(progressChangedValue+" w/m²");
                J[i]= (short) progressChangedValue;
                graphsInit(J[i],T[i]);
                if(usbTag)usbTask((byte)0);
                //config changed enable id
                Data.idTag=true;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try{
            if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction().toString())){

                Toast.makeText(ConfigActivity.this, "USB ATTACHED", Toast.LENGTH_SHORT).show();
                //guiUpdate=true;
                usbTag=true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();

        if(id==R.id.update){

              Toast.makeText(ConfigActivity.this,"Configuration applied ! ",Toast.LENGTH_SHORT).show();
              saveToHistory();
              finish();

        }else if(id==R.id.cancel_id_menu){

            finish();
            //loadPreferences();
            //LoadHistory();
        }


        return super.onOptionsItemSelected(item);
    }
    boolean checkUsb() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while ((deviceIterator.hasNext())) {

            UsbDevice device = deviceIterator.next();
            if(device.getVendorId()==VENDOR_ID){

                usbInterface = MyUsb.findAdbInterface(device);
                if (usbInterface != null){
                    stmUsb=device;
                    usbTag=true;
                   // Toast.makeText(ConfigActivity.this,"STM found",Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        boolean result = false;
        try {
            float diffY = motionEvent1.getY() - motionEvent.getY();
            float diffX = motionEvent1.getX() - motionEvent.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {

                        i++;
                        if(i==4)i=0;
                        checkSwip();
                    } else {

                        i--;
                        if(i<0)i=3;
                        checkSwip();
                    }
                    result = true;
                }
            }} catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }
    void checkSwip(){

        rad_text.setText(J[i]+" w/m²");
        temp_text.setText(T[i]+" º");
        rBar.setProgress(J[i]);
        tBar.setProgress(T[i]);
        graphsInit(J[i],T[i]);
        savePreferences();

        if(i==0)text.setText("Panel 1");
        else if(i==1) text.setText("Panel 2");
        else if(i==2) text.setText("Panel 3");
        else text.setText("Panel 4");
    }

    public void init(){

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ACTION_USB_DETACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(UsbDetachReceiver,intentFilter);
        usbTag=true;

        setContentView(R.layout.activity_config);
        graph=(GraphView)findViewById(R.id.graph2);
        text=(TextView)findViewById(R.id.text_id2);
        temp_text=(TextView)findViewById(R.id.temp_text_id2);
        rad_text=(TextView)findViewById(R.id.rad_text_id2);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gestureDetector=new GestureDetector(this,this);

        tBar=(SeekBar)findViewById(R.id.bar1);
        rBar=(SeekBar)findViewById(R.id.bar2);
        tp= Typeface.createFromAsset(getAssets(),"timeburnerbold.ttf");
        text.setTypeface(tp);
        temp_text.setTypeface(tp);
        rad_text.setTypeface(tp);

        for(int i=0;i<4;i++) {
            rBar.setProgress(J[i]);
            tBar.setProgress(T[i]);
        }


        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    if(value!=0)return super.formatLabel(value, isValueX) + " V";
                    else return "0";
                } else {
                    // show currency for y values
                    if(value!=0)return super.formatLabel(value, isValueX) + " W";
                    else return "";
                }
            }
        });

        graph.getSecondScale().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    if(value!=0)return super.formatLabel(value, isValueX) + " V";
                    else return "0";
                } else {
                    // show currency for y values
                    if(value!=0)return super.formatLabel(value, isValueX) + " A";
                    else return "";
                }
            }
        });
    }
    void graphsInit(double j,double t){

        graph.removeAllSeries();
        graph.getSecondScale().removeAllSeries();

        for(int i=0;i<LENGTH;i++)v[i]=i;
        DataPoint[] dataPoint=new DataPoint[LENGTH];

        int maxSec=3,maxFirst=80;
        for(int i=0;i<LENGTH;i++){
            dataPoint[i]=new DataPoint(v[i],v[i]*CurrentFunction(v[i],j,t));
            if(v[i]*CurrentFunction(v[i],j,t)>maxFirst)maxFirst=(int)(v[i]*CurrentFunction(v[i],j,t)+1);
        }


        line=new LineGraphSeries<>(dataPoint);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(maxFirst);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.addSeries(line);

        for(int i=0;i<LENGTH;i++){
            dataPoint[i]=new DataPoint(v[i],CurrentFunction(v[i],j,t));
            if(CurrentFunction(v[i],j,t)>maxSec)maxSec=(int)(CurrentFunction(v[i],j,t)+1);
        }

        serie1=new LineGraphSeries<>(dataPoint);
        serie1.setColor(Color.parseColor("#EB9114"));
        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(maxSec);
        graph.getSecondScale().addSeries(serie1);

        line.setTitle("P,V Curve");
        serie1.setTitle("I,V Curve");
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

    }

    @Override
    protected void onStop() {
        super.onStop();
        savePreferences();
        unregisterReceiver(UsbDetachReceiver);


    }



    public void loadPreferences(){

        SharedPreferences load = getSharedPreferences("save",0);
        for(int i=0;i<4;i++){
            T[i] = (byte) load.getFloat("temp"+i, 25);
            J[i] = (int) load.getFloat("rad"+i, 2000);
        }
        rad_text.setText(J[i]+" w/m²");
        temp_text.setText((int)T[i]+" º");
        tBar.setProgress((int)T[i]);
        rBar.setProgress(J[i]);
        graphsInit(J[i],T[i]);

    }

    void savePreferences(){

        @SuppressLint("WrongConstant") SharedPreferences save = getSharedPreferences("save",400);
        SharedPreferences.Editor editor = save.edit();

        for(int i=0;i<4;i++){
            editor.putFloat("temp"+i,(float)T[i]);
            editor.putFloat("rad"+i,(float)J[i]);
        }

        editor.commit();

    }

    void usbTask(byte code){

        MainActivity.code=code;
        try{
            if(checkUsb()) MyUsb.usbCom(stmUsb,ConfigActivity.this);
           // else Toast.makeText(ConfigActivity.this,"STM not found",Toast.LENGTH_SHORT).show();


        }catch (Exception e){
            Toast.makeText(ConfigActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    void saveToHistory() {

        FileOutputStream fos=null;
        String save="";

        for(int i=0;i<4;i++){
            save +=T[i];
            save+="\r\n";
            save+=J[i];
            save+="\r\n";
        }

        try{

            fos=openFileOutput(FILE_NAME,MODE_PRIVATE);
            fos.write(save.getBytes());

        }catch(FileNotFoundException fe){
            fe.printStackTrace();

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if(fos!=null){

                try{
                    fos.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
