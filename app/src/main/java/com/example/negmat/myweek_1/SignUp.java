package com.example.negmat.myweek_1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class SignUp extends AppCompatActivity {

    private final short RES_OK = 0,
            RES_SRV_ERR = -1,
            RES_FAIL = 1;

    public void userRegister(String usrEmail, String usrLogin, String usrPassword) {
        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("email", usrEmail);
        jsonSend.addProperty("username", usrLogin);
        jsonSend.addProperty("password", usrPassword);

        String url = "https://qobiljon.pythonanywhere.com/users/register";

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
                                    Toast.makeText(getApplicationContext(), "Registration is successfull", Toast.LENGTH_SHORT).show();
                                    Intent i2 = new Intent(SignUp.this, SignIn.class);
                                    startActivity(i2);
                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                                    break;
                                case RES_SRV_ERR:
                                    Toast.makeText(getApplicationContext(), "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case RES_FAIL:
                                    Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    break;
                            }
                        } catch (JSONException e1) {
                            Log.wtf("json", e1);
                        }
                    }
                });
    }

    @BindView(R.id.txt_email)
    EditText email;
    @BindView(R.id.txt_login)
    EditText login;
    @BindView(R.id.txt_password)
    EditText password;
    @BindView(R.id.txt_conf_password)
    EditText confPassword;
    @BindView(R.id.btn_register)
    TextView btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            bar.setTitle("Sign up");
    }

    // region Registration button handler
    @OnClick(R.id.btn_register)
    public void Register() {
        String usrEmail = email.getText().toString();
        String usrLogin = login.getText().toString();
        String usrPassword = password.getText().toString();
        String usrConfirmPass = confPassword.getText().toString();
        if (validationCheck(usrEmail, usrLogin, usrPassword, usrConfirmPass)) {
            userRegister(usrEmail, usrLogin, usrPassword);
        } else {
            Toast.makeText(this, "Wrong input", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    // region Validation Function
    public boolean validationCheck(String email, String login, String password, String confirmPass) {
        //TODO: validate the input data
        if ((email != null && login != null && password != null && confirmPass != null) && (email.contains("@")) &&
                (login.length() >= 4 && login.length() <= 12) && (password.length() >= 6 && password.length() <= 16) && (password.equals(confirmPass))) {
            return true;
        } else
            return false;
    }
    // endregion
}

