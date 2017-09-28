package com.example.negmat.myweek_1;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignUp extends AppCompatActivity {
    boolean userRegister() {
        return true;
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
        if(validationCheck(usrEmail, usrLogin,usrPassword,usrConfirmPass)){
            if (userRegister()) {
                Toast.makeText(this, "Registration is successfull", Toast.LENGTH_SHORT).show();
                Intent i2 = new Intent(this, SignIn.class);
                startActivity(i2);
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            }
            else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Wrong input", Toast.LENGTH_SHORT).show();
        }
    }
    //endregion

    // region Validation Function
    public boolean validationCheck(String email, String login, String password, String confirmPass){
        //TODO: validate the input data
        return true;
    }
    // endregion
}

