package com.example.negmat.myweek;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;


class Tools {
    // region Server Constants
    static final int RES_OK = 0, RES_SRV_ERR = -1, RES_FAIL = 1;
    // endregion

    // region Client Constants
    static final int
            NFC_SINGLE = 0,
            NFC_GROUP = 1;
    static final String
            KEY_NFC_SINGLE = "NFC_SINGLE",
            KEY_NFC_GROUP = "NFC_GROUP";
    // endregion

    static String post(String _url, JSONObject json_body) {
        try {
            URL url = new URL(_url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(json_body != null);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.connect();

            if (json_body != null) {
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(json_body.toString());
                wr.flush();
                wr.close();
            }

            int status = con.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                con.disconnect();
                return null;
            } else {
                byte[] buf = new byte[1024];
                int rd;
                StringBuilder sb = new StringBuilder();
                BufferedInputStream is = new BufferedInputStream(con.getInputStream());
                while ((rd = is.read(buf)) > 0)
                    sb.append(new String(buf, 0, rd, "utf-8"));
                is.close();
                con.disconnect();
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // region Time formatting
    static String[] decode_time(Calendar calendar) {
        String time_part = String.format(Locale.US, "%02d:00", calendar.get(Calendar.HOUR));
        String date_part = String.format(Locale.US, "%s. %d, %d",
                calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.YEAR)
        );

        return new String[]{date_part, time_part};
    }

    static String[] decode_time(int time) {
        Calendar cal = time2cal(time);
        String time_part = String.format(Locale.US, "%02d:00", cal.get(Calendar.HOUR_OF_DAY));
        String date_part = String.format(Locale.US, "%s. %d, %d",
                cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.YEAR)
        );

        return new String[]{date_part, time_part};
    }

    static int suggestion2time(int suggestion) {
        return cal2time(suggestion2cal(suggestion));
    }

    static Calendar suggestion2cal(int suggestion) {
        // create calendar on current day
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), suggestion / 10, 0, 0);

        // shift date to closest match suggested weekday
        while (calendar.get(Calendar.DAY_OF_WEEK) != suggestion % 10)
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        return calendar;
    }

    static int cal2time(Calendar c) {
        return Integer.parseInt(String.format(Locale.US,
                "%02d%02d%02d%02d",
                c.get(Calendar.YEAR) % 100,
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY)
        ));
    }

    static Calendar time2cal(int time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(2000 + (time / 1000000) % 100, (time / 10000) % 100 - 1, (time / 100) % 100, time % 100, 0, 0);

        return calendar;
    }

    static int alter_date(int origin, int year, int month, int day) {
        return year % 100 * 1000000 + month * 10000 + day * 100 + origin % 100;
    }

    static int alter_hour(int origin, int hour) {
        return origin / 100 * 100 + hour;
    }

    static void disable_touch(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    static void enable_touch(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
    // endregion

    static void setAlarm(Activity activity, Calendar when, String event_name, String event_note) {
        when = (Calendar) when.clone();

        AlarmManager manager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent;
        PendingIntent pendingIntent;

        myIntent = new Intent(activity, MainActivity.AlarmNotificationReceiver.class);
        myIntent.putExtra("event_name", event_name);
        myIntent.putExtra("event_note", event_note);
        pendingIntent = PendingIntent.getBroadcast(activity, 0, myIntent, 0);
        when.add(Calendar.MINUTE, -2);
        if (manager != null) {
            manager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pendingIntent);
        } else
            Log.e("ERROR", "manager is null");
    }
}

class Event {
    public Event(int start_time, int day, short length, String event_name, String event_note, long event_id, int category_id) {
        this.start_time = start_time;
        this.day = day;
        this.length = length;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
        this.category_id = category_id;
    }

    // region Constants
    static final int NEW_EVENT = 0;
    // endregion

    //region Variables
    int start_time;
    int day;
    short length;
    String event_name = "";
    String event_note = "";
    int category_id;
    long event_id;
    //endregion

    static Event parseJson(JSONObject data) {
        int start_time;
        int repeat_mode;
        String event_name;
        String event_note;
        int ev_cat_id;
        long event_id;
        short length;

        try {
            start_time = data.getInt("start_time");
            repeat_mode = data.getInt("day");
            event_name = data.getString("event_name");
            event_note = data.getString("event_note");
            event_id = data.getLong("event_id");
            ev_cat_id = data.getInt("category_id");
            length = (short) data.getInt("length");

            return new Event(start_time, repeat_mode, length, event_name, event_note, event_id, ev_cat_id);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    JSONObject toJson(boolean newInstance) {
        try {
            return new JSONObject()
                    .put("start_time", start_time)
                    .put("day", day)
                    .put("length", length)
                    .put("event_name", event_name)
                    .put("event_note", event_note)
                    .put("event_id", newInstance ? NEW_EVENT : event_id)
                    .put("category_id", category_id);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

abstract class MyRunnable implements Runnable {
    MyRunnable(Object... args) {
        this.args = Arrays.copyOf(args, args.length);
    }

    Object[] args;
}