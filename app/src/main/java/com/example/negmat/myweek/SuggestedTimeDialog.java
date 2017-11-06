package com.example.negmat.myweek;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


class SuggestedTimeDialog extends Dialog {

    // region Variables
    private Activity activity;
    public String event;

    private TextView suggest;
    // endregion

    SuggestedTimeDialog(Activity a, String event) {
        super(a);
        activity = a;
        this.event=event;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_aisuggest);
        suggest = findViewById(R.id.suggested_time);
        suggest.setText(suggest.getText().toString() + " " + event);

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                });
            }
        });
    }

    public void setConfirmDialog(ConfirmEventDialog dialog){
        this.dialog = dialog;
    }
    ConfirmEventDialog dialog;

    @Override
    protected void onStop() {
        dialog.show(activity.getFragmentManager(), "confirmdialog");
        super.onStop();
    }
}
