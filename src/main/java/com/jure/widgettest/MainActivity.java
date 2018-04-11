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

    /*
        This activity handles the input of the user data and starts
        the update service for each individual widget.
     */


    UpdateService updateService;
    DataWriter writer;
    Intent serviceIntent;

    // Server data
    private String Channel_ID = "", API_Key = "", serverURL = "http://api.thingspeak.com";

    // Update frequency in minutes
    private int updateInterval = 1;

    // Data we get from server
    String updateData = "";

    // The id of the current widget
    int id;

    // An array of empty strings used to reset field values
    String[] empty = {"", "", "", "", "", "", "", ""};

    // Timeout data
    boolean timeoutAlert = false; // Is timeout alert on
    boolean timeoutRepeat = false; // Is it on repeat
    int timeoutAlertMinutes = 0; // How many minutes of timeout before we alert

    // Alerts through meta data
    boolean metaAlert = false; // Is meta alert on
    boolean metaRepeat = false; // Is it on repeat
    String metaAlertString = ""; // The string that triggers the alert

    // Repeat values for all 8 fields
    boolean[] alertRepeat = new boolean[8];

    // The data object of the current widget
    WidgetData currentWidgetData;


    // Fields, spinners and switches of all options
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
    final Switch[] repeatSwitches = {
            findViewById(R.id.boundsRepeat1),
            findViewById(R.id.boundsRepeat2),
            findViewById(R.id.boundsRepeat3),
            findViewById(R.id.boundsRepeat4),
            findViewById(R.id.boundsRepeat5),
            findViewById(R.id.boundsRepeat6),
            findViewById(R.id.boundsRepeat7),
            findViewById(R.id.boundsRepeat8)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the data writer
        writer = new DataWriter(this);

        // Create the update service
        updateService = new UpdateService(this);

        // Get the id of current widget
        getIdOfCurrentWidget(savedInstanceState);

        // Load the widget data of current widget
        currentWidgetData = updateService.widgetDataFromPreferences(writer.sharedPreferences, id);

        // Set default URL of server if widget does not contain a server URL
        if (currentWidgetData.serverURL.isEmpty())
            currentWidgetData.serverURL = serverURL;

        // Start listeners for all input fields, buttons, etc.
        seekBarListener();
        timeoutListener();
        metaListener();
        repeatListener();
        channelInputListener();
        apiInputListener();
        serverInputListener();
        fieldListSelectionListener();
        doneButtonListener();
        cancelButtonListener();

        // Restore widget options from previous sessions
        setWidgetOptions();
    }

    // Restores widget options from previous sessions
    void setWidgetOptions() {
        WidgetData w = currentWidgetData;
        if (w != null) {

            // Restore channel id
            Channel_ID = w.Channel_ID;
            final EditText channelInput = findViewById(R.id.ChannelID);
            channelInput.setText(w.Channel_ID);

            // Restore API Key
            API_Key = w.API_Key;
            final EditText apiInput = findViewById(R.id.APIKey);
            apiInput.setText(w.API_Key);

            // Restore update frequency bar
            final SeekBar updateIntervalBar = findViewById(R.id.UpdateTimeBar);
            updateIntervalBar.setProgress(w.updateInterval - 1);

            // Restore timeout alert options
            final Switch timeoutAlert = findViewById(R.id.timeoutAlertSwitch);
            timeoutAlert.setChecked(w.timeoutAlert);
            final Switch timeoutAlertRepeat = findViewById(R.id.repeatTimeoutAlert);
            timeoutAlertRepeat.setChecked(w.repeatTimeout);
            final SeekBar timeoutAlertDelay = findViewById(R.id.timoutSeeker);
            timeoutAlertDelay.setProgress(w.timeoutAlertThreshold);

            // Restore meta alert options
            final Switch metaAlert = findViewById(R.id.metaAlertSwitch);
            metaAlert.setChecked(w.metaAlert);
            final Switch repeatMeta = findViewById(R.id.repeatMetaAlert);
            repeatMeta.setChecked(w.repeatMeta);
            final EditText metaString = findViewById(R.id.metaAlertInput);
            metaString.setText(w.metaAlertString);

            // Restore server URL
            final EditText serverURL = findViewById(R.id.serverURLInput);
            serverURL.setText(w.serverURL);


            // Restore options of all 8 fields
            for (int i = 0; i < 8; i++) {
                int selection = (w.fieldsInOrder[i] >= 0) ? w.fieldsInOrder[i] : 0;
                spinners[i].setSelection(selection);
                if (w.upperBoundAlert[i])
                    upperText[i].setText(w.upperBound[i] + "");
                if (w.lowerBoundAlert[i])
                    lowerText[i].setText(w.lowerBound[i] + "");
                if (w.decimalPlaces[i] >= 0)
                    decimalText[i].setText(w.decimalPlaces[i] + "");
                repeatSwitches[i].setChecked(w.repeatBounds[i]);
            }
        }
    }

    // Sets the listener for meta alert options
    void metaListener() {
        // On/off switch
        final Switch metaSwitch = findViewById(R.id.metaAlertSwitch);
        final LinearLayout metaInputLayer = findViewById(R.id.metaAlertInputLayer);
        metaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Make the options visible only when checked
                if (isChecked) {
                    metaInputLayer.setVisibility(View.VISIBLE);
                    metaAlert = true;
                } else {
                    metaInputLayer.setVisibility(View.GONE);
                    metaAlert = false;
                }
            }
        });

        // Input trigger stirng
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

        // Repeat switch
        final Switch metaAlertRepeat = findViewById(R.id.repeatMetaAlert);
        metaAlertRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    metaRepeat = true;
                } else {
                    metaRepeat = false;
                }
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

        // Repeat
        final Switch timeoutAlertRepeat = findViewById(R.id.repeatTimeoutAlert);
        timeoutAlertRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    timeoutRepeat = true;
                } else {
                    timeoutRepeat = false;
                }
            }
        });
    }

    void repeatListener() {
        // Switches
        final Switch[] repeatSwitches = {
                findViewById(R.id.boundsRepeat1),
                findViewById(R.id.boundsRepeat2),
                findViewById(R.id.boundsRepeat3),
                findViewById(R.id.boundsRepeat4),
                findViewById(R.id.boundsRepeat5),
                findViewById(R.id.boundsRepeat6),
                findViewById(R.id.boundsRepeat7),
                findViewById(R.id.boundsRepeat8)
        };
        for (int i = 0; i < 8; i++) {
            final int index = i;
            repeatSwitches[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        alertRepeat[index] = true;
                    } else {
                        alertRepeat[index] = false;
                    }
                }
            });
        }
    }

    void fieldListSelectionListener() {
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
        final EditText serverInput = findViewById(R.id.serverURLInput);
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
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, newFieldList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (Spinner s : spinners) {
            if (s != null) {
                int selected = s.getSelectedItemPosition();
                s.setAdapter(spinnerArrayAdapter);
                s.setSelection(selected);
            }
        }


    }

    // Returns selected fields in order
    public int[] getFields() {
        int[] fields = {-1, -1, -1, -1, -1, -1, -1, -1};
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

    boolean[] repeat = new boolean[8];

    public void setAlerts() {
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

        WidgetData w = currentWidgetData;
        if (w != null) {
            Log.e("DATA", "DATA");
            w.setLowerBound(lowerBoundSet.clone(), lowerBoundValue.clone());
            w.setUpperBound(upperBoundSet.clone(), upperBoundValue.clone());
            w.setTimeoutAlert(timeoutAlert, timeoutAlertMinutes);
            w.setMetaAlert(metaAlert, metaAlertString);
            w.setBoundsRepeat(alertRepeat);
            w.repeatTimeout = timeoutRepeat;
            w.repeatMeta = metaRepeat;
        }
    }

    int decimals[] = {-1, -1, -1, -1, -1, -1, -1, -1};

    public void setDecimals() {
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

        WidgetData w = currentWidgetData;
        if (w != null) {
            w.setDecimalPlaces(decimals);
        }
    }


    void setRepeating() {
        updateService.addWidget(writer, currentWidgetData);
        updateService.saveWidgetData(writer, currentWidgetData, true);
        final AlarmManager manager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        serviceIntent = new Intent(this, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 60000, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        if (serviceIntent != null)
            stopService(serviceIntent);
        super.onDestroy();
    }

    void sendToWidget() {
        currentWidgetData.init(id, serverURL, Channel_ID, API_Key, updateInterval, getFields());
        if (updateData.equals("netFail")) {
            WidgetData w = currentWidgetData;
            if (w != null) {
                w.netFail = true;
            }
        } else {
            Log.e("Widget update", "Application call.");
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
