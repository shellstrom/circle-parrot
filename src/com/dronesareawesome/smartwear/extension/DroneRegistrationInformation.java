package com.dronesareawesome.smartwear.extension;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.parrot.dronesareawesome.freeflight.R;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.HostApplicationInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;

public class DroneRegistrationInformation extends RegistrationInformation{
	Context mContext;

	protected DroneRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
	}

	@Override
    public int getRequiredControlApiVersion() {
        return 1;
    }

	@Override
    public int getRequiredSensorApiVersion() {
        return 1;
    }
	
	@Override
	public int getRequiredWidgetApiVersion() {
		return RegistrationInformation.API_NOT_REQUIRED;
	}
	
	@Override
	public int getRequiredNotificationApiVersion() {
		return RegistrationInformation.API_NOT_REQUIRED;
	}

	@Override
    public ContentValues getExtensionRegistrationConfiguration() {
		Log.d("DroneControl", "getExtensionRegistrationConfiguration");
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String iconExtension = ExtensionUtils.getUriString(mContext, R.drawable.icon);

        ContentValues values = new ContentValues();

        values.put(Registration.ExtensionColumns.NAME, "SmartDrone");
        values.put(Registration.ExtensionColumns.EXTENSION_KEY, DroneService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconExtension);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        return (DroneController.isWidthSupported(mContext, width) && DroneController.isHeightSupported(mContext, height));
    }

    @Override
    public boolean isSensorSupported(AccessorySensor sensor) {
        return Registration.SensorTypeValue.ACCELEROMETER.equals(sensor.getType().getName());
    }

    @Override
    public boolean isSupportedSensorAvailable(Context context, HostApplicationInfo hostApplication) {
        // Both control and sensor needs to be supported to register as sensor.
        return super.isSupportedSensorAvailable(context, hostApplication) && super.isSupportedControlAvailable(context, hostApplication);
    }

    @Override
    public boolean isSupportedControlAvailable(Context context, HostApplicationInfo hostApplication) {
        // Both control and sensor needs to be supported to register as control.
        return super.isSupportedSensorAvailable(context, hostApplication) && super.isSupportedControlAvailable(context, hostApplication);
    }
}
