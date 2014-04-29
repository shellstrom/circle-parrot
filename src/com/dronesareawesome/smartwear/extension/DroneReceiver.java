package com.dronesareawesome.smartwear.extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DroneReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setClass(context, DroneService.class);
		context.startService(intent);
	}

}
