package com.example.negmat.myweek_1;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ViewEventDialog extends Dialog implements
        android.view.View.OnClickListener {

    public Activity c;
    public String EventTxt;
    public Dialog d;
    public Button edit, delete,cancel;

    public TextView tv;
    public ViewEventDialog(Activity a, String event) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.EventTxt=event;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_event_dialog);
        edit = (Button) findViewById(R.id.btn_edit);
        delete = (Button) findViewById(R.id.btn_delete);
        cancel = (Button) findViewById(R.id.btn_cancel);
        tv=(TextView)findViewById(R.id.txt_event);
        edit.setOnClickListener(this);
        cancel.setOnClickListener(this);
        delete.setOnClickListener(this);
        tv.setText(EventTxt);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_edit:
                Toast.makeText(c, "Edit event", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_delete:
                Toast.makeText(c, "Delete event", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_cancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}
