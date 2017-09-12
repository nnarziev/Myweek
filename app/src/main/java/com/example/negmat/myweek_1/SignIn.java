package com.example.negmat.myweek_1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

    private final short RES_OK = 0,
            RES_SRV_ERR = 1,
            RES_USR_NOT_EXS = 2;

    @BindView(R.id.txt_login)
    EditText login;
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
        getSupportActionBar().setTitle("Sign in");

    }

    @OnClick(R.id.btn_signin)
    public void SignIn() {
        String usrLogin = login.getText().toString();
        String usrPass = userPassword.getText().toString();
        JsonObject jsonSend = new JsonObject();
        jsonSend.addProperty("login", usrLogin);
        jsonSend.addProperty("password", usrPass);

        String url = "http://api.icndb.com/jokes/count";

        Ion.with(getApplicationContext())
                .load(url)
                .setJsonObjectBody(jsonSend)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //process data or error
                        try {
                            JSONObject json = new JSONObject(String.valueOf(result));
                            int resultNumber = 2;//json.getInt("result");
                            switch (resultNumber) {
                                case RES_OK:
                                    Intent intent = new Intent(SignIn.this, MainActivity.class);
                                    intent.putExtra("result", resultNumber);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                                    break;
                                case RES_SRV_ERR:
                                    Toast.makeText(SignIn.this, "ERROR with Server happened", Toast.LENGTH_SHORT).show();
                                    break;
                                case RES_USR_NOT_EXS:
                                    Toast.makeText(SignIn.this, "No such user exists", Toast.LENGTH_SHORT).show();
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

    @OnClick(R.id.btn_signup)
    public void SignUp() {
        Intent i2 = new Intent(this, SignUp.class);
        startActivity(i2);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
    }
}
