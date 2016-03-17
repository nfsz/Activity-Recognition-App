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

public class BoundedService extends Service implements SensorEventListener, LocationListener {
    public BoundedService() {
    }

    private MyBinder mybinder_ = new MyBinder();
    private LocationManager locationManager_;
    private SensorManager sensormanager_;
    private Sensor accelerometer_, gyroscope_;
    private final int DELAY = 100;
    private Handler myHandler = new Handler();
    private double acclx;
    private double accly;
    private double acclz;
    private double axisX; //gyroscope x-cood speed measured in rads/sec
    private double axisY; //gyroscope y-cood speed measured in rads/sec
    private double axisZ; //gyroscope z-cood speed measured in rads/sec
    private float locAcc; //location accuracy measured in centimeters
    private float locSpeed; //location speed meassured in cm/sec


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
        return "x: " + acclx + "\n" + "y: " + accly + "\n" + "z: " + acclz + "\n";
    }

    public String gyroData() {
        return "x: " + axisX + "\n" + "y: " + axisY + "\n" + "z: " + axisZ + "\n";
    }

    public String locData() {
        return "location (meters): " + locAcc + "\n" + "location speed (meters/sec): " + locSpeed + "\n";
    }

    private class AcclWork implements Runnable {

        private SensorEvent event_;

        public AcclWork(SensorEvent event) {
            event_ = event;
        }

        @Override
        public void run() {

            acclx = event_.values[0];
            accly = event_.values[1];
            acclz = event_.values[2];

/*            acclxe.setText(new Double(acclx).toString());
            acclye.setText(new Double(accly).toString());
            acclze.setText(new Double(acclz).toString());*/

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
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        switch (mySensor.getType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                //double acclx = event.values[0];
                //double accly = event.values[1];
                //double acclz = event.values[2];

                //acclxe.setText(new Double(acclx).toString());
                //acclye.setText(new Double(accly).toString());
                //acclze.setText(new Double(acclz).toString());
                myHandler.post(new AcclWork(event));
                break;
            case (Sensor.TYPE_GYROSCOPE_UNCALIBRATED):
                myHandler.post(new GyroWork(event));
                break;
        }
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

    @Override
    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.

        locAcc = 100 * location.getAccuracy();
        locSpeed = 100 * location.getSpeed();
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
