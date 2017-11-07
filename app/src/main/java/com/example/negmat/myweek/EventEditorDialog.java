package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class EventEditorDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_eventeditor, container, false);
        ButterKnife.bind(this, view);
        initialize();
        return view;
    }

    public EventEditorDialog(Activity activity, Event event, boolean readOnly) {
        this.activity = activity;
        this.event = event;
        this.readOnly = readOnly;

        this.calculated_repeat_mode = event.repeat_mode;
    }

    private void initialize() {
        delete = false;

        String[] dateTime = Tools.eventDateTimeToString(event.start_time);
        txtEventDate.setText(dateTime[0]);
        txtEventTime.setText(dateTime[1]);
        txtEventName.setText(event.event_name);
        txtEventNote.setText(event.event_note);

        // region Weekday ToggleButtons setup
        weekDayMap.clear();
        weekDayMap.put(getResources().getString(R.string.mon), Tools.MON);
        weekDayMap.put(getResources().getString(R.string.tue), Tools.TUE);
        weekDayMap.put(getResources().getString(R.string.wed), Tools.WED);
        weekDayMap.put(getResources().getString(R.string.thu), Tools.THU);
        weekDayMap.put(getResources().getString(R.string.fri), Tools.FRI);
        weekDayMap.put(getResources().getString(R.string.sat), Tools.SAT);
        weekDayMap.put(getResources().getString(R.string.sun), Tools.SUN);

        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    calculated_repeat_mode |= weekDayMap.get(String.valueOf(compoundButton.getTag()));
                    compoundButton.setTextColor(Color.WHITE);
                } else {
                    calculated_repeat_mode &= ~weekDayMap.get(String.valueOf(compoundButton.getTag()));
                    compoundButton.setTextColor(Color.BLACK);
                }
            }
        };

        for (int n = 0; n < toggleBtnParent.getChildCount(); n++)
            ((ToggleButton) toggleBtnParent.getChildAt(n)).setOnCheckedChangeListener(listener);
        // endregion

        refreshEditMode();

        for (int n = 0; n < toggleBtnParent.getChildCount(); n++) {
            ToggleButton button = (ToggleButton) toggleBtnParent.getChildAt(n);
            button.setChecked((event.repeat_mode & weekDayMap.get(button.getTag().toString())) != 0);
        }
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
    ViewGroup toggleBtnParent;
    @BindView(R.id.eventActionButtonsContainer)
    ViewGroup eventActionBtnsParent;
    //endregion

    static ExecutorService exec;

    private boolean delete = false;
    private boolean readOnly = false;
    private Activity activity;
    private Event event;

    private int calculated_repeat_mode = 0;
    private HashMap<String, Short> weekDayMap = new HashMap<>();
    // endregion


    private void refreshEditMode() {
        for (int n = 0; n < eventActionBtnsParent.getChildCount(); n++) {
            View view = eventActionBtnsParent.getChildAt(n);
            view.setVisibility(readOnly == Boolean.parseBoolean((String) view.getTag()) ? View.VISIBLE : View.GONE);
        }

        txtEventName.setEnabled(!readOnly);
        txtEventTime.setEnabled(!readOnly);
        txtEventDate.setEnabled(!readOnly);
        txtEventNote.setEnabled(!readOnly);

        for (int n = 0; n < toggleBtnParent.getChildCount(); n++)
            toggleBtnParent.getChildAt(n).setClickable(!readOnly);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).updateClick(null);
    }

    private void createEvent(final Event event) {
        final String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
        final String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject data = new JSONObject();
                    data.put("username", usrName);
                    data.put("password", usrPassword);
                    data.put("category_id", event.category_id);
                    data.put("start_time", event.start_time);
                    data.put("repeat_mode", event.repeat_mode);
                    data.put("length", event.length);
                    data.put("event_name", event.event_name);
                    data.put("event_note", event.event_note);

                    String url = String.format(Locale.US, "%s/events/create", getResources().getString(R.string.server_ip));

                    JSONObject raw = new JSONObject(Tools.post(url, data));
                    if (raw.getInt("result") != Tools.RES_OK)
                        throw new Exception();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteAfterEdit(final String username, final String password) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();
        exec = Executors.newCachedThreadPool();

        exec.execute(new Runnable() {
            @Override
            public void run() {
                String url = String.format(Locale.US, "%s/events/disable", getResources().getString(R.string.server_ip));
                try {
                    String result = Tools.post(url, new JSONObject()
                            .put("username", username)
                            .put("password", password)
                            .put("event_id", event.event_id)
                    );

                    JSONObject json = new JSONObject(String.valueOf(result));
                    int resultNumber = json.getInt("result");
                    switch (resultNumber) {
                        case Tools.RES_OK:
                            Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show();
                            break;
                        case Tools.RES_SRV_ERR:
                            Toast.makeText(activity.getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                            break;
                        case Tools.RES_FAIL:
                            Toast.makeText(activity.getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_edit)
    public void onEditClick() {
        readOnly = false;
        refreshEditMode();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_cancel)
    public void onCancelClick() {
        dismiss();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_save)
    public void saveClick() {
        int calculated_time = event.start_time % 1000000;
        calculated_time = (event.start_time / 1000000 + 1) * 1000000 + calculated_time;

        event.start_time = calculated_time;
        event.repeat_mode = calculated_repeat_mode;
        event.event_note = txtEventNote.getText().toString();

        createEvent(event);
        if (delete)
            deleteAfterEdit(SignInActivity.loginPrefs.getString(SignInActivity.username, null), SignInActivity.loginPrefs.getString(SignInActivity.password, null));
        dismiss();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_delete)
    public void delete() {
        dismiss();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.txt_event_date)
    public void selectDate() {
        short day = (short) (event.start_time % 1000000 / 10000);
        short month = (short) (event.start_time % 100000000 / 1000000);
        short year = (short) (event.start_time / 100000000 + 2000);
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                short time_part = (short) (event.start_time % 10000);
                int date_part = ((y - 2000) * 100 + m) * 100 + d;
                event.start_time = date_part * 10000 + time_part;
                txtEventDate.setText(Tools.eventDateTimeToString(event.start_time)[0]);
            }
        }, year, month, day);
        datePicker.setTitle("Select date");
        datePicker.show();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.txt_event_time)
    public void selectTime() {
        short hour = (short) (event.start_time % 10000 / 100);

        TimePickerDialog mTimePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                event.start_time = ((event.start_time / 10000) * 100 + selectedHour) * 100;
                txtEventTime.setText(Tools.eventDateTimeToString(event.start_time)[1]);
            }
        }, hour, 0, true);

        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }
}
