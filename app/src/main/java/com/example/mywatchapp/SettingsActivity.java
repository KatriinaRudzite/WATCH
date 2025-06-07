package com.example.mywatchapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    static final String PREFS_NAME = "AppPrefs";
    static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    static final int REQUEST_PROFILE_EDIT = 1001;

    Switch notificationsSwitch;
    Button btnChangePassword, btnProfile, btnSupport, btnLogout, btnDeleteAccount;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnProfile = findViewById(R.id.btnProfile);
        btnSupport = findViewById(R.id.btnSupport);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
        notificationsSwitch.setChecked(enabled);

        notificationsSwitch.setOnCheckedChangeListener((CompoundButton b, boolean on) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, on);
            editor.apply();

            if (on) {
                NotificationHelper.scheduleDailyReminder(this);
                Toast.makeText(this, "Paziņojumi ieslēgti", Toast.LENGTH_SHORT).show();
            } else {
                NotificationHelper.cancelReminder(this);
                Toast.makeText(this, "Paziņojumi izslēgti", Toast.LENGTH_SHORT).show();
            }
        });

        btnChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        btnProfile.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Intent i = new Intent(this, ProfileActivity.class);
                startActivityForResult(i, REQUEST_PROFILE_EDIT);
            } else {
                Toast.makeText(this, "Lietotājs nav pieslēdzies", Toast.LENGTH_SHORT).show();
            }
        });

        btnSupport.setOnClickListener(v -> {
            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle("Atbalsts")
                    .setMessage("Sazinieties ar atbalstu: atbalsts@gmail.com")
                    .setPositiveButton("Labi", null)
                    .create();
            d.show();
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
        });

        btnLogout.setOnClickListener(v -> {
            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle("Apstiprināt iziešanu")
                    .setMessage("Vai tiešām vēlies iziet?")
                    .setCancelable(true)
                    .setPositiveButton("Jā", (dlg, w) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Nē", (dlg, w) -> dlg.dismiss())
                    .create();
            d.show();
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
        });

        btnDeleteAccount.setOnClickListener(v -> {
            EditText passwordInput = new EditText(this);
            passwordInput.setHint("Ievadi paroli");
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle("Dzēst kontu")
                    .setMessage("Lūdzu ievadi paroli, lai apstiprinātu.")
                    .setView(passwordInput)
                    .setPositiveButton("Apstiprināt", (dlg, w) -> {
                        String pwd = passwordInput.getText().toString().trim();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if (user != null && user.getEmail() != null) {
                            AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), pwd);
                            user.reauthenticate(cred).addOnCompleteListener(t1 -> {
                                if (t1.isSuccessful()) {
                                    user.delete().addOnCompleteListener(t2 -> {
                                        if (t2.isSuccessful()) {
                                            Toast.makeText(this, "Konts dzēsts", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Neizdevās dzēst kontu", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(this, "Nepareiza parole", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Atcelt", (dlg, w) -> dlg.dismiss())
                    .create();
            d.show();
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
        });
    }

    @Override
    protected void onActivityResult(int code, int result, @Nullable Intent data) {
        super.onActivityResult(code, result, data);
        if (code == REQUEST_PROFILE_EDIT && result == RESULT_OK) {
            setResult(RESULT_OK);
        }
    }
}
