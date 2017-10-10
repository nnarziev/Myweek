package com.example.negmat.myweek_1;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.renderscript.Sampler;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.SupportMenuInflater;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.MultipartBodyBuilder;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity {

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

    public static final String PREFS_NAME = "UserLogin";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);



        initialize();

        /*Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.finish();
                    }
                });
            }
        });*/
    }

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
                SharedPreferences shPref = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = shPref.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(MainActivity.this, SignIn.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.sync:
                //initialize();
                sendStartDate();
                /*Executor exec = Executors.newCachedThreadPool();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        String res = raw_json("http://qobiljon.pythonanywhere.com/users/login", 1710020100, 1710022400);
                        Log.v("RESULT: ", res);
                    }
                });*/
                //Toast.makeText(this, "Syncronized", Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    // region Constants
    private final short GRID_ADDITEM = 2;
    private final short GRID_DELETEITEM = 3;
    private final short RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;
    // endregion

    // region Variables
    @BindView(R.id.event_grid)
    protected GridLayout event_grid;
    private String[] weekDays;
    private int hour = 1;
    private int count = 0;//counter for change day half
    Calendar selCalDate = Calendar.getInstance(); //selected Calendar date, keeps changing
    // endregion

    // region Initialization Functions
    private void initialize() {
        //Initialize current time and current week
        Calendar c = Calendar.getInstance(); //always shows current date
        String currentDateString = DateFormat.getDateInstance().format(new Date());
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", c.get(Calendar.WEEK_OF_MONTH), new DateFormatSymbols().getMonths()[c.get(Calendar.MONTH)].substring(0, 3), c.get(Calendar.YEAR));
        txtCurrentDate.setText(currentDateString);
        txtSelectedWeek.setText(selectedWeek);


        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int cellDimen = width / grid_fixed.getColumnCount();
        weekDays = new String[]{"", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (int i = 0; i < grid_fixed.getColumnCount(); i++) {
            TextView weekNames = new TextView(getApplicationContext());
            weekNames.setBackgroundResource(R.drawable.cell_shape);
            weekNames.setTypeface(null, Typeface.BOLD);
            weekNames.setText(weekDays[i]);
            weekNames.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            weekNames.setWidth(cellDimen);
            weekNames.setHeight(cellDimen);
            grid_fixed.addView(weekNames);
        }
        //endregion

        initGrid(); //initialize the grid view
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
        {

            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {
                    // TODO: check for existing data downloaded from server

                    // TODO: case where no data exists
                    final TextView space = new TextView(getApplicationContext());
                    space.setBackgroundResource(R.drawable.cell_shape);
                    if (m == 0 && n < 8) {
                        space.setTypeface(null, Typeface.BOLD);
                        space.setText(weekDays[n]);
                        space.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    if (n == 0 && m > 0) {
                        if (count == 0) {
                            space.setTypeface(null, Typeface.BOLD);
                            space.setGravity(Gravity.CENTER_HORIZONTAL);
                            space.setText(hour + "am");
                        } else {
                            space.setTypeface(null, Typeface.BOLD);
                            space.setGravity(Gravity.CENTER_HORIZONTAL);
                            space.setText(hour + "pm");
                            if (hour == 11)
                                count = -1;
                        }

                        space.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ViewEventDialog ved=new ViewEventDialog(MainActivity.this,space.getText().toString());
                                ved.show();
                            }
                        });

                        if (hour == 11)
                            count++;
                        if (hour == 12) {
                            hour = 0;
                        }
                        hour++;
                    }

                    space.setWidth(cellDimen);
                    space.setHeight(cellDimen);
                    event_grid.addView(space);
                }
            }
        }


    }

    private void updateGrid(short action, short cellX, short cellY) {

        // aoidjsoidjasoijdoiasj
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
    int myYear = selCalDate.get(selCalDate.YEAR);
    int myMonth = selCalDate.get(selCalDate.MONTH);
    int myDay = selCalDate.get(selCalDate.DAY_OF_MONTH);

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
                    new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
            txtSelectedWeek.setText(selectedWeek);
        }
    };
    //endregion

    //region Arrows buttons handling

    // Right button handling
    @OnClick(R.id.btn_arrow_right)
    public void weekRight() {
        selCalDate.add(Calendar.DATE, 7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
    }

    //Lefat buttin handling
    @OnClick(R.id.btn_arrow_left)
    public void weekLeft() {
        selCalDate.add(Calendar.DATE, -7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
    }

    //endregion

    //region Function to send start date of week
    public void sendStartDate() {
        int week_start_hour = 1;
        int week_end_hour = 24;
        int minute = 0;
        int move = selCalDate.get(selCalDate.DAY_OF_WEEK) - selCalDate.getFirstDayOfWeek();
        Calendar c = selCalDate;
        c.add(selCalDate.DATE, -move + 1);

        //String start_date = String.format("%02d%02d%02d%02d%02d", c.get(c.YEAR)-2000, c.get(c.MONTH)+1, c.get(c.DAY_OF_MONTH), week_start_hour, minute);
        int start_date = (c.get(c.YEAR) - 2000) * 100000000 + (c.get(c.MONTH) + 1) * 1000000 + c.get(c.DAY_OF_MONTH) * 10000 + week_start_hour * 100 + minute;
        c.add(c.DATE, 6);
        int end_date = (c.get(c.YEAR) - 2000) * 100000000 + (c.get(c.MONTH) + 1) * 1000000 + c.get(c.DAY_OF_MONTH) * 10000 + week_start_hour * 100 + minute;
        //String end_date = String.format("%02d%02d%02d%02d%02d", c.get(c.YEAR) - 2000, c.get(c.MONTH) + 1, c.get(c.DAY_OF_MONTH), week_end_hour, minute);

        selCalDate = Calendar.getInstance();
        c = selCalDate;
        Log.v("Start date: ", start_date + "");
        Log.v("End date: ", end_date + "");
        /*Toast.makeText(this, "Start date: "+start_date, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "End date: "+end_date, Toast.LENGTH_LONG).show();*/
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);

        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("username", usrName);
        jsonSend.addProperty("password", usrPassword);
        jsonSend.addProperty("period_from", 1710020100);
        jsonSend.addProperty("period_till", 1710092400);
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
                            JSONArray jArray = json.getJSONArray("array");
                            switch (resultNumber) {
                                case RES_OK:
                                    //TODO: take array of JSON objects and return this
                                    //JSONObject js = new JSONObject(String.valueOf(jArray));
                                    JSONArray jarray = json.getJSONArray("array");
                                    final Event[] events = new Event[jarray.length()];
                                    for(int n = 0; n < jarray.length(); n++)
                                        events[n] = Event.parseJson(jarray.getJSONObject(n));


                                    String eventName = events[0].getEvent_name();
                                    Toast.makeText(MainActivity.this, eventName, Toast.LENGTH_SHORT).show();
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            for(Event event : events){
                                                //TODO: assign each even to its appropriate cell
                                                for (int n = 0; n < event_grid.getColumnCount(); n++) {
                                                    for (int m = 0; m < event_grid.getRowCount(); m++) {


                                                    }
                                                }
                                            }
                                        }
                                    });
                                    break;
                                case RES_SRV_ERR:
                                    Toast.makeText(getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case RES_FAIL:
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
