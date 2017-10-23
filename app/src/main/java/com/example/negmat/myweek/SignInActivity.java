package com.example.negmat.myweek;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SignInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            setTitle("Sign in");

        final String[] usrLogin = new String[1];
        final String[] usrPass = new String[1];
        usrLogin[0] = userLogin.getText().toString();
        usrPass[0] = userPassword.getText().toString();

        loginPrefs = getSharedPreferences("UserLogin", 0);
        if (loginPrefs.contains("Login") && loginPrefs.contains("Password")) {
            signInClick(loginPrefs.getString("Login", null), loginPrefs.getString("Password", null));
        } else
            Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usrLogin[0] = userLogin.getText().toString();
                usrPass[0] = userPassword.getText().toString();
                signInClick(usrLogin[0], usrPass[0]);
            }
        });

        Executor exec = Executors.newCachedThreadPool();
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json_body = new JSONObject();
                    json_body.put("username", "negmatjon");
                    json_body.put("password", "12345678");

                    String raw_json = Tools.post("https://qobiljon.pythonanywhere.com/users/login", json_body);

                    Log.e("RAW_JSON", raw_json + "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // region Variables
    static SharedPreferences loginPrefs = null;


    @BindView(R.id.txt_login)
    EditText userLogin;
    @BindView(R.id.txt_password)
    EditText userPassword;
    @BindView(R.id.btn_signin)
    Button btnSignIn;
    // @BindView(R.id.btn_signup)  TextView btnSignUp;
    // endregion

    public void signInClick(String usrLogin, String usrPass) {
        if (userIsValid(usrLogin, usrPass)) {
            JsonObject jsonSend = new JsonObject();
            jsonSend.addProperty("username", usrLogin);
            jsonSend.addProperty("password", usrPass);

            String url = "https://qobiljon.pythonanywhere.com/users/login";

            final String finalUsrLogin = usrLogin;
            final String finalUsrPass = usrPass;
            Ion.with(getApplicationContext())
                    .load("POST", url)
                    .addHeader("Content-Type", "application/json")
                    .setJsonObjectBody(jsonSend)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            //process data or error
                            try {
                                JSONObject json = new JSONObject(String.valueOf(result));
                                int resultNumber = json.getInt("result");
                                switch (resultNumber) {
                                    case Tools.RES_OK:
                                        SharedPreferences.Editor editor = SignInActivity.loginPrefs.edit();
                                        editor.putString("Login", finalUsrLogin);
                                        editor.putString("Password", finalUsrPass);
                                        editor.apply();
                                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
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
                            } catch (JSONException e1) {
                                Log.wtf("json", e1);
                            }
                        }
                    });
        } else Toast.makeText(this, "Wrong input...", Toast.LENGTH_SHORT).show();


    }

    public void signUpClick(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
    }

    public boolean userIsValid(String login, String password) {
        //TODO: validate the input data
        return login != null && password != null;
    }
}
