package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EditViewDialog extends DialogFragment {

    public EditViewDialog(Event event, boolean readOnly, MyRunnable onExit) {
        this.event = event;
        this.readOnly = readOnly;
        this.exitJob = onExit;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_editview, container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        MainActivity.isSingleMode = true;
        if (exitJob != null)
            exitJob.run();
    }

    // region Variables
    //region UI variables
    private EditText txtEventName;
    private TextView txtEventDate;
    private TextView txtEventTime;
    private TextView txtEventLength;
    private SeekBar lengthChooser;
    private EditText txtEventNote;
    private RadioGroup weekdaysParent;
    private ViewGroup eventActionBtnsParent;
    //endregion

    private static ExecutorService exec;
    private MyRunnable exitJob;

    private boolean readOnly = false;
    private Event event;

    private HashMap<String, Integer> map_str2day = new HashMap<>();
    private SparseArray<String> map_day2str = new SparseArray<>();
    private SparseArray<RadioButton> map_day2btn = new SparseArray<>();
    // endregion

    private void initialize(View root) {
        // region Assign UI variables
        txtEventName = root.findViewById(R.id.txt_event_name);
        txtEventDate = root.findViewById(R.id.txt_event_date);
        txtEventTime = root.findViewById(R.id.txt_event_time);
        lengthChooser = root.findViewById(R.id.lengthChooser);
        txtEventLength = root.findViewById(R.id.txt_event_length);
        txtEventNote = root.findViewById(R.id.txt_event_note);
        weekdaysParent = root.findViewById(R.id.weekdaysGroup);
        eventActionBtnsParent = root.findViewById(R.id.eventActionButtonsContainer);

        lengthChooser.setMax((Event.MAX_LENGTH - Event.MIN_LENGTH) / Event.MIN_LENGTH);
        lengthChooser.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                short length = (short) (Event.MIN_LENGTH * (value + 1));
                txtEventLength.setText(String.format(
                        Locale.US,
                        getResources().getString(R.string.minutes),
                        length
                ));
                event.length = length;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // endregion

        // region Assign OnClickListeners
        root.findViewById(R.id.btn_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readOnly = false;
                refreshEditMode();
            }
        });
        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        root.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                event.event_note = txtEventNote.getText().toString();
                event.event_name = txtEventName.getText().toString();
                createEvent(event);
            }
        });
        root.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteEvent();
            }
        });
        txtEventDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Tools.time2cal(event.start_time);

                DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                        Calendar today = Calendar.getInstance();
                        today.setFirstDayOfWeek(Calendar.MONDAY);

                        Calendar selec = Calendar.getInstance();
                        selec.setFirstDayOfWeek(Calendar.MONDAY);
                        selec.set(y, m, d);

                        if (selec.before(today))
                            // if a date before today (invalid) is choosen, change it to proper one
                            event.start_time = Tools.suggestion2time(10 + selec.get(Calendar.DAY_OF_WEEK));
                        else
                            // if a valid date is chosen, set it to event.start_time
                            event.start_time = Tools.alter_date(event.start_time, y, m + 1, d);

                        event.day = Tools.time2cal(event.start_time).get(Calendar.DAY_OF_WEEK);
                        txtEventDate.setText(Tools.decode_time(event.start_time)[0]);

                        RadioButton button = map_day2btn.get(event.day);
                        weekdaysParent.check(button.getId());
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

                datePicker.setTitle("Select date");
                datePicker.show();
            }
        });
        txtEventTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
        // endregion

        String[] dateTime = Tools.decode_time(event.start_time);
        txtEventDate.setText(dateTime[0]);
        txtEventTime.setText(dateTime[1]);
        txtEventName.setText(event.event_name);
        txtEventNote.setText(event.event_note);
        lengthChooser.setProgress((event.length / Event.MIN_LENGTH) - 1);
        // region Weekday ToggleButtons setup
        map_day2str.put(Calendar.SUNDAY, getResources().getString(R.string.SUN));
        map_day2str.put(Calendar.MONDAY, getResources().getString(R.string.MON));
        map_day2str.put(Calendar.TUESDAY, getResources().getString(R.string.TUE));
        map_day2str.put(Calendar.WEDNESDAY, getResources().getString(R.string.WED));
        map_day2str.put(Calendar.THURSDAY, getResources().getString(R.string.THU));
        map_day2str.put(Calendar.FRIDAY, getResources().getString(R.string.FRI));
        map_day2str.put(Calendar.SATURDAY, getResources().getString(R.string.SAT));

        map_str2day.put(getResources().getString(R.string.SUN), Calendar.SUNDAY);
        map_str2day.put(getResources().getString(R.string.MON), Calendar.MONDAY);
        map_str2day.put(getResources().getString(R.string.TUE), Calendar.TUESDAY);
        map_str2day.put(getResources().getString(R.string.WED), Calendar.WEDNESDAY);
        map_str2day.put(getResources().getString(R.string.THU), Calendar.THURSDAY);
        map_str2day.put(getResources().getString(R.string.FRI), Calendar.FRIDAY);
        map_str2day.put(getResources().getString(R.string.SAT), Calendar.SATURDAY);

        CompoundButton.OnClickListener weekdayClickListener = new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                CompoundButton selectedButton = (CompoundButton) view;
                Calendar c = Tools.suggestion2cal(10 + map_str2day.get(String.valueOf(selectedButton.getTag())));
                event.start_time = Tools.alter_date(event.start_time, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
                event.day = c.get(Calendar.DAY_OF_WEEK);
                txtEventDate.setText(Tools.decode_time(c)[0]);
            }
        };
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) compoundButton.setTextColor(Color.WHITE);
                else compoundButton.setTextColor(Color.BLACK);
            }
        };

        int checkedButtonId = -1;
        for (int n = 0; n < weekdaysParent.getChildCount(); n++) {
            RadioButton button = (RadioButton) weekdaysParent.getChildAt(n);
            int day = map_str2day.get(String.valueOf(button.getTag()));

            if (day == event.day)
                checkedButtonId = button.getId();

            map_day2btn.put(day, button);
            button.setOnClickListener(weekdayClickListener);
            button.setOnCheckedChangeListener(checkedChangeListener);
        }
        weekdaysParent.check(checkedButtonId);
        // endregion

        Calendar today = Calendar.getInstance();
        today.setFirstDayOfWeek(Calendar.MONDAY);
        if (!Tools.time2cal(event.start_time).before(today))
            refreshEditMode();
        else
            loadViewHistoryMode();

        setNFCEventSender();
    }

    private void loadViewHistoryMode() {
        for (int n = 0; n < eventActionBtnsParent.getChildCount(); n++) {
            View view = eventActionBtnsParent.getChildAt(n);
            view.setVisibility(view.getId() == R.id.btn_cancel || view.getId() == R.id.btn_delete ? View.VISIBLE : View.GONE);
        }

        txtEventName.setEnabled(false);
        txtEventTime.setEnabled(false);
        txtEventDate.setEnabled(false);
        txtEventNote.setEnabled(false);
        lengthChooser.setEnabled(false);
        txtEventLength.setText(String.format(Locale.US, getResources().getString(R.string.minutes), event.length));

        for (int n = 0; n < weekdaysParent.getChildCount(); n++)
            weekdaysParent.getChildAt(n).setClickable(false);
    }

    private void refreshEditMode() {
        if (event.event_id == Event.NEW_EVENT_ID)
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
        lengthChooser.setEnabled(!readOnly);

        for (int n = 0; n < weekdaysParent.getChildCount(); n++)
            weekdaysParent.getChildAt(n).setClickable(!readOnly);
    }

    private void createEvent(Event event) {
        Tools.disable_touch(getActivity());

        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();
        exec = Executors.newCachedThreadPool();
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

                    // if event must be set to multiple users, group event creation must be called in create API
                    if (GroupEventDialog.users.size() > 0)
                        data.put("users", new JSONArray(GroupEventDialog.users));

                    String url = String.format(Locale.US, "%s/events/create", getResources().getString(R.string.server_ip));

                    JSONObject raw = new JSONObject(Tools.post(url, data));
                    activity.runOnUiThread(new MyRunnable(activity, event, raw.getInt("result")) {
                        @Override
                        public void run() {
                            Activity activity = (Activity) args[0];
                            Event event = (Event) args[1];
                            int result = (int) args[2];

                            if (result == Tools.RES_OK) {
                                Calendar cal = Tools.time2cal(event.start_time);
                                Toast.makeText(activity, String.format(Locale.US, "Event has been created on %d/%d/%d at %02d:%02d",
                                        cal.get(Calendar.DAY_OF_MONTH),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE)),
                                        Toast.LENGTH_LONG
                                ).show();
                                Tools.setAlarm(getActivity(), event);

                                dismiss();
                            } else if (result == Tools.RES_FAIL)
                                Toast.makeText(activity, "Specified time is already occupied. \nPlease choose another time!", Toast.LENGTH_LONG).show();

                            Tools.enable_touch(activity);
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

    public void deleteEvent() {
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

                    activity.runOnUiThread(new MyRunnable(args[1], event) {
                        @Override
                        public void run() {
                            Activity activity = (Activity) args[0];
                            Event event = (Event) args[1];

                            Toast.makeText(activity, "Event was deleted successfully!", Toast.LENGTH_SHORT).show();
                            AlarmNotificationReceiver.toggleNotification(getActivity(), event, false);

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

    private void setNFCEventSender() {
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
                JSONObject msg = null;
                try {
                    msg = event.toJson(true);
                    msg.put(SignInActivity.username, SignInActivity.loginPrefs.getString(SignInActivity.username, null));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return new NdefMessage(NdefRecord.createMime("text/plain", msg.toString().getBytes()));
            }
        }, getActivity());
    }
}
