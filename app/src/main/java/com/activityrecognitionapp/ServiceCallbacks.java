package com.activityrecognitionapp;

/**used to pass messages from the service back to the main activity
 *
 */
public interface ServiceCallbacks {
    public void predictActivity(String activity);

}
