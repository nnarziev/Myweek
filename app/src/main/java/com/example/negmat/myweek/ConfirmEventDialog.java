package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import net.gotev.speech.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Nematjon on 10/17/2017.
 */

public class ConfirmEventDialog extends DialogFragment {

    final String[] select_day = {
            "Select the day(s)", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public String event_name;
    public String event_note;
    public String event_time;

    @BindView(R.id.btn_save)
    Button btnSave;
    @BindView(R.id.btn_delete)
    Button btnDelete;
    @BindView(R.id.txt_event_name)
    EditText txtEventName;
    @BindView(R.id.txt_event_time)
    EditText txtEventTime;
    @BindView(R.id.txt_event_note)
    EditText txtEventNote;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.diaog_confirmevent, container, false);
        ButterKnife.bind(this, view);
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        txtEventName.setText(event_name);
        txtEventTime.setText(event_time);
        txtEventNote.setText(event_note);

        return view;
    }

    @OnClick(R.id.btn_save)
    public void save(){
       // createEvent();
    }
    @OnClick(R.id.btn_delete)
    public void delete(){

    }


    public Activity a;

    public ConfirmEventDialog(Activity activity, String ev_name, String ev_time, String ev_note) {
        this.a = activity;
        this.event_name = ev_name;
        this.event_note = ev_note;
        this.event_time = ev_time;
    }

    public void createEvent(int category_id, int suggested_time, int repeat_mode, short length, boolean is_active, String event_name, String event_note) {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);

        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("username", usrName);
        jsonSend.addProperty("password", usrPassword);
        jsonSend.addProperty("category_id", category_id);
        jsonSend.addProperty("start_time", suggested_time);
        jsonSend.addProperty("repeat_mode", repeat_mode);
        jsonSend.addProperty("length", length);
        jsonSend.addProperty("is_active", is_active);
        jsonSend.addProperty("event_name", event_name);
        jsonSend.addProperty("event_note", event_note);
        String url = "http://qobiljon.pythonanywhere.com/events/create";
        Ion.with(getActivity().getApplicationContext())
                .load("POST", url)
                .addHeader("Content-Type", "application/json")
                .setJsonObjectBody(jsonSend)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //process data or error
                        try {
                            JSONObject json = new JSONObject(String.valueOf(result));
                            short resultNumber = (short) json.getInt("result");
                            switch (resultNumber) {
                                case Constants.RES_OK:
                                    Toast.makeText(getActivity(), "Event was created", Toast.LENGTH_SHORT).show();
                                    break;
                                case Constants.RES_SRV_ERR:
                                    Toast.makeText(getActivity().getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case Constants.RES_FAIL:
                                    Toast.makeText(getActivity().getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e1) {
                            Log.v("json", String.valueOf(e1));
                        }
                    }
                });
    }
}
