package com.example.negmat.myweek;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diaog_editview, container, false);
        ButterKnife.bind(this, view);

        initialize();
        return view;
    }

    public EditViewDialog(Event event, boolean readOnly) {
        this.event = event;
        this.readOnly = readOnly;
        this.calc_calendar = Tools.time2cal(event.start_time);
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
                    calc_calendar.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    txtEventDate.setText(Tools.decode_time(calc_calendar)[0]);
                    compoundButton.setTextColor(Color.WHITE);
                } else
                    compoundButton.setTextColor(Color.BLACK);
            }
        };

        calc_calendar = Tools.time2cal(event.start_time);

        for (int n = 0; n < weekdaysParent.getChildCount(); n++) {
            RadioButton button = (RadioButton) weekdaysParent.getChildAt(n);

            button.setOnCheckedChangeListener(listener);
            day2btn.put(str2day.get(String.valueOf(button.getTag())), button);

            if (event.day == calc_calendar.get(Calendar.DAY_OF_WEEK))
                weekdaysParent.check(button.getId());
        }
        // endregion

        refreshEditMode();
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

    private boolean readOnly = false;
    private Event event;

    private Calendar calc_calendar = Calendar.getInstance();

    private HashMap<String, Integer> str2day = new HashMap<>();
    private SparseArray<String> day2str = new SparseArray<>();
    private SparseArray<RadioButton> day2btn = new SparseArray<>();
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

        for (int n = 0; n < weekdaysParent.getChildCount(); n++)
            weekdaysParent.getChildAt(n).setClickable(!readOnly);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).updateClick(null);
    }

    private void createEvent(Event event) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        exec.execute(new MyRunnable(event, getActivity()) {
            @Override
            public void run() {
                try {
                    Event event = (Event) args[0];
                    final Activity activity = (Activity) args[1];

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

                    Log.e("CREATED TIME", event.start_time + "");

                    String url = String.format(Locale.US, "%s/events/create", getResources().getString(R.string.server_ip));

                    JSONObject raw = new JSONObject(Tools.post(url, data));
                    if (raw.getLong("result") != Tools.RES_OK)
                        throw new Exception();

                    activity.runOnUiThread(new MyRunnable(Tools.time2cal(event.start_time), activity) {
                        @Override
                        public void run() {
                            Calendar c = (Calendar) args[0];
                            Toast.makeText((Activity) args[1], String.format(Locale.US, "Event has been created on %d/%d/%d at %02d:%02d",
                                    c.get(Calendar.DAY_OF_MONTH),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE)),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
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
        event.start_time = Tools.cal2time(calc_calendar);
        event.day = calc_calendar.get(Calendar.DAY_OF_WEEK);
        event.event_note = txtEventNote.getText().toString();

        createEvent(event);
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
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), 0, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                calc_calendar.set(y, m, d);
                txtEventDate.setText(Tools.decode_time(calc_calendar)[0]);
                RadioButton button = day2btn.get(calc_calendar.get(Calendar.DAY_OF_WEEK));
                ((RadioGroup) button.getParent()).check(button.getId());
            }
        }, calc_calendar.get(Calendar.YEAR), calc_calendar.get(Calendar.MONTH), calc_calendar.get(Calendar.DAY_OF_MONTH));

        datePicker.setTitle("Select date");
        datePicker.show();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.txt_event_time)
    public void selectTime() {
        Calendar c = Tools.time2cal(event.start_time);

        TimePickerDialog timePicker = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int h, int m) {
                calc_calendar.set(Calendar.HOUR_OF_DAY, h);
                calc_calendar.set(Calendar.MINUTE, 0);
                calc_calendar.set(Calendar.SECOND, 0);

                txtEventTime.setText(Tools.decode_time(calc_calendar)[1]);
            }
        }, c.get(Calendar.HOUR_OF_DAY), 0, true);

        timePicker.setTitle("Select Time");
        timePicker.show();
    }
}
