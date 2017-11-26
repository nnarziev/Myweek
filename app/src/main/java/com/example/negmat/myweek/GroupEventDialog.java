package com.example.negmat.myweek;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class GroupEventDialog extends DialogFragment {

    public GroupEventDialog(MyRunnable onExit) {
        this.exitJob = onExit;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_group_event, container, false);
        initialize(view);
        return view;
    }

    // region Variables
    static List<String> users = new ArrayList<>();
    private TextView textMembers;
    private MyRunnable exitJob;
    // endregion

    private void initialize(View root) {
        MainActivity.isSingleMode = false;
        textMembers = root.findViewById(R.id.txt_usernames);
        updateMembers();

        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitJob.run();
                dismiss();
            }
        });

        root.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (users.size() == 0) {
                    Toast.makeText(getActivity(), "Group member list cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                SpeechDialog speechDialog = new SpeechDialog();
                speechDialog.show(getFragmentManager(), "speech-dialog");

                exitJob.run();
                dismiss();
            }
        });
    }

    private String array2string() {
        StringBuilder res = new StringBuilder();
        if (users.size() == 0)
            return "empty list.";
        else if (users.size() == 1)
            res = new StringBuilder(users.get(0) + ".");
        else if (users.size() == 2)
            res = new StringBuilder(users.get(0) + " and " + users.get(1) + ".");
        else {
            int n;
            for (n = 0; n < users.size() - 1; n++)
                res.append(users.get(n)).append(", ");
            res.append("and ").append(users.get(n)).append(".");
        }

        return res.toString();
    }

    void updateMembers() {
        textMembers.setText(String.format(
                Locale.US,
                getResources().getString(R.string.members),
                array2string()
        ));
    }
}
