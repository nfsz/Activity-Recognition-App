package com.activityrecognitionapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener  {

    Button start, stop, log, walk, sit, lay;
    BoundedService.MyBinder binder_;
    BoundedService myService;
    Boolean connected = false;

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
                break;
            case R.id.buttonStop:
                walk.setVisibility(View.GONE);
                sit.setVisibility(View.GONE);
                lay.setVisibility(View.GONE);
                break;

            case R.id.buttonLog:
                //Log.d("BoundedService", "pressed Log");
                if (connected == true) {
                    String msg_= myService.acclData();
                    Log.d("BoundedService", msg_);
                }else{
                    Log.d("BoundedService", "Hit start to collect data");
                }
                break;
            case R.id.buttonWalk:
                break;
            case R.id.buttonSit:
                break;
            case R.id.buttonLay:
                break;

        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder_ = (BoundedService.MyBinder) service;
            myService = binder_.getService();
            connected = true;
            myService.startSensors();
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
    }
    @Override
    protected void onPause(){
        super.onPause();
        if (connected == true) {
            myService.pauseSensors();
        }
    }
}
