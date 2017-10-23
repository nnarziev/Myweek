package com.example.negmat.myweek;

import android.Manifest;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddEventDialog extends DialogFragment implements SpeechDelegate {

    //    private String event_name = "";
    private static final int REQUEST_MICROPHONE = 2;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.btn_speech)
    ImageButton btnSpeech;
    // @BindView(R.id.btn_cancel) Button btnCancel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_newevent, container, false);
        ButterKnife.bind(this, view);

        Speech.init(getActivity(), getActivity().getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        //getCategoties();
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
    public void onSpeechResult(final String result) {
        btnSpeech.setVisibility(View.VISIBLE);
        text.setText(result);

        if (result.isEmpty()) {
            Speech.getInstance().say("Repeat please");
        }
        //        else {
//            //Speech.getInstance().say(result);
//        }
        //event_name = result;
        //        //TODO: take a result and make string matchig and find the category id and send to suggest API
        //        int category_id = stringMatchingWithCategories(result);
        //        suggestTime(category_id);
        //        //TODO: after time was suggested, use create event API

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object[] match = stringMatchingWithCategories(result);
                    if (match == null)
                        throw new Exception();

                    int category_id = (int) match[0];
                    String key = (String) match[1];
                    key = Character.toUpperCase(key.charAt(0)) + key.substring(1);

                    String usrName = SignInActivity.loginPrefs.getString("Login", null);
                    String usrPassword = SignInActivity.loginPrefs.getString("Password", null);

                    JSONObject body = new JSONObject();
                    body.put("username", usrName);
                    body.put("password", usrPassword);
                    body.put("category_id", category_id);
                    body.put("today", 20171023);
                    body.put("weekend", 20171029);
                    JSONObject raw = new JSONObject(Tools.post("https://qobiljon.pythonanywhere.com/events/suggest", body));

                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();

                    int suggested_time = raw.getInt("suggested_time");
                    ConfirmEventDialog.createEvent(category_id, suggested_time, 120, (short) 60, true, key, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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

    //private HashMap<String, Integer> category_ids = new HashMap<>();

//    public void getCategoties() {
//        SharedPreferences pref = getActivity().getSharedPreferences(Tools.PREFS_NAME, 0);
//        String usrName = pref.getString("Login", null);
//        String usrPassword = pref.getString("Password", null);
//
//        JsonObject jsonSend = new JsonObject();
//        jsonSend.addProperty("username", usrName);
//        jsonSend.addProperty("password", usrPassword);
//        String url = "http://qobiljon.pythonanywhere.com/events/categories";
//        Ion.with(getActivity().getApplicationContext())
//                .load("POST", url)
//                .addHeader("Content-Type", "application/json")
//                .setJsonObjectBody(jsonSend)
//                .asJsonObject()
//                .setCallback(new FutureCallback<JsonObject>() {
//                    @Override
//                    public void onCompleted(Exception e, JsonObject result) {
//                        //process data or error
//                        try {
//                            JSONObject json = new JSONObject(String.valueOf(result));
//                            short resultNumber = (short) json.getInt("result");
//                            switch (resultNumber) {
//                                case Tools.RES_OK:
//                                    //TODO: take array of JSON objects and return this
//                                    //JSONObject js = new JSONObject(String.valueOf(jArray));
//                                    final int length = json.getInt("length");
//                                    final JSONArray jarray = json.getJSONArray("categories");
//                                    category_ids.clear();
//
//                                    for (int i = 0; i < length; i++) {
//                                        String cat_name = jarray.getJSONObject(i).names().getString(0);
//                                        category_ids.put(cat_name, jarray.getJSONObject(i).getInt(cat_name));
//                                    }
//
//                                    for (String name : category_ids.keySet()) {
//                                        String value = category_ids.get(name).toString();
//                                        Log.v(name, ": " + value);
//                                    }
//                                    break;
//                                case Tools.RES_SRV_ERR:
//                                    Toast.makeText(getActivity().getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
//                                    break;
//                                case Tools.RES_FAIL:
//                                    Toast.makeText(getActivity().getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
//                                    break;
//                                default:
//                                    break;
//                            }
//                        } catch (JSONException e1) {
//                            Log.wtf("json", e1);
//                        }
//                    }
//                });
//    }
//
//    public void suggestTime(final int category_id) {
//        String usrName = SignInActivity.loginPrefs.getString("Login", null);
//        String usrPassword = SignInActivity.loginPrefs.getString("Password", null);
//
//        JsonObject jsonSend = new JsonObject();
//        jsonSend.addProperty("username", usrName);
//        jsonSend.addProperty("password", usrPassword);
//        jsonSend.addProperty("category_id", category_id);
//        String url = "http://qobiljon.pythonanywhere.com/events/suggest";
//        Ion.with(getActivity().getApplicationContext())
//                .load("POST", url)
//                .addHeader("Content-Type", "application/json")
//                .setJsonObjectBody(jsonSend)
//                .asJsonObject()
//                .setCallback(new FutureCallback<JsonObject>() {
//                    @Override
//                    public void onCompleted(Exception e, JsonObject result) {
//                        //process data or error
//                        try {
//                            final JSONObject json = new JSONObject(String.valueOf(result));
//                            short resultNumber = (short) json.getInt("result");
//                            switch (resultNumber) {
//                                case Tools.RES_OK:
//
//                                    final int suggested_time = json.getInt("suggested_time");
//                                    Toast.makeText(getActivity(), category_id + "\n" + event_name + "\n" + suggested_time, Toast.LENGTH_SHORT).show();
//                                    /*ConfirmEventDialog conf = new ConfirmEventDialog(getActivity(), event_name, String.valueOf(suggested_time), "some note");
//                                    conf.show();*/
//                                    FragmentManager manager = getFragmentManager();
//                                    ConfirmEventDialog conf = new ConfirmEventDialog(getActivity(), event_name, String.valueOf(suggested_time), "some note");
//                                    conf.show(manager, "ConfirmDialog");
//                                    dismiss();
//                                    //createEvent(category_id, suggested_time, 120, (short) 60, true, event_name, "");
//
//
//                                    //Toast.makeText(getActivity(), String.valueOf(json.getInt("suggested_time")), Toast.LENGTH_SHORT).show();
//                                    break;
//                                case Tools.RES_SRV_ERR:
//                                    Toast.makeText(getActivity().getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
//                                    break;
//                                case Tools.RES_FAIL:
//                                    Toast.makeText(getActivity().getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
//                                    break;
//                                default:
//                                    break;
//                            }
//                        } catch (JSONException e1) {
//                            Log.wtf("json", e1);
//                        }
//                    }
//                });
//    }

    public Object[] stringMatchingWithCategories(String event_text) {
        String raw_json = Tools.post("http://qobiljon.pythonanywhere.com/events/categories", null);
        if (raw_json == null)
            return null;

        HashMap<String, Integer> map = new HashMap<>();

        // region Load map
        try {
            JSONObject cat_json = new JSONObject(raw_json);
            short resultNumber = (short) cat_json.getInt("result");
            switch (resultNumber) {
                case Tools.RES_OK:
                    JSONArray jarray = cat_json.getJSONArray("categories");
                    map.clear();

                    for (int i = 0; i < jarray.length(); i++) {
                        String cat_name = jarray.getJSONObject(i).names().getString(0);
                        map.put(cat_name, jarray.getJSONObject(i).getInt(cat_name));
                    }

                    for (String name : map.keySet()) {
                        String value = map.get(name).toString();
                        Log.v(name, ": " + value);
                    }
                    break;
                case Tools.RES_SRV_ERR:
                    Toast.makeText(getActivity().getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // endregion

        int cat = -1;
        String key = null;
        for (String _key : map.keySet())
            if (event_text.contains(_key)) {
                key = _key;
                cat = map.get(_key);
                break;
            }
        if (cat == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Matching lacks data, random category selection is on!", Toast.LENGTH_SHORT).show();
            key = map.keySet().toArray(new String[map.keySet().size()])[0];
            cat = map.get(key);
            return new Object[]{cat, key};
        }

        return new Object[]{cat, key};
    }
}