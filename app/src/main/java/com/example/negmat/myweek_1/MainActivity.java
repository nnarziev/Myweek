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
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.SupportMenuInflater;
import android.util.AttributeSet;
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

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    public static final String PREFS_NAME = "UserLogin";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
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
                initialize();
                Toast.makeText(this, "Syncronized", Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    // region Constants
    private final short GRID_ADDITEM = 2;
    private final short GRID_DELETEITEM = 3;
    // endregion

    // region Variables
    @BindView(R.id.event_grid) protected GridLayout event_grid;
    @BindView(R.id.event_grid_fixed) protected GridLayout event_grid_fixed;
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
        weekDays = new String[]{"", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};




        int cellDimen = width / event_grid.getColumnCount();
        {
            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {
                    // TODO: check for existing data downloaded from server

                    // TODO: case where no data exists
                    TextView space = new TextView(getApplicationContext());
                    space.setBackgroundResource(R.drawable.cell_shape);
                    if (m == 0 && n < 8) {
                        space.setText(weekDays[n]);
                        space.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    }
                    if (n == 0 && m > 0) {
                        if (count == 0) {
                            space.setText(hour + "am");
                        } else {
                            space.setText(hour + "pm");
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
    public void SelectWeek(){
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
            @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH), new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
            txtSelectedWeek.setText(selectedWeek);
        }
    };
    //endregion

    //region Arrows buttons handling

    // Right button handling
    @OnClick(R.id.btn_arrow_right)
    public void weekRight(){
        selCalDate.add(Calendar.DATE, 7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH), new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
    }

    //Lefat buttin handling
    @OnClick(R.id.btn_arrow_left)
    public void weekLeft(){
        selCalDate.add(Calendar.DATE, -7);
        @SuppressLint("DefaultLocale") String selectedWeek = String.format("Week %d, %s., %d", selCalDate.get(Calendar.WEEK_OF_MONTH), new DateFormatSymbols().getMonths()[selCalDate.get(Calendar.MONTH)].substring(0, 3), selCalDate.get(Calendar.YEAR));
        txtSelectedWeek.setText(selectedWeek);
    }

    //endregion
}
