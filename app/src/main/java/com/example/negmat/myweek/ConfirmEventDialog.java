package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
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
    @BindView(R.id.weekdaysGroup)
    ViewGroup toggleBtnParent;
    //endregion

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_confirmevent, container, false);
        ButterKnife.bind(this, view);

        txtEventName.setText(getEvent_name());
        txtEventDate.setText(showEv_date_string(getEvent_time()));
        txtEventTime.setText(showEv_time_string(getEvent_time()));
        txtEventNote.setText(getEvent_note());

        weekDayMap.clear();
        weekDayMap.put(getResources().getString(R.string.mon), Tools.MON);
        weekDayMap.put(getResources().getString(R.string.tue), Tools.TUE);
        weekDayMap.put(getResources().getString(R.string.wed), Tools.WED);
        weekDayMap.put(getResources().getString(R.string.thu), Tools.THU);
        weekDayMap.put(getResources().getString(R.string.fri), Tools.FRI);
        weekDayMap.put(getResources().getString(R.string.sat), Tools.SAT);
        weekDayMap.put(getResources().getString(R.string.sun), Tools.SUN);

        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    repeat_mode |= weekDayMap.get(String.valueOf(compoundButton.getTag()));
                    compoundButton.setTextColor(Color.WHITE);
                } else {
                    repeat_mode &= ~weekDayMap.get(String.valueOf(compoundButton.getTag()));
                    compoundButton.setTextColor(Color.BLACK);
                }
            }
        };

        for (int n = 0; n < toggleBtnParent.getChildCount(); n++)
            ((ToggleButton) toggleBtnParent.getChildAt(n)).setOnCheckedChangeListener(listener);

        return view;
    }

    // region Variables
    public long event_id;
    public String event_name;
    public String event_note;
    public int cat_id;
    public int event_time;
    public int event_repeat_mode;
    public boolean isEditing = false;
    public Activity activity;
    public Event event;

    private short repeat_mode = 0;
    private HashMap<String, Short> weekDayMap = new HashMap<>();
    // endregion

    public ConfirmEventDialog(Activity activity, Event e, boolean isEditing) {
        this.activity = activity;
        this.event = e;
        this.isEditing = isEditing;

        this.event_id = e.getEvent_id();
        setCat_id(e.getEvent_cat_id());
        setEvent_name(e.getEvent_name());
        setEvent_time(e.getStart_time());
        setEvent_note(e.getEvent_note());
        setEvent_repeat_mode(e.getRepeat_mode());
    }

    public ConfirmEventDialog(Activity activity, int cat_id, String ev_name, String ev_note, int ev_time) {
        this.activity = activity;

        setCat_id(cat_id);
        setEvent_name(ev_name);
        setEvent_time(ev_time);
        setEvent_note(ev_note);
    }

    public String showEv_time_string(int event_time) {
        short time = (short) (event_time % 10000 / 100);
        return String.format(Locale.US, "%d:00", time);
    }

    public String showEv_date_string(int event_time) {
        short day = (short) (event_time % 1000000 / 10000);
        short month = (short) (event_time % 100000000 / 1000000);
        short year = (short) (event_time / 100000000);

        Calendar cal = Calendar.getInstance();
        cal.set(year + 2000, month, day);

        return String.format(Locale.US, "%s. %d, %d",
                cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), day, year + 2000);
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

    public int getEvent_repeat_mode() {
        return event_repeat_mode;
    }

    public void setEvent_repeat_mode(int event_repeat_mode) {
        this.event_repeat_mode = event_repeat_mode;
    }

    //region Buttons handler; Date and Time pick handler
    @OnClick(R.id.btn_save)
    public void saveClick(View view) {
        int remainder = getEvent_time() % 1000000;
        int time = (getEvent_time() / 1000000 + 1) * 1000000 + remainder;
        setEvent_time(time);
        if (isEditing) {
            createEvent(repeat_mode, (short) 60, true);
            deleteAfterEdit();
        } else
            createEvent(repeat_mode, (short) 60, true);
        dismiss();
    }

    @OnClick(R.id.btn_delete)
    public void delete() {
        dismiss();
    }

    //region Selecting date and time
    @OnClick(R.id.txt_event_date)
    public void selectDate() {
        short day = (short) (getEvent_time() % 1000000 / 10000);
        short month = (short) (getEvent_time() % 100000000 / 1000000);
        short year = (short) (getEvent_time() / 100000000 + 2000);
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                short time = (short) (getEvent_time() % 10000);
                int o = ((y - 2000) * 100 + m) * 100 + d;
                int date = o * 10000 + time;
                setEvent_time(date);
                txtEventDate.setText(showEv_date_string(getEvent_time()));
                
            }
        }, year, month, day);
        datePicker.setTitle("Select date");
        datePicker.show();
    }

    @OnClick(R.id.txt_event_time)
    public void selectTime() {
        short hour = (short) (event_time % 10000 / 100);

        TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                int time = ((getEvent_time() / 10000) * 100 + selectedHour) * 100;
                setEvent_time(time);
                txtEventTime.setText(showEv_time_string(getEvent_time()));
            }
        }, hour, 00, true);

        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }
    //endregion

    //endregion

    //region Create event function
    public void createEvent(final int repeat_mode, final short length, final boolean is_active) {
        final String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
        final String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);
        setEvent_note(txtEventNote.getText().toString());

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
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
                    String url = "http://165.246.165.130:2222/events/create";

                    JSONObject raw = new JSONObject(Tools.post(url, jsonSend));
                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //endregion

    public void deleteAfterEdit() {
        String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
        String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);
        JsonObject jsonDelete = new JsonObject();
        jsonDelete.addProperty("username", usrName);
        jsonDelete.addProperty("password", usrPassword);
        jsonDelete.addProperty("event_id", event_id);
        String url = "http://165.246.165.130:2222/events/disable";
        Ion.with(activity.getApplicationContext())
                .load("POST", url)
                .addHeader("Content-Type", "application/json")
                .setJsonObjectBody(jsonDelete)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //process data or error
                        try {
                            JSONObject json = new JSONObject(String.valueOf(result));
                            int resultNumber = json.getInt("result");
                            switch (resultNumber) {
                                case Tools.RES_OK:
                                    Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show();
                                    break;
                                case Tools.RES_SRV_ERR:
                                    Toast.makeText(activity.getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case Tools.RES_FAIL:
                                    Toast.makeText(activity.getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e1) {
                            Log.wtf("json", e1);
                        }
                    }
                });
    }
}
