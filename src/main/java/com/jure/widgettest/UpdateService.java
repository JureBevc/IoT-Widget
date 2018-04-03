package com.jure.widgettest;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

import layout.AppWidget;

public class UpdateService extends Service {

    public static UpdateService currentService;
    public LinkedList<WidgetData> widgetData = new LinkedList<>();
    public boolean serviceRunning = false;

    public UpdateService(Context applicationContext) {
        super();
        currentService = this;
    }

    public UpdateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("ON CREATE", "SERVICE ON CREATE");
        currentService = this;
    }

    public WidgetData addWidget(DataWriter writer, int id, String serverURL, String Channel_ID, String API_Key, int updateInterval, int[] fieldsInOrder) {
        boolean found = false;
        for (int i = 0; i < widgetData.size(); i++) {
            WidgetData w = widgetData.get(i);
            if (w == null) continue;
            if (w.widgetID == id) {
                found = true;
                w.serverURL = serverURL;
                w.Channel_ID = Channel_ID;
                w.API_Key = API_Key;
                w.updateInterval = updateInterval;
                w.fieldsInOrder = fieldsInOrder;
                saveWidgetData(writer, w);
                return w;
            }
        }

        if (!found) {
            widgetData.add(new WidgetData(id, serverURL, Channel_ID, API_Key, updateInterval, fieldsInOrder));
            saveWidgetData(writer, widgetData.getLast());
            return widgetData.getLast();
        }
        return null;
    }

    public void removeWidget(DataWriter writer, Context context, int id) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;
        if (writer == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            editor = prefs.edit();
        } else {
            prefs = writer.sharedPreferences;
            editor = writer.editor;
        }
        String newIds = prefs.getString("all_ids", "").replace(id + ",", "");
        editor.putString("all_ids", newIds);
        editor.apply();

        for (int i = 0; i < widgetData.size(); i++) {
            WidgetData w = widgetData.get(i);
            if (w == null) continue;
            if (w.widgetID == id) {
                widgetData.remove(i);
                break;
            }
        }
    }

    public void setWidgetData(int id, String data, String[] values) {
        for (int i = 0; i < widgetData.size(); i++) {
            WidgetData w = widgetData.get(i);
            if (w == null) {
                Log.e("set NULL WIDGET DATA", id + "");
                continue;
            }
            if (w.widgetID == id) {
                double[] v = new double[8];
                for (int j = 0; j < 8; j++) {
                    try {
                        v[j] = Double.parseDouble(values[j]);
                    } catch (Exception e) {
                        v[j] = 0;
                    }
                }
            }
        }
    }

    public WidgetData getWidgetData(int id) {
        for (int i = 0; i < widgetData.size(); i++) {
            WidgetData w = widgetData.get(i);
            if (w == null) {
                Log.e("get NULL WIDGET DATA", id + "");
                continue;
            }
            if (w.widgetID == id) {
                return w;
            }
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.e("Service", "ON START");
        loadWidgetData(null);
        updateWidgets();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent("com.jure.widget.RestartService");
        sendBroadcast(broadcastIntent);
    }


    //int refreshSeconds = 60; // Should always be 60, set lower for faster refreshing when debugging
    //private Handler handler = new Handler();

    public void startTimer() {


        //Log.i("Service", "Starting timer");

        //schedule the handler, to wake up every 60 seconds
        //handler.postDelayed(runnable, 1000 * refreshSeconds);
    }
/*
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            updateWidgets();
            //handler.postDelayed(this, 1000 * refreshSeconds);
        }
    };
*/

    void updateWidgets() {
        Log.i("Service", "Updating widgets (" + widgetData.size() + ")");
        for (int i = 0; i < widgetData.size(); i++) {
            WidgetData w = widgetData.get(i);
            if (w == null) continue;
            if (w.currentUpdateTime == 0) {
                Log.i("Updating", "ID: " + w.widgetID);
                new RetrieveData().execute(w.widgetID + "", w.serverURL, w.Channel_ID, w.API_Key);
                w.currentUpdateTime = w.updateInterval;
            }
            if (w.netFail) {
                Log.i("Updating a failed", "ID: " + w.widgetID);
                new RetrieveData().execute(w.widgetID + "", w.serverURL, w.Channel_ID, w.API_Key);
            }
            w.currentUpdateTime--;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    void sendToWidget(int id, String serverURL, String Channel_ID, String API_Key, String data) {
        WidgetData w = getWidgetData(id);
        if (data.equals("netFail")) {
            if (w != null) {
                w.netFail = true;
                w.timeoutTries++;
            }
            Intent intent = new Intent(this, AppWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra("checkTimeoutOfId", id);
            int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AppWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        } else {
            if (w != null) {
                w.netFail = false;
                w.timeoutTries = 0;
            }
            Intent intent = new Intent(this, AppWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            intent.putExtra("updateAPIKey", API_Key);
            intent.putExtra("updateChannel", Channel_ID);
            intent.putExtra("updateURL", serverURL);
            intent.putExtra("updateID", id);
            intent.putExtra("updateData", data);

            //Log.e("sendToWidget", "Sending data to widget");
            int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AppWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    void loadWidgetData(DataWriter writer) {
        widgetData.clear();

        SharedPreferences prefs;
        if (writer == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } else {
            prefs = writer.sharedPreferences;
        }

        String ids[] = prefs.getString("all_ids", "").split(",");

        for (String id : ids) {
            if (!id.equals("")) {
                WidgetData w = widgetDataFromPreferences(prefs, Integer.parseInt(id));
                if (w != null)
                    widgetData.add(w);
            }
        }
    }

    WidgetData widgetDataFromPreferences(SharedPreferences prefs, int widgetID) {
        WidgetData w = new WidgetData();
        w.widgetID = widgetID;

        w.Channel_ID = prefs.getString("channel_" + w.widgetID, "--");
        w.API_Key = prefs.getString("api_" + w.widgetID, "--");
        w.serverURL = prefs.getString("server_" + w.widgetID, "--");
        w.ChannelName = prefs.getString("name_" + w.widgetID, "--");

        w.updateInterval = prefs.getInt("updateInterval_" + w.widgetID, 0);
        w.currentUpdateTime = prefs.getInt("currentUpdateTime_" + w.widgetID, 0);
        w.setDecimalPlaces(prefs.getString("decimalPlaces_" + w.widgetID, ""));

        w.netFail = prefs.getBoolean("netFail_" + w.widgetID, false);
        w.timeoutTries = prefs.getInt("timeoutTries_" + w.widgetID, 0);
        w.timeoutAlert = prefs.getBoolean("timeoutAlert_" + w.widgetID, false);
        w.timeoutAlertThreshold = prefs.getInt("timeoutAlertThreshold_" + w.widgetID, 0);

        w.metaAlert = prefs.getBoolean("metaAlert_" + w.widgetID, false);
        w.metaAlertString = prefs.getString("metaAlertString_" + w.widgetID, "--");

        w.repeatTimeout = prefs.getBoolean("repeatTimeout_" + w.widgetID, false);
        w.repeatMeta = prefs.getBoolean("repeatMeta_" + w.widgetID, false);
        w.setRepeatBounds(prefs.getString("repeatBounds_" + w.widgetID, ""));


        w.setBounds(prefs.getString("upperBoundAlert_" + w.widgetID, ""),
                prefs.getString("lowerBoundAlert_" + w.widgetID, ""),
                prefs.getString("upperBound_" + w.widgetID, ""),
                prefs.getString("lowerBound_" + w.widgetID, ""));

        w.setFieldsInOrder(prefs.getString("fieldsInOrder_" + w.widgetID, ""));
        return w;
    }

    void saveWidgetData(DataWriter writer, WidgetData w) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;
        if (writer == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            editor = prefs.edit();
        } else {
            prefs = writer.sharedPreferences;
            editor = writer.editor;
        }
        String all_ids = prefs.getString("all_ids", "");
        boolean exists = all_ids.contains("" + w.widgetID);
        if (!exists) {
            editor.putString("all_ids", all_ids + w.widgetID + ",");
        }

        editor.putString("channel_" + w.widgetID, w.Channel_ID);
        editor.putString("api_" + w.widgetID, w.API_Key);
        editor.putString("server_" + w.widgetID, w.serverURL);
        editor.putString("name_" + w.widgetID, w.ChannelName);

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
        editor.putBoolean("repeatMeta_" + w.widgetID, w.repeatMeta);
        editor.putString("repeatBounds_" + w.widgetID, w.formatRepeatBounds());

        editor.putString("upperBoundAlert_" + w.widgetID, w.formatUpperBoundAlert());
        editor.putString("lowerBoundAlert_" + w.widgetID, w.formatLowerBoundAlert());
        editor.putString("upperBound_" + w.widgetID, w.formatUpperBound());
        editor.putString("lowerBound_" + w.widgetID, w.formatLowerBound());

        editor.putString("fieldsInOrder_" + w.widgetID, w.formatFieldsInOrder());

        editor.apply();
    }


    class RetrieveData extends AsyncTask<String, String, String> {

        String ret, serverURL, Channel_ID, API_Key;
        int id;

        @Override
        protected String doInBackground(String... strings) {

            try {
                id = Integer.parseInt(strings[0]);
                serverURL = strings[1];
                Channel_ID = strings[2];
                API_Key = strings[3];
                String urlString = serverURL + "/channels/" + Channel_ID + "/feed.json?metadata=true&api_key=" + API_Key;
                //Log.e("Getting widget data", urlString);

                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                //int code = con.getResponseCode();
                // Log
                //System.out.println("GET request: " + urlString);
                //System.out.println("Response code: " + code);

                // Get response
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                ret = "";
                String line;
                while ((line = br.readLine()) != null) {
                    ret += line + "\n";
                }
                br.close();
            } catch (Exception e) {
                System.out.println("Data retrieving error:");
                ret = "netFail";
                //e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                sendToWidget(id, serverURL, Channel_ID, API_Key, ret);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}