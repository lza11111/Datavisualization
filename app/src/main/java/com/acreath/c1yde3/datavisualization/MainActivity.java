package com.acreath.c1yde3.datavisualization;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.mylhyl.acp.Acp;
import com.mylhyl.acp.AcpListener;
import com.mylhyl.acp.AcpOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;


public class MainActivity extends AppCompatActivity implements Button.OnClickListener{
    private static String TAG = "Sensor";
    private SensorManager sm;
    private Sensor ac;
//    private Sensor g;
    private TestSensorListener acListener;
//    private TestSensorListener gListener;

    private Button btnStart;
    private Button btnStop;
    private Chronometer chronometer;
    private static String[] TITLE = {"TIME","X","Y","Z"};
    private File file;
    private String fileName;
    private List<Points> points = new ArrayList<>();
    private ArrayList<ArrayList<String>> list = new ArrayList<>();
    private int step = 0;
    private TextView tv;
    private float x;
    private float y;
    private float z;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //权限
        Acp.getInstance(this).request(new AcpOptions.Builder()
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                        .build(),
                new AcpListener() {
                    @Override
                    public void onGranted() {
                    }

                    @Override
                    public void onDenied(List<String> permissions) {
                        Toast.makeText(MainActivity.this,permissions.toString() + "权限拒绝",Toast.LENGTH_LONG).show();
                    }
                });

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(this);

        chronometer = (Chronometer) findViewById(R.id.clock);
        chronometer.setBase(SystemClock.elapsedRealtime());

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        ac = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        g = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        acListener = new TestSensorListener();
//        gListener = new TestSensorListener();
        sm.unregisterListener(acListener);
//        sm.unregisterListener(gListener);

       tv = (TextView) findViewById(R.id.textView);
    }

    /**
     * 导出excel
     */
    public void exportExcel() {
        fileName = getSDPath() + "/表/表.xls";
        file = new File(getSDPath() + "/表");
        makeDir(file);
//        if (new File(fileName).exists()){
//            fileName =
//        }
        ExcelUtils.initExcel(file.toString() + "/表.xls", TITLE);

        ExcelUtils.writeObjListToExcel(getRecordData(), fileName, this);
    }

    /**
     * 得到存储卡路径
     * @return String dir
     */
    private  String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        String dir = sdDir.toString();
        return dir;
    }

    /**
     * 创建文件夹
     * @param dir
     */
    public  void makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        dir.mkdir();
    }

    /**
     * 整理数据
     * @return ArrayList list
     */
    private  ArrayList<ArrayList<String>> getRecordData() {
        for (int i = 0; i <points.size(); i++) {
            Points point = points.get(i);
            ArrayList<String> beanList = new ArrayList<String>();
            beanList.add(point.getTime());
            beanList.add(point.getX());
            beanList.add(point.getY());
            beanList.add(point.getZ());
            list.add(beanList);
        }
        return list;
    }
    /**
     * 异步线程
     */
    AsyncTask<Void,Integer,Void> asyncTask = new AsyncTask<Void, Integer, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final int valueLength=40;
            final int loopTimes=14;
            final int offset=7;
            float[] Acc=new float[valueLength];
            int nowLength = 0;
            //int step = 0;
            float aAvg = 0;
            float bAvg = 0;
            float aStd = 0;
            float bStd = 0;
            while(true){
                while(nowLength < valueLength){
                    float x = getX();    //获取数据
                    float y = getY();
                    float z = getZ();
                    Acc[ nowLength++ ]=(float) Math.sqrt(x * x + y * y + z * z);
                }
                float maxcc = -1;
                float Std = -1;
                int maxLength  = -1;
                for(int i = 1; i <= loopTimes; i++){
                    float[] aArray=new float[i+offset];
                    float[] bArray=new float[i+offset];
                    for(int j = 0; j < i + offset; j++){
                        aArray[j] = Acc[j];
                        bArray[j] = Acc[j + loopTimes];
                    }
                    aAvg = calcAvg(aArray);
                    bAvg = calcAvg(bArray);
                    aStd = calcStd(aArray);
                    bStd = calcStd(bArray);
                    float cc = 0;
                    for(int j = 0; j < i + offset; j++){
                        cc += Math.abs((aArray[j] - aAvg)*(bArray[j] - bAvg));
                        //Log.d("cc",":"+cc);
                    }
                    Log.d("cc",":"+cc);
                    cc /= (i + offset) * aStd * bStd;
                    if(cc > maxcc){
                        maxcc = cc;
                        Std = bStd;
                        maxLength = i + offset;
                    }
                }
                if(Std > 0.5 && maxcc > 0.7){
                    step++;    //记一步
                    publishProgress(step);
                }
                for(int j = 0;j < valueLength - maxLength ; j++){
                    Log.i("线程"," "+(j+maxLength)+":"+x+":"+y+":"+z+":"+step+" max:"+maxLength+" aavg:"+aAvg+" bAbg:"+bAvg+" astd:"+aStd+" bstd:"+bStd);
                    Acc[j]=Acc[j + maxLength];
                }
                nowLength = valueLength - maxLength;
            }



            //return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            tv.setText(values[0]);
        }
    };
    private float getX(){
        return x;
    }
    private float getY(){
        return y;
    }
    private float getZ(){
        return z;
    }

    /**
     * 处理数据
     * @param
     */

    private void start(){


        //Log.d("sixunhuan","weijieshu");
    }

    private float calcAvg(float[] array){
        int len = array.length;
        float avg = 0;
        float sum = 0;
        for(int i = 0; i < len; i++){
            avg += array[i];
        }
        avg /= len;
        return avg;
    }
    private float calcStd(float[] array){
        int len = array.length;
        float sum = 0;
        float avg = calcAvg(array);
        for(int i = 0; i < len; i++){
            sum += (array[i] - avg) * (array[i] - avg);
        }
        return (float) Math.sqrt(sum / len);
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                sm.registerListener(acListener, ac, 50000);
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
                Toast.makeText(this,"开始记录",Toast.LENGTH_SHORT).show();
                asyncTask.execute();
                break;


            case R.id.btn_stop:
                Toast.makeText(this,"结束记录",Toast.LENGTH_SHORT).show();
                //chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.stop();
                sm.unregisterListener(acListener);
                exportExcel();
                points.clear();
                list.clear();
                break;
        }
    }




    private class TestSensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // 读取加速度传感器数值，values数组0,1,2分别对应x,y,z轴的加速度
            Log.i(TAG, "onSensorChanged: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
//            pointX.add(event.values[0]);
//            pointY.add(event.values[1]);
//            pointZ.add(event.values[2]);
            //start(event.values[0],event.values[1],event.values[2]);
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            Calendar c = Calendar.getInstance();
            points.add(new Points(c.get(Calendar.YEAR)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.DATE)+"  "+c.get(Calendar.HOUR_OF_DAY)+ ":"+ c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND)+":"+c.get(Calendar.MILLISECOND),String.valueOf(event.values[0]),String.valueOf(event.values[1]),String.valueOf(event.values[2])));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(TAG, "onAccuracyChanged");
        }

    }



    @Override
    protected void onPause() {
        super.onPause();

        sm.unregisterListener(acListener);
    }
}
