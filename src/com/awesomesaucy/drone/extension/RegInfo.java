package com.awesomesaucy.drone.extension;

import android.content.ContentValues;
import android.content.Context;

import com.parrot.freeflight.R;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

public class RegInfo extends RegistrationInformation{
	Context mContext;

	protected RegInfo(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
	}

	@Override
	public int getRequiredNotificationApiVersion() {
		return 0;
	}

	@Override
    public int getRequiredControlApiVersion() {
        return 2;
    }

	@Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String iconExtension = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String iconExtension48 = ExtensionUtils.getUriString(mContext, R.drawable.icon);
        String iconExtensionBw = ExtensionUtils.getUriString(mContext, R.drawable.icon);

        ContentValues values = new ContentValues();

        //values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, PreferenceActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, "Conf");
        values.put(Registration.ExtensionColumns.NAME, "Awesome drone controller");
        values.put(Registration.ExtensionColumns.EXTENSION_KEY, ExtensionDroneService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconExtension);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, iconExtension48);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI_BLACK_WHITE, iconExtensionBw);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }
	
	@Override
	public int getRequiredWidgetApiVersion() {
		return 0;
	}

	@Override
    public int getRequiredSensorApiVersion() {
        return 2;
    }
	
	@Override
	public boolean isDisplaySizeSupported(int w, int h)
	{
		boolean sw2 = w == DeviceInfoHelper.getSmartWatch2Width(mContext) && h == DeviceInfoHelper.getSmartWatch2Height(mContext);
		boolean seg = w == Helper.getSmartEyeglassWidth(mContext) && h == Helper.getSmartEyeglassHeight(mContext);
		return sw2 || seg;
	}

}
