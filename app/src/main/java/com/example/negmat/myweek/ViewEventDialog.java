package com.example.negmat.myweek;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;


class ViewEventDialog extends Dialog implements OnClickListener {

    // region Variables
    private Activity activity;
    private String event_name;
    private String event_note;
    private int event_date_time;
    private String event_date;
    private String event_time;
    private short event_duration;
    private int event_repeat;
    private int event_cat_id;
    public Event event;

    private String EventTxt;

    private Button edit, delete, cancel;
    private long event_id;

    private TextView txtEventName;
    private TextView txtEventDate;
    private TextView txtEventTime;
    private TextView txtEventRepeat;
    private TextView txtEventNote;
    // endregion

    ViewEventDialog(Activity a, Event e/*String event, long event_id*/) {
        super(a);
        activity = a;
        event = e;
        // TODO Auto-generated constructor stub
        event_date_time = e.getStart_time();
        short time = (short) (event_date_time % 10000 / 100);
        short day = (short) (event_date_time % 1000000 / 10000);
        short month = (short) (event_date_time % 100000000 / 1000000);
        short year = (short) (event_date_time / 100000000);
        Calendar cal = Calendar.getInstance();
        cal.set(year + 2000, month, day);

        event_id = e.getEvent_id();
        event_name = e.getEvent_name();
        event_note = e.getEvent_note();
        event_time = String.format(Locale.US, "%d:00", time);
        event_date = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + ". " + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.YEAR);
        event_duration = e.getLength();
        event_duration = e.getLength();
        event_repeat = e.getRepeat_mode();
        event_cat_id = e.getEvent_cat_id();
        int repeat = e.getRepeat_mode();
        String reason = e.getReason();
        String eventInfo = String.format("%s\n%s\nDate: %s\nFrom: %d:00\nDuration: %d min\nRepeat: %d times", event_name, event_note,
                event_date, time, event_duration, event_repeat);

        this.EventTxt = eventInfo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_viewevent);
        edit = findViewById(R.id.btn_edit);
        delete = findViewById(R.id.btn_delete);
        cancel = findViewById(R.id.btn_cancel);
        txtEventName = findViewById(R.id.txt_event_name);
        txtEventDate = findViewById(R.id.txt_event_date);
        txtEventTime = findViewById(R.id.txt_event_time);
        txtEventRepeat = findViewById(R.id.txt_event_repeat);
        txtEventNote = findViewById(R.id.txt_event_note);
        edit.setOnClickListener(this);
        cancel.setOnClickListener(this);
        delete.setOnClickListener(this);
        txtEventName.setText(txtEventName.getText().toString() + " " + event_name);
        txtEventDate.setText(txtEventDate.getText().toString() + " " + event_date);
        txtEventTime.setText(txtEventTime.getText().toString() + " " + event_time);

        String rep = "";
        for (int n = 0; n < days.length; n++)
            if ((event_repeat & (1 << (days.length - 1 - n))) != 0) {
                if (rep.length() > 0)
                    rep += ", ";
                rep += days[n];
            }
        txtEventRepeat.setText(txtEventRepeat.getText().toString() + " " + (rep.length() == 0 ? "No Repeat" : rep));

        txtEventNote.setText(txtEventNote.getText().toString() + " " + event_note);

    }

    // TODO: Temporary
    static String[] days = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_edit:
                ConfirmEventDialog conf = new ConfirmEventDialog(activity, event, true);
                conf.show(activity.getFragmentManager(), "confirmdialog");


                break;
            case R.id.btn_delete:
                deleteEvent();
                break;
            case R.id.btn_cancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }

    private void deleteEvent() {
        String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
        String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);
        JsonObject jsonDelete = new JsonObject();
        jsonDelete.addProperty("username", usrName);
        jsonDelete.addProperty("password", usrPassword);
        jsonDelete.addProperty("event_id", event_id);
        String url = "http://qobiljon.pythonanywhere.com/events/disable";
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
