package com.example.negmat.myweek;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int cellDimen = width / grid_fixed.getColumnCount();
        weekDays = new String[]{"", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        for (int i = 0; i < grid_fixed.getColumnCount(); i++) {
            TextView weekNames = new TextView(getApplicationContext());
            weekNames.setTextColor(Color.BLACK);
            weekNames.setBackgroundResource(R.drawable.cell_shape);
            weekNames.setTypeface(null, Typeface.BOLD);
            weekNames.setText(weekDays[i]);
            weekNames.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            weekNames.setWidth(cellDimen);
            weekNames.setHeight(cellDimen);
            grid_fixed.addView(weekNames);
        }
        //endregion

        initialize();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    // region UI Variables
    @BindView(R.id.btn_add_event)
    ImageButton btnAddEvent;
    @BindView(R.id.txt_current_date)
    TextView txtCurrentDate;
    @BindView(R.id.txt_selected_week)
    TextView txtSelectedWeek;
    @BindView(R.id.btn_arrow_left)
    ImageButton btnArrowLeft;
    @BindView(R.id.btn_arrow_right)
    ImageButton btnArrowRight;
    @BindView(R.id.grid_fixed)
    GridLayout grid_fixed;
    // endregion

    // region Options menu create function
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }
    // endregion

    //region Options menu item selected function
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.sync:
                sendStartDate();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    // region Tools
    private final short GRID_ADDITEM = 2;
    private final short GRID_DELETEITEM = 3;
    // endregion

    // region Variables
    @BindView(R.id.event_grid)
    protected GridLayout event_grid;
    private String[] weekDays;
    private int hour = 1;
    private int count = 0;//counter for change day half
    static Calendar selCalDate = Calendar.getInstance(); //selected Calendar date, keeps changing
    TextView[][] tv = new TextView[8][25];
    Event[] events;//global array of events to use in several functions
    // endregion

    // region Initialization Functions
    private void initialize() {
        //Initialize current time and current week
        Calendar c = Calendar.getInstance(); //always shows current date
        String currentDateString = DateFormat.getDateInstance().format(new Date());
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", c.get(Calendar.WEEK_OF_MONTH), c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), c.get(Calendar.YEAR));
        txtCurrentDate.setText(currentDateString);
        txtSelectedWeek.setText(selectedWeek);

        initGrid(); //initialize the grid view
        sendStartDate();
    }

    private void initGrid() {
        // TODO: download and save the events of the user to a variable


        // clean out the gridlayout
        event_grid.removeAllViews();

        // TODO: set downloaded events into gridlayout
        //int gridWidth = event_grid.getMeasuredWidth();


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;


        int cellDimen = width / event_grid.getColumnCount();
        int height = cellDimen * event_grid.getRowCount();
        {

            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {
                    // TODO: check for existing data downloaded from server

                    // TODO: case where no data exists
                    //TextView space = new TextView(getApplicationContext());
                    tv[n][m] = new TextView(getApplicationContext());
                    tv[n][m].setTextColor(Color.BLACK);
                    tv[n][m].setBackgroundResource(R.drawable.cell_shape);
                    if (m == 0 && n < 8) {
                        tv[n][m].setTypeface(null, Typeface.BOLD);
                        tv[n][m].setText(weekDays[n]);
                        tv[n][m].setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    if (n == 0 && m > 0) {
                        if (count == 0) {
                            tv[n][m].setTypeface(null, Typeface.BOLD);
                            tv[n][m].setGravity(Gravity.CENTER_HORIZONTAL);
                            tv[n][m].setText(hour + "am");
                        } else {
                            tv[n][m].setTypeface(null, Typeface.BOLD);
                            tv[n][m].setGravity(Gravity.CENTER_HORIZONTAL);
                            tv[n][m].setText(hour + "pm");
                            if (hour == 11)
                                count = -1;
                        }

                        if (hour == 11)
                            count++;
                        if (hour == 12) {
                            hour = 0;
                        }
                        hour++;
                    }


                    tv[n][m].setWidth(cellDimen);
                    tv[n][m].setHeight(cellDimen);

                    //GridLayout.Spec row1 = GridLayout.spec(0, 2);
                    //event_grid.addView(space);
                    /*
                    if (n == 0 && m == 0) {
                        Button btn=new Button(getApplicationContext());
                        GridLayout.LayoutParams param =new GridLayout.LayoutParams();
                        param.height = GridLayout.LayoutParams.WRAP_CONTENT;
                        param.width = GridLayout.LayoutParams.WRAP_CONTENT;
                        param.columnSpec = GridLayout.spec(n,2);
                        param.rowSpec = GridLayout.spec(m);
                        event_grid.addView(btn,param);
                    }*/
                    //btn.setLayoutParams(param);


                    event_grid.addView(tv[n][m]);
                }
            }
        }
    }

    private void populateGrid(Event[] events, int from, int till) {
        initGrid();

        for (Event event : events) {
            //TODO: assign each even to its appropriate cell
            int event_start_time = event.getStart_time();
            short time = (short) (event_start_time % 10000 / 100);
            short day = (short) (event_start_time % 1000000 / 10000);
            short month = (short) ((event_start_time % 100000000 / 1000000));
            short year = (short) (event_start_time / 100000000);

            Calendar calendar = Calendar.getInstance();
            calendar.set(year + 2000, month, day);
            short day_of_week = (short) calendar.get(Calendar.DAY_OF_WEEK);

            tv[day_of_week][time].setBackgroundResource(R.color.color_single_mode);
            tv[day_of_week][time].setText(event.getEvent_name());
            tv[day_of_week][time].setTag(event.getEvent_id());

        }
        for (int n = 0; n < event_grid.getColumnCount(); n++) {
            for (int m = 0; m < event_grid.getRowCount(); m++) {
                if (!tv[n][m].getText().toString().equals("")) {
                    tv[n][m].setOnClickListener(onTextClick);
                }
            }
        }
    }

    private View.OnClickListener onTextClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TextView textView = ((TextView) view);
            if (textView.getTag() == null)
                return;
            long event_id = ((long) textView.getTag());

            int index = 0;//index of event in array
            //take an index of event
            //if current event_id==event[index].getEventId()
            for (int i = 0; i < events.length; i++) {
                if (event_id == events[i].getEvent_id()) {
                    index = i;
                    break;
                }
            }

            /*int event_start_time = events[index].getStart_time();
            short time = (short) (event_start_time % 10000 / 100);
            short day = (short) (event_start_time % 1000000 / 10000);
            short month = (short) ((event_start_time % 100000000 / 1000000));
            short year = (short) (event_start_time / 100000000);
            Calendar cal = Calendar.getInstance();
            cal.set(year + 2000, month, day);

            String event_name = events[index].getEvent_name();
            String note = events[index].getEvent_note();
            int duration = events[index].getLength();
            int repeat = events[index].getRepeat_mode();
            String reason = events[index].getReason();

            if (reason == null)
                reason = "no reason";

            @SuppressLint("DefaultLocale") String eventInfo = String.format("%s\n%s\nDate: %s. %d, %d\nFrom: %d:00\nDuration: %d min\nRepeat: %d times\nReason: %s", event_name, note,
                    cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR), time, duration, repeat, reason);

            //make event global and show all info on it*/
            ViewEventDialog ved = new ViewEventDialog(MainActivity.this, events[index]);
            ved.show();
            ved.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    sendStartDate();
                }
            });
        }
    };

    private void updateGrid(short action, short cellX, short cellY) {
        if (action == GRID_ADDITEM) {

        } else if (action == GRID_DELETEITEM) {

        }
    }

    // endregion

    // region Add an event button handling
    @OnClick(R.id.btn_add_event)
    public void addEvent() {
        FragmentManager manager = getFragmentManager();
        AddEventDialog dialog = new AddEventDialog();
        dialog.show(manager, "Dialog");

    }
    // endregion

    //region Date picker dialog
    int DIALOG_DATE = 1;
    int myYear = selCalDate.get(Calendar.YEAR);
    int myMonth = selCalDate.get(Calendar.MONTH);
    int myDay = selCalDate.get(Calendar.DAY_OF_MONTH);

    @OnClick(R.id.txt_selected_week)
    public void SelectWeek() {
        showDialog(DIALOG_DATE);
    }

    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_DATE) {
            DatePickerDialog tpd = new DatePickerDialog(this, myCallBack, myYear, myMonth, myDay);
            return tpd;
        }
        return super.onCreateDialog(id);
    }

    DatePickerDialog.OnDateSetListener myCallBack = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            selCalDate.set(year, monthOfYear, dayOfMonth);
            @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                    selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), selCalDate.get(Calendar.YEAR));
            txtSelectedWeek.setText(selectedWeek);
            sendStartDate();
        }
    };
    //endregion

    //region Arrows buttons handling

    // Right button handling
    @OnClick(R.id.btn_arrow_right)
    public void weekRight() {
        selCalDate.add(Calendar.DATE, 7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
        sendStartDate();
    }

    // Left buttin handling
    @OnClick(R.id.btn_arrow_left)
    public void weekLeft() {
        selCalDate.add(Calendar.DATE, -7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
        sendStartDate();
    }

    //endregion

    //region Function to send start date of week
    public void sendStartDate() {
        int week_start_hour = 1;
        int week_end_hour = 24;
        int minute = 0;
        int move = selCalDate.get(Calendar.DAY_OF_WEEK) - selCalDate.getFirstDayOfWeek();
        Calendar c = Calendar.getInstance();
        c.setTime(selCalDate.getTime());
        c.add(Calendar.DATE, -move);
        final int start_date = (c.get(Calendar.YEAR) - 2000) * 100000000 + (c.get(Calendar.MONTH) + 1) * 1000000 + c.get(Calendar.DAY_OF_MONTH) * 10000 + week_start_hour * 100 + minute;
        c.add(Calendar.DATE, 6);
        final int end_date = (c.get(Calendar.YEAR) - 2000) * 100000000 + (c.get(Calendar.MONTH) + 1) * 1000000 + c.get(Calendar.DAY_OF_MONTH) * 10000 + week_end_hour * 100 + minute;

        final String usrName = SignInActivity.loginPrefs.getString("Login", null);
        final String usrPassword = SignInActivity.loginPrefs.getString("Password", null);

        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("username", usrName);
        jsonSend.addProperty("password", usrPassword);
        jsonSend.addProperty("period_from", start_date);
        jsonSend.addProperty("period_till", end_date);
        String url = "http://qobiljon.pythonanywhere.com/events/fetch";
        Ion.with(getApplicationContext())
                .load("POST", url)
                .addHeader("Content-Type", "application/json")
                .setJsonObjectBody(jsonSend)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //process data or error
                        try {
                            JSONObject json = new JSONObject(String.valueOf(result));
                            int resultNumber = json.getInt("result");
                            switch (resultNumber) {
                                case Tools.RES_OK:
                                    //TODO: take array of JSON objects and return this
                                    //JSONObject js = new JSONObject(String.valueOf(jArray));
                                    final JSONArray jarray = json.getJSONArray("array");
                                    events = new Event[jarray.length()];
                                    if (jarray.length() != 0) {
                                        for (int n = 0; n < jarray.length(); n++)
                                            events[n] = Event.parseJson(jarray.getJSONObject(n));

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                populateGrid(events, start_date, end_date);
                                            }
                                        });
                                    } else {
                                        initGrid();
                                        Toast.makeText(MainActivity.this, "Empty week", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case Tools.RES_SRV_ERR:
                                    Toast.makeText(getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case Tools.RES_FAIL:
                                    Toast.makeText(getApplicationContext(), "Failure", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e1) {
                            Log.wtf("json", e1);
                        }
                    }
                });

    }
    //endregion
}
