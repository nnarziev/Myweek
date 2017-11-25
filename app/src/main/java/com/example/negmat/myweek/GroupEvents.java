package com.example.negmat.myweek;

import android.app.DialogFragment;
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
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class GroupEvents extends DialogFragment {

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

            try {
                JSONObject object = new JSONObject(new String(message.getRecords()[0].getPayload()));
                if (object.getInt(Tools.KEY_NFC_GROUP) == Tools.NFC_GROUP) {
                    String username = object.getString("username");
                    Toast.makeText(getActivity(), username + " is received!", Toast.LENGTH_SHORT).show();
                    usernames.add(username);
                    adapter.notifyDataSetChanged();
                } else if (object.getInt(Tools.KEY_NFC_SINGLE) != Tools.NFC_SINGLE) {
                    // must not happen
                    throw new JSONException("JSON from NFC doesn't contain field " + Tools.KEY_NFC_GROUP);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    // region Variables
    private ListView lvUsernames;
    private ArrayList<String> usernames;
    private ArrayAdapter<String> adapter;
    // endregion
}
