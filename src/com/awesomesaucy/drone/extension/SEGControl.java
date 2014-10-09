package com.awesomesaucy.drone.extension;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.ui.gl.GLBGVideoSprite;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

class SEGControl extends ControlExtension {

	private static String TAG = "AwesomeDrone";
	
	private Context context;
	
    SEGControl(final String hostAppPackageName, final Context context) {
        super(context, hostAppPackageName);
        this.context = context;
    }
    
    private Bitmap hudBmp;
    
    private Bitmap getHUD()
    {
    	int w = Helper.getSmartEyeglassWidth(mContext);
    	int h = Helper.getSmartEyeglassHeight(mContext);

    	float yaw = 0.5f; // left right speed
    	float gaz = 0.3f; // speed

    	// Free last frame
    	if (hudBmp != null) {
    		hudBmp.recycle();
    		hudBmp = null;
    	}
    	
    	// Try to get drone video frame
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
    	paint.setStrokeWidth(2);
    	
    	Canvas canvas = new Canvas(hudBmp);
    	canvas.drawCircle(w/2, h/2, h/2, paint);
    	canvas.drawLine(0, h/2, w, h/2, paint);
    	
    	return hudBmp;
    }
    
    @Override
    public void onResume() {
        Log.d(ExtensionDroneService.LOG_TAG, "Starting control");
        setScreenState(Control.Intents.SCREEN_STATE_DIM);
        showBitmap(getHUD());        
    }
    
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

}
