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
import net.gotev.speech.TextToSpeechCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SpeechDialog extends DialogFragment implements SpeechDelegate {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_speech, container, false);
        ButterKnife.bind(this, view);

        Speech.init(getActivity(), getActivity().getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);

        //granting permission to user
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        else onRecordAudioPermissionGranted();

        return view;
    }

    // region Variables
    static ExecutorService exec;

    private static final int REQUEST_MICROPHONE = 2;
    @BindView(R.id.text)
    TextView text;
    // endregion

    // region Speech handlers
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
    public void onSpeechResult(String result) {
        result = result.toLowerCase();
        text.setText(result);
        if (result.isEmpty()) {
            Speech.getInstance().say("Repeat please", new TextToSpeechCallback() {
                @Override
                public void onStart() {

                }

                @Override
                public void onCompleted() {
                    onRecordAudioPermissionGranted();
                }

                @Override
                public void onError() {

                }
            });
            return;
        }

        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();
        exec = Executors.newCachedThreadPool();

        exec.execute(new MyRunnable(result) {
            @Override
            public void run() {
                try {
                    String speech_result = (String) args[0];

                    Object[] match = stringMatchingWithCategories(speech_result);
                    if (match == null)
                        throw new Exception();

                    String category = Character.toUpperCase(((String) match[1]).charAt(0)) + ((String) match[1]).substring(1);
                    int category_id = (int) match[0];

                    String username = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
                    String password = SignInActivity.loginPrefs.getString(SignInActivity.password, null);

                    JSONObject body = new JSONObject();
                    body.put("username", username);
                    body.put("password", password);
                    body.put("category_id", category_id);

                    JSONObject raw = new JSONObject(Tools.post(String.format(Locale.US, "%s/events/suggest", getResources().getString(R.string.server_ip)), body));
                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();

                    int suggested_time = raw.getInt("suggested_time");
                    getActivity().runOnUiThread(new MyRunnable(
                            Tools.suggestion2time(suggested_time), // chosen, suggested time
                            suggested_time % 10, // day of week
                            category,
                            speech_result,
                            category_id
                    ) {
                        @Override
                        public void run() {
                            Event event = new Event(
                                    (int) args[0],
                                    (int) args[1],
                                    (short) 60,
                                    (String) args[2],
                                    (String) args[3],
                                    Event.NEW_EVENT,
                                    (int) args[4]
                            );

                            EditViewDialog conf = new EditViewDialog(event, false, new MyRunnable(getActivity()) {
                                @Override
                                public void run() {
                                    if (args[0] instanceof MainActivity) {
                                        ((MainActivity) args[0]).updateClick(null);
                                    }
                                }
                            });
                            conf.show(getActivity().getFragmentManager(), "confirmdialog");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v("ERROR: ", e.getMessage());
                }

                dismiss();
            }
        });
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
    // endregion

    @Override
    public void onDestroy() {
        super.onDestroy();
        Speech.getInstance().shutdown();
    }

    @OnClick(R.id.btn_cancel)
    public void cancelClick() {
        dismiss();
    }

    public Object[] stringMatchingWithCategories(String event_text) {
        String raw_json = Tools.post(String.format(Locale.US, "%s/events/categories", getResources().getString(R.string.server_ip)), null);
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
                    break;
                case Tools.RES_SRV_ERR:
                    Toast.makeText(getActivity().getApplicationContext(), "Error occurred on the Server side!", Toast.LENGTH_SHORT).show();
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
            //Toast.makeText(getActivity().getApplicationContext(), "Matching lacks data, random category selection is on!", Toast.LENGTH_SHORT).show();
            key = map.keySet().toArray(new String[map.keySet().size()])[0];
            cat = map.get(key);
            return new Object[]{cat, key};
        }

        return new Object[]{cat, key};
    }
}