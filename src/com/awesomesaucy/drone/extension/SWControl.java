package com.awesomesaucy.drone.extension;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.parrot.freeflight.activities.ControlDroneActivity;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorTypeValue;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor.SensorAccuracy;
import com.sonyericsson.extras.liveware.extension.util.R;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;

/**
 * The sample sensor control handles the accelerometer sensor on an accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SWControl extends ControlExtension {

    private int mCurrentSensor = 0;

    private List<AccessorySensor> mSensors = new ArrayList<AccessorySensor>();

    private final AccessorySensorEventListener mListener = new AccessorySensorEventListener() {

        @Override
        public void onSensorEvent(AccessorySensorEvent sensorEvent) {
            Log.d(ExtensionDroneService.LOG_TAG, "Listener: OnSensorEvent");
            //updateCurrentDisplay(sensorEvent);
            float[] data = sensorEvent.getSensorValues();
            float x = data[0];
            float y = data[1];
            float z = data[2];
            
            float yaw = y/10;
            float gaz = x/5;
            Log.d("Sensor", "yaw: "+yaw);
            if(yaw < -1) yaw = -1;
            if(yaw > 1) yaw = 1;

            if(gaz < -1) gaz = 1;
            if(gaz > 1) gaz = 1;

            try{
            	ControlDroneActivity.droneControlService.setYaw(yaw);
            	ControlDroneActivity.droneControlService.setGaz(gaz);
            }catch(Exception e){
            	e.printStackTrace();
            }
        }
    };

    SWControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);

        AccessorySensorManager manager = new AccessorySensorManager(context, hostAppPackageName);
        // Add accelerometer, if supported
        if (DeviceInfoHelper.isSensorSupported(context, hostAppPackageName,
                SensorTypeValue.ACCELEROMETER)) {
            mSensors.add(manager.getSensor(SensorTypeValue.ACCELEROMETER));
        }
        // Add magnetic field sensor, if supported
        if (DeviceInfoHelper.isSensorSupported(context, hostAppPackageName,
                SensorTypeValue.MAGNETIC_FIELD)) {
            mSensors.add(manager.getSensor(SensorTypeValue.MAGNETIC_FIELD));
        }
        // Add light sensor, if supported
        if (DeviceInfoHelper.isSensorSupported(context, hostAppPackageName, SensorTypeValue.LIGHT)) {
            mSensors.add(manager.getSensor(SensorTypeValue.LIGHT));
        }
    }

    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        this.showLayout(com.parrot.freeflight.R.layout.sw_layout, null);
        register();
    }

    @Override
    public void onPause() {
        unregister();
    }

    @Override
    public void onDestroy() {
        unregisterAndDestroy();
    }

    public static boolean isWidthSupported(Context context, int width) {
        return true;
    }

    public static boolean isHeightSupported(Context context, int height) {
        return true;
    }

    @Override
    public void onTouch(ControlTouchEvent event) {
        super.onTouch(event);
        if (event.getAction() == Control.Intents.TOUCH_ACTION_RELEASE) {
	        try{
	        	ControlDroneActivity.droneControlService.triggerTakeOff();
	        }catch(Exception e){
	        	Log.e("Control", "exception in onTouch");
	        	e.printStackTrace();
	        }
	    }
    }

    private AccessorySensor getCurrentSensor() {
        return mSensors.get(mCurrentSensor);
    }

    private void register() {
        Log.d(ExtensionDroneService.LOG_TAG, "Register listener");
        AccessorySensor sensor = getCurrentSensor();
        if (sensor != null) {
            try {
                if (sensor.isInterruptModeSupported()) {
                    sensor.registerInterruptListener(mListener);
                } else {
                    sensor.registerFixedRateListener(mListener,
                            Sensor.SensorRates.SENSOR_DELAY_UI);
                }
            } catch (AccessorySensorException e) {
                Log.d(ExtensionDroneService.LOG_TAG, "Failed to register listener", e);
            }
        }
    }

    private void unregister() {
        AccessorySensor sensor = getCurrentSensor();
        if (sensor != null) {
            sensor.unregisterListener();
        }
    }

    private void unregisterAndDestroy() {
        unregister();
        mSensors.clear();
        mSensors = null;
    }
}
