package com.example.negmat.myweek_1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SignIn extends AppCompatActivity {

    public static final String PREFS_NAME = "UserLogin";
    private final short RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;


    @BindView(R.id.txt_login)
    EditText userLogin;
    @BindView(R.id.txt_password)
    EditText userPassword;
    @BindView(R.id.btn_signin)
    Button btnSignIn;
    @BindView(R.id.btn_signup)
    TextView btnSignUp;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            setTitle("Sign in");

        final String[] usrLogin = new String[1];
        final String[] usrPass = new String[1];
        usrLogin[0] = userLogin.getText().toString();
        usrPass[0] = userPassword.getText().toString();

        SharedPreferences shPref = getSharedPreferences(PREFS_NAME, 0);
        if (shPref.contains("Login") && shPref.contains("Password")) {
            Sign_In(shPref.getString("Login", null), shPref.getString("Password", null));
        } else
            Toast.makeText(this, "No log in yet", Toast.LENGTH_SHORT).show();


        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usrLogin[0] = userLogin.getText().toString();
                usrPass[0] = userPassword.getText().toString();
                Sign_In(usrLogin[0], usrPass[0]);
            }
        });
    }

    // region Sign In Function
    @SuppressWarnings("unused")
    public void Sign_In(String usrLogin, String usrPass) {
        if (validationCheck(usrLogin, usrPass)) {
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
                                    case RES_OK:
                                        SharedPreferences login = getSharedPreferences(PREFS_NAME, 0);
                                        SharedPreferences.Editor editor = login.edit();
                                        editor.putString("Login", finalUsrLogin);
                                        editor.putString("Password", finalUsrPass);
                                        editor.apply();
                                        Intent intent = new Intent(SignIn.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                                        break;
                                    case RES_SRV_ERR:
                                        Toast.makeText(SignIn.this, "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                        break;
                                    case RES_FAIL:
                                        Toast.makeText(SignIn.this, "Incorrect credentials", Toast.LENGTH_SHORT).show();
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
    // endregion

    // region Validation Function
    public boolean validationCheck(String login, String password) {
        //TODO: validate the input data
        return true;
    }
    // endregion

    //region Sign up button handler
    @OnClick(R.id.btn_signup)
    public void SignUp() {
        Intent i2 = new Intent(this, SignUp.class);
        startActivity(i2);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
    }
    //endregion

}
