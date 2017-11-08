package com.example.negmat.myweek;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignUpActivity extends AppCompatActivity {

    // region Variables
    static ExecutorService exec;

    @BindView(R.id.txt_email)
    EditText email;
    @BindView(R.id.txt_login)
    EditText login;
    @BindView(R.id.txt_password)
    EditText password;
    @BindView(R.id.txt_conf_password)
    EditText confPassword;
    @BindView(R.id.loadingPanel)
    RelativeLayout loadingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            bar.setTitle("Sign up");
    }
    // @BindView(R.id.btn_register) TextView btnRegister;
    // endregion

    public void userRegister(final String email, final String username, final String password) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();
        loadingPanel.setVisibility(View.VISIBLE);

        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = String.format(Locale.US, "%s/users/register", getResources().getString(R.string.server_ip));

                    String result = Tools.post(url, new JSONObject()
                            .put("email", email)
                            .put("username", username)
                            .put("password", password)
                    );

                    JSONObject json = new JSONObject(String.valueOf(result));
                    int resultNumber = json.getInt("result");
                    switch (resultNumber) {
                        case Tools.RES_OK:
                            Toast.makeText(getApplicationContext(), "Registration is successfull", Toast.LENGTH_SHORT).show();
                            Intent i2 = new Intent(SignUpActivity.this, SignInActivity.class);
                            startActivity(i2);
                            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                            break;
                        case Tools.RES_SRV_ERR:
                            Log.e("SERVER ERROR", String.format(Locale.US, "Failure code %d", resultNumber));
                            break;
                        case Tools.RES_FAIL:
                            Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ERROR", e.getMessage());
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

    public void registerClick(View view) {
        String usrEmail = email.getText().toString();
        String usrLogin = login.getText().toString();
        String usrPassword = password.getText().toString();
        String usrConfirmPass = confPassword.getText().toString();
        if (isRegistrationValid(usrEmail, usrLogin, usrPassword, usrConfirmPass)) {
            userRegister(usrEmail, usrLogin, usrPassword);
        } else {
            Toast.makeText(this, "Wrong input", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRegistrationValid(String email, String login, String password, String confirmPass) {
        //TODO: validate the input data
        return (email != null && login != null && password != null && confirmPass != null) && (email.contains("@")) &&
                (login.length() >= 4 && login.length() <= 12) && (password.length() >= 6 && password.length() <= 16) && (password.equals(confirmPass));
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
}

