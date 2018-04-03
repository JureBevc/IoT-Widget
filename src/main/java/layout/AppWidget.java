package layout;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.jure.widgettest.MainActivity;
import com.jure.widgettest.R;
import com.jure.widgettest.UpdateService;
import com.jure.widgettest.WidgetData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    static int updateID = AppWidgetManager.INVALID_APPWIDGET_ID;
    static String updateURL = "";
    static String updateChannel = "";
    static String updateAPIKey = "";
    static String updateData = "";
    static boolean checkTimeout = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.e("Updating widget", "" + appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        // Update widget data
        if (appWidgetId == updateID && (updateData != null || checkTimeout)) {
            updateWidgetData(context, views, updateData);
        }

        // Open App button
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pIntent = PendingIntent.getActivity(context, appWidgetId, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.OpenAppButton, pIntent);

        // Open App button on channel
        views.setOnClickPendingIntent(R.id.ChannelText, pIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        //Log.e("UPDATE", "onUpdate...");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        //Log.e("onRecieve", "Widget got something " + intent.toString() + " " + intent.getAction());

        // Update
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (!intent.hasExtra("checkTimeoutOfId")) {
                    updateURL = extras.getString("updateURL");
                    updateChannel = extras.getString("updateChannel");
                    updateAPIKey = extras.getString("updateAPIKey");
                    updateID = extras.getInt("updateID");
                    updateData = extras.getString("updateData");

                    int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    if (appWidgetIds != null && appWidgetIds.length > 0) {
                        this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
                    }
                    checkTimeout = false;
                } else {
                    updateID = extras.getInt("checkTimeoutOfId");
                    checkTimeout = true;
                    int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    if (appWidgetIds != null && appWidgetIds.length > 0) {
                        this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
                    }
                }

            }
        }
    }


    static void updateWidgetData(Context context, RemoteViews remoteViews, String updateData) {
        WidgetData w = UpdateService.currentService.getWidgetData(updateID);

        // Check for timeout
        if (checkTimeout) {
            if (w != null && w.timeoutAlert && w.timeoutTries - 1 >= w.timeoutAlertThreshold) {
                sendNotification(context, "Timeout alert. Could not fetch data.", w.ChannelName);
                setAllColor(remoteViews, Color.parseColor("#FF2222"));
            }
            return;
        }
        setAllColor(remoteViews, Color.WHITE);

        // Update widgetData object

        // Extract data
        String[] fieldNames = new String[8];
        String[] fieldValues = new String[8];
        processData(context, updateData, fieldNames, fieldValues);
        UpdateService.currentService.setWidgetData(updateID, updateData, fieldValues);

        // Check for meta data alert
        if (w.metaAlert)
            try {
                String metaData = new JSONObject(updateData).getJSONObject("channel").getString("metadata");
                if (metaData.contains(w.metaAlertString)) {
                    sendNotification(context, "Alert string found in matadata: " + w.metaAlertString, w.ChannelName);
                }
            } catch (Exception e) {
                // Error getting meta data
            }

        // Set data
        // Change channel name
        try {
            w.ChannelName = new JSONObject(updateData).getJSONObject("channel").getString("name");
            remoteViews.setTextViewText(R.id.ChannelText, w.ChannelName);
        } catch (Exception e) {
            // No channel name
        }

        // Set field values
        int[] fieldNameTexts = {R.id.FieldText1, R.id.FieldText2, R.id.FieldText3, R.id.FieldText4, R.id.FieldText5, R.id.FieldText6, R.id.FieldText7, R.id.FieldText8};
        int[] fieldValueTexts = {R.id.Field1, R.id.Field2, R.id.Field3, R.id.Field4, R.id.Field5, R.id.Field6, R.id.Field7, R.id.Field8};
        int nextField = 0;
        for (int i = 0; i < 8; i++) {
            if (w == null) {
                Log.e("ERROR", "Could not get widgetData object of this widget.");
                break;
            }
            if (w.fieldsInOrder[i] != -1) {
                remoteViews.setTextViewText(fieldNameTexts[nextField], fieldNames[w.fieldsInOrder[i] - 1]);

                if (w.decimalPlaces[i] >= 0 && isDecimal(fieldValues[w.fieldsInOrder[i] - 1])) {
                    double rounded = Math.round(Double.parseDouble(fieldValues[w.fieldsInOrder[i] - 1]) * Math.pow(10, w.decimalPlaces[i])) / Math.pow(10, w.decimalPlaces[i]);
                    remoteViews.setTextViewText(fieldValueTexts[nextField], (rounded == (long) rounded) ? (long) rounded + "" : rounded + "");
                } else {
                    remoteViews.setTextViewText(fieldValueTexts[nextField], fieldValues[w.fieldsInOrder[i] - 1]);
                }


                int b = isOutOfBounds(w, fieldValues[w.fieldsInOrder[i] - 1], i);
                if (b != 0) {
                    remoteViews.setTextColor(fieldNameTexts[nextField], Color.parseColor("#FF2222"));
                    remoteViews.setTextColor(fieldValueTexts[nextField], Color.parseColor("#FF2222"));
                    if (b > 0)
                        sendNotification(context, fieldNames[w.fieldsInOrder[i] - 1] + ": Upper threshold exceeded!", w.ChannelName);
                    if (b < 0)
                        sendNotification(context, fieldNames[w.fieldsInOrder[i] - 1] + ": Lower threshold exceeded!", w.ChannelName);
                } else {
                    remoteViews.setTextColor(fieldNameTexts[nextField], Color.parseColor("#FFFFFF"));
                    remoteViews.setTextColor(fieldValueTexts[nextField], Color.parseColor("#FFFFFF"));
                }
                nextField++;
            }
        }
        // Set empty fields
        for (int i = nextField; i < 8; i++) {
            remoteViews.setTextViewText(fieldNameTexts[i], "");
            remoteViews.setTextViewText(fieldValueTexts[i], "");
        }

        // Set time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d. HH:mm");
        remoteViews.setTextViewText(R.id.DateAndTimeText, sdf.format(Calendar.getInstance().getTime()));
        updateID = AppWidgetManager.INVALID_APPWIDGET_ID;
    }

    static void processData(Context context, String updateData, String[] fieldNames, String[] fieldValues) {
        try {
            JSONObject data = new JSONObject(updateData);
            // Get field names
            for (int i = 1; i <= 8; i++) {
                try {
                    String fieldName = data.getJSONObject("channel").getString("field" + i);
                    //System.out.println("field" + i + ": " + fieldName);
                    fieldNames[i - 1] = fieldName;
                } catch (Exception e) {
                    // Field does not exist
                    //System.out.println("field" + i + ": " + " - ");
                    fieldNames[i - 1] = "-";
                }
            }

            try {
                JSONArray feeds = data.getJSONArray("feeds");
                for (int i = 1; i <= 8; i++) {
                    try {
                        fieldValues[i - 1] = feeds.getJSONObject(feeds.length() - 1).getString("field" + i);
                        fieldValues[i - 1] = (int) (Double.parseDouble(fieldValues[i - 1]) * 100) / 100.0f + "";

                    } catch (Exception e) {
                        // Field does not exist
                        fieldValues[i - 1] = "-";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //appWidgetManager.updateAppWidget(id, remoteViews);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int isOutOfBounds(WidgetData w, String widgetStringValue, int fieldIndex) {

        if (widgetStringValue == null || !isDecimal(widgetStringValue))
            return 0;
        double widgetValue = Double.parseDouble(widgetStringValue);
        if ((w.upperBoundAlert[fieldIndex] && w.upperBound[fieldIndex] < widgetValue))
            return 1;
        if (w.lowerBoundAlert[fieldIndex] && w.lowerBound[fieldIndex] > widgetValue)
            return -1;
        return 0;
    }

    static void sendNotification(Context context, String text, String channelName) {
        NotificationCompat.Builder n = new NotificationCompat.Builder(context)
                .setContentTitle("IoT Widget Alert" + ((channelName != null) ? " - " + channelName : ""))
                .setContentText(text)
                .setSmallIcon(R.drawable.alert_icon)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 100, 50, 100, 100})
                .setLights(Color.RED, 500, 1000);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        try {
            notificationManager.notify(updateID, n.build());
        } catch (Exception e) {
            Log.e("WidgetApp", "Notification error");
            e.printStackTrace();
        }
    }

    static void setAllColor(RemoteViews remoteViews, int color) {
        int[] fieldNameTexts = {R.id.FieldText1, R.id.FieldText2, R.id.FieldText3, R.id.FieldText4, R.id.FieldText5, R.id.FieldText6, R.id.FieldText7, R.id.FieldText8};
        int[] fieldValueTexts = {R.id.Field1, R.id.Field2, R.id.Field3, R.id.Field4, R.id.Field5, R.id.Field6, R.id.Field7, R.id.Field8};
        for (int i = 0; i < 8; i++) {
            remoteViews.setTextColor(fieldNameTexts[i], color);
            remoteViews.setTextColor(fieldValueTexts[i], color);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        // Remove all deleted widgets
        for (int id : appWidgetIds) {
            UpdateService.currentService.removeWidget(null, context, id);
        }
    }

    static boolean isDecimal(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
