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
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button getActivities;
    private TextView activitiesList;
    private BoundedService.MyBinder binder_;
    private BoundedService myService;
    private Boolean connected = false;
    private Context context;
    private FileOutputStream out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getActivities = (Button) findViewById(R.id.buttonActivities);
        getActivities.setOnClickListener(this);

        activitiesList = (TextView) findViewById(R.id.textActivities);

        context = this;

        Intent myIntent = new Intent(this, BoundedService.class);
        bindService(myIntent, mConnection, BIND_AUTO_CREATE);
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

            case R.id.buttonActivities:
                String activities = "";
                List list = myService.getActivities();

                for (int i = 0; i < list.size(); i++){
                    activities += list.get(i) + "\n";
                }
                activitiesList.setText(activities);
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


    public Thread displayActivities = new Thread(new Runnable() {
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);

                    String activity = myService.getLastActivity();

                    writeToFile(activity + "\n");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    });

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder_ = (BoundedService.MyBinder) service;
            myService = binder_.getService();
            connected = true;
            myService.startSensors();
            myService.parseData.start();
            displayActivities.start();
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

}
