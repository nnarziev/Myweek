package com.example.negmat.myweek_1;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;


public class ViewEventDialog extends Dialog implements
        android.view.View.OnClickListener{

    public Activity c;
    public String EventTxt;
    public Button edit, delete,cancel;

    public long event_id;
    public TextView tv;
    public ViewEventDialog(Activity a, String event, long event_id) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.EventTxt=event;
        this.event_id=event_id;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_event_dialog);
        edit = (Button) findViewById(R.id.btn_edit);
        delete = (Button) findViewById(R.id.btn_delete);
        cancel = (Button) findViewById(R.id.btn_cancel);
        tv=(TextView)findViewById(R.id.txt_event);
        edit.setOnClickListener(this);
        cancel.setOnClickListener(this);
        delete.setOnClickListener(this);
        tv.setText(EventTxt);

    }

    @Override
    public void onClick(View v) {
        SharedPreferences pref = c.getSharedPreferences(Constants.PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);
        switch (v.getId()) {
            case R.id.btn_edit:
                Toast.makeText(c, "Edit event", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_delete:
                JsonObject jsonDelete = new JsonObject();
                jsonDelete.addProperty("username", usrName);
                jsonDelete.addProperty("password", usrPassword);
                jsonDelete.addProperty("event_id", event_id);
                String url = "http://qobiljon.pythonanywhere.com/events/disable";
                Ion.with(c.getApplicationContext())
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
                                case Constants.RES_OK:
                                    break;
                                case Constants.RES_SRV_ERR:
                                    Toast.makeText(c.getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case Constants.RES_FAIL:
                                    Toast.makeText(c.getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e1) {
                            Log.wtf("json", e1);
                        }
                    }
                });
                break;
            case R.id.btn_cancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }




}
