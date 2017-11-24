package com.example.negmat.myweek;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class GroupEvents extends DialogFragment{

    ListView lvUsernames;
    ArrayList<String> usernames;
    ArrayAdapter<String> adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_group_event, container, false);
        lvUsernames = view.findViewById(R.id.lv_usernames);

        usernames = new ArrayList<>();
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, usernames);

        lvUsernames.setAdapter(adapter);
        return view;
    }





    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getActivity().getIntent();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0]; // only one message transferred

            usernames.add(message.toString());
            adapter.notifyDataSetChanged();

        }
    }

}
