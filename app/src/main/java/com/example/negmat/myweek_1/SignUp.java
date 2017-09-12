package com.example.negmat.myweek_1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignUp extends AppCompatActivity {
    boolean userRegister(){
        return true;
    }
    @BindView(R.id.txt_name)
    EditText name;
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
        getSupportActionBar().setTitle("Registration");
    }

    @OnClick(R.id.btn_register)
    public void Register(){
        if(userRegister()){
            Toast.makeText(this, "Registration is successfull", Toast.LENGTH_SHORT).show();
            Intent i2 = new Intent(this, SignIn.class);
            startActivity(i2);
            overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
        }
    }
}

