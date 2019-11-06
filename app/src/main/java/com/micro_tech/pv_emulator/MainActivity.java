package com.micro_tech.pv_emulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import static com.micro_tech.pv_emulator.Data.ACTION_USB_ATTACHED;
import static com.micro_tech.pv_emulator.Data.ACTION_USB_DETACHED;
import static com.micro_tech.pv_emulator.Data.FILE_NAME;
import static com.micro_tech.pv_emulator.Data.HELP;
import static com.micro_tech.pv_emulator.Data.J;
import static com.micro_tech.pv_emulator.Data.T;
import static com.micro_tech.pv_emulator.Data.VENDOR_ID;
import static com.micro_tech.pv_emulator.Data.WIDTH;
import static com.micro_tech.pv_emulator.Data.manager;
import static com.micro_tech.pv_emulator.Data.stmUsb;
import static com.micro_tech.pv_emulator.Data.timeStamp;
import static com.micro_tech.pv_emulator.Data.tp;
import static com.micro_tech.pv_emulator.Data.usbInterface;
import static com.micro_tech.pv_emulator.Data.vFunction;

public class MainActivity extends AppCompatActivity {

    public static double ISC=2,VOC=36;

    public static TextView textView;
    public static byte code=0;

    private int count = 0;
    long startTime = System.currentTimeMillis();
    public static boolean homeTag = true;

    public static byte[] bytes;
    public static double vp=20;

    private GraphView graph;
    private LineGraphSeries<DataPoint> serie1,line;
    private PointsGraphSeries<DataPoint> point1,point2;
    private ImageButton a,b,c;

    public final static int SIZE=20000;
    public static boolean usbTag=true;

    public static android.os.Handler handler;
    public static Runnable runnable;

    final DataPoint[] IVdata=new DataPoint[SIZE];
    final DataPoint[] PVdata=new DataPoint[SIZE];

    int vcomp=0,index=0,inc=0;

    private final BroadcastReceiver UsbDetachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            UsbDevice device;
            if (intent.getAction()== ACTION_USB_DETACHED) {

                Toast.makeText(MainActivity.this, "USB DETTACHED", Toast.LENGTH_SHORT).show();
                device =  intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == VENDOR_ID) {
                    usbTag=false;
                }
            }else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){

                Toast.makeText(MainActivity.this, "USB ATTACHED", Toast.LENGTH_SHORT).show();
                device =  intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getVendorId() == VENDOR_ID) {
                    usbTag=false;
                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        point1.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "A( "+(float)dataPoint.getX()+","+(float)dataPoint.getY()+" )", Toast.LENGTH_SHORT).show();
            }
        });

        point2.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "B( "+(float)dataPoint.getX()+","+(float)dataPoint.getY()+" )", Toast.LENGTH_SHORT).show();
            }
        });

        // identification button
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {

                       if(usbTag)
                           usbTask((byte) 1);
                        if(MyUsb.first[0]!=0){
                            graphsInit();
                            //enable update
                            c.setAlpha(255);
                            c.setEnabled(true);
                            //disable id
                            a.setAlpha(100);
                            a.setEnabled(false);
                            Data.idTag = false;

                       if(usbTag)
                           usbTask((byte) 3);
                       getActivePoint(vp);
                       CriticalPointUpdate(IVdata[index].getX(), IVdata[index].getY());

                     }else  Toast.makeText(MainActivity.this, "No Voltage Applied ! ", Toast.LENGTH_SHORT).show();

                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "error : "+e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        //config activity
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                 Data.start=true;
                 c.setImageResource(R.drawable.start);
                 startActivity(new Intent(MainActivity.this,ConfigActivity.class));
            }
        });

        //update button
        c.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {

                    if (usbTag) {

                        usbTask((byte) 3);
                        getActivePoint(vp);
                       // Log.d("Nazim", "vcomp =" + vcomp + " x= " + IVdata[index].getX() + " y= " + IVdata[index].getY());

                        CriticalPointUpdate(IVdata[index].getX(), IVdata[index].getY());
                        handler.post(runnable);

                        Data.start = !Data.start;
                        if (!Data.start) c.setImageResource(R.drawable.stop);
                        else c.setImageResource(R.drawable.start);
                    }else Toast.makeText(MainActivity.this, "usb not connected ", Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        Toast.makeText(MainActivity.this, "err " + e.toString(), Toast.LENGTH_SHORT).show();
                    }
            }
        });

        handler=new android.os.Handler(Looper.getMainLooper());
        runnable=new Runnable() {
            @Override
            public void run() {

                try{
                    if(!Data.start && usbTag ){

                        usbTask((byte) 3);
                        getActivePoint(vp);

                        CriticalPointUpdate(IVdata[index].getX(),IVdata[index].getY());
                    }
                }catch (Exception e){
                    Toast.makeText(MainActivity.this,"error : "+e.toString(),Toast.LENGTH_SHORT).show();
                }
                handler.postDelayed(runnable,timeStamp);
            }
        };

        handler.post(runnable);

        File file=new File(FILE_NAME);
        if(file.exists())LoadHistory();

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secretClick();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    void usbTask(byte code){

        MainActivity.code=code;
        try{
            if(checkUsb()) {

                MyUsb.usbCom(stmUsb,MainActivity.this);
            }
            else Toast.makeText(MainActivity.this,"STM not found",Toast.LENGTH_SHORT).show();

        }catch (Exception e){
            Toast.makeText(MainActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
        }
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
                    //Toast.makeText(MainActivity.this,"STM found",Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        usbTag=false;
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();

        if(id==R.id.config)LoadHistory();
        else if(id==R.id.identif)helpDialog();

        return super.onOptionsItemSelected(item);
    }

    void getActivePoint(double v){

        double vs=v;
        vs*=3.3;
        vs/=4095;
        vs*=126.4;
        inc=0;
        //Toast.makeText(MainActivity.this, "vp = "+vs,Toast.LENGTH_SHORT).show();
        while(inc<SIZE){
            vcomp=(int)vs;
            if(vcomp==(int)IVdata[inc].getX()){
                index=inc;
                break;

            }
            inc++;
        }

    }
    void CriticalPointUpdate(double v,double i){

        graph.removeSeries(point1);
        graph.getSecondScale().removeSeries(point2);
        //if(v==0)i=ISC;
        DataPoint[] dt=new DataPoint[1];
        dt[0]=new DataPoint(v,v*i);

        point1=new PointsGraphSeries<>(dt);
        point1.setShape(PointsGraphSeries.Shape.POINT);
        point1.setSize(5);
        point1.setColor(Color.RED);
        graph.addSeries(point1);

        dt[0]=new DataPoint(v,i);
        point2=new PointsGraphSeries<>(dt);
        point2.setShape(PointsGraphSeries.Shape.POINT);
        point2.setSize(5);
        point2.setColor(Color.RED);
        graph.getSecondScale().addSeries(point2);

    }

    void graphsInit(){

        //remove previous graph draws
        graph.removeAllSeries();
        graph.getSecondScale().removeAllSeries();

        new Thread(new Runnable() {
            @Override
            public void run() {

                DataPoint[] data=new DataPoint[SIZE];
                double Ic=2.01,v1,v2,v3,v4;
                double DI=0.0001;
                long start=System.currentTimeMillis();

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


                for(int i=0;i<SIZE;i++){
                    IVdata[i]=data[i];
                    PVdata[i]=new DataPoint(IVdata[i].getX(),IVdata[i].getX()*IVdata[i].getY());
                }
               // long diff=System.currentTimeMillis()-start;
               // Log.d("Nazim","diff ="+diff);
               // for(int i=0;i<4;i++)Log.d("Nazim","j ="+J[i]+" t1="+T[i]);

            }
        }).start();

        try {

            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        line=new LineGraphSeries<>(PVdata);
        graph.getViewport().setMinY(0);

        graph.getViewport().setMaxY(WIDTH*2);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(WIDTH+4);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

        graph.addSeries(line);

        serie1=new LineGraphSeries<>(IVdata);
        serie1.setColor(Color.BLACK);
        serie1.setDrawAsPath(true);
        line.setDrawAsPath(true);
        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(2.4);
        graph.getSecondScale().addSeries(serie1);
        graph.getSecondScale().addSeries(point2);

        line.setTitle("P,V Curve");
        serie1.setTitle("I,V Curve");

        //graph.getViewport().setDrawBorder(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

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

    private void init(){

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ACTION_USB_DETACHED);
        intentFilter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(UsbDetachReceiver,intentFilter);


        //init widgets
        textView=(TextView)findViewById(R.id.text_id);
        serie1 = new LineGraphSeries<>();
        serie1.setColor(Color.parseColor("#EB9114"));
        line = new LineGraphSeries<>();
        point1=new PointsGraphSeries<>();
        point2=new PointsGraphSeries<>();
        graph=(GraphView)findViewById(R.id.graph);

        a=(ImageButton)findViewById(R.id.a);
        b=(ImageButton)findViewById(R.id.b);
        c=(ImageButton)findViewById(R.id.c);
        a.setBackgroundColor(Color.TRANSPARENT);
        b.setBackgroundColor(Color.TRANSPARENT);
        c.setBackgroundColor(Color.TRANSPARENT);

        setButtons();

        tp= Typeface.createFromAsset(getAssets(),"timeburnerbold.ttf");
        textView.setTypeface(tp);

       onCreateGraph();
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

        usbTag=true;

    }

    @Override
    protected void onResume() {
        super.onResume();
      // graphsInit();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable);
        super.onPause();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction().toString())){

            Toast.makeText(MainActivity.this, "USB ATTACHED", Toast.LENGTH_SHORT).show();
            usbTag=true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(runnable);
        super.onStop();

    }

    @Override
    protected void onStart() {

        super.onStart();
        setButtons();

        try{
            LoadHistory();
        }catch (Exception e){
            myToast(e.toString(),MainActivity.this);
        }


    }

    public static void myToast(String msg, Context activity){
        Toast.makeText(activity,msg,Toast.LENGTH_LONG).show();
    }
    void helpDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.help_dialog,null);
        builder.setView(dialogView);


        builder.setCancelable(true);
        TextView textView=(TextView)dialogView.findViewById(R.id.help_text);
        textView.setText(HELP);

        LinearLayout layout=(LinearLayout)dialogView.findViewById(R.id.help_layout);
        layout.getBackground().setAlpha(255);

        AlertDialog alertDialog=builder.create();
        alertDialog.show();

    }

    void LoadHistory(){

        FileInputStream fileInputStream=null;
        StringBuilder sb=new StringBuilder();

        try {
            fileInputStream=openFileInput(FILE_NAME);
            InputStreamReader isr=new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader=new BufferedReader(isr);

            String text="";

            int f=0;

            while(f<4){

                text=bufferedReader.readLine();
                T[f]=Byte.parseByte(text);
                text=bufferedReader.readLine();
                J[f]=Integer.parseInt(text);
                sb.append(T[f]+"\n");
                sb.append(J[f]+"\n");
                f++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"file not found  ",Toast.LENGTH_SHORT).show();

        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"io exception  ",Toast.LENGTH_SHORT).show();

        }finally {
            if(fileInputStream!=null){

                try{
                    fileInputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

    }

    void setButtons(){
        if(Data.idTag){
            //enable id
            a.setEnabled(true);
            a.setAlpha(255);
            //disable update
            c.setAlpha(100);
            c.setEnabled(false);

        }
    }
    void onCreateGraph(){

        graph.removeAllSeries();
        graph.getSecondScale().removeAllSeries();

        DataPoint[] dataPoint=new DataPoint[SIZE];
        for(int i=0;i<SIZE;i++)dataPoint[i]=new DataPoint(0,0);

        line=new LineGraphSeries<>(dataPoint);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(80);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.addSeries(line);


        serie1=new LineGraphSeries<>(dataPoint);
        serie1.setColor(Color.BLACK);
        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(2.5);
        graph.getSecondScale().addSeries(serie1);

        line.setTitle("P,V Curve");
        serie1.setTitle("I,V Curve");
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

    }
    public void secretClick(){

        long difference=System.currentTimeMillis()-startTime;
        if(difference>1000)count=0;
        else count++;
        startTime=System.currentTimeMillis();
        if(count>=8) {

            homeTag=!homeTag;
            if(!homeTag){

                    Toast.makeText(MainActivity.this,"Flash Mode",Toast.LENGTH_LONG).show();
                    shareIt();

            }

            count=0;
        }
    }

    public void shareIt(){
        try {

            Intent i = getPackageManager().getLaunchIntentForPackage("com.lenovo.anyshare.gps");
            startActivity(i);

        }
        catch (Exception e) {

            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

}
