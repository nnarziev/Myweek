package com.example.negmat.myweek_1;

import android.Manifest;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddEventDialog extends DialogFragment implements SpeechDelegate {

    private String event_name = "";
    private static final int REQUEST_MICROPHONE = 2;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.btn_speech)
    ImageButton btnSpeech;
    @BindView(R.id.btn_cancel)
    Button btnCancel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_event_dialog, container, false);
        ButterKnife.bind(this, view);

        Speech.init(getActivity(), getActivity().getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        getCategoties();
        return view;
    }

    @Override
    public void onStartOfSpeech() {

    }

    @Override
    public void onSpeechRmsChanged(float value) {

    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        text.setText("");
        for (String partial : results) {
            text.append(partial + " ");
        }
    }

    @Override
    public void onSpeechResult(String result) {
        btnSpeech.setVisibility(View.VISIBLE);
        text.setText(result);

        if (result.isEmpty()) {
            Speech.getInstance().say("Repeat please");
        } /*else {
            //Speech.getInstance().say(result);
        }*/
        event_name = result.toString();
        //TODO: take a result and make string matchig and find the category id and send to suggest API
        int category_id = stringMatchingWithCategories(result.toString());

        suggestTime(category_id);
        //TODO: after time was suggested, use create event API

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // prevent memory leaks when activity is destroyed
        Speech.getInstance().shutdown();
    }

    private void onRecordAudioPermissionGranted() {
        btnSpeech.setVisibility(View.GONE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(this);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    public void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(getActivity().getApplicationContext());
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Speech recognition is not available on this device. Do you want to install Google app to have speech recognition?")
                .setCancelable(false)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Please enable Google Voice Typing to use speech recognition!")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }

    @OnClick(R.id.btn_speech)
    public void Speech() {
        //granting permission to user
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }
        onRecordAudioPermissionGranted();
    }

    @OnClick(R.id.btn_cancel)
    public void AddEventCancel() {
        dismiss();
    }

    private HashMap<String, Integer> category_ids = new HashMap<>();

    public void getCategoties() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);

        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("username", usrName);
        jsonSend.addProperty("password", usrPassword);
        String url = "http://qobiljon.pythonanywhere.com/events/categories";
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
                                    //TODO: take array of JSON objects and return this
                                    //JSONObject js = new JSONObject(String.valueOf(jArray));
                                    final int length = json.getInt("length");
                                    final JSONArray jarray = json.getJSONArray("categories");
                                    category_ids.clear();

                                    for (int i = 0; i < length; i++) {
                                        String cat_name = jarray.getJSONObject(i).names().getString(0);
                                        category_ids.put(cat_name, jarray.getJSONObject(i).getInt(cat_name));
                                    }

                                    for (String name : category_ids.keySet()) {
                                        String key = name.toString();
                                        String value = category_ids.get(name).toString();
                                        Log.v(key, ": " + value);
                                    }
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
                            Log.wtf("json", e1);
                        }
                    }
                });
    }

    public void suggestTime(final int category_id) {

        SharedPreferences pref = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);

        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("username", usrName);
        jsonSend.addProperty("password", usrPassword);
        jsonSend.addProperty("category_id", category_id);
        String url = "http://qobiljon.pythonanywhere.com/events/suggest";
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
                            final JSONObject json = new JSONObject(String.valueOf(result));
                            short resultNumber = (short) json.getInt("result");
                            switch (resultNumber) {
                                case Constants.RES_OK:

                                    final int suggested_time = json.getInt("suggested_time");
                                    Toast.makeText(getActivity(), category_id + "\n" + event_name + "\n" + suggested_time, Toast.LENGTH_SHORT).show();
                                    /*ConfirmEventDialog conf = new ConfirmEventDialog(getActivity(), event_name, String.valueOf(suggested_time), "some note");
                                    conf.show();*/
                                    FragmentManager manager = getFragmentManager();
                                    ConfirmEventDialog conf = new ConfirmEventDialog(getActivity(), event_name, String.valueOf(suggested_time), "some note");
                                    conf.show(manager, "ConfirmDialog");
                                    //createEvent(category_id, suggested_time, 120, (short) 60, true, event_name, "");


                                    //Toast.makeText(getActivity(), String.valueOf(json.getInt("suggested_time")), Toast.LENGTH_SHORT).show();
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
                            Log.wtf("json", e1);
                        }
                    }
                });
    }



    public int stringMatchingWithCategories(String event_text) {

        return 0;
    }

}