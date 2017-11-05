package com.example.negmat.myweek;

import android.Manifest;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // region Variables
    private static final int REQUEST_MICROPHONE = 2;
    @BindView(R.id.text)
    TextView text;
    // endregion

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_newevent, container, false);
        ButterKnife.bind(this, view);

        Speech.init(getActivity(), getActivity().getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);

        //granting permission to user
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        } else
            onRecordAudioPermissionGranted();

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
        for (String partial : results)
            text.append(partial + " ");
    }

    @Override
    public void onSpeechResult(String _result) {
        final String result = _result.toLowerCase();

        text.setText(_result);

        if (_result.isEmpty()) {
            Speech.getInstance().say("Repeat please");
            return;
        }

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object[] match = stringMatchingWithCategories(result);
                    if (match == null)
                        throw new Exception();

                    final int category_id = (int) match[0];
                    // TODO: Temporary, for presentation use only
                    final String key = Character.toUpperCase(((String) match[1]).charAt(0)) + ((String) match[1]).substring(1);

                    String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
                    String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);

                    JSONObject body = new JSONObject();
                    body.put("username", usrName);
                    body.put("password", usrPassword);
                    body.put("category_id", category_id);

                    // Calendar c = Calendar.getInstance();
                    // int today = (c.get(Calendar.YEAR) % 100) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
                    // c.add(Calendar.DATE, 6 - c.get(Calendar.DAY_OF_WEEK));
                    // int weekend = (c.get(Calendar.YEAR) % 100) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);

                    body.put("today", 171022);
                    body.put("weekend", 171028);
                    JSONObject raw = new JSONObject(Tools.post("http://165.246.165.130:2222/events/suggest", body));

                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();

                    final int suggested_time = raw.getInt("suggested_time");

                    Log.e("DATA", suggested_time + "");

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int remainder = suggested_time%1000000;
                            int time = (suggested_time/1000000 - 1) * 1000000 + remainder;
                            ConfirmEventDialog conf = new ConfirmEventDialog(getActivity(), category_id, key, result, time);
                            conf.show(getActivity().getFragmentManager(), "confirmdialog");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v("EXEPTION: ", e.toString());
                }
                dismiss();
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
        // btnSpeech.setVisibility(View.GONE);

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

    @OnClick(R.id.btn_cancel)
    public void AddEventCancel() {
        dismiss();
    }

    public Object[] stringMatchingWithCategories(String event_text) {
        String raw_json = Tools.post("http://165.246.165.130:2222/events/categories", null);
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

        if (event_text.contains("hiking") || event_text.contains("jumping"))
            return new Object[]{map.get("adrenaline"), "adrenaline"};
        else if (event_text.contains("museum"))
            return new Object[]{map.get("silent"), "silent"};
        else if (event_text.contains("romantic"))
            return new Object[]{map.get("couple"), "couple"};
        else if (event_text.contains("walk") || event_text.contains("wander"))
            return new Object[]{map.get("alone"), "alone"};
        else if (event_text.contains("club") || event_text.contains("party"))
            return new Object[]{map.get("loud"), "loud"};

        int cat = -1;
        String key = null;
        for (String _key : map.keySet())
            if (event_text.contains(_key)) {
                key = _key;
                cat = map.get(_key);
                break;
            }
        if (cat == -1) {
            //Toast.makeText(getActivity().getApplicationContext(), "Matching lacks data, random category selection is on!", Toast.LENGTH_SHORT).show();
            key = map.keySet().toArray(new String[map.keySet().size()])[0];
            cat = map.get(key);
            return new Object[]{cat, key};
        }

        return new Object[]{cat, key};
    }
}