package com.example.negmat.myweek_1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignIn extends AppCompatActivity {

    @BindView(R.id.txt_uname)
    EditText userName;
    @BindView(R.id.txt_password)
    EditText userPassword;
    @BindView(R.id.btn_signin)
    Button btnSignIn;
    @BindView(R.id.btn_signup)
    Button btnSignUp;
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

    }
}
