package com.activityrecognitionapp;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class BoundedService extends Service implements SensorEventListener, LocationListener {
    private final long THREAD_SLEEP_TIME = 5000;
    private final int LOCATION_CHANGES = 1; //threshold to predict walking or running


    private MyBinder mybinder_ = new MyBinder();
    private LocationManager locationManager_;
    private SensorManager sensormanager_;
    private Sensor accelerometer_, gyroscope_;
    private final int DELAY = 100;
    private float NUM_PREDICTIONS;
    private float CORRECT_PREDICTIONS;
    private Handler myHandler = new Handler();
    private int NUM_LOCATION_CHANGES;
    private ServiceCallbacks serviceCallbacks;
    private double acclX;
    private double acclY;
    private double acclZ;
    private double axisX; //gyroscope x-cood speed measured in rads/sec
    private double axisY; //gyroscope y-cood speed measured in rads/sec
    private double axisZ; //gyroscope z-cood speed measured in rads/sec
    private float locAcc; //location accuracy measured in centimeters
    private float locSpeed; //location speed meassured in cm/sec
    private int currentActivity = 0; //0 - no activity, 1 - walk, 2 - sit, 3 - lay
    List<AcclDataPoint> accDataList = Collections.synchronizedList(new ArrayList<AcclDataPoint>());
    List<GyroDataPoint> gyroDataList = Collections.synchronizedList(new ArrayList<GyroDataPoint>());
    List<LocDataPoint> locDataList = Collections.synchronizedList(new ArrayList<LocDataPoint>());
    List<AcclDataPoint> chunkedAccDataList;
    List<GyroDataPoint> chunkedGyroDataList;
    List<LocDataPoint> chunkedLocDataList;


    public BoundedService() {
        CORRECT_PREDICTIONS += 1;
    }

    public void incrSuccess() {
        CORRECT_PREDICTIONS += 1;
    }

    public float succRate() {
        return CORRECT_PREDICTIONS/NUM_PREDICTIONS;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mybinder_;
    }

    public class MyBinder extends Binder {
        BoundedService getService() {
            return BoundedService.this;
        }
    }

    public String msg() {
        return "Hello World";
    }

    public String acclData() {
        return "x: " + acclX + " " + "y: " + acclY + " " + "z: " + acclZ + "\n";
    }

    public String gyroData() {
        return "x: " + axisX + " " + "y: " + axisY + " " + "z: " + axisZ + "\n";
    }

    private double rms() {
        return Math.sqrt((1.0/3)*(acclX*acclX + acclY*acclY + acclZ*acclZ));
    }

    public String locData() {
        return "location (meters): " + locAcc + " " + "location speed (meters/sec): " + locSpeed + "\n";
    }

    public Thread parseData = new Thread(new Runnable() {
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(THREAD_SLEEP_TIME);
                    NUM_PREDICTIONS += 1;
                    NUM_LOCATION_CHANGES = 0;
                    chunkData();
                    runAlgorithm();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    });

    private void chunkData() {
        chunkedAccDataList = accDataList;
        chunkedGyroDataList = gyroDataList;
        chunkedLocDataList = locDataList;
        accDataList = Collections.synchronizedList(new ArrayList<AcclDataPoint>());
        gyroDataList = Collections.synchronizedList(new ArrayList<GyroDataPoint>());
        locDataList = Collections.synchronizedList(new ArrayList<LocDataPoint>());
    }

    private void runAlgorithm() {
        Log.d("Log", "Running Algorithm");

        if(NUM_LOCATION_CHANGES >= LOCATION_CHANGES) {
            if(serviceCallbacks != null) {
                serviceCallbacks.predictActivity("Walking or Running");
            }
        }
        else {
            if(serviceCallbacks != null) {
                double rms = rms();
                if(rms >= 2) {
                    serviceCallbacks.predictActivity("Sitting");
                }
                else {
                    serviceCallbacks.predictActivity("Laying");

                }
            }
        }

/*        synchronized(chunkedAccDataList) {
            Iterator i = chunkedAccDataList.iterator();
            while (i.hasNext()) {
                //foo(i.next());
            }
        }

        synchronized(chunkedGyroDataList) {
            Iterator i = chunkedGyroDataList.iterator();
            while (i.hasNext()) {
                //foo(i.next());
            }
        }

        synchronized(chunkedLocDataList) {
            Iterator i = chunkedLocDataList.iterator();
            while (i.hasNext()) {
                //foo(i.next());
            }
        }*/

    }

    private class AcclWork implements Runnable {

        private SensorEvent event_;

        public AcclWork(SensorEvent event) {
            event_ = event;
        }

        @Override
        public void run() {

            acclX = event_.values[0];
            acclY = event_.values[1];
            acclZ = event_.values[2];

            accDataList.add(new AcclDataPoint(acclX, acclY, acclZ, currentActivity));

/*            acclxe.setText(new Double(acclX).toString());
            acclye.setText(new Double(acclY).toString());
            acclze.setText(new Double(acclZ).toString());*/
        }
    }

    private class GyroWork implements Runnable {

        private SensorEvent event_;

        public GyroWork(SensorEvent event) {
            event_ = event;
        }

        @Override
        public void run() {

            // Axis of the rotation sample, not normalized yet.
            axisX = event_.values[0];
            axisY = event_.values[1];
            axisZ = event_.values[2];

            gyroDataList.add(new GyroDataPoint(axisX, axisY, axisZ, currentActivity));
        }
    }

    private class AcclDataPoint{
        double acclX;
        double acclY;
        double acclZ;
        int activity; //0 - no activity, 1 - walk, 2 - sit, 3 - lay
        Calendar time;

        AcclDataPoint(double x, double y, double z, int activity){
            acclX = x;
            acclY = y;
            acclZ = z;
            this.activity = activity;
            time = Calendar.getInstance();
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss a");
//            String strTime = sdf.format(c.getTime());
        }

        AcclDataPoint(double x, double y, double z){
            this(x, y, z, 0);
        }
    }

    private class GyroDataPoint{
        double axisX; //gyroscope x-cood speed measured in rads/sec
        double axisY; //gyroscope y-cood speed measured in rads/sec
        double axisZ; //gyroscope z-cood speed measured in rads/sec
        int activity; //0 - no activity, 1 - walk, 2 - sit, 3 - lay
        Calendar time;

        GyroDataPoint(double x, double y, double z, int activity){
            axisX = x;
            axisY = y;
            axisZ = z;
            this.activity = activity;
            time = Calendar.getInstance();
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss a");
//            String strTime = sdf.format(c.getTime());
        }

        GyroDataPoint(double x, double y, double z){
            this(x, y, z, 0);
        }
    }

    private class LocDataPoint{
        float locAcc; //location accuracy measured in centimeters
        float locSpeed; //location speed meassured in cm/sec
        int activity; //0 - no activity, 1 - walk, 2 - sit, 3 - lay
        Calendar time;

        LocDataPoint(float acc, float speed, int activity){
            locAcc = acc;
            locSpeed = speed;
            this.activity = activity;
            time = Calendar.getInstance();
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss a");
//            String strTime = sdf.format(c.getTime());
        }

        LocDataPoint(float acc, float speed){
            this(acc, speed, 0);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        switch (mySensor.getType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                //double acclX = event.values[0];
                //double acclY = event.values[1];
                //double acclZ = event.values[2];

                //acclxe.setText(new Double(acclX).toString());
                //acclye.setText(new Double(acclY).toString());
                //acclze.setText(new Double(acclZ).toString());
                myHandler.post(new AcclWork(event));
                break;
            case (Sensor.TYPE_GYROSCOPE_UNCALIBRATED):
                myHandler.post(new GyroWork(event));
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.

        locAcc = 100 * location.getAccuracy();
        locSpeed = 100 * location.getSpeed();
        NUM_LOCATION_CHANGES += 1;
        locDataList.add(new LocDataPoint(locAcc, locSpeed, currentActivity));
    }


    public void startSensors() {

        sensormanager_ = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscope_ = sensormanager_.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        accelerometer_ = sensormanager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensormanager_.registerListener(this, gyroscope_, SensorManager.SENSOR_DELAY_NORMAL, DELAY);
        sensormanager_.registerListener(this, accelerometer_, SensorManager.SENSOR_DELAY_NORMAL, DELAY);

        locationManager_ = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager_.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);

    }

    public void pauseSensors(){
        sensormanager_.unregisterListener(this);
    }

    public void setCurrentActivity(int currentActivity) {
        this.currentActivity = currentActivity;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
