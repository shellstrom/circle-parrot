package com.dronesareawesome.smartwear.extension;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.dronesareawesome.freeflight.R;
import com.parrot.freeflight.activities.ControlDroneActivity;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.parrot.freeflight.service.DroneControlService;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorTypeValue;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorManager;

class SW1DroneController extends ControlExtension {

    private Context context;
    private int mCurrentSensor = 0;
    private List<AccessorySensor> mSensors = new ArrayList<AccessorySensor>();

    long mLastTouch;
    boolean flyMode = true;
    boolean isFlying = false;
    boolean isHovering = false;

    SW1DroneController(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
        AccessorySensorManager manager = new AccessorySensorManager(context,
                hostAppPackageName);
        // Add accelerometer, if supported
        if (DeviceInfoHelper.isSensorSupported(context, hostAppPackageName, SensorTypeValue.ACCELEROMETER)) {
            mSensors.add(manager.getSensor(SensorTypeValue.ACCELEROMETER));
        }
    }

    @Override
    public void onResume() {
        Log.d(DroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon);
        showBitmap(bitmap);
        register();

        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);
        localBroadcastMgr.registerReceiver(emergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        unregister();
    }

    @Override
    public void onDestroy() {
        unregisterAndDestroy();
    }

    @Override
    public void onTouch(ControlTouchEvent event) {
        super.onTouch(event);
        Log.d(DroneService.LOG_TAG, "onTouch");
        if (event.getAction() == Control.Intents.TOUCH_ACTION_LONGPRESS) {
//            Log.d(DroneService.LOG_TAG, "longpress");
        }
    }

    private final AccessorySensorEventListener mListener = new AccessorySensorEventListener() {

        @Override
        public void onSensorEvent(AccessorySensorEvent sensorEvent) {
            float[] data = sensorEvent.getSensorValues();
            float x = data[0];
            float y = data[1];
            //float z = data[2];
            
            // Rotation / Roll
            float leftRight = magicThreshold(y, 2, 9);
            // Height / Pitch
            float upDown = magicThreshold(x, 2, 5);

            try{
                Log.d(DroneService.LOG_TAG, "PitchRoll x:" +x+ " y:" +y);
                ControlDroneActivity.droneControlService.setPitch(upDown);
                ControlDroneActivity.droneControlService.setRoll(leftRight);
            }catch(Exception e){
                //e.printStackTrace();
            }
        }
    };

    /**
     * Sends vibrations if an emergency status is reported from the drone
     */
    private DroneEmergencyChangeReceiver emergencyReceiver = new DroneEmergencyChangeReceiver(ControlDroneActivity.controlDroneActivity){
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            //int code = intent.getIntExtra(DroneControlService.EXTRA_EMERGENCY_CODE, 0);
            startVibrator(200, 200, 3);
        }
    };

    /**
     * Converts any real number (e.g. sensor value) into range -1..0..1.
     * Used for thresholding small values while still having linear range.
     * Works universally for positive and negative numbers.
     * Simply: Take the values from..to as 0..1 and values -to..-from as -1..0.
     * Everything outside the range is 0, 1 or -1.
     * 
     * @param x Value to convert
     * @param from This is the minimal value which will represent 0 in result.
     * @param to This is the maximal value which will represent 1 in result.
     * @return Converted number in range -1..0..1.
     */
	private float magicThreshold(float x, float from, float to) {
        if (x > 0) {
            if (x < from) return 0;
            else if (x > to) return 1;
            else return (x - from) / (to - from);
        } else {
            if (x > -from) return 0;
            else if (x < -to) return -1;
            else return (x + from) / (to - from);
        }
    }

    public static boolean isWidthSupported(Context context, int width) {
        return true;
    }

    public static boolean isHeightSupported(Context context, int height) {
        return true;
    }

    private void register() {
        Log.d(DroneService.LOG_TAG, "Register listener");
        AccessorySensor sensor = getCurrentSensor();
        if (sensor != null) {
            try {
                if (sensor.isInterruptModeSupported()) {
                    sensor.registerInterruptListener(mListener);
                } else {
                    sensor.registerFixedRateListener(mListener, Sensor.SensorRates.SENSOR_DELAY_NORMAL);
                }
            } catch (AccessorySensorException e) {
                Log.d(DroneService.LOG_TAG, "Failed to register listener", e);
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

    private AccessorySensor getCurrentSensor() {
        return mSensors.get(mCurrentSensor);
    }
}
