package com.example.negmat.myweek;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import net.gotev.speech.Logger;

import org.json.JSONObject;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConfirmEventDialog extends DialogFragment {

    // region Variables
    final String[] select_day = {"Select the day(s)", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public String event_name;
    public String event_note = "";
    public int cat_id;
    public int event_time;

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

    public int getCat_id() {
        return cat_id;
    }

    public void setCat_id(int cat_id) {
        this.cat_id = cat_id;
    }

    public int getEvent_time() {
        return event_time;
    }

    public void setEvent_time(int event_time) {
        this.event_time = event_time;
    }

    @BindView(R.id.btn_save)
    Button btnSave;
    @BindView(R.id.btn_delete)
    Button btnDelete;
    @BindView(R.id.txt_event_name)
    EditText txtEventName;
    @BindView(R.id.txt_event_time)
    TextView txtEventTime;
    @BindView(R.id.txt_event_note)
    EditText txtEventNote;
    // endregion

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.diaog_confirmevent, container, false);
        ButterKnife.bind(this, view);
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        txtEventName.setText(getEvent_name());
        txtEventTime.setText(String.valueOf(getEvent_time()));
        txtEventNote.setText(event_note);

        return view;
    }

    @OnClick(R.id.btn_save)
    public void save() {
        createEvent(120, (short) 60, true);
    }

    @OnClick(R.id.btn_delete)
    public void delete() {

    }

    @OnClick(R.id.txt_event_time)
    public void selectTime(){
        //region Date picker dialog
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                txtEventTime.setText( selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
        //endregion
    }



    public Activity activity;

    public ConfirmEventDialog(Activity activity, int cat_id, String ev_name, int ev_time) {
        this.activity = activity;
        setCat_id(cat_id);
        setEvent_name(ev_name);
        setEvent_time(ev_time);
        Toast.makeText(activity, "Here", Toast.LENGTH_SHORT).show();
    }

    public void createEvent(int repeat_mode, short length, boolean is_active) {
        String usrName = SignInActivity.loginPrefs.getString("Login", null);
        String usrPassword = SignInActivity.loginPrefs.getString("Password", null);

        JSONObject jsonSend = new JSONObject();
        try {
            jsonSend.put("username", usrName);
            jsonSend.put("password", usrPassword);
            jsonSend.put("category_id", getCat_id());
            jsonSend.put("start_time", getEvent_time());
            jsonSend.put("repeat_mode", repeat_mode);
            jsonSend.put("length", length);
            jsonSend.put("is_active", is_active);
            jsonSend.put("event_name", getEvent_name());
            jsonSend.put("event_note", getEvent_note());
            String url = "http://qobiljon.pythonanywhere.com/events/create";

            String res = Tools.post(url, jsonSend);

            Log.e("DATAAAA", res + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
