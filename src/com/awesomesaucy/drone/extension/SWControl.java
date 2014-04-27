package com.awesomesaucy.drone.extension;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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

/**
 * The sample sensor control handles the accelerometer sensor on an accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SWControl extends ControlExtension {

	private static String TAG = "AwesomeDrone";
	
    private int mCurrentSensor = 0;

    private List<AccessorySensor> mSensors = new ArrayList<AccessorySensor>();

    long mLastTouch;
    boolean flyMode = false;
    boolean fly = false;
    boolean hover = false;
	long startTime;
	Context context;
	private boolean runThread = true;

	Thread thread;
	
    private final AccessorySensorEventListener mListener = new AccessorySensorEventListener() {

        @Override
        public void onSensorEvent(AccessorySensorEvent sensorEvent) {
            float[] data = sensorEvent.getSensorValues();
            float x = data[0];
            float y = data[1];
            float z = data[2];
            
            // Rotation / Roll
            float yaw = petersMagicThreshold(y, 2, 9);
            // Height / Pitch
            float gaz = petersMagicThreshold(x, 2, 5);

            try{
	            if (flyMode) {
	            	ControlDroneActivity.droneControlService.setPitch(gaz);
	            	ControlDroneActivity.droneControlService.setRoll(yaw);
	            } else {
	            	ControlDroneActivity.droneControlService.setYaw(yaw);
	            	ControlDroneActivity.droneControlService.setGaz(gaz);
	            }
            }catch(Exception e){
            	e.printStackTrace();
            }
        }
    };

    SWControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
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
    
    public void toggleHover(){
    	hover = !hover;
    	int image = com.parrot.freeflight.R.drawable.state_hover;
    	
    	if(!hover && !flyMode)
    		image = com.parrot.freeflight.R.drawable.state_control;
    	else if(!hover && flyMode)
    		image = com.parrot.freeflight.R.drawable.state_move;
    	
    	sendImage(com.parrot.freeflight.R.id.sw_state, image);
    }

    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);
        showLayout(com.parrot.freeflight.R.layout.sw_layout, null);
        register();
        
        thread = new Thread(timeTakingRunnable);
        thread.start();
        
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);
        localBroadcastMgr.registerReceiver(emergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));

    }
    
    DroneEmergencyChangeReceiver emergencyReceiver = new DroneEmergencyChangeReceiver(ControlDroneActivity.controlDroneActivity){
    	@Override
    	public void onReceive(Context context, Intent intent) 
    	{
    		int code = intent.getIntExtra(DroneControlService.EXTRA_EMERGENCY_CODE, 0);
    		startVibrator(200, 200, 3);
    	}
    };

    @Override
    public void onObjectClick(ControlObjectClickEvent event) {
    	int id = event.getLayoutReference();
    	Log.d("control", "is long: "+(event.getClickType() == Control.Intents.CLICK_TYPE_LONG));
    	
    	try{
	    	switch(id){
	    	case com.parrot.freeflight.R.id.picture_button:
	    		ControlDroneActivity.droneControlService.takePhoto();
	    		break;
	    	case com.parrot.freeflight.R.id.emergency_button:
	    		ControlDroneActivity.droneControlService.triggerEmergency();
	    		break;
	    	case com.parrot.freeflight.R.id.time_text:
	    	case com.parrot.freeflight.R.id.sw_state:
	    		toggleMode();
	    		break;
	    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

    @Override
    public void onPause() {
        unregister();
    }

    @Override
    public void onDestroy() {
        unregisterAndDestroy();
        runThread = false;
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
        Log.d("control", "onTouch");
        if (event.getAction() == Control.Intents.TOUCH_ACTION_LONGPRESS) {
        	Log.d("control", "longpress");
        	toggleHover();
        }/*else if (event.getAction() == Control.Intents.TOUCH_ACTION_RELEASE) {
            boolean doubleTouch = System.currentTimeMillis() - mLastTouch < 700;
            if(doubleTouch){
                toggleFly();
            }
            mLastTouch = System.currentTimeMillis();
         }*/
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
        	Log.e("Control", "exception in onTouch");
        	e.printStackTrace();
        }
    }
    
    private void toggleFly() {
    	fly = !fly;
    	
    	int image = com.parrot.freeflight.R.drawable.state_stopped;
    	
    	if(fly && !flyMode)
    		image = com.parrot.freeflight.R.drawable.state_control;
    	else if(fly && flyMode)
    		image = com.parrot.freeflight.R.drawable.state_move;
    	
    	sendImage(com.parrot.freeflight.R.id.sw_state, image);
    	
    	try{
            ControlDroneActivity.droneControlService.triggerTakeOff();
        }catch(Exception e){
            Log.e("Control", "exception in onTouch");
            e.printStackTrace();
        }
    	
    	if(fly){
    		startTime = System.currentTimeMillis();
    		flyMode = false;
    	}
    	
	}

	private float petersMagicThreshold(float x, float from, float to) {
        if (x > 0) {
            if (x < from) return 0;
            else {
            	float out = (x - from)/(to - from);
            	return out > 1 ? 1 : out;
            }
            	
        } else {
            if (x > -from) return 0;
            else {
            	float out = (x + from) / (to - from);
            	return out < -1 ? -1 : out;
            }
        }
    }
    
    private void toggleMode() {
    	if(!fly || hover) return;
    	
        flyMode = !flyMode;
        try{
	        if (flyMode) {
	            // Stop Height/Rotation movement
	            ControlDroneActivity.droneControlService.setYaw(0);
	            ControlDroneActivity.droneControlService.setGaz(0);
	        } else {
	            // Stop Pitch/Rotation movement
	            ControlDroneActivity.droneControlService.setRoll(0);
	            ControlDroneActivity.droneControlService.setPitch(0);
	            //sendImage(R., com.parrot.freeflight.R.drawable.rotate_mode);
	        }
	
	        ControlDroneActivity.droneControlService.setProgressiveCommandEnabled(flyMode);
	        ControlDroneActivity.droneControlService.setProgressiveCommandCombinedYawEnabled(false);
	        ControlDroneActivity.running = flyMode;
        }catch(Exception e){
        	e.printStackTrace();
        }
        sendImage(com.parrot.freeflight.R.id.sw_state, flyMode ? com.parrot.freeflight.R.drawable.state_move : com.parrot.freeflight.R.drawable.state_control);
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
			while(runThread){
				try {
					Thread.sleep(823);
				} catch (Exception e) {
					e.printStackTrace();
				}
				float currentTime = (System.currentTimeMillis() - startTime) / (float) 1000;
				if(fly)
					sendText(com.parrot.freeflight.R.id.time_text, String.format("%.2fs", currentTime));
			}
		}
    };
}
