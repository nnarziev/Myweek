package com.example.negmat.myweek_1;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Nematjon on 10/5/2017.
 */

public class Event {

    //region Variables

    //region Constant variables
    private final String  MODE_REPEAT = "MODE_REPEAT"; //static
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

    //region original Constructor
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
    //endregion

    //region Constructor for fetch event
    public Event(String username, int start_time, int repeat_mode, short length, String event_name, String event_note, long event_id) {
        this.username = username;
        this.start_time = start_time;
        this.repeat_mode = repeat_mode;
        this.length = length;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
    }
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

    public static Event parseJson(JSONObject data){

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
