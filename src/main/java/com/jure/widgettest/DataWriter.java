package com.jure.widgettest;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import layout.AppWidget;

/**
 * Created by Jure on 26/03/2018.
 */

public class DataWriter {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public DataWriter(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int widgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, AppWidget.class));
        String allIds = "";
        for (int id : widgetIds) {
            allIds += id + ",";
        }
        editor.putString("all_ids", allIds);
        editor.apply();
    }

    public DataWriter(Context context, int remove_id) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int widgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, AppWidget.class));
        String allIds = "";
        for (int id : widgetIds) {
            if (id != remove_id)
                allIds += id + ",";
        }
        editor.putString("all_ids", allIds);
        editor.apply();
    }

    public void saveWidgetData(WidgetData w, boolean allData) {

        String all_ids = sharedPreferences.getString("all_ids", "");
        boolean exists = all_ids.contains("" + w.widgetID);
        if (!exists) {
            editor.putString("all_ids", all_ids + w.widgetID + ",");
        }
        if (allData) {
            editor.putString("channel_" + w.widgetID, w.Channel_ID);
            editor.putString("api_" + w.widgetID, w.API_Key);
            editor.putString("server_" + w.widgetID, w.serverURL);
            editor.putString("name_" + w.widgetID, w.ChannelName);
        }
        editor.putInt("updateInterval_" + w.widgetID, w.updateInterval);
        editor.putInt("currentUpdateTime_" + w.widgetID, w.currentUpdateTime);
        editor.putString("decimalPlaces_" + w.widgetID, w.formatDecimals());

        editor.putBoolean("netFail_" + w.widgetID, w.netFail);
        editor.putInt("timeoutTries_" + w.widgetID, w.timeoutTries);
        editor.putBoolean("timeoutAlert_" + w.widgetID, w.timeoutAlert);
        editor.putInt("timeoutAlertThreshold_" + w.widgetID, w.timeoutAlertThreshold);

        editor.putBoolean("metaAlert_" + w.widgetID, w.metaAlert);
        editor.putString("metaAlertString_" + w.widgetID, w.metaAlertString);

        editor.putBoolean("repeatTimeout_" + w.widgetID, w.repeatTimeout);
        editor.putBoolean("timeoutAlerted_" + w.widgetID, w.timeoutAlerted);
        editor.putBoolean("repeatMeta_" + w.widgetID, w.repeatMeta);
        editor.putBoolean("metaAlerted_" + w.widgetID, w.metaAlerted);
        editor.putString("repeatBounds_" + w.widgetID, w.formatRepeatBounds());
        editor.putString("boundsAlerted_" + w.widgetID, w.formatBoundsAlerted());

        editor.putString("upperBoundAlert_" + w.widgetID, w.formatUpperBoundAlert());
        editor.putString("lowerBoundAlert_" + w.widgetID, w.formatLowerBoundAlert());
        editor.putString("upperBound_" + w.widgetID, w.formatUpperBound());
        editor.putString("lowerBound_" + w.widgetID, w.formatLowerBound());

        editor.putString("fieldsInOrder_" + w.widgetID, w.formatFieldsInOrder());

        editor.apply();
    }

}
