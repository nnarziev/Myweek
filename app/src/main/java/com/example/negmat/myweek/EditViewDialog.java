package com.example.negmat.myweek;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class EditViewDialog extends DialogFragment {

    private void startAlarm(Calendar when, String event_name, String event_note) {
        AlarmManager manager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent myIntent;
        PendingIntent pendingIntent;

        myIntent = new Intent(getActivity(), MainActivity.AlarmNotificationReceiver.class);
        myIntent.putExtra("event_name", event_name);
        myIntent.putExtra("event_note", event_note);
        pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, myIntent, 0);
        when.add(Calendar.MINUTE, -2);
        if (manager != null) {
            manager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pendingIntent);
        } else
            Log.e("ERROR", "manager is null");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpNFCSender();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setUpNFCSender() {
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (mAdapter == null) {
            Toast.makeText(getActivity(), "Sorry this device does not have NFC.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mAdapter.isEnabled())
            Toast.makeText(getActivity(), "Please enable NFC via Settings.", Toast.LENGTH_LONG).show();

        mAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
            @Override
            public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                JSONObject msg = event.toJson(true);
                try {
                    msg.put(Tools.KEY_NFC_SINGLE, Tools.NFC_SINGLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return new NdefMessage(NdefRecord.createMime("text/plain", msg.toString().getBytes()));
            }
        }, getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_editview, container, false);
        ButterKnife.bind(this, view);

        initialize();
        return view;
    }

    public EditViewDialog(Event event, boolean readOnly, MyRunnable onExit) {
        this.event = event;
        this.readOnly = readOnly;
        this.exitJob = onExit;
    }

    private void initialize() {
        String[] dateTime = Tools.decode_time(event.start_time);

        txtEventDate.setText(dateTime[0]);
        txtEventTime.setText(dateTime[1]);

        txtEventName.setText(event.event_name);
        txtEventNote.setText(event.event_note);

        // region Weekday ToggleButtons setup
        day2str.put(Calendar.SUNDAY, getResources().getString(R.string.SUN));
        day2str.put(Calendar.MONDAY, getResources().getString(R.string.MON));
        day2str.put(Calendar.TUESDAY, getResources().getString(R.string.TUE));
        day2str.put(Calendar.WEDNESDAY, getResources().getString(R.string.WED));
        day2str.put(Calendar.THURSDAY, getResources().getString(R.string.THU));
        day2str.put(Calendar.FRIDAY, getResources().getString(R.string.FRI));
        day2str.put(Calendar.SATURDAY, getResources().getString(R.string.SAT));

        str2day.put(getResources().getString(R.string.SUN), Calendar.SUNDAY);
        str2day.put(getResources().getString(R.string.MON), Calendar.MONDAY);
        str2day.put(getResources().getString(R.string.TUE), Calendar.TUESDAY);
        str2day.put(getResources().getString(R.string.WED), Calendar.WEDNESDAY);
        str2day.put(getResources().getString(R.string.THU), Calendar.THURSDAY);
        str2day.put(getResources().getString(R.string.FRI), Calendar.FRIDAY);
        str2day.put(getResources().getString(R.string.SAT), Calendar.SATURDAY);

        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    Calendar c = Tools.suggestion2cal(10 + str2day.get(String.valueOf(compoundButton.getTag())));
                    event.start_time = Tools.alter_date(event.start_time, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
                    event.day = c.get(Calendar.DAY_OF_WEEK);
                    txtEventDate.setText(Tools.decode_time(c)[0]);
                    compoundButton.setTextColor(Color.WHITE);
                } else
                    compoundButton.setTextColor(Color.BLACK);
            }
        };

        for (int n = 0; n < weekdaysParent.getChildCount(); n++) {
            RadioButton button = (RadioButton) weekdaysParent.getChildAt(n);
            int day = str2day.get(String.valueOf(button.getTag()));

            if (day == event.day)
                weekdaysParent.check(button.getId());

            day2btn.put(day, button);
            button.setOnCheckedChangeListener(listener);
        }
        // endregion

        if (!Tools.time2cal(event.start_time).before(Calendar.getInstance()))
            refreshEditMode();
        else
            loadViewHistoryMode();
    }

    // region Variables
    //region UI variables
    @BindView(R.id.txt_event_name)
    EditText txtEventName;
    @BindView(R.id.txt_event_date)
    TextView txtEventDate;
    @BindView(R.id.txt_event_time)
    TextView txtEventTime;
    @BindView(R.id.txt_event_note)
    EditText txtEventNote;
    @BindView(R.id.weekdaysGroup)
    RadioGroup weekdaysParent;
    @BindView(R.id.eventActionButtonsContainer)
    ViewGroup eventActionBtnsParent;
    //endregion

    static ExecutorService exec;
    private MyRunnable exitJob;

    private boolean readOnly = false;
    private Event event;

    private HashMap<String, Integer> str2day = new HashMap<>();
    private SparseArray<String> day2str = new SparseArray<>();
    private SparseArray<RadioButton> day2btn = new SparseArray<>();
    // endregion


    private void loadViewHistoryMode() {
        for (int n = 0; n < eventActionBtnsParent.getChildCount(); n++) {
            View view = eventActionBtnsParent.getChildAt(n);
            view.setVisibility(view.getId() == R.id.btn_cancel || view.getId() == R.id.btn_delete ? View.VISIBLE : View.GONE);
        }

        txtEventName.setEnabled(false);
        txtEventTime.setEnabled(false);
        txtEventDate.setEnabled(false);
        txtEventNote.setEnabled(false);

        for (int n = 0; n < weekdaysParent.getChildCount(); n++)
            weekdaysParent.getChildAt(n).setClickable(false);
    }

    private void refreshEditMode() {
        if (event.event_id == Event.NEW_EVENT)
            for (int n = 0; n < eventActionBtnsParent.getChildCount(); n++) {
                View view = eventActionBtnsParent.getChildAt(n);
                view.setVisibility(view.getId() == R.id.btn_cancel || view.getId() == R.id.btn_save ? View.VISIBLE : View.GONE);
            }
        else
            for (int n = 0; n < eventActionBtnsParent.getChildCount(); n++) {
                View view = eventActionBtnsParent.getChildAt(n);
                view.setVisibility(readOnly == Boolean.parseBoolean((String) view.getTag()) ? View.VISIBLE : View.GONE);
            }

        txtEventName.setEnabled(!readOnly);
        txtEventTime.setEnabled(!readOnly);
        txtEventDate.setEnabled(!readOnly);
        txtEventNote.setEnabled(!readOnly);

        for (int n = 0; n < weekdaysParent.getChildCount(); n++)
            weekdaysParent.getChildAt(n).setClickable(!readOnly);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (exitJob != null)
            exitJob.run();
    }


    private void createEvent(Event event) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        Tools.disable_touch(getActivity());
        exec.execute(new MyRunnable(event, getActivity()) {
            @Override
            public void run() {
                final Event event = (Event) args[0];
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
                            Calendar c = (Calendar) args[0];
                            Activity activity = (Activity) args[1];
                            startAlarm(c, event.event_name, event.event_note);
                            Toast.makeText(activity, String.format(Locale.US, "Event has been created on %d/%d/%d at %02d:%02d",
                                    c.get(Calendar.DAY_OF_MONTH),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE)),
                                    Toast.LENGTH_LONG
                            ).show();
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


    @SuppressWarnings("unused")
    @OnClick(R.id.btn_edit)
    public void editButtonClick() {
        readOnly = false;
        refreshEditMode();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_cancel)
    public void cancelButtonClick() {
        dismiss();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_save)
    public void saveButtonClick() {
        event.event_note = txtEventNote.getText().toString();
        createEvent(event);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_delete)
    public void deleteButtonClick() {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        Tools.disable_touch(getActivity());
        exec.execute(new MyRunnable(event, getActivity()) {
            @Override
            public void run() {
                Activity activity = (Activity) args[1];
                Event event = (Event) args[0];

                try {
                    JSONObject data = new JSONObject();
                    data.put("username", SignInActivity.loginPrefs.getString(SignInActivity.username, null));
                    data.put("password", SignInActivity.loginPrefs.getString(SignInActivity.password, null));
                    data.put("event_id", event.event_id);

                    String url = String.format(Locale.US, "%s/events/disable", getResources().getString(R.string.server_ip));

                    JSONObject raw = new JSONObject(Tools.post(url, data));

                    if (raw.getLong("result") != Tools.RES_OK) {
                        activity.runOnUiThread(new MyRunnable(activity) {
                            @Override
                            public void run() {
                                Tools.enable_touch((Activity) args[0]);
                            }
                        });
                        throw new Exception();
                    }

                    activity.runOnUiThread(new MyRunnable(args[1]) {
                        @Override
                        public void run() {
                            Activity activity = (Activity) args[0];
                            Toast.makeText(activity, "Event was deleted successfully!", Toast.LENGTH_SHORT).show();
                            Tools.enable_touch(activity);
                            dismiss();
                        }
                    });

                } catch (Exception e) {
                    activity.runOnUiThread(new MyRunnable(activity) {
                        @Override
                        public void run() {
                            Tools.enable_touch(((Activity) args[0]));
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.txt_event_date)
    public void selectDateClick() {
        Calendar cal = Tools.time2cal(event.start_time);

        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                Calendar today = Calendar.getInstance();
                Calendar selec = Calendar.getInstance();
                selec.set(y, m, d);
                if (selec.before(today))
                    event.start_time = Tools.suggestion2time(10 + selec.get(Calendar.DAY_OF_WEEK));
                else
                    event.start_time = Tools.alter_date(event.start_time, y, m + 1, d);

                event.day = Tools.time2cal(event.start_time).get(Calendar.DAY_OF_WEEK);
                txtEventDate.setText(Tools.decode_time(event.start_time)[0]);

                RadioButton button = day2btn.get(event.day);
                weekdaysParent.check(button.getId());
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePicker.setTitle("Select date");
        datePicker.show();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.txt_event_time)
    public void selectTimeClick() {
        Calendar c = Tools.time2cal(event.start_time);

        TimePickerDialog timePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int h, int m) {
                event.start_time = Tools.alter_hour(event.start_time, h);
                txtEventTime.setText(Tools.decode_time(event.start_time)[1]);
            }
        }, c.get(Calendar.HOUR_OF_DAY), 0, true);

        timePicker.setTitle("Select Time");
        timePicker.show();
    }
}
