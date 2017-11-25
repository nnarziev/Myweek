package com.example.negmat.myweek;

import android.Manifest;
import android.app.DialogFragment;
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
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.TextToSpeechCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SpeechDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_speech, container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        Speech.getInstance().shutdown();
    }

    // region Variables
    private TextView text;

    private static ExecutorService exec;
    final int REQUEST_MICROPHONE = 2;
    // endregion

    private void initialize(View root) {
        // region Initialize UI Variables
        text = root.findViewById(R.id.text);
        // endregion

        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        Speech.init(getActivity().getApplicationContext());

        // requesting microphone permission on devices with latest Android versions
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        else startSpeechToText();
    }

    void startSpeechToText() {
        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(new SpeechDelegate() {
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
                                startSpeechToText();
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

                                        AISuggestDialog dialog = new AISuggestDialog(event, new MyRunnable(getActivity()) {
                                            @Override
                                            public void run() {
                                                if (args[0] instanceof MainActivity) {
                                                    ((MainActivity) args[0]).updateClick(null);
                                                }
                                            }
                                        });
                                        dialog.show(getActivity().getFragmentManager(), "suggest_dialog");
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
            });
        } catch (SpeechRecognitionNotAvailable exc) {
            // when device doesn't support SpeechToText feature
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("App won't work without Google App. Please re-enable Google App to have speech recognition.")
                    .setCancelable(true)
                    .show();
        } catch (GoogleVoiceTypingDisabledException exc) {
            // when GoogleTextToSpeech feature is disabled on device
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Please re-enable Google Speech-to-Text to use speech recognition feature.")
                    .setCancelable(true)
                    .show();
        }
    }

    public Object[] stringMatchingWithCategories(String event_text) {
        // fixme: there's an error either in finding a category or in matching a default category if not found (probably this is an error case)
        String raw_json = Tools.post(String.format(Locale.US, "%s/events/categories", getResources().getString(R.string.server_ip)), null);
        if (raw_json == null)
            return null;

        HashMap<String, Integer> map_cat2code = new HashMap<>();

        // region Load map
        try {
            JSONObject cat_json = new JSONObject(raw_json);
            short resultNumber = (short) cat_json.getInt("result");
            switch (resultNumber) {
                case Tools.RES_OK:
                    JSONArray jarray = cat_json.getJSONArray("categories");
                    map_cat2code.clear();

                    for (int i = 0; i < jarray.length(); i++) {
                        String cat_name = jarray.getJSONObject(i).names().getString(0);
                        map_cat2code.put(cat_name, jarray.getJSONObject(i).getInt(cat_name));
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
        for (String _key : map_cat2code.keySet())
            if (event_text.contains(_key)) {
                key = _key;
                cat = map_cat2code.get(_key);
                break;
            }
        if (cat == -1) {
            key = map_cat2code.keySet().toArray(new String[map_cat2code.keySet().size()])[0];
            cat = map_cat2code.get(key);
            return new Object[]{cat, key};
            //return new Object[]{map.get(def), def};
        }

        return new Object[]{cat, key};
    }
}