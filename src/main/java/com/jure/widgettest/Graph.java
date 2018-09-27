package com.jure.widgettest;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Graph extends Activity {

    DataWriter writer;
    int id; // Widget ID

    int fields = 0;
    DataPoint[][] fieldDataPoints;
    String[] fieldNames;

    int colors[] = {Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.BLACK, Color.CYAN, Color.YELLOW, Color.GRAY};

    @SuppressWarnings("unchecked")
    LineGraphSeries<DataPoint>[] allSeries = new LineGraphSeries[8];

    GraphView graph;

    CheckBox[] checkBoxes;

    Date firstTime = null;

    WidgetData widgetData;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Create the data writer
        writer = new DataWriter(this);

        // Radio button listener
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) findViewById(checkedId);
                changeTime(rb.getText().toString());
            }
        });

        // Get the id of current widget
        getIdOfCurrentWidget(savedInstanceState);

        // Load the widget data of current widget
        widgetData = writer.loadWidgetData(id);
        context = this;
        processData(context, widgetData.latestData);

        // Checkboxes
        checkboxInit();

        // Create series
        for (int i = 0; i < fields; i++) {
            Log.e("Field " + i, "Points " + fieldDataPoints[i].length);
            if (fieldDataPoints[i] != null && fieldDataPoints[i].length > 0) {
                allSeries[i] = new LineGraphSeries<>(fieldDataPoints[i]);
                allSeries[i].setTitle(fieldNames[i]);
                allSeries[i].setDrawDataPoints(true);
                allSeries[i].setDataPointsRadius(10);
            }
        }

        graph = (GraphView) findViewById(R.id.graph);
        graph.setBackgroundColor(getResources().getColor(R.color.appBackground));
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(false); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(false); // enables vertical zooming and scrolling
        graph.setTitle(widgetData.ChannelName); // Title
        graph.setTitleTextSize(40);
        graph.setTitleColor(Color.parseColor("#222222"));
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Date date = new Date((long) value);
                    return date.getHours() + ":" + date.getMinutes();
                } else
                    return super.formatLabel(value, isValueX);

            }
        });

        checkBoxes[0].setChecked(true);
        hideSystemUI();
    }

    void changeTime(String text) {
        if (text.equalsIgnoreCase("all")) {
            firstTime = null;
        } else if (text.equalsIgnoreCase("month")) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            firstTime = calendar.getTime();
        } else if (text.equalsIgnoreCase("week")) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -7);
            firstTime = calendar.getTime();
        } else if (text.equalsIgnoreCase("day")) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            firstTime = calendar.getTime();
        } else if (text.equalsIgnoreCase("hour")) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -1);
            firstTime = calendar.getTime();
        } else {
            firstTime = null;
        }
        processData(context, widgetData.latestData);
        setSeries();
    }

    /**
     * Get the Id of Current Widget from the intent of the Widget
     **/
    void getIdOfCurrentWidget(Bundle savedInstanceState) {

        // get the appwidget id from the intent
        Intent intent = getIntent();
        Log.e("INTENT: ", intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) + "");
        id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        Log.e("Widget ID", "" + id);
        // if we weren't started properly, finish here
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "Open App through a widget", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void processData(Context context, String updateData) {
        try {
            int numberOfFields = 8;
            fieldNames = new String[8];
            JSONObject data = new JSONObject(updateData);
            // Get field names
            for (int i = 1; i <= 8; i++) {
                try {
                    String fieldName = data.getJSONObject("channel").getString("field" + i);
                    //System.out.println("field" + i + ": " + fieldName);
                    fieldNames[i - 1] = fieldName;
                } catch (Exception e) {
                    if (numberOfFields > i - 1)
                        numberOfFields = i - 1;
                    // Field does not exist
                    //System.out.println("field" + i + ": " + " - ");
                    fieldNames[i - 1] = "-";
                }
            }
            fields = numberOfFields;

            try {
                JSONArray feeds = data.getJSONArray("feeds");
                Log.e("FEEDS", "" + feeds.length());
                fieldDataPoints = new DataPoint[8][];
                List<ArrayList<DataPoint>> temp = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    temp.add(new ArrayList<DataPoint>());
                }

                for (int i = 0; i < feeds.length(); i++) {
                    for (int j = 1; j <= numberOfFields; j++) {
                        try {
                            String numberString = feeds.getJSONObject(i).getString("field" + j);
                            if (numberString != null && isDouble(numberString)) {
                                double value = Double.parseDouble(numberString);
                                String time = feeds.getJSONObject(i).getString("created_at");
                                Calendar calendar = toCalendar(time);
                                double millis = calendar.getTimeInMillis();
                                if (firstTime == null || firstTime.before(calendar.getTime())) {
                                    // fieldDataPoints[j - 1][index[j - 1]] = new DataPoint(millis, value);
                                    temp.get(j - 1).add(new DataPoint(millis, value));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                for (int i = 0; i < 8; i++) {
                    if (temp.get(i).isEmpty()) {
                        fieldDataPoints[i] = new DataPoint[0];
                    } else {
                        fieldDataPoints[i] = temp.get(i).toArray(new DataPoint[temp.get(i).size()]);
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

    private boolean isDouble(String numberString) {
        try{
            Double.parseDouble(numberString);
            return true;
        }catch (Exception e){
            return false;
        }
    }


    void setSeries() {
        graph.removeAllSeries();
        boolean init = false;
        double minX = 0, maxX = 0, minY = 0, maxY = 0;
        int ci = 0;
        for (int i = 0; i < fields; i++) {
            checkBoxes[i].setTextColor(Color.BLACK);
            if (checkBoxes[i].isChecked() && allSeries[i] != null) {
                allSeries[i].setColor(colors[ci]);
                allSeries[i].setAnimated(true);
                checkBoxes[i].setTextColor(colors[ci]);
                ci++;
                graph.addSeries(allSeries[i]);
                for (int j = 0; j < fieldDataPoints[i].length; j++) {
                    DataPoint d = fieldDataPoints[i][j];
                    if (!init) {
                        if (d != null) {
                            init = true;
                            minX = d.getX();
                            maxX = d.getX();
                            minY = d.getY();
                            maxY = d.getY();
                        }
                    } else {
                        if (minX > d.getX())
                            minX = d.getX();
                        if (maxX < d.getX())
                            maxX = d.getX();
                        if (minY > d.getY())
                            minY = d.getY();
                        if (maxY < d.getY())
                            maxY = d.getY();
                    }
                }
            }
        }
        graph.getViewport().setMinX(minX);
        graph.getViewport().setMaxX(maxX);
        graph.getViewport().setMinY(minY);
        graph.getViewport().setMaxY(maxY);
        //graph.getViewport().setMinimalViewport(minX, maxX, minY, maxY);
    }

    void checkboxInit() {
        checkBoxes = new CheckBox[]{findViewById(R.id.field1),
                findViewById(R.id.field2),
                findViewById(R.id.field3),
                findViewById(R.id.field4),
                findViewById(R.id.field5),
                findViewById(R.id.field6),
                findViewById(R.id.field7),
                findViewById(R.id.field8)};
        for (int i = 0; i < fields; i++) {
            checkBoxes[i].setVisibility(View.VISIBLE);
            checkBoxes[i].setText(fieldNames[i]);
            checkBoxes[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    processData(context, widgetData.latestData);
                    setSeries();
                }
            });
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= 19) {
            flags = flags | View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            flags = flags | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            flags = flags | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            flags = flags | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags = flags | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        decorView.setSystemUiVisibility(flags);
    }


    Calendar toCalendar(final String iso8601string) {
        Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");
        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            Date date = df.parse(s);
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
