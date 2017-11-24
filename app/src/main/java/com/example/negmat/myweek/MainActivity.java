package com.example.negmat.myweek;

import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static class AlarmNotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notification_channel");
            builder.setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(intent.getStringExtra("event_name") + " after 2 minutes")
                    .setContentText(intent.getStringExtra("event_note"))
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                    .setContentInfo("Info");

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(1, builder.build());
            else
                Log.e("ERROR", "notificationManager is null");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();

        Calendar cal = Calendar.getInstance();
        cal.set(2017, 10, 20, 2, 0, 0);
        //startAlarm(cal, "Event note");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0]; // only one message transferred
            Event event = null;
            try {
                event = Event.parseJson(new JSONObject(new String(message.getRecords()[0].getPayload())));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            EditViewDialog dialog = new EditViewDialog(event, true, new MyRunnable() {
                @Override
                public void run() {
                    updateClick(null);
                }
            });
            FragmentManager manager = getFragmentManager();
            dialog.show(manager, "Editdialog");

        }
    }

    // region Variables
    @BindView(R.id.btn_add_event)
    ImageButton btnAddEvent;
    @BindView(R.id.grid_fixed)
    GridLayout grid_fixed;
    @BindView(R.id.event_grid)
    protected GridLayout event_grid;

    static Calendar selCalDate = Calendar.getInstance(); //selected Calendar date, keeps changing
    TextView[][] tv = new TextView[8][24];
    Event[] events;
    LongSparseArray<Integer> eventIdIndexMap = new LongSparseArray<>();

    static ExecutorService exec;

    private int cellDimen = -1;
    // endregion

    // region Initialization Functions
    private void initialize() {
        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        cellDimen = width / grid_fixed.getColumnCount();
        //endregion

        selCalDate = Calendar.getInstance();
        String selectedWeek;
        if (selCalDate.get(Calendar.WEEK_OF_MONTH) % 5 == 0) {
            Calendar thisMonth = (Calendar) selCalDate.clone();
            thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK) - 1);

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1);

            selectedWeek = String.format(Locale.US,
                    "%s, %s",
                    thisMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                    nextMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            );
        } else selectedWeek = selCalDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        setTitle(selectedWeek);

        initGrid(); //initialize the grid view
        updateClick(null);
    }

    private void initGrid() {
        event_grid.removeAllViews();

        for (int col = 0; col < event_grid.getColumnCount(); col++)
            for (int row = 0; row < event_grid.getRowCount(); row++) {
                // Properties for all cells in the DataGridView
                tv[col][row] = new TextView(getApplicationContext());
                tv[col][row].setTextColor(getResources().getColor(R.color.event_text));
                tv[col][row].setBackgroundResource(R.drawable.bg_cell_empty);
                tv[col][row].setWidth(cellDimen);
                tv[col][row].setHeight(cellDimen);
                tv[col][row].setPadding(10, 5, 10, 5);
                tv[col][row].setTextSize(10f);
                tv[col][row].setMaxLines(1);
                tv[col][row].setEllipsize(TextUtils.TruncateAt.END);

                if (col == 0) { // First column properties (for time)
                    tv[col][row].setTypeface(null, Typeface.BOLD);
                    tv[col][row].setGravity(Gravity.CENTER_HORIZONTAL);
                    tv[col][row].setText(String.format(Locale.US, "%02d:00", row));
                }

                // Populate the prepared TextView
                event_grid.addView(tv[col][row]);
            }

        Calendar cal = (Calendar) selCalDate.clone();
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK) - 1);

        // region Update the fixed weekdays gridview
        if (grid_fixed.getChildCount() == 0) {
            // inflate first
            Space space = new Space(getApplicationContext());
            grid_fixed.addView(space, cellDimen, cellDimen);
            for (int i = 1; i < grid_fixed.getColumnCount(); i++) {
                TextView weekNames = new TextView(getApplicationContext());
                weekNames.setTextColor(Color.BLACK);
                weekNames.setBackgroundResource(R.drawable.bg_cell_empty);
                weekNames.setTypeface(null, Typeface.BOLD);
                weekNames.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0f);
                weekNames.setText(String.format(
                        Locale.US,
                        getResources().getString(R.string.weekday),
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
                ));
                cal.add(Calendar.DATE, 1);
                weekNames.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                weekNames.setWidth(cellDimen);
                weekNames.setHeight(cellDimen);
                grid_fixed.addView(weekNames);
            }
        } else {
            // simply update (already inflated)
            for (int i = 1; i < grid_fixed.getColumnCount(); i++) {
                TextView weekNames = (TextView) grid_fixed.getChildAt(i);
                weekNames.setText(String.format(
                        Locale.US,
                        getResources().getString(R.string.weekday),
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)
                ));
                cal.add(Calendar.DATE, 1);
            }
        }
    }

    private void populateGrid(Event[] events) {
        initGrid();

        Calendar today = Calendar.getInstance();
        for (Event event : events) {
            Calendar time = Tools.time2cal(event.start_time);

            int row = time.get(Calendar.HOUR_OF_DAY);
            int col = time.get(Calendar.DAY_OF_WEEK);

            tv[col][row].setBackgroundResource(time.before(today) ? R.drawable.bg_cell_inactive : R.drawable.bg_cell_active);
            tv[col][row].setText(event.event_name);
            tv[col][row].setTag(event.event_id);
        }

        for (int n = 0; n < event_grid.getColumnCount(); n++)
            for (int m = 0; m < event_grid.getRowCount(); m++)
                if (!tv[n][m].getText().toString().equals(""))
                    tv[n][m].setOnClickListener(onTextClick);
    }

    private View.OnClickListener onTextClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getTag() == null)
                return;

            long event_id = (long) view.getTag();
            int index = eventIdIndexMap.get(event_id);

            //make event global and show all info on it*/
            EditViewDialog dialog = new EditViewDialog(events[index], true, new MyRunnable() {
                @Override
                public void run() {
                    updateClick(null);
                }
            });
            FragmentManager manager = getFragmentManager();
            dialog.show(manager, "Editdialog");
        }
    };

    // endregion

    //region Options-Menu Buttons' handlers
    public void navNextWeekClick(MenuItem item) {
        selCalDate.add(Calendar.DATE, 7);
        String selectedWeek;
        if (selCalDate.get(Calendar.WEEK_OF_MONTH) % 5 == 0) {
            Calendar thisMonth = (Calendar) selCalDate.clone();
            thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK) - 1);

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1);

            selectedWeek = String.format(Locale.US,
                    "%s, %s",
                    thisMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                    nextMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            );
        } else selectedWeek = selCalDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        setTitle(selectedWeek);
        updateClick(null);
    }

    public void navPrevWeekClick(MenuItem item) {
        selCalDate.add(Calendar.DATE, -7);
        String selectedWeek;
        if (selCalDate.get(Calendar.WEEK_OF_MONTH) % 5 == 0) {
            Calendar thisMonth = (Calendar) selCalDate.clone();
            thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK) - 1);

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1);

            selectedWeek = String.format(Locale.US,
                    "%s, %s",
                    thisMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                    nextMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            );
        } else selectedWeek = selCalDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        setTitle(selectedWeek);
        updateClick(null);
    }

    public void onLogoutClick(MenuItem item) {
        SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
        editor.clear();
        editor.apply();
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    public void updateClick(MenuItem item) {
        if (exec != null && !exec.isShutdown() && !exec.isTerminated())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        exec.execute(new Runnable() {
            @Override
            public void run() {
                Calendar c = (Calendar) selCalDate.clone();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);

                c.add(Calendar.DATE, c.getFirstDayOfWeek() - c.get(Calendar.DAY_OF_WEEK) - 1);
                int period_from = Tools.cal2time(c);

                c.set(Calendar.HOUR_OF_DAY, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                c.add(Calendar.DATE, 6);
                int period_till = Tools.cal2time(c);

                JSONObject body = null;
                try {
                    body = new JSONObject()
                            .put("username", SignInActivity.loginPrefs.getString(SignInActivity.username, null))
                            .put("password", SignInActivity.loginPrefs.getString(SignInActivity.password, null))
                            .put("period_from", period_from)
                            .put("period_till", period_till);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new MyRunnable(
                        Tools.post(String.format(Locale.US, "%s/events/fetch", getResources().getString(R.string.server_ip)), body)
                ) {
                    @Override
                    public void run() {
                        try {
                            JSONObject json = new JSONObject((String) args[0]);
                            int resultNumber = json.getInt("result");
                            if (resultNumber == Tools.RES_OK) {
                                JSONArray jarray = json.getJSONArray("array");
                                events = new Event[jarray.length()];

                                if (jarray.length() != 0) {
                                    for (int n = 0; n < jarray.length(); n++) {
                                        events[n] = Event.parseJson(jarray.getJSONObject(n));
                                        eventIdIndexMap.put(events[n].event_id, n);
                                    }
                                    populateGrid(events);
                                } else {
                                    initGrid();
                                    Toast.makeText(MainActivity.this, "Empty week", Toast.LENGTH_SHORT).show();
                                }
                            } else
                                Log.e("ERROR", "Code: " + resultNumber);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void onShareEventClick(MenuItem item) {
        FragmentManager manager = getFragmentManager();
        GroupEvents GroupEventsDialog = new GroupEvents();
        GroupEventsDialog.show(manager, "Dialog");
    }

    public void settingsClick(MenuItem item) {

    }

    //endregion


    public void addEventClick(View view) {
        FragmentManager manager = getFragmentManager();
        SpeechDialog dialog = new SpeechDialog();
        dialog.show(manager, "Dialog");
    }

    public void selectWeekClick(MenuItem item) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                selCalDate.set(year, month, day);
                String selectedWeek;
                if (selCalDate.get(Calendar.WEEK_OF_MONTH) % 5 == 0) {
                    Calendar thisMonth = (Calendar) selCalDate.clone();
                    thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK) - 1);

                    Calendar nextMonth = (Calendar) thisMonth.clone();
                    nextMonth.add(Calendar.MONTH, 1);

                    selectedWeek = String.format(Locale.US,
                            "%s, %s",
                            thisMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                            nextMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                    );
                } else
                    selectedWeek = selCalDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
                setTitle(selectedWeek);
                updateClick(null);
            }
        };

        DatePickerDialog dialog = new DatePickerDialog(this, listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

}

