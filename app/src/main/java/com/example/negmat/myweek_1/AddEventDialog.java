package com.example.negmat.myweek_1;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddEventDialog extends DialogFragment implements SpeechDelegate {

    private static final int REQUEST_MICROPHONE = 2;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.btn_speech)
    ImageButton btnSpeech;
    @BindView(R.id.btn_cancel)
    Button btnCancel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_event_dialog, container, false);
        ButterKnife.bind(this, view);
        setCancelable(false);

        Speech.init(getActivity(), getActivity().getPackageName());
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        return view;
    }

    @Override
    public void onStartOfSpeech() {

    }

    @Override
    public void onSpeechRmsChanged(float value) {

    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        text.setText("");
        for (String partial : results) {
            text.append(partial + " ");
        }
    }

    @Override
    public void onSpeechResult(String result) {
        btnSpeech.setVisibility(View.VISIBLE);
        text.setText(result);

        if (result.isEmpty()) {
            Speech.getInstance().say("Repeat please");
        } /*else {
            //Speech.getInstance().say(result);
        }*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // prevent memory leaks when activity is destroyed
        Speech.getInstance().shutdown();
    }

    private void onRecordAudioPermissionGranted() {
        btnSpeech.setVisibility(View.GONE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(this);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    public void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(getActivity().getApplicationContext());
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Speech recognition is not available on this device. Do you want to install Google app to have speech recognition?")
                .setCancelable(false)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Please enable Google Voice Typing to use speech recognition!")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }

    @OnClick(R.id.btn_speech)
    public void Speech() {
        //granting permission to user
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }
        onRecordAudioPermissionGranted();
    }

    @OnClick(R.id.btn_cancel)
    public void AddEventCancel() {
        dismiss();
    }

}