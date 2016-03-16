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
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private final float EPSILON = 0.001f;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mybinder_;
    }

    public class MyBinder extends Binder {
        BoundedService getService(){
            return BoundedService.this;
        }
    }

    public String msg(){
        return "Hello World";
    }

    public String acclData(){
        return "x: " + acclx + "\n" + "y: " + accly + "\n" + "z: " + acclz + "\n";
    }

    public double getAcclx() {
        return acclx;
    }

    public double getAccly() {
        return accly;
    }

    public double getAcclz() {
        return acclz;
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
        private float timestamp_;
        private float[] orientation_ = new float[9];

        public GyroWork(SensorEvent event) {
            event_ = event;
            orientation_ = calculateInitialOrientation();

        }

        private float[] calculateInitialOrientation()
        {
             return SensorManager.getRotationMatrix(
                    initialRotationMatrix, null, acceleration, magnetic);

        }

        @Override
        public void run() {
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp_ != 0) {
                final float dT = (event_.timestamp - timestamp_) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event_.values[0];
                float axisY = event_.values[1];
                float axisZ = event_.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }

            timestamp_ = event_.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        switch(mySensor.getType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                //double acclx = event.values[0];
                //double accly = event.values[1];
                //double acclz = event.values[2];

                //acclxe.setText(new Double(acclx).toString());
                //acclye.setText(new Double(accly).toString());
                //acclze.setText(new Double(acclz).toString());
                myHandler.post(new AcclWork(event));
                break;
            case(Sensor.TYPE_GYROSCOPE_UNCALIBRATED):
                myHandler.post(new GyroWork(event));
                break;
        }
    }

    public void startSensors(){

        sensormanager_ = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer_ = sensormanager_.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope_ = sensormanager_.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
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
