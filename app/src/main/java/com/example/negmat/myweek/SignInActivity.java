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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);

        if (loginPrefs == null)
            loginPrefs = getSharedPreferences("UserLogin", 0);
        if (loginPrefs.contains(SignInActivity.username) && loginPrefs.contains(SignInActivity.password)) {
            loadingPanel.setVisibility(View.VISIBLE);
            signIn(loginPrefs.getString(SignInActivity.username, null), loginPrefs.getString(SignInActivity.password, null));
        } else
            Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();
    }

    // region Variables
    static SharedPreferences loginPrefs = null;
    static final String username = "username", password = SignInActivity.password;
    static ExecutorService exec;

    @BindView(R.id.txt_login)
    EditText userLogin;
    @BindView(R.id.txt_password)
    EditText userPassword;
    @BindView(R.id.loadingPanel)
    RelativeLayout loadingPanel;
    // @BindView(R.id.btn_signup)  TextView btnSignUp;
    // endregion

    public void signInClick(View view) {
        signIn(userLogin.getText().toString(), userPassword.getText().toString());
    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void signIn(final String usrLogin, final String usrPass) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();

        loadingPanel.setVisibility(View.VISIBLE);

        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String raw_json = Tools.post("http://165.246.165.130:2222/users/login", new JSONObject()
                            .put("username", usrLogin)
                            .put("password", usrPass));
                    if (raw_json == null)
                        throw new Exception();

                    JSONObject json = new JSONObject(raw_json);
                    int resultNumber = json.getInt("result");

                    if (resultNumber == Tools.RES_OK)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                                editor.putString(SignInActivity.username, usrLogin);
                                editor.putString(SignInActivity.password, usrPass);
                                editor.apply();
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                                overridePendingTransition(0, 0);
                            }
                        });
                    else {
                        Log.e("ERROR", "Code: " + resultNumber);
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

    @Override
    protected void onStop() {
        super.onStop();
        loadingPanel.setVisibility(View.GONE);
        if (exec != null && !exec.isShutdown() && !exec.isTerminated())
            exec.shutdownNow();
    }
}
