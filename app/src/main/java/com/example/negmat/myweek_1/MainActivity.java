package com.example.negmat.myweek_1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;

import org.w3c.dom.Text;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "UserLogin";
    @BindView(R.id.btn_logout)
    Button btnLogout;
    @BindView(R.id.btn_speech)
    Button btnSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getSupportActionBar().setTitle("Main activity");
        Speech.init(this, getPackageName());

    }

    @OnClick (R.id.btn_speech)
    public void Speech(){
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        Logger.setLoggerDelegate(new Logger.LoggerDelegate() {
            @Override
            public void error(String tag, String message) {
                //your own implementation here
            }

            @Override
            public void error(String tag, String message, Throwable exception) {
                //your own implementation here
            }

            @Override
            public void debug(String tag, String message) {
                //your own implementation here
            }

            @Override
            public void info(String tag, String message) {
                //your own implementation here
            }
        });


        try {
            Speech.getInstance().startListening(new SpeechDelegate() {
                @Override
                public void onStartOfSpeech() {
                    Log.i("speech", "speech recognition is now active");
                }

                @Override
                public void onSpeechRmsChanged(float value) {
                    Log.d("speech", "rms is now: " + value);
                }

                @Override
                public void onSpeechPartialResults(List<String> results) {
                    StringBuilder str = new StringBuilder();
                    for (String res : results) {
                        str.append(res).append(" ");
                    }

                    Log.i("speech", "partial result: " + str.toString().trim());
                }

                @Override
                public void onSpeechResult(String result) {
                    Log.i("speech", "result: " + result);
                }
            });
        } catch (SpeechRecognitionNotAvailable speechRecognitionNotAvailable) {
            Log.e("speech", "Speech recognition is not available on this device!");
            // You can prompt the user if he wants to install Google App to have
            // speech recognition, and then you can simply call:
            //
            // SpeechUtil.redirectUserToGoogleAppOnPlayStore(this);
            //
            // to redirect the user to the Google App page on Play Store
        } catch (GoogleVoiceTypingDisabledException e) {
            Log.e("speech", "Google voice typing must be enabled!");
        }


    }


    @OnClick (R.id.btn_logout)
    public void LogOut(){
        SharedPreferences shPref = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = shPref.edit();
        editor.clear();
        editor.commit();
        Intent intent = new Intent(MainActivity.this, SignIn.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // prevent memory leaks when activity is destroyed
        Speech.getInstance().shutdown();
    }
}
