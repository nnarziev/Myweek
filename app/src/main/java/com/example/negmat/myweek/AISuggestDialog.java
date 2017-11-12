package com.example.negmat.myweek;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Nematjon on 11/12/2017.
 */

public class AISuggestDialog extends DialogFragment {

    Event event;
    public AISuggestDialog(Event event){
        this.event = event;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_aisuggest, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    //region UI variables
    @BindView(R.id.text)
    TextView txtSuggestTime;
    @BindView(R.id.btn_ok)
    Button btnOK;
    @BindView(R.id.btn_edit)
    Button btnEdit;
    @BindView(R.id.btn_cancel)
    Button btnCancel;
    //endregion

    @OnClick(R.id.btn_edit)
    public void editClick(){}

    @OnClick(R.id.btn_ok)
    public void OKClick(){}

    @OnClick(R.id.btn_cancel)
    public void cancelClick(){}
}
