package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConfirmEventDialog extends DialogFragment {

    //region UI variables
    @BindView(R.id.btn_save)
    Button btnSave;
    @BindView(R.id.btn_delete)
    Button btnDelete;
    @BindView(R.id.txt_event_name)
    EditText txtEventName;
    @BindView(R.id.txt_event_date)
    TextView txtEventDate;
    @BindView(R.id.txt_event_time)
    TextView txtEventTime;
    @BindView(R.id.txt_event_note)
    EditText txtEventNote;
    //endregion

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_confirmevent, container, false);
        ButterKnife.bind(this, view);
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        txtEventName.setText(getEvent_name());
        txtEventDate.setText(showEv_date_string(getEvent_time()));
        txtEventTime.setText(showEv_time_string(getEvent_time()));
        txtEventNote.setText(event_note);
        return view;
    }

    // region Class environment

    public String event_name;
    public String event_note;
    public int cat_id;
    public int event_time;
    public Activity activity;

    public ConfirmEventDialog(Activity activity, int cat_id, String ev_name, int ev_time) {
        this.activity = activity;
        setCat_id(cat_id);
        setEvent_name(ev_name);
        setEvent_time(ev_time);
        Toast.makeText(activity, "Here", Toast.LENGTH_SHORT).show();
    }

    public String showEv_time_string(int event_time){
        short time = (short) (event_time % 10000 / 100);
        short day = (short) (event_time % 1000000 / 10000);
        short month = (short) (event_time % 100000000 / 1000000);
        short year = (short) (event_time / 100000000);

        Calendar cal = Calendar.getInstance();
        cal.set(year + 2000, month, day);

        return String.format(Locale.US, "%d:00",time);
    }

    public String showEv_date_string(int event_time){
        short day = (short) (event_time % 1000000 / 10000);
        short month = (short) (event_time % 100000000 / 1000000);
        short year = (short) (event_time / 100000000);

        Calendar cal = Calendar.getInstance();
        cal.set(year + 2000, month, day);

        return String.format(Locale.US, "%s. %d, %d",
                new DateFormatSymbols().getMonths()[month-1].substring(0, 3), day, year + 2000);
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
    // endregion

    //region Buttons handler; Date and Time pick handler
    @OnClick(R.id.btn_save)
    public void save() {
        createEvent(120, (short) 60, true);
        dismiss();
    }

    @OnClick(R.id.btn_delete)
    public void delete() {
        dismiss();
    }

    //region Selecting date and time
    @OnClick(R.id.txt_event_date)
    public void selectDate(){
        short day = (short) (getEvent_time() % 1000000 / 10000);
        short month = (short) (getEvent_time() % 100000000 / 1000000);
        short year = (short) (getEvent_time() / 100000000 + 2000);
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener(){

            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                short time = (short) (getEvent_time() % 10000);
                int o = ((y-2000)*100 + m)*100+d;
                int date = o*10000 + time;
                setEvent_time(date);
                txtEventDate.setText(showEv_date_string(getEvent_time()));
            }
        }, year, month, day);
        datePicker.setTitle("Select date");
        datePicker.show();
    }

    @OnClick(R.id.txt_event_time)
    public void selectTime(){
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                int time = ((getEvent_time()/10000)*100 + selectedHour) * 100;
                setEvent_time(time);
                txtEventTime.setText(showEv_time_string(getEvent_time()));
            }
        }, hour, minute, false);//Yes 24 hour time
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }
    //endregion

    //endregion

    //region Create event function
    public void createEvent(final int repeat_mode, final short length, final boolean is_active) {
        final String usrName = SignInActivity.loginPrefs.getString("Login", null);
        final String usrPassword = SignInActivity.loginPrefs.getString("Password", null);
        setEvent_note(txtEventNote.getText().toString());

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    JSONObject jsonSend = new JSONObject();
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

                    JSONObject raw = new JSONObject(Tools.post(url, jsonSend));
                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();


                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    //endregion
}
