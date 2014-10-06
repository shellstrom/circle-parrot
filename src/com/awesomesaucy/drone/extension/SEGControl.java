package com.awesomesaucy.drone.extension;

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
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

class SEGControl extends ControlExtension {

	private static String TAG = "AwesomeDrone";
	
	private Context context;

    long mLastTouch;
    boolean flyMode = false;
    boolean isFlying = false;
    boolean isHovering = false;
	
    SEGControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
    }
    
    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);
        showLayout(R.layout.seg_layout, null);
        
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);
        localBroadcastMgr.registerReceiver(emergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));
    }
    
    private DroneEmergencyChangeReceiver emergencyReceiver = new DroneEmergencyChangeReceiver(ControlDroneActivity.controlDroneActivity){
    	@Override
    	public void onReceive(Context context, Intent intent) 
    	{
    		//int code = intent.getIntExtra(DroneControlService.EXTRA_EMERGENCY_CODE, 0);
    		//TODO
    	}
    };

    @Override
    public void onTouch(ControlTouchEvent event) {
        super.onTouch(event);
    }
    
    @Override
    public void onKey(int action, int keyCode, long timeStamp) {
    	super.onKey(action, keyCode, timeStamp);
    	if (action == Control.Intents.KEY_ACTION_RELEASE && keyCode == Control.KeyCodes.KEYCODE_OPTIONS) {
            ;//TODO
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

}
