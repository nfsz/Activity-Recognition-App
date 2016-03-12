package com.activityrecognitionapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class BoundedService extends Service {
    public BoundedService() {
    }

    private MyBinder mybinder_ = new MyBinder();

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
}
