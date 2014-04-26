package com.awesomesaucy.drone.extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExtensionReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setClass(context, ExtensionDroneService.class);
		context.startService(intent);
	}

}
