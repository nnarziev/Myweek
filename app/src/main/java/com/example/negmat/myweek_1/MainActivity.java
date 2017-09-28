package com.example.negmat.myweek_1;

import android.content.Context;
import android.graphics.Color;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TextView;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_add_event) ImageButton btnAddEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
    }

    // region Constants
    private final short GRID_ADDITEM = 2;
    private final short GRID_DELETEITEM = 3;
    // endregion

    // region Variables
    @BindView(R.id.event_grid)
    protected GridLayout event_grid;
    private String[] weekDays;
    private int hour=1;
    private int count=0;//counter for change day half
    // endregion

    // region Initialization Functions
    private void initialize() {
        initGrid();
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
                    if(m==0 && n<8)
                    {
                        space.setText(weekDays[n]);
                    }
                    if(n==0 && m>0)
                    {
                        if(count==0)
                        {
                            space.setText(hour+"am");
                        }else{
                            space.setText(hour+"pm");
                            if(hour==11)
                                count=-1;
                        }

                        if(hour==11)
                            count++;
                        if(hour==12)
                        {
                            hour=0;
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
    public void addEvent(){
        FragmentManager manager = getFragmentManager();
        AddEventDialog dialog = new AddEventDialog();
        dialog.show(manager, "Dialog");
    }
    // endregion
}
