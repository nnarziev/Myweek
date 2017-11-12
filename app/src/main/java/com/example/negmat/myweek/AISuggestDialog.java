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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class AISuggestDialog extends DialogFragment implements TextToSpeech.OnInitListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_aisuggest, container, false);
        ButterKnife.bind(this, view);

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
        return view;
    }

    public AISuggestDialog(Event event, MyRunnable onExit) {
        this.event = event;
        this.exitJob = onExit;
    }

    //region Variables
    @BindView(R.id.text_suggest)
    TextView text_suggest;
    Event event;
    private TextToSpeech tts;
    static ExecutorService exec;
    private MyRunnable exitJob;
    //endregion


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

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                speakText();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    @OnClick(R.id.btn_edit)
    public void editButtonClick() {
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

    @OnClick(R.id.btn_ok)
    public void OKButtonClick() {
        createEvent(event);
    }

    @OnClick(R.id.btn_cancel)
    public void cancelButtonClick() {
        dismiss();
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
                    if (raw.getLong("result") != Tools.RES_OK) {
                        activity.runOnUiThread(new MyRunnable(activity) {
                            @Override
                            public void run() {
                                Tools.enable_touch(((Activity) args[0]));
                            }
                        });
                        throw new Exception();
                    }

                    activity.runOnUiThread(new MyRunnable(Tools.time2cal(event.start_time), activity) {
                        @Override
                        public void run() {
                            Activity activity = (Activity) args[1];
                            Toast.makeText(activity, "Event was created successfuly!", Toast.LENGTH_SHORT).show();
                            Tools.enable_touch(activity);
                            dismiss();
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
