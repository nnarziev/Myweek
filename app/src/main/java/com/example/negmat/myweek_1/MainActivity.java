package com.example.negmat.myweek_1;

import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Space;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
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
    // endregion

    // region Initialization Functions
    private void initialize() {
        initGrid();
    }

    private void initGrid() {
        // TODO: download and save the events of the user to a variable

        // clean out the gridlayout
        //event_grid.removeAllViews();

        // TODO: set downloaded events into gridlayout
        int gridWidth = event_grid.getMeasuredWidth();
        int cellDimen = gridWidth / event_grid.getColumnCount();
        {
            for (int n = 0; n < event_grid.getColumnCount(); n++) {
                for (int m = 0; m < event_grid.getRowCount(); m++) {
                    // TODO: check for existing data downloaded from server

                    // TODO: case where no data exists
                    Button space = new Button(getApplicationContext());
                    space.setBackgroundColor(Color.RED);
                    event_grid.addView(space, cellDimen, cellDimen);
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
}
