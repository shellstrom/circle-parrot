package com.awesomesaucy.drone.extension;

import android.content.Context;

import com.sony.smarteyeglass.sdk.R;
import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.DisplayInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.HostApplicationInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationAdapter;

public class Helper {

	private static final int SMARTEYEGLASS_API_LEVEL = 4;
	
    public static int getSmartEyeglassWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_width);
    }

    public static int getSmartEyeglassHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smarteyeglass_control_height);
    }

    private static HostApplicationInfo getHostApp(Context context, String hostAppPackageName) {
        HostApplicationInfo hostApp = RegistrationAdapter.getHostApplication(
                context, hostAppPackageName);
        return hostApp;
    }

    public static boolean isSmartEyeglassApiAndScreenDetected(Context context,
            String hostAppPackageName) {
        HostApplicationInfo hostApp = getHostApp(context, hostAppPackageName);
        if (hostApp == null) {
            Dbg.d("Host app was null, returning");
            return false;
        }
        // Get screen dimensions, unscaled
        final int controlSWWidth = getSmartEyeglassWidth(context);
        final int controlSWHeight = getSmartEyeglassHeight(context);

        if (hostApp.getControlApiVersion() >= SMARTEYEGLASS_API_LEVEL) {
            for (DeviceInfo device : RegistrationAdapter.getHostApplication(context,
                    hostAppPackageName).getDevices()) {
                for (DisplayInfo display : device.getDisplays()) {
                    if (display.sizeEquals(controlSWWidth, controlSWHeight)) {
                        return true;
                    }
                }
            }
        } else {
            Dbg.d("Host had control API version: " + hostApp.getControlApiVersion() + ", returning");
        }
        return false;
    }

}
