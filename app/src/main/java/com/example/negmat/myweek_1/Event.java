package com.example.negmat.myweek_1;

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
    private String start_time;
    private String repeat_mode;
    private Boolean is_active;
    private String event_name = "";
    private String event_note = "";
    private long event_id;
    private String reason;
    //endregion

    //region Constructor

    public Event(String username, String start_time, String repeat_mode, Boolean is_active,
                 String event_name, String event_note, long event_id, String reason) {
        this.username = username;
        this.start_time = start_time;
        this.repeat_mode = repeat_mode;
        this.is_active = is_active;
        this.event_name = event_name;
        this.event_note = event_note;
        this.event_id = event_id;
        this.reason = reason;
    }

    //endregion

    //region Setters and Getters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getRepeat_mode() {
        return repeat_mode;
    }

    public void setRepeat_mode(String repeat_mode) {
        this.repeat_mode = repeat_mode;
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

    //endregion\

}
