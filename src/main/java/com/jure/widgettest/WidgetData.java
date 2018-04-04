package com.jure.widgettest;


public class WidgetData {

    public int widgetID;
    public String Channel_ID = "", API_Key = "", serverURL = "http://api.thingspeak.com";
    public String ChannelName = null;
    public int updateInterval;
    public int currentUpdateTime;

    public int decimalPlaces[] = {-1, -1, -1, -1, -1, -1, -1, -1};


    // Timeout variables
    public boolean netFail = false;
    public int timeoutTries = 0;
    public boolean timeoutAlert = false;
    public int timeoutAlertThreshold = 0;

    // Meta alert variables
    public boolean metaAlert = false;
    public String metaAlertString = "";

    // Alert repetition
    public boolean repeatTimeout = false;
    public boolean timeoutAlerted = false;
    public boolean repeatMeta = false;
    public boolean metaAlerted = false;
    public boolean[] boundsAlerted = new boolean[8];
    public boolean[] repeatBounds = new boolean[8];


    // Bound (threshold) variables
    public boolean[] upperBoundAlert = new boolean[8];
    public boolean[] lowerBoundAlert = new boolean[8];
    public double[] upperBound = new double[8];
    public double[] lowerBound = new double[8];

    public int[] fieldsInOrder = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};

    public WidgetData(int widgetID, String serverURL, String Channel_ID, String API_Key, int updateInterval, int[] fieldsInOrder) {
        this.widgetID = widgetID;
        this.Channel_ID = Channel_ID;
        this.API_Key = API_Key;
        this.serverURL = serverURL;
        this.updateInterval = updateInterval;
        currentUpdateTime = updateInterval - 1;
        this.fieldsInOrder = fieldsInOrder;
    }

    public WidgetData() {

    }

    public void init(int widgetID, String serverURL, String Channel_ID, String API_Key, int updateInterval, int[] fieldsInOrder) {
        this.widgetID = widgetID;
        this.Channel_ID = Channel_ID;
        this.API_Key = API_Key;
        this.serverURL = serverURL;
        this.updateInterval = updateInterval;
        currentUpdateTime = updateInterval - 1;
        this.fieldsInOrder = fieldsInOrder;
    }

    public void setUpperBound(boolean[] alerts, double[] values) {
        for (int i = 0; i < 8; i++) {
            upperBoundAlert[i] = alerts[i];
            upperBound[i] = values[i];
        }
    }


    public void setLowerBound(boolean[] alerts, double[] values) {
        for (int i = 0; i < 8; i++) {
            lowerBoundAlert[i] = alerts[i];
            lowerBound[i] = values[i];
        }
    }

    public void setTimeoutAlert(boolean alert, int value) {
        timeoutAlert = alert;
        timeoutAlertThreshold = value;
    }

    public void setMetaAlert(boolean alert, String metaString) {
        metaAlert = alert;
        metaAlertString = metaString;
    }

    public void setDecimalPlaces(int[] dec) {
        decimalPlaces = dec.clone();
    }

    public void setDecimalPlaces(String decString) {
        int i = 0;
        for (String s : decString.split(",")) {
            try {
                decimalPlaces[i] = Integer.parseInt(s);
            } catch (Exception e) {
            }
            i++;
        }
    }

    public void setRepeatBounds(String s) {
        String[] spl = s.split(",");
        for (int i = 0; i < spl.length; i++) {
            try {
                repeatBounds[i] = Boolean.parseBoolean(spl[i]);
            } catch (Exception e) {
            }
        }
    }

    public void setBoundsAlerted(String s) {
        String[] spl = s.split(",");
        for (int i = 0; i < spl.length; i++) {
            try {
                boundsAlerted[i] = Boolean.parseBoolean(spl[i]);
            } catch (Exception e) {
            }
        }
    }

    public void setBounds(String upperAlert, String lowerAlert, String upper, String lower) {
        String[] uas = upperAlert.split(",");
        String[] las = lowerAlert.split(",");
        String[] us = upper.split(",");
        String[] ls = lower.split(",");

        for (int i = 0; i < 8; i++) {
            try {
                if (i < uas.length)
                    upperBoundAlert[i] = Boolean.parseBoolean(uas[i]);
                if (i < las.length)
                    lowerBoundAlert[i] = Boolean.parseBoolean(las[i]);
                if (i < us.length)
                    upperBound[i] = Double.parseDouble(us[i]);
                if (i < ls.length)
                    lowerBound[i] = Double.parseDouble(ls[i]);
            } catch (Exception e) {
            }
        }
    }

    public void setFieldsInOrder(String fieldString) {
        String[] fs = fieldString.split(",");
        for (int i = 0; i < 8; i++) {
            try {
                if (i < fs.length)
                    fieldsInOrder[i] = Integer.parseInt(fs[i]);
            } catch (Exception e) {
            }
        }
    }

    public void setBoundsRepeat(boolean[] alertRepeat) {
        repeatBounds = alertRepeat.clone();
    }

    public String formatDecimals() {
        String r = "";
        for (int i : decimalPlaces) {
            r += i + ",";
        }
        return r;
    }

    public String formatRepeatBounds() {
        String r = "";
        for (boolean b : repeatBounds) {
            r += b + ",";
        }
        return r;
    }

    public String formatBoundsAlerted() {
        String r = "";
        for (boolean b : boundsAlerted) {
            r += b + ",";
        }
        return r;
    }

    public String formatUpperBoundAlert() {
        String r = "";
        for (boolean b : upperBoundAlert) {
            r += b + ",";
        }
        return r;
    }

    public String formatLowerBoundAlert() {
        String r = "";
        for (boolean b : lowerBoundAlert) {
            r += b + ",";
        }
        return r;
    }

    public String formatUpperBound() {
        String r = "";
        for (double i : upperBound) {
            r += i + ",";
        }
        return r;
    }

    public String formatLowerBound() {
        String r = "";
        for (double i : lowerBound) {
            r += i + ",";
        }
        return r;
    }

    public String formatFieldsInOrder() {
        String r = "";
        for (int i : fieldsInOrder) {
            r += i + ",";
        }
        return r;
    }


}
