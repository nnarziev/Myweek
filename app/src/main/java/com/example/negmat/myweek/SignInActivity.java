package com.example.negmat.myweek;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        initialize();
    }

    @Override
    protected void onStop() {
        super.onStop();
        loadingPanel.setVisibility(View.GONE);
        if (exec != null && !exec.isShutdown() && !exec.isTerminated())
            exec.shutdownNow();
    }

    // region Variables
    private EditText userLogin;
    private EditText userPassword;
    private RelativeLayout loadingPanel;

    static SharedPreferences loginPrefs = null;
    static final String username = "username", password = "password";
    private static ExecutorService exec;
    // endregion

    private void initialize() {
        // region Initialize UI Variables
        userLogin = findViewById(R.id.txt_login);
        userPassword = findViewById(R.id.txt_password);
        loadingPanel = findViewById(R.id.loadingPanel);
        // endregion

        if (loginPrefs == null)
            loginPrefs = getSharedPreferences("UserLogin", 0);

        if (loginPrefs.contains(SignInActivity.username) && loginPrefs.contains(SignInActivity.password)) {
            loadingPanel.setVisibility(View.VISIBLE);
            signIn(loginPrefs.getString(SignInActivity.username, null), loginPrefs.getString(SignInActivity.password, null));
        } else Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();
    }

    public void signInClick(View view) {
        signIn(userLogin.getText().toString(), userPassword.getText().toString());
    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void signIn(String username, String password) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        loadingPanel.setVisibility(View.VISIBLE);

        exec.execute(new MyRunnable(username, password) {
            @Override
            public void run() {
                try {

                    String raw_json = Tools.post(String.format(Locale.US, "%s/users/login", getResources().getString(R.string.server_ip)), new JSONObject()
                            .put("username", args[0])
                            .put("password", args[1]));
                    if (raw_json == null)
                        throw new Exception();

                    JSONObject json = new JSONObject(raw_json);
                    int resultNumber = json.getInt("result");

                    if (resultNumber == Tools.RES_OK)
                        runOnUiThread(new MyRunnable(args) {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                                editor.putString(SignInActivity.username, (String) args[0]);
                                editor.putString(SignInActivity.password, (String) args[1]);
                                editor.apply();

                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                startActivity(intent);

                                finish();
                                overridePendingTransition(0, 0);
                            }
                        });
                    else {
                        Thread.sleep(2000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignInActivity.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                                loadingPanel.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SignInActivity.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                            loadingPanel.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
}
