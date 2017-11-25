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
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
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
        initialize();
        setUpNFCSender();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0]; // only one message transferred
            Event event;
            try {
                JSONObject object = new JSONObject(new String(message.getRecords()[0].getPayload()));

                if (object.getInt(Tools.KEY_NFC_SINGLE) == Tools.NFC_SINGLE) {
                    event = Event.parseJson(object);
                    new EditViewDialog(event, true, new MyRunnable() {
                        @Override
                        public void run() {
                            updateClick(null);
                        }
                    }).show(getFragmentManager(), "Editdialog");
                } else if (object.getInt(Tools.KEY_NFC_GROUP) != Tools.NFC_GROUP) {
                    // must not happen
                    throw new JSONException("JSON from NFC doesn't contain field " + Tools.KEY_NFC_GROUP);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == speechDialog.REQUEST_MICROPHONE)
            if (grantResults[0] == 0)
                speechDialog.startSpeechToText();
            else speechDialog.dismiss();
    }

    // region Variables
    private GridLayout grid_fixed;
    private GridLayout event_grid;

    private SpeechDialog speechDialog;

    private static Calendar selCalDate;
    private TextView[][] tv = new TextView[8][24];
    private Event[] events;
    private LongSparseArray<Integer> eventIdIndexMap = new LongSparseArray<>();
    private static ExecutorService exec;
    private int cellDimen = -1;
    // endregion

    private void initialize() {
        // region Initialize UI Variables
        grid_fixed = findViewById(R.id.grid_fixed);
        event_grid = findViewById(R.id.event_grid);
        // endregion

        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        cellDimen = width / grid_fixed.getColumnCount();
        //endregion

        selCalDate = Calendar.getInstance();
        selCalDate.setFirstDayOfWeek(Calendar.MONDAY);
        Log.e("TODAY", selCalDate.getTime().toString()+"");
        String selectedWeek;

        // region Update the fixed weekdays gridview
        Calendar cal = (Calendar) selCalDate.clone();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            cal.add(Calendar.DATE, -6);
        else
            cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));

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
        //endregion

        //check if current week consists of two months
        if (Tools.twoMonthsWeek(selCalDate)) {
            Calendar thisMonth = (Calendar) selCalDate.clone();

            //identifying the month of first day of week(Monday)
            if (thisMonth.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                thisMonth.add(Calendar.DATE, -6);
            else
                thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK));

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1); //identifying the next month of currnet week

            selectedWeek = String.format(Locale.US,
                    "%s, %s",
                    thisMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US),
                    nextMonth.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
            );
        } else selectedWeek = selCalDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);

        setTitle(selectedWeek);
        initGrid();
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


        // region Update the fixed weekdays gridview
        Calendar cal = (Calendar) selCalDate.clone();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            cal.add(Calendar.DATE, -6);
        else
            cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));

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
        //endregion
    }

    private void populateGrid() {
        initGrid();

        Calendar today = Calendar.getInstance();
        today.setFirstDayOfWeek(Calendar.MONDAY);
        for (Event event : events) {
            Calendar time = Tools.time2cal(event.start_time);

            int row = time.get(Calendar.HOUR_OF_DAY);
            int col;
            if (time.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                col = 7;
            else
                col = time.get(Calendar.DAY_OF_WEEK) - 1;

            tv[col][row].setBackgroundResource(time.before(today) ? R.drawable.bg_cell_inactive : R.drawable.bg_cell_active);
            tv[col][row].setText(event.event_name);
            tv[col][row].setTag(event.event_id);
        }

        View.OnClickListener onCellClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag() == null)
                    return;

                long event_id = (long) view.getTag();
                int index = eventIdIndexMap.get(event_id);
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

        for (int n = 0; n < event_grid.getColumnCount(); n++)
            for (int m = 0; m < event_grid.getRowCount(); m++)
                if (!tv[n][m].getText().toString().equals(""))
                    tv[n][m].setOnClickListener(onCellClick);
    }

    private void setUpNFCSender() {
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "Sorry this device does not have NFC.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mAdapter.isEnabled())
            Toast.makeText(this, "Please enable NFC via Settings.", Toast.LENGTH_LONG).show();

        mAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
            @Override
            public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                try {
                    JSONObject msg = new JSONObject();
                    msg.put(Tools.KEY_NFC_GROUP, Tools.NFC_GROUP);
                    msg.put("username", SignInActivity.loginPrefs.getString(SignInActivity.username, null));
                    return new NdefMessage(NdefRecord.createMime("text/plain", msg.toString().getBytes()));
                } catch (JSONException | NullPointerException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }, this);
    }

    public void newVoiceEventClick(View view) {
        FragmentManager manager = getFragmentManager();
        speechDialog = new SpeechDialog();
        speechDialog.show(manager, "Speech-dialog");
    }

    //region Options-Menu Buttons' handlers
    public void selectWeekClick(MenuItem item) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Tools.disable_touch(MainActivity.this);

                selCalDate.set(year, month, day);
                String selectedWeek;

                //check if current week consists of two months
                if (Tools.twoMonthsWeek(selCalDate)) {
                    Calendar thisMonth = (Calendar) selCalDate.clone();

                    //identifying the month of first day of week(Monday)
                    if (thisMonth.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                        thisMonth.add(Calendar.DATE, -6);
                    else
                        thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK));

                    Calendar nextMonth = (Calendar) thisMonth.clone();
                    nextMonth.add(Calendar.MONTH, 1); //identifying the next month of currnet week

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

    public void navNextWeekClick(MenuItem item) {
        Tools.disable_touch(this);

        selCalDate.add(Calendar.DATE, 7);
        String selectedWeek;

        //check if current week consists of two months
        if (Tools.twoMonthsWeek(selCalDate)) {
            Calendar thisMonth = (Calendar) selCalDate.clone();

            //identifying the month of first day of week(Monday)
            if (thisMonth.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                thisMonth.add(Calendar.DATE, -6);
            else
                thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK));

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1); //identifying the next month of currnet week

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
        Tools.disable_touch(this);

        selCalDate.add(Calendar.DATE, -7);
        String selectedWeek;

        //check if current week consists of two months
        if (Tools.twoMonthsWeek(selCalDate)) {
            Calendar thisMonth = (Calendar) selCalDate.clone();

            //identifying the month of first day of week(Monday)
            if (thisMonth.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                thisMonth.add(Calendar.DATE, -6);
            else
                thisMonth.add(Calendar.DATE, selCalDate.getFirstDayOfWeek() - selCalDate.get(Calendar.DAY_OF_WEEK));

            Calendar nextMonth = (Calendar) thisMonth.clone();
            nextMonth.add(Calendar.MONTH, 1); //identifying the next month of currnet week

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
        Tools.disable_touch(this);

        if (exec != null && !exec.isShutdown() && !exec.isTerminated())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        exec.execute(new Runnable() {
            @Override
            public void run() {
                Calendar cal = (Calendar) selCalDate.clone();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);

                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    cal.add(Calendar.DATE, -6);
                else
                    cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));
                int period_from = Tools.cal2time(cal);

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.add(Calendar.DATE, 6);
                int period_till = Tools.cal2time(cal);

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
                                    populateGrid();
                                } else {
                                    initGrid();
                                    Toast.makeText(MainActivity.this, "Empty week", Toast.LENGTH_SHORT).show();
                                }
                            } else
                                Log.e("ERROR", "Code: " + resultNumber);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Tools.enable_touch(MainActivity.this);
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
}

