package com.example.negmat.myweek;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

//Initialization of Tools
class Tools {
    static final String PREFS_NAME = "UserLogin";
    static final short RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;

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
                String res = "";
                BufferedInputStream is = new BufferedInputStream(con.getInputStream());
                while ((rd = is.read(buf)) > 0)
                    res += new String(buf, 0, rd, "utf-8");
                is.close();
                con.disconnect();
                return res;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

class Event {

    public Event(String username, int start_time, int repeat_mode, short length, Boolean is_active, String event_name, String event_note, long event_id, String reason) {
        this.username = username;
        this.start_time = start_time;
        this.repeat_mode = repeat_mode;
        this.length = length;
        this.is_active = is_active;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
        this.reason = reason;
    }

    public Event(String username, int start_time, int repeat_mode, short length, String event_name, String event_note, long event_id) {
        this.username = username;
        this.start_time = start_time;
        this.repeat_mode = repeat_mode;
        this.length = length;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
    }

    //region Variables

    //region Constant variables
    private final String MODE_REPEAT = "MODE_REPEAT"; //static
    private final String MODE_SINGLE = "MODE_SINGLE"; //dynamic
    //endregion

    private String username;
    private int start_time;
    private int repeat_mode;
    private short length;
    private Boolean is_active;
    private String event_name = "";
    private String event_note = "";
    private long event_id;
    private String reason;
    //endregion

    //region Setters and Getters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStart_time() {
        return start_time;
    }

    public void setStart_time(int start_time) {
        this.start_time = start_time;
    }

    public int getRepeat_mode() {
        return repeat_mode;
    }

    public void setRepeat_mode(int repeat_mode) {
        this.repeat_mode = repeat_mode;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }

    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public String getEvent_note() {
        return event_note;
    }

    public void setEvent_note(String event_note) {
        this.event_note = event_note;
    }

    public long getEvent_id() {
        return event_id;
    }

    public void setEvent_id(long event_id) {
        this.event_id = event_id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    //endregion

    static Event parseJson(JSONObject data) {

        String username;
        int start_time;
        int repeat_mode;
        String event_name;
        String event_note;
        long event_id;
        short length;
        try {
            username = data.getString("username");
            start_time = data.getInt("start_time");
            repeat_mode = data.getInt("repeat_mode");
            event_name = data.getString("event_name");
            event_note = data.getString("event_note");
            event_id = data.getLong("event_id");
            length = (short) data.getInt("length");
            Event obj = new Event(username, start_time, repeat_mode, length, event_name, event_note, event_id);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }
}
