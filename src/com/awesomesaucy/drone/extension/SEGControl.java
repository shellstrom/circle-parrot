package com.awesomesaucy.drone.extension;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.freeflight.R;
import com.parrot.freeflight.activities.ControlDroneActivity;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.parrot.freeflight.service.DroneControlService;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEvent;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorEventListener;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorException;

/**
 * The sample sensor control handles the accelerometer sensor on an accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SEGControl extends ControlExtension {

	private static String TAG = "AwesomeDrone";
	
	private Context context;
	private int mCurrentSensor = 0;
    private List<AccessorySensor> mSensors = new ArrayList<AccessorySensor>();

    long mLastTouch;
    boolean flyMode = false;
    boolean isFlying = false;
    boolean isHovering = false;
	
    private long startTime;
	private boolean runTimerThread = true;
	private Thread timerThread;
	
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
	            if (flyMode) {
	            	ControlDroneActivity.droneControlService.setPitch(upDown);
	            	ControlDroneActivity.droneControlService.setRoll(leftRight);
	            } else {
	            	ControlDroneActivity.droneControlService.setYaw(leftRight);
	            	ControlDroneActivity.droneControlService.setGaz(upDown);
	            }
            }catch(Exception e){
            	e.printStackTrace();
            }
        }
    };

    SEGControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
    }
    
    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);
        showLayout(R.layout.sw_layout, null);
        register();
        
        timerThread = new Thread(timeTakingRunnable);
        timerThread.start();
        
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);
        localBroadcastMgr.registerReceiver(emergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));

    }
    
    private DroneEmergencyChangeReceiver emergencyReceiver = new DroneEmergencyChangeReceiver(ControlDroneActivity.controlDroneActivity){
    	@Override
    	public void onReceive(Context context, Intent intent) 
    	{
    		//int code = intent.getIntExtra(DroneControlService.EXTRA_EMERGENCY_CODE, 0);
    		startVibrator(200, 200, 3);
    	}
    };

    @Override
    public void onPause() {
        unregister();
    }

    @Override
    public void onDestroy() {
        unregisterAndDestroy();
        runTimerThread = false;
    }

    @Override
    public void onTouch(ControlTouchEvent event) {
        super.onTouch(event);
    }
    
    @Override
    public void onKey(int action, int keyCode, long timeStamp) {
    	super.onKey(action, keyCode, timeStamp);
    	if (action == Control.Intents.KEY_ACTION_RELEASE && keyCode == Control.KeyCodes.KEYCODE_OPTIONS) {
            toggleFly();
        }
    }
    
    @Override
    public void onSwipe(int direction) {
    	super.onSwipe(direction);
    	try{
        	ControlDroneActivity.droneControlService.doLeftFlip();
        }catch(Exception e){
        	Log.e(TAG, "exception in onTouch");
        	e.printStackTrace();
        }
    }
    
    private void toggleFly() {
    	isFlying = !isFlying;
    	
    	int image = R.drawable.state_stopped;
    	
    	if(isFlying && !flyMode)
    		image = R.drawable.state_control;
    	else if(isFlying && flyMode)
    		image = R.drawable.state_move;
    	
    	sendImage(R.id.sw_state, image);
    	
    	try{
            ControlDroneActivity.droneControlService.triggerTakeOff();
        }catch(Exception e){
            Log.e(TAG, "exception in onTouch");
            e.printStackTrace();
        }
    	
    	if(isFlying){
    		startTime = System.currentTimeMillis();
    		flyMode = false;
    	}
    	
	}

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
                            Sensor.SensorRates.SENSOR_DELAY_NORMAL);
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
    
    Runnable timeTakingRunnable = new Runnable(){
    	
		@Override
		public void run() {
			while(runTimerThread){
				try {
					Thread.sleep(823);
				} catch (Exception e) {
					e.printStackTrace();
				}
				float currentTime = (System.currentTimeMillis() - startTime) / (float) 1000;
				//TODO
				//if(isFlying)
					//sendText(R.id.time_text, String.format("%.2fs", currentTime));
			}
		}
    };
}
