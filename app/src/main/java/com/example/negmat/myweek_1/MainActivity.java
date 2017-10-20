package com.example.negmat.myweek_1;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
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
import com.koushikdutta.async.http.body.StreamBody;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        //region Fixed head with week names
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int cellDimen = width / grid_fixed.getColumnCount();
        weekDays = new String[]{"", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
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

        initGrid(); //initialize the grid view
        sendStartDate();
    }

    TextView[][] tv = new TextView[8][25];

    Event[] events;//global array of events to use in several functions

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
        int height=cellDimen*event_grid.getRowCount();
        {

            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {
                    // TODO: check for existing data downloaded from server

                    // TODO: case where no data exists
                    //TextView space = new TextView(getApplicationContext());
                    tv[n][m] = new TextView(getApplicationContext());
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




        /*{

            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {

                    TextView space = new TextView(getApplicationContext());
                    //tv[n][m] = new TextView(getApplicationContext());
                    //tv[n][m].setBackgroundResource(R.drawable.cell_shape);
                    if (m == 0 && n < 8) {
                        //tv[n][m].setTypeface(null, Typeface.BOLD);
                        //tv[n][m].setText(weekDays[n]);
                        //tv[n][m].setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
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



                }
            }
        }*/

    }

    private void populateGrid(Event[] events, int from, int till, short num_of_events) {
        initGrid();
        short start_time = (short) (from%10000/100);
        short start_day = (short) (from%1000000/10000);
        short start_month = (short) (from%100000000/1000000);
        short start_year = (short) (from/100000000);

        Calendar cal = Calendar.getInstance();
        cal.set(start_year+2000, start_month, start_day);
        if (cal.get(Calendar.WEEK_OF_MONTH) == selCalDate.get(Calendar.WEEK_OF_MONTH) && num_of_events!=0) {
            for (Event event : events) {
                //TODO: assign each even to its appropriate cell
                //Toast.makeText(this, String.valueOf(event.getStart_time()), Toast.LENGTH_SHORT).show();
                short time = (short) (event.getStart_time()%10000/100);
                short day = (short) (event.getStart_time()%1000000/10000);
                short month = (short) (event.getStart_time()%100000000/1000000);
                short year = (short) (event.getStart_time()/100000000);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.MONTH, month - 1);
                calendar.set(Calendar.YEAR, year+2000);
                short day_of_week = (short) calendar.get(Calendar.DAY_OF_WEEK);

                tv[day_of_week][time].setText(event.getEvent_name());
                tv[day_of_week][time].setTag(event.getEvent_id());
            }
        } else {
            initGrid();
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

            int index=0;//index of event in array
            //take an index of event
            //if current event_id==event[index].getEventId()
            for(int i=0;i<events.length;i++)
            {
                if(event_id==events[i].getEvent_id())
                {
                    index=i;break;
                }
            }

            short time = (short) (events[index].getStart_time()%10000/100);
            short day = (short) (events[index].getStart_time()%1000000/10000);
            short month = (short) (events[index].getStart_time()%100000000/1000000);
            short year = (short) (events[index].getStart_time()/100000000);

            String event_name=events[index].getEvent_name();
            String note=events[index].getEvent_note();
            int duration=events[index].getLength();
            int repeat=events[index].getRepeat_mode();
            String reason=events[index].getReason();

            if(reason==null)
                reason="no reason";

            @SuppressLint("DefaultLocale") String eventInfo = String.format("%s\n%s\nDate: %s. %d, %d\nFrom: %d:00\nDuration: %d min\nRepeat: %d times\nReason: %s", event_name, note,
                    new DateFormatSymbols().getMonths()[month].substring(0, 3),day,year+2000, time,duration, repeat,reason);

            Toast.makeText(MainActivity.this, String.valueOf(index), Toast.LENGTH_SHORT).show();
            //make event global and show all info on it
            ViewEventDialog ved = new ViewEventDialog(MainActivity.this,eventInfo, event_id);
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
        // aoidjsoidjasoijdoiasj
        if (action == GRID_ADDITEM) {

        } else if (action == GRID_DELETEITEM) {

        }
    }

    // endregion

    // region Add an event button handling
    @OnClick(R.id.btn_add_event)
    public void addEvent() {
        /*String a = "hello my name is Nematjon, I am a 4th year student! I like playing chess";
        Pattern pattern = Pattern.compile("playing");
        Matcher matcher = pattern.matcher(a);
        if(matcher.find()){
            Toast.makeText(this, matcher.group(0), Toast.LENGTH_SHORT).show();
        }else
            Toast.makeText(this, "Could not find", Toast.LENGTH_SHORT).show();*/
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
                    new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
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
                new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
        sendStartDate();
    }

    // Left buttin handling
    @OnClick(R.id.btn_arrow_left)
    public void weekLeft() {
        selCalDate.add(Calendar.DATE, -7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH),
                new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
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
        //String start_date = String.format("%02d%02d%02d%02d%02d", c.get(c.YEAR)-2000, c.get(c.MONTH)+1, c.get(c.DAY_OF_MONTH), week_start_hour, minute);
        final int start_date = (c.get(Calendar.YEAR) - 2000) * 100000000 + (c.get(Calendar.MONTH) + 1) * 1000000 + c.get(Calendar.DAY_OF_MONTH) * 10000 + week_start_hour * 100 + minute;
        c.add(Calendar.DATE, 6);
        final int end_date = (c.get(Calendar.YEAR) - 2000) * 100000000 + (c.get(Calendar.MONTH) + 1) * 1000000 + c.get(Calendar.DAY_OF_MONTH) * 10000 + week_end_hour * 100 + minute;
        //String end_date = String.format("%02d%02d%02d%02d%02d", c.get(c.YEAR) - 2000, c.get(c.MONTH) + 1, c.get(c.DAY_OF_MONTH), week_end_hour, minute);

        /*selCalDate = Calendar.getInstance();
        c = selCalDate;*/
        /*Log.v("Start date: ", start_date + "");
        Log.v("End date: ", end_date + "");*/
        /*Toast.makeText(this, "Start date: "+start_date, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "End date: "+end_date, Toast.LENGTH_LONG).show();*/

        SharedPreferences pref = getSharedPreferences(PREFS_NAME, 0);
        String usrName = pref.getString("Login", null);
        String usrPassword = pref.getString("Password", null);

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
                                case RES_OK:
                                    //TODO: take array of JSON objects and return this
                                    //JSONObject js = new JSONObject(String.valueOf(jArray));
                                    final JSONArray jarray = json.getJSONArray("array");
                                    events = new Event[jarray.length()];
                                    if(jarray.length()!=0){
                                        for (int n = 0; n < jarray.length(); n++)
                                            events[n] = Event.parseJson(jarray.getJSONObject(n));
                                        //String eventName = events[0].getEvent_name();
                                    }
                                    else
                                        Toast.makeText(MainActivity.this, "Empty week", Toast.LENGTH_SHORT).show();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            populateGrid(events, start_date, end_date, (short) jarray.length());
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
