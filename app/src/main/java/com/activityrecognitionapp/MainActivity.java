package com.activityrecognitionapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceCallbacks {


    private Button start, stop, log, walk, sit, lay;
    private TextView logText, predictText, locationChanged;
    private BoundedService.MyBinder binder_;
    private BoundedService myService;
    private Boolean connected = false;
    private Context context;
    private String activity_;
    private String currentActivity_;
    private boolean isActivitySet_;
    private FileOutputStream out;
    private boolean isChanged;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        start = (Button) findViewById(R.id.buttonStart);
        start.setOnClickListener(this);
        stop = (Button) findViewById(R.id.buttonStop);
        stop.setOnClickListener(this);
        log = (Button) findViewById(R.id.buttonLog);
        log.setOnClickListener(this);
        walk = (Button) findViewById(R.id.buttonWalk);
        walk.setOnClickListener(this);
        sit = (Button) findViewById(R.id.buttonSit);
        sit.setOnClickListener(this);
        lay = (Button) findViewById(R.id.buttonLay);
        lay.setOnClickListener(this);

        walk.setVisibility(View.GONE);
        sit.setVisibility(View.GONE);
        lay.setVisibility(View.GONE);

        logText = (TextView) findViewById(R.id.logText);
        predictText = (TextView) findViewById(R.id.predictText);
        locationChanged = (TextView) findViewById(R.id.locationText);
        context = this;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
              case R.id.buttonStart:
                //Log.d("BoundedService", "pressed BoundedS");
                Intent myIntent = new Intent(this, BoundedService.class);
                bindService(myIntent, mConnection, BIND_AUTO_CREATE);
                  walk.setVisibility(View.VISIBLE);
                  sit.setVisibility(View.VISIBLE);
                  lay.setVisibility(View.VISIBLE);
                  isActivitySet_ = false;
                break;
            case R.id.buttonStop:
                walk.setVisibility(View.GONE);
                sit.setVisibility(View.GONE);
                lay.setVisibility(View.GONE);
                isActivitySet_ = false;

                if (connected == true) {
                    myService.setCurrentActivity(0);
                    myService.parseData.interrupt();
                    myService.pauseSensors();
                }
                break;

            case R.id.buttonLog:
                //Log.d("BoundedService", "pressed Log");
                if (connected == true) {
                    String accMsg = myService.acclData();
                    String gyroMsg = myService.gyroData();
                    String locMsg = myService.locData();
                    Log.d("Activity: ", currentActivity_);
                    Log.d("Acceleration data: ", accMsg);
                    Log.d("Gyroscope data: ", gyroMsg);
                    Log.d("Location data: ", locMsg);
                    Log.d("-- ", "--");

                    if(isChanged) {
                        isChanged = false;
                        locationChanged.setText("Location changed");
                    }
                    else{
                        locationChanged.setText("Has not changed");
                    }
                    logText.setText(accMsg + " " + gyroMsg + " " + locMsg + " ");
                    if(isActivitySet_) {
                        isActivitySet_ = false;
                        predictText.setText("Predicted activity: " + activity_ + "\n"
                                            + "Actual activity: " + currentActivity_ + "\n"
                                            + "Accuracy: " + Float.toString(myService.succRate()));
                    }
                    else {
                        predictText.setText("Waiting for prediction...");
                    }
                    String msg = myService.acclData();
                    writeToFile(msg + "\n");

                }else{
                    logText.setText("Data collection has not started. Hit start to collect data");
                    Log.d("BoundedService", "Hit start to collect data");
                }
                break;
            case R.id.buttonWalk:
                myService.setCurrentActivity(1);
                currentActivity_ = "Walking";
                break;
            case R.id.buttonSit:
                myService.setCurrentActivity(2);
                currentActivity_ = "Sitting";
                break;
            case R.id.buttonLay:
                myService.setCurrentActivity(3);
                currentActivity_ = "Laying";
                break;

        }
    }

    public void writeToFile(String txt) {

        if (isExternalStorageWritable()) {

            try{
                out.write(txt.getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }else{
            Log.d("Error", "External storage not mounted");
        }

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder_ = (BoundedService.MyBinder) service;
            myService = binder_.getService();
            connected = true;
            myService.startSensors();
            myService.setCallbacks(MainActivity.this); // setting this activity as the callback
            //if (!myService.parseData.isAlive()) {
            myService.parseData.start();
            //}
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };
    @Override
    protected void onResume(){
        super.onResume();
        if (connected == true) {
            myService.startSensors();
        }

        if (isExternalStorageWritable()) {

//            Log.d("paths", ContextCompat.getExternalFilesDirs(context, null)[0].toString());
//            Log.d("paths", ContextCompat.getExternalFilesDirs(context, null)[1].toString());

            File file = new File(ContextCompat.getExternalFilesDirs(context, null)[1], "activities.txt");
            //File file = new File("/storage/extSdCard/Android/data/com.activityrecognitionapp/files", "activities.txt");
            //File file = new File("context.getExternalFilesDir(null);", "activities.txt");
            //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "activities.txt");

            try {
                out = new FileOutputStream(file);
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Log.d("Error", "External storage not mounted");
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        if (connected == true) {
            myService.parseData.interrupt();
            myService.pauseSensors();
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void predictActivity(String activity) {
        activity_ = activity;
        isActivitySet_ = true;
        if(activity.equals(currentActivity_)) {
            myService.incrSuccess();
        }
        //Log.d("predicted activity: ", "activity");
    }

    @Override
    public void locationChanged() {
        isChanged = true;
    }
}
