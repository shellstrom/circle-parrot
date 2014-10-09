package com.awesomesaucy.drone.extension;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.ui.gl.GLBGVideoSprite;
import com.sony.smarteyeglass.extension.util.SmartEyeglassControlUtils;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

class SEGControl extends ControlExtension {

	private static String TAG = "AwesomeDrone";
	
	private static final int FPS = 8;
	private SmartEyeglassControlUtils utils;
	private Context context;
    private Bitmap hudBmp;
    private Timer timer;
	
    SEGControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
        
        utils = new SmartEyeglassControlUtils(hostAppPackageName, null);
        utils.activate(mContext);
    }    
    
    private Bitmap getHUD()
    {
    	int w = Helper.getSmartEyeglassWidth(mContext);
    	int h = Helper.getSmartEyeglassHeight(mContext);
    	int STROKE_WIDTH=2;
    	int radius = h/2-STROKE_WIDTH;

    	float pitch = DroneControlService.instance.lastPitch; // speed
    	float roll = DroneControlService.instance.lastRoll; // left right speed
    	
    	float gaz = DroneControlService.instance.lastGaz; // up down speed
    	float yaw = DroneControlService.instance.lastYaw; // left right turning    	
    	
    	// Free last frame
    	if (hudBmp != null) {
    		hudBmp.recycle();
    		hudBmp = null;
    	}
    	
    	// Try to get drone video frame
    	if (GLBGVideoSprite.videoFrameLock != null)
	    	synchronized (GLBGVideoSprite.videoFrameLock)
	    	{
	    		if (GLBGVideoSprite.video != null)
	    		{
	    			hudBmp = Bitmap.createScaledBitmap(GLBGVideoSprite.video, w, h, true);
	    		}
	    	}
    	
    	// Blank bitmap if no video frame available
    	hudBmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
    	hudBmp.eraseColor(Color.BLACK);
    	
    	Paint paint = new Paint();
    	paint.setAntiAlias(true);
    	paint.setColor(Color.WHITE);
    	paint.setStyle(Paint.Style.STROKE);
    	paint.setStrokeWidth(STROKE_WIDTH);
    	paint.setTextSize(18);
    	
    	Canvas canvas = new Canvas(hudBmp);
    	canvas.drawCircle(w/2, h/2, radius, paint);
    	canvas.drawLine(w-STROKE_WIDTH, 0, w-STROKE_WIDTH, h, paint);
    	
    	// Left right tilt
    	int tiltOffset = (int) (roll*h/2);
    	canvas.drawLine(0, h/2-tiltOffset, w, h/2+tiltOffset, paint);
    	
    	// Speed
    	float speed = -pitch*10;
    	canvas.drawText(String.format("%.1f mps", speed), 0, h/2+20, paint);
    	
    	// Vertical speed
    	float verSpeedOffset = (int) (gaz*h/2);
    	paint.setStrokeWidth(STROKE_WIDTH*4);
    	canvas.drawLine(w-15, h/2-verSpeedOffset, w, h/2-verSpeedOffset, paint);
    	
    	//Turning speed
    	double maxAngle = Math.PI/2;
    	double angle = yaw * maxAngle;
    	float cX = (float) (w/2 + Math.sin(angle) * radius);
    	float cY = (float) (h/2 - Math.cos(angle) * radius);
    	canvas.drawCircle(cX, cY, 2, paint);
    	
    	return hudBmp;
    }
    
    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);
        showBitmap(getHUD());
        
        //utils.setPowerMode(SmartEyeglassControl.Intents.POWER_MODE_HIGH);
        
        timer = new Timer();
        timer.schedule(new TimerTask() {
    		
    		@Override
    		public void run() {
    			showBitmap(getHUD());
    		}
    	}, 500, 1000/FPS);
    }
    
    
	public void onPause() {
		//utils.setPowerMode(SmartEyeglassControl.Intents.POWER_MODE_NORMAL);
		timer.cancel();
		timer.purge();
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
        	DroneControlService.instance.doLeftFlip();
        }catch(Exception e){
        	Log.e(TAG, "exception in onTouch");
        	e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
    	utils.deactivate();    	
    	super.onDestroy();
    }
}
