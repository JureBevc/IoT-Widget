package com.jure.widgettest;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    public void putInt(String key, int value) {
        editor.putInt(key, value);
    }

    public void putString(String key, String value) {
        editor.putString(key, value);
    }

    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
    }

    public int getInt(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    public String getString(String key) {
        return sharedPreferences.getString(key, "");
    }

    public boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }
}
