package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AISuggestDialog extends DialogFragment implements TextToSpeech.OnInitListener {

    public AISuggestDialog(Event event, MyRunnable onExit) {
        this.event = event;
        this.exitJob = onExit;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_aisuggest, container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

        Tools.enable_touch(getActivity());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (exitJob != null)
            exitJob.run();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                Log.e("TTS", "This Language is not supported");
            else speakText();
        } else Log.e("TTS", "Initilization Failed!");
    }

    //region Variables
    private TextView text_suggest;

    private Event event;
    private TextToSpeech tts;
    private static ExecutorService exec;
    private MyRunnable exitJob;
    //endregion

    private void initialize(View root) {
        text_suggest = root.findViewById(R.id.text_suggest);

        tts = new TextToSpeech(getActivity(), this);

        Tools.disable_touch(getActivity());
        Calendar cal = Tools.time2cal(event.start_time);
        String text = String.format(
                Locale.US,
                "I think the best time for this event is %s o'clock on %s, %s %d, %d.",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US),
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.YEAR)
        );
        text_suggest.setText(text);

        root.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEvent(event);
            }
        });
        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        root.findViewById(R.id.btn_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditViewDialog conf = new EditViewDialog(event, false, new MyRunnable(getActivity()) {
                    @Override
                    public void run() {
                        if (args[0] instanceof MainActivity) {
                            ((MainActivity) args[0]).updateClick(null);
                        }
                    }
                });
                conf.show(getActivity().getFragmentManager(), "confirmdialog");
                dismiss();
            }
        });
        text_suggest = root.findViewById(R.id.text_suggest);
    }

    private void createEvent(Event event) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        Tools.disable_touch(getActivity());
        exec.execute(new MyRunnable(event, getActivity()) {
            @Override
            public void run() {
                Event event = (Event) args[0];
                Activity activity = (Activity) args[1];

                try {
                    JSONObject data = new JSONObject();
                    data.put("username", SignInActivity.loginPrefs.getString(SignInActivity.username, null));
                    data.put("password", SignInActivity.loginPrefs.getString(SignInActivity.password, null));
                    data.put("event_id", event.event_id);
                    data.put("category_id", event.category_id);
                    data.put("start_time", event.start_time);
                    data.put("day", event.day);
                    data.put("length", event.length);
                    data.put("event_name", event.event_name);
                    data.put("event_note", event.event_note);

                    String url = String.format(Locale.US, "%s/events/create", getResources().getString(R.string.server_ip));

                    JSONObject raw = new JSONObject(Tools.post(url, data));
                    activity.runOnUiThread(new MyRunnable(activity, raw.getInt("result"), event) {
                        @Override
                        public void run() {
                            Activity activity = (Activity) args[0];
                            int result = (int) args[1];
                            Event event = (Event) args[2];
                            Calendar cal = Tools.time2cal(event.start_time);

                            if (result == Tools.RES_OK) {
                                Toast.makeText(activity, "Event was created successfuly!", Toast.LENGTH_SHORT).show();
                                Toast.makeText(activity, String.format(Locale.US, "Event has been created on %d/%d/%d at %02d:%02d",
                                        cal.get(Calendar.DAY_OF_MONTH),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE)),
                                        Toast.LENGTH_LONG
                                ).show();
                                Tools.setAlarm(getActivity(), cal, event.event_name, event.event_note);
                                Tools.enable_touch(activity);
                                dismiss();
                            }
                        }
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(new MyRunnable(activity) {
                        @Override
                        public void run() {
                            Tools.enable_touch((Activity) args[0]);
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    private void speakText() {

        String toSpeak = text_suggest.getText().toString();

        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }
}
