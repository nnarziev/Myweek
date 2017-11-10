package com.example.negmat.myweek;

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

    public void userRegister(String email, String username, String password) {
        if (exec != null && !exec.isTerminated() && !exec.isShutdown())
            exec.shutdownNow();

        exec = Executors.newCachedThreadPool();
        loadingPanel.setVisibility(View.VISIBLE);

        exec.execute(new MyRunnable(email, username, password) {
            @Override
            public void run() {
                try {
                    String result = Tools.post(
                            String.format(Locale.US, "%s/users/register", getResources().getString(R.string.server_ip)),

                            new JSONObject()
                                    .put("email", args[0])
                                    .put("username", args[1])
                                    .put("password", args[2])
                    );

                    runOnUiThread(new MyRunnable(
                            new JSONObject(result).getInt("result")
                    ) {
                        @Override
                        public void run() {
                            switch ((int) args[0]) {
                                case Tools.RES_OK:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Successfully signed up. You can sign in now!", Toast.LENGTH_SHORT).show();
                                            onBackPressed();
                                        }
                                    });
                                    break;
                                case Tools.RES_SRV_ERR:
                                    Log.e("SERVER ERROR", String.format(Locale.US, "Failure code %d", (int) args[0]));
                                    break;
                                case Tools.RES_FAIL:
                                    Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
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

        if (isRegistrationValid(usrEmail, usrLogin, usrPassword, usrConfirmPass))
            userRegister(usrEmail, usrLogin, usrPassword);
        else
            Toast.makeText(this, "Wrong input", Toast.LENGTH_SHORT).show();
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

