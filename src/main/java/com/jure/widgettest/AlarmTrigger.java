package com.jure.widgettest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.util.Calendar;

public class AlarmTrigger extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("ALARM TRIGGER", "SERVICE TRIGGERED");
        Intent myService = new Intent(context, UpdateService.class);
        try {
            context.startService(myService);
        }catch (Exception e) {

            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent serviceIntent = new Intent(context, AlarmTrigger.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            //manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 60000, pendingIntent);
            Calendar calendar = Calendar.getInstance();
            manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 60000, pendingIntent);
        }
    }
}
