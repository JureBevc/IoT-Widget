package com.jure.widgettest;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import layout.AppWidget;

public class MainActivity extends Activity {

    private String Channel_ID = "", API_Key = "", serverURL = "http://api.thingspeak.com";
    private int updateInterval = 1;

    UpdateService updateService;
    DataWriter writer;
    Intent serviceIntent;

    String updateData = "";

    int id;
    Intent resultValue;

    String[] empty = {"", "", "", "", "", "", "", ""};

    boolean timeoutAlert = false;
    int timeoutAlertMinutes = 0;

    boolean metaAlert = false;
    String metaAlertString = "";

    WidgetData currentWidgetData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        writer = new DataWriter(this);
        updateService = new UpdateService(this);
        getIdOfCurrentWidget(savedInstanceState);
        currentWidgetData = updateService.addWidget(writer, id, serverURL, Channel_ID, API_Key, updateInterval, getFields());
        setWidgetOptions();
        seekBarListener();
        timeoutListener();
        metaListener();
        channelInputListener();
        apiInputListener();
        serverInputListener();
        fieldListSelectionListener();
        doneButtonListener();
        cancelButtonListener();
    }

    void setWidgetOptions() {
        WidgetData w = updateService.getWidgetData(id);
        if (w != null) {
            Channel_ID = w.Channel_ID;
            final EditText channelInput = findViewById(R.id.ChannelID);
            channelInput.setText(w.Channel_ID);

            API_Key = w.API_Key;
            final EditText apiInput = findViewById(R.id.APIKey);
            apiInput.setText(w.API_Key);

            setData();
        }
    }

    void metaListener() {
        // Switch
        final Switch metaSwitch = findViewById(R.id.metaAlertSwitch);
        final LinearLayout metaInputLayer = findViewById(R.id.metaAlertInputLayer);
        metaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    metaInputLayer.setVisibility(View.VISIBLE);
                    metaAlert = true;
                } else {
                    metaInputLayer.setVisibility(View.GONE);
                    metaAlert = false;
                }
            }
        });

        // Input text
        final EditText timeoutSeeker = findViewById(R.id.metaAlertInput);

        timeoutSeeker.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                metaAlertString = timeoutSeeker.getText().toString();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    void timeoutListener() {
        // Switch
        final Switch timeoutSwitch = findViewById(R.id.timeoutAlertSwitch);
        final LinearLayout timoutSeekerLayout = findViewById(R.id.timeoutSeekerLayout);
        timeoutSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    timoutSeekerLayout.setVisibility(View.VISIBLE);
                    timeoutAlert = true;
                } else {
                    timoutSeekerLayout.setVisibility(View.GONE);
                    timeoutAlert = false;
                }
            }
        });

        // Seek bar
        SeekBar timeoutSeeker = findViewById(R.id.timoutSeeker);
        final TextView timeoutSeekerText = findViewById(R.id.timeoutSeekerText);

        timeoutSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeoutSeekerText.setText(progress + " min");
                timeoutAlertMinutes = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    void fieldListSelectionListener() {
        Spinner[] spinners = new Spinner[]{
                findViewById(R.id.spinner1),
                findViewById(R.id.spinner2),
                findViewById(R.id.spinner3),
                findViewById(R.id.spinner4),
                findViewById(R.id.spinner5),
                findViewById(R.id.spinner6),
                findViewById(R.id.spinner7),
                findViewById(R.id.spinner8)
        };
        final LinearLayout[] fieldOptions = new LinearLayout[]{
                findViewById(R.id.fieldOptions1),
                findViewById(R.id.fieldOptions2),
                findViewById(R.id.fieldOptions3),
                findViewById(R.id.fieldOptions4),
                findViewById(R.id.fieldOptions5),
                findViewById(R.id.fieldOptions6),
                findViewById(R.id.fieldOptions7),
                findViewById(R.id.fieldOptions8)
        };
        for (int i = 0; i < 8; i++) {
            final int index = i;
            spinners[i].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (position == 0) {
                        fieldOptions[index].setVisibility(View.GONE);
                    } else {
                        fieldOptions[index].setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    fieldOptions[index].setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * Get the Id of Current Widget from the intent of the Widget
     **/
    void getIdOfCurrentWidget(Bundle savedInstanceState) {

        // get the appwidget id from the intent
        Intent intent = getIntent();
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        // make the result intent and set the result to canceled
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(RESULT_CANCELED, resultValue);

        Log.e("Widget ID", "" + id);
        // if we weren't started properly, finish here
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "Open App through a widget", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void seekBarListener() {
        SeekBar seekBar = findViewById(R.id.UpdateTimeBar);
        final TextView seekBarValue = findViewById(R.id.UpdateTimeBarText);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue.setText(String.valueOf(progress + 1));
                updateInterval = progress + 1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void channelInputListener() {
        final EditText channelInput = findViewById(R.id.ChannelID);
        channelInput.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                changeChannelName("");
                changeFieldList(empty);
                Channel_ID = channelInput.getText().toString();
                setData();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    public void serverInputListener() {
        final EditText serverInput = findViewById(R.id.serverURL);
        serverInput.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                changeChannelName("");
                changeFieldList(empty);
                serverURL = serverInput.getText().toString();
                setData();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    public void apiInputListener() {
        final EditText apiInput = findViewById(R.id.APIKey);
        apiInput.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                changeChannelName("");
                changeFieldList(empty);
                API_Key = apiInput.getText().toString();
                setData();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    public void doneButtonListener() {
        Button doneButton = findViewById(R.id.DoneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setAlerts();
                setDecimals();
                sendToWidget();
                setRepeating();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    public void cancelButtonListener() {
        Button cancelButton = findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void changeChannelName(String name) {
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.app_widget);
        // Change channel name
        remoteViews.setTextViewText(R.id.ChannelText, name);
        TextView channelText = findViewById(R.id.ChannelNameText);
        channelText.setText(name);
    }

    public void changeFieldList(String[] fieldNames) {
        String[] newFieldList = new String[9];
        for (int i = 0; i < 9; i++) {
            if (i == 0) {
                newFieldList[i] = "-";
            } else {
                newFieldList[i] = i + fieldNames[i - 1];
            }
        }
        Spinner[] spinners = new Spinner[]{
                findViewById(R.id.spinner1),
                findViewById(R.id.spinner2),
                findViewById(R.id.spinner3),
                findViewById(R.id.spinner4),
                findViewById(R.id.spinner5),
                findViewById(R.id.spinner6),
                findViewById(R.id.spinner7),
                findViewById(R.id.spinner8)
        };

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, newFieldList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Spinner s : spinners) {
            if (s != null) {
                s.setAdapter(spinnerArrayAdapter);
            }
        }


    }

    // Returns selected fields in order
    public int[] getFields() {
        int[] fields = {-1, -1, -1, -1, -1, -1, -1, -1};
        Spinner[] spinners = new Spinner[]{
                findViewById(R.id.spinner1),
                findViewById(R.id.spinner2),
                findViewById(R.id.spinner3),
                findViewById(R.id.spinner4),
                findViewById(R.id.spinner5),
                findViewById(R.id.spinner6),
                findViewById(R.id.spinner7),
                findViewById(R.id.spinner8)
        };
        for (int i = 0; i < 8; i++) {
            Spinner s = spinners[i];
            if (s != null && s.getSelectedItemPosition() != 0) {
                fields[i] = s.getSelectedItemPosition();
            }
        }
        return fields;
    }

    boolean[] lowerBoundSet = new boolean[8];
    boolean[] upperBoundSet = new boolean[8];
    double[] lowerBoundValue = new double[8];
    double[] upperBoundValue = new double[8];

    public void setAlerts() {
        Spinner[] spinners = new Spinner[]{
                findViewById(R.id.spinner1),
                findViewById(R.id.spinner2),
                findViewById(R.id.spinner3),
                findViewById(R.id.spinner4),
                findViewById(R.id.spinner5),
                findViewById(R.id.spinner6),
                findViewById(R.id.spinner7),
                findViewById(R.id.spinner8)
        };
        EditText[] lowerText = new EditText[]{
                findViewById(R.id.lowerAlert1),
                findViewById(R.id.lowerAlert2),
                findViewById(R.id.lowerAlert3),
                findViewById(R.id.lowerAlert4),
                findViewById(R.id.lowerAlert5),
                findViewById(R.id.lowerAlert6),
                findViewById(R.id.lowerAlert7),
                findViewById(R.id.lowerAlert8)
        };
        EditText[] upperText = new EditText[]{
                findViewById(R.id.upperAlert1),
                findViewById(R.id.upperAlert2),
                findViewById(R.id.upperAlert3),
                findViewById(R.id.upperAlert4),
                findViewById(R.id.upperAlert5),
                findViewById(R.id.upperAlert6),
                findViewById(R.id.upperAlert7),
                findViewById(R.id.upperAlert8)
        };
        for (int i = 0; i < 8; i++) {
            Spinner s = spinners[i];
            if (s != null && s.getSelectedItemPosition() != 0) {
                if (!lowerText[i].getText().toString().equals("")) {
                    lowerBoundSet[i] = true;
                    lowerBoundValue[i] = Double.parseDouble(lowerText[i].getText().toString());
                } else {
                    lowerBoundSet[i] = false;
                }
                if (!upperText[i].getText().toString().equals("")) {
                    upperBoundSet[i] = true;
                    upperBoundValue[i] = Double.parseDouble(upperText[i].getText().toString());
                } else {
                    upperBoundSet[i] = false;
                }
            }
        }

        WidgetData w = updateService.getWidgetData(id);
        if (w != null) {
            w.setLowerBound(lowerBoundSet.clone(), lowerBoundValue.clone());
            w.setUpperBound(upperBoundSet.clone(), upperBoundValue.clone());
            w.setTimeoutAlert(timeoutAlert, timeoutAlertMinutes);
            w.setMetaAlert(metaAlert, metaAlertString);
        }
    }

    int decimals[] = {-1, -1, -1, -1, -1, -1, -1, -1};

    public void setDecimals() {
        Spinner[] spinners = new Spinner[]{
                findViewById(R.id.spinner1),
                findViewById(R.id.spinner2),
                findViewById(R.id.spinner3),
                findViewById(R.id.spinner4),
                findViewById(R.id.spinner5),
                findViewById(R.id.spinner6),
                findViewById(R.id.spinner7),
                findViewById(R.id.spinner8)
        };
        EditText[] decimalText = new EditText[]{
                findViewById(R.id.decimalPlaces1),
                findViewById(R.id.decimalPlaces2),
                findViewById(R.id.decimalPlaces3),
                findViewById(R.id.decimalPlaces4),
                findViewById(R.id.decimalPlaces5),
                findViewById(R.id.decimalPlaces6),
                findViewById(R.id.decimalPlaces7),
                findViewById(R.id.decimalPlaces8)
        };

        for (int i = 0; i < 8; i++) {
            Spinner s = spinners[i];
            if (s != null && s.getSelectedItemPosition() != 0) {
                if (!decimalText[i].getText().toString().equals("")) {
                    decimals[i] = Integer.parseInt(decimalText[i].getText().toString());
                } else {
                    decimals[i] = -1;
                }
            }
        }

        WidgetData w = updateService.getWidgetData(id);
        if (w != null) {
            w.setDecimalPlaces(decimals);
        }
    }


    void setRepeating() {
        updateService.addWidget(writer, id, serverURL, Channel_ID, API_Key, updateInterval, getFields());

        final AlarmManager manager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        serviceIntent = new Intent(this, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 60000, pendingIntent);

        /*
        updateService = new UpdateService(this);
        serviceIntent = new Intent(this, UpdateService.class);
        if (!isMyServiceRunning(updateService.getClass())) {
            Log.e("MainAct", "Starting service");
            startService(serviceIntent);
        }
        */
    }

    @Override
    protected void onDestroy() {
        if (serviceIntent != null)
            stopService(serviceIntent);
        super.onDestroy();
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    void sendToWidget() {
        if (updateData.equals("netFail")) {
            WidgetData w = updateService.getWidgetData(id);
            if (w != null) {
                w.netFail = true;
            }
        } else {
            Intent intent = new Intent(this, AppWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            intent.putExtra("updateAPIKey", API_Key);
            intent.putExtra("updateChannel", Channel_ID);
            intent.putExtra("updateURL", serverURL);
            intent.putExtra("updateID", id);
            intent.putExtra("updateData", updateData);

            int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AppWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    RetrieveData currentTask = null;

    void setData() {
        if (currentTask != null)
            currentTask.cancel(true);
        currentTask = new RetrieveData();
        currentTask.execute();
    }


    class RetrieveData extends AsyncTask<String, String, String> {


        String ret = null;

        @Override
        protected String doInBackground(String... strings) {

            try {
                String urlString = serverURL + "/channels/" + Channel_ID + "/feed.json?metadata=true&api_key=" + API_Key;

                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                System.out.print("Response code: " + con.getResponseCode());
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
                // Change channel name in app
                if (!ret.equals("netFail")) {
                    try {
                        JSONObject obj = new JSONObject(ret);
                        String channelName = obj.getJSONObject("channel").getString("name");
                        changeChannelName(channelName);
                    } catch (Exception e) {
                        // No channel name
                        changeChannelName("");
                    }

                    // Change field names in lists
                    String[] fieldNames = empty.clone();
                    try {
                        JSONObject obj = new JSONObject(ret);
                        for (int i = 0; i < 8; i++) {
                            fieldNames[i] = ": " + obj.getJSONObject("channel").getString("field" + (i + 1));
                        }
                    } catch (Exception e) {
                        // Field name error
                    }
                    changeFieldList(fieldNames);
                } else {
                    changeChannelName("");
                    changeFieldList(empty);
                }

                updateData = ret;
            } catch (Exception e) {
                e.printStackTrace();
                changeChannelName("");
                changeFieldList(empty);
            }
        }
    }

}
