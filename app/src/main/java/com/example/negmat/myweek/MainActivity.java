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
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity{

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

    // region Variables
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
    @BindView(R.id.event_grid)
    protected GridLayout event_grid;

    private String[] weekDays;
    static Calendar selCalDate = Calendar.getInstance(); //selected Calendar date, keeps changing
    TextView[][] tv = new TextView[8][25];
    Event[] events;//global array of events to use in several functions

    private final short GRID_ADDITEM = 2;
    private final short GRID_DELETEITEM = 3;

    static ExecutorService exec;
    // endregion

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }

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
                updateWeekView();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // region Initialization Functions
    private void initialize() {
        //Initialize current time and current week
        Calendar c = Calendar.getInstance(); //always shows current date
        String currentDateString = DateFormat.getDateInstance().format(new Date());
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", c.get(Calendar.WEEK_OF_MONTH), c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), c.get(Calendar.YEAR));
        txtCurrentDate.setText(currentDateString);
        txtSelectedWeek.setText(selectedWeek);

        initGrid(); //initialize the grid view
        updateWeekView();
    }

    private void initGrid() {
        // TODO: download and saveClick the events of the user to a variable


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

        for (int col = 0; col < event_grid.getColumnCount(); col++) {
            for (int row = 0; row < event_grid.getRowCount(); row++) {
                // Properties for all cells in the DataGridView
                tv[col][row] = new TextView(getApplicationContext());
                tv[col][row].setTextColor(getResources().getColor(R.color.event_text_color));
                tv[col][row].setBackgroundResource(R.drawable.cell_shape);
                tv[col][row].setWidth(cellDimen);
                tv[col][row].setHeight(cellDimen);
                tv[col][row].setTextSize(10f);
                tv[col][row].setMaxLines(1);
                tv[col][row].setEllipsize(TextUtils.TruncateAt.END);

                if (col == 0) { // First column properties (for time)
                    tv[col][row].setTypeface(null, Typeface.BOLD);
                    tv[col][row].setGravity(Gravity.CENTER_HORIZONTAL);
                    tv[col][row].setText(String.format(Locale.US, "%02d:00", row % 24));
                } else {
                    tv[col][row].setTextColor(Color.BLUE);
                }

                // Populate the prepared TextView
                event_grid.addView(tv[col][row]);
            }
        }
    }

    private void populateGrid(Event[] events, int from, int till) {
        initGrid();

        for (Event event : events) {
            //TODO: assign each event to its appropriate cell
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

            //make event global and show all info on it*/
            ViewEventDialog ved = new ViewEventDialog(MainActivity.this, events[index]);
            ved.show();
            ved.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    updateWeekView();
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
            updateWeekView();
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
        updateWeekView();
    }

    // Left buttin handling
    @OnClick(R.id.btn_arrow_left)
    public void weekLeft() {
        selCalDate.add(Calendar.DATE, -7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
        updateWeekView();
    }

    //endregion

    public void addEventClick(View view) {
        FragmentManager manager = getFragmentManager();
        AddEventDialog dialog = new AddEventDialog();
        dialog.show(manager, "Dialog");
    }

    public void updateWeekView() {
        if (exec != null && !exec.isShutdown() && !exec.isTerminated())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        exec.execute(new Runnable() {
            @Override
            public void run() {
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

                String usrName = SignInActivity.loginPrefs.getString(SignInActivity.username, null);
                String usrPassword = SignInActivity.loginPrefs.getString(SignInActivity.password, null);

                JSONObject body = null;
                try {
                    body = new JSONObject()
                            .put("username", usrName)
                            .put("password", usrPassword)
                            .put("period_from", start_date)
                            .put("period_till", end_date);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final String result = Tools.post("http://165.246.165.130:2222/events/fetch", body);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject json = new JSONObject(String.valueOf(result));
                            int resultNumber = json.getInt("result");
                            if (resultNumber == Tools.RES_OK) {
                                JSONArray jarray = json.getJSONArray("array");
                                events = new Event[jarray.length()];
                                if (jarray.length() != 0) {
                                    for (int n = 0; n < jarray.length(); n++)
                                        events[n] = Event.parseJson(jarray.getJSONObject(n));
                                    populateGrid(events, start_date, end_date);
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

}
