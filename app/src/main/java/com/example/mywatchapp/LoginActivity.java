package com.example.mywatchapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    TextView forgotPassword, signupText;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Intent i = new Intent(this, MainDashboardActivity.class);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.login_username);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forgotpassword);
        signupText = findViewById(R.id.signupRedirectText);

        loginButton.setOnClickListener(v -> checkLogin());
        forgotPassword.setOnClickListener(v -> {
            Intent i = new Intent(this, ResetPasswordActivity.class);
            startActivity(i);
        });
        signupText.setOnClickListener(v -> {
            Intent i = new Intent(this, SingupAction.class);
            startActivity(i);
        });
    }

    void checkLogin() {
        String email = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Ievadiet e-pastu un paroli", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Nederīgs e-pasts", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Veiksmīga pieteikšanās", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, MainDashboardActivity.class);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, "Pieteikšanās neizdevās", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            View f = getCurrentFocus();
            if (f instanceof EditText) {
                Rect r = new Rect();
                f.getGlobalVisibleRect(r);
                if (!r.contains((int) e.getRawX(), (int) e.getRawY())) {
                    f.clearFocus();
                    InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (im != null) {
                        im.hideSoftInputFromWindow(f.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(e);
    }
}
