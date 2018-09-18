package com.jure.widgettest;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Graph extends Activity {

    DataWriter writer;
    int id; // Widget ID

    int fields = 0;
    DataPoint[][] fieldDataPoints;
    String[] fieldNames;

    int colors[] = {Color.BLUE,Color.RED, Color.GREEN, Color.MAGENTA, Color.BLACK, Color.CYAN, Color.YELLOW, Color.GRAY};

    @SuppressWarnings("unchecked")
    LineGraphSeries<DataPoint>[] allSeries = new LineGraphSeries[8];

    GraphView graph;

    CheckBox[] checkBoxes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Create the data writer
        writer = new DataWriter(this);

        // Get the id of current widget
        getIdOfCurrentWidget(savedInstanceState);

        // Load the widget data of current widget
        WidgetData widgetData = writer.loadWidgetData(id);
        processData(this, widgetData.latestData);

        // Checkboxes
        checkboxInit();

        // Create series
        for (int i = 0; i < fields; i++) {
            if (fieldDataPoints[i].length > 0) {
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
                    Date date = new Date((int) value);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                    return sdf.format(date);
                } else
                    return super.formatLabel(value, isValueX);

            }
        });

        checkBoxes[0].setChecked(true);
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
                fieldDataPoints = new DataPoint[8][feeds.length()];

                for (int i = 0; i < feeds.length(); i++) {
                    for (int j = 1; j <= numberOfFields; j++) {
                        try {
                            double value = Double.parseDouble(feeds.getJSONObject(i).getString("field" + j));
                            String time = feeds.getJSONObject(i).getString("created_at").split("T")[1];
                            time = time.substring(0, time.length() - 1);
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                            calendar.setTime(sdf.parse(time));
                            double millis = calendar.getTimeInMillis();
                            fieldDataPoints[j - 1][i] = new DataPoint(millis, value);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

    void setSeries() {
        graph.removeAllSeries();
        boolean init = false;
        double minX = 0, maxX = 0, minY = 0, maxY = 0;
        int ci = 0;
        for (int i = 0; i < fields; i++) {
            checkBoxes[i].setTextColor(Color.BLACK);
            if (checkBoxes[i].isChecked()) {
                allSeries[i].setColor(colors[ci]);
                allSeries[i].setAnimated(true);
                checkBoxes[i].setTextColor(colors[ci]);
                ci++;
                graph.addSeries(allSeries[i]);
                for (int j = 0; j < fieldDataPoints[i].length; j++) {
                    DataPoint d = fieldDataPoints[i][j];
                    if (!init) {
                        init = true;
                        minX = d.getX();
                        maxX = d.getX();
                        minY = d.getY();
                        maxY = d.getY();
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
        System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
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
                    setSeries();
                }
            });
        }
    }

}
