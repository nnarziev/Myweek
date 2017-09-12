package com.example.negmat.myweek_1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignIn extends AppCompatActivity {

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
    public void SignIn(){

    }
    @OnClick(R.id.btn_signup)
    public void SignUp(){
        Intent i2 = new Intent(this, SignUp.class);
        startActivity(i2);
        overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
    }
}
