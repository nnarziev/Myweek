package com.example.negmat.myweek;

import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
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
    // endregion

    // region Initialization Functions
    private void initialize() {
        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        String[] weekDays = new String[]{"", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        int width = size.x;
        int cellDimen = width / grid_fixed.getColumnCount();

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

        Calendar c = Calendar.getInstance();
        String selectedWeek = String.format(Locale.US, "Week %d, %s.", c.get(Calendar.WEEK_OF_MONTH), c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()).substring(0, 3));
        setTitle(selectedWeek);

        initGrid(); //initialize the grid view
        updateClick(null);
    }

    private void initGrid() {
        event_grid.removeAllViews();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        int cellDimen = width / event_grid.getColumnCount();

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
                    tv[col][row].setText(String.format(Locale.US, "%02d:00", row));
                } else {
                    tv[col][row].setTextColor(Color.BLUE);
                }

                // Populate the prepared TextView
                event_grid.addView(tv[col][row]);
            }
        }
    }

    private void populateGrid(Event[] events) {
        initGrid();

        for (Event event : events) {
            Calendar calendar = Tools.time2cal(event.start_time);

            Log.e("POPULATE TIME", event.start_time + "");

            int row = calendar.get(Calendar.HOUR_OF_DAY);
            int col = calendar.get(Calendar.DAY_OF_WEEK);

            tv[col][row].setBackgroundResource(R.drawable.single_mode_bg);
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
            EditViewDialog dialog = new EditViewDialog(events[index], true);
            FragmentManager manager = getFragmentManager();
            dialog.show(manager, "Editdialog");
        }
    };

    // endregion

    //region Options-Menu Buttons' handlers
    public void navNextWeekClick(MenuItem item) {
        selCalDate.add(Calendar.DATE, 7);
        String selectedWeek = String.format(Locale.US, "Week %d, %s.", selCalDate.get(Calendar.WEEK_OF_MONTH),
                selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        setTitle(selectedWeek);
        updateClick(null);
    }

    public void navPrevWeekClick(MenuItem item) {
        selCalDate.add(Calendar.DATE, -7);
        String selectedWeek = String.format(Locale.US, "Week %d, %s.", selCalDate.get(Calendar.WEEK_OF_MONTH),
                selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
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

                c.add(Calendar.DATE, c.getFirstDayOfWeek() - c.get(Calendar.DAY_OF_WEEK));
                int period_from = Tools.cal2time(c);
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
                String selectedWeek = String.format(Locale.US, "Week %d, %s.",
                        selCalDate.get(Calendar.WEEK_OF_MONTH),
                        selCalDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));

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
