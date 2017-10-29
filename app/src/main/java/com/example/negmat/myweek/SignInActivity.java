package com.example.negmat.myweek;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
        if (loginPrefs.contains("login") && loginPrefs.contains("password")) {
            loadingPanel.setVisibility(View.VISIBLE);
            signIn(loginPrefs.getString("login", null), loginPrefs.getString("password", null));
        } else
            Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();
    }

    // region Variables
    static SharedPreferences loginPrefs = null;
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

    public void signIn(final String usrLogin, final String usrPass) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();
        else
            exec = Executors.newCachedThreadPool();

        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json_body = new JSONObject();
                    json_body.put("username", usrLogin);
                    json_body.put("password", usrPass);

                    String raw_json = Tools.post("https://qobiljon.pythonanywhere.com/users/login", json_body);
                    if (raw_json == null)
                        throw new Exception();

                    JSONObject json = new JSONObject(raw_json);
                    int resultNumber = json.getInt("result");

                    switch (resultNumber) {
                        case Tools.RES_OK:
                            SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                            editor.putString("login", usrLogin);
                            editor.putString("password", usrPass);
                            editor.apply();
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(0, 0);
                            break;
                        case Tools.RES_SRV_ERR:
                            Toast.makeText(SignInActivity.this, "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                            break;
                        case Tools.RES_FAIL:
                            Toast.makeText(SignInActivity.this, "Incorrect credentials", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingPanel.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
    }
}
