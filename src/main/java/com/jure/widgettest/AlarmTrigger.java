package com.jure.widgettest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmTrigger extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("ALARM TRIGGER", "SERVICE TRIGGERED");
        Intent myService = new Intent(context, UpdateService.class);
        context.startService(myService);
    }
}
