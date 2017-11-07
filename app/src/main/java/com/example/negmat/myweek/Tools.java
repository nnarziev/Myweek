package com.example.negmat.myweek;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

//Initialization of Tools
class Tools {
    static final short
            RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;

    static final short
            MON = 0b0100000,
            TUE = 0b0010000,
            WED = 0b0001000,
            THU = 0b0000100,
            FRI = 0b0000010,
            SAT = 0b0000001,
            SUN = 0b1000000;

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

    static String[] eventDateTimeToString(int event_time) {
        // calculate time part (hh:mm)
        short time = (short) (event_time % 10000 / 100);
        String timeStr = String.format(Locale.US, "%d:00", time);

        // calculate date part (mmm. dd, yy)
        short day = (short) (event_time % 1000000 / 10000);
        short month = (short) (event_time % 100000000 / 1000000);
        short year = (short) (event_time / 100000000);
        Calendar cal = Calendar.getInstance();
        cal.set(year + 2000, month, day);
        String dateStr = String.format(Locale.US, "%s. %d, %d", cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), day, year + 2000);

        // return as a sequenced array
        return new String[]{dateStr, timeStr};
    }
}

class Event {
    public Event(int start_time, int repeat_mode, short length, String event_name, String event_note, long event_id, int category_id) {
        this.start_time = start_time;
        this.repeat_mode = repeat_mode;
        this.length = length;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
        this.category_id = category_id;
    }

    //region Variables
    int start_time;
    int repeat_mode;
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
            repeat_mode = data.getInt("repeat_mode");
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
}
