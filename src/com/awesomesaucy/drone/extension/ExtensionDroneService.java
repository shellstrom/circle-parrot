package com.awesomesaucy.drone.extension;


import android.util.Log;

import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The Sample Extension Service handles registration and keeps track of all
 * sensors on all accessories.
 */
public class ExtensionDroneService extends ExtensionService {

    public static final int NOTIFY_STOP_ALERT = 1;

    public static final String EXTENSION_KEY = "com.sonyericsson.extras.liveware.extension.sensorsample.key";

    public static final String LOG_TAG = "SampleSensorExtension";

    public final String CLASS = getClass().getSimpleName();

    public ExtensionDroneService() {
        super(EXTENSION_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, CLASS + ": onCreate");
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new RegInfo(this);
    }

    /*
     * (non-Javadoc)
     * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
     * keepRunningWhenConnected()
     */
    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }

    @Override
    public ControlExtension createControlExtension(String hostAppPackageName) {
    	boolean sw2 = DeviceInfoHelper.isSmartWatch2ApiAndScreenDetected(this, hostAppPackageName);
    	if(sw2)
    		return new SWControl(hostAppPackageName, this);
    	else
    		return new SW1Control(hostAppPackageName, this);
    }
}
