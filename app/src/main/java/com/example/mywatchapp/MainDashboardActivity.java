package com.example.mywatchapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class MainDashboardActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView profileImageView;
    private Button btnSteps, btnSleep, btnHeart, btnSpO2, btnSettings, btnRecommendations;
    private HealthDatabaseHelper dbHelper;
    private String uid;
    private SwipeRefreshLayout swipeRefresh;

    private static final int REQUEST_PROFILE_UPDATE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lietotājs nav pieteicies", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = user.getUid();
        dbHelper = new HealthDatabaseHelper(this);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        welcomeText = findViewById(R.id.welcomeText);
        profileImageView = findViewById(R.id.profileImageView);

        btnSteps = findViewById(R.id.btn3);
        btnSleep = findViewById(R.id.btn4);
        btnHeart = findViewById(R.id.btn5);
        btnSpO2 = findViewById(R.id.btn6);
        btnSettings = findViewById(R.id.btn2);
        btnRecommendations = findViewById(R.id.btnRecommendations);

        swipeRefresh.setOnRefreshListener(() -> {
            loadUserProfile();
            swipeRefresh.setRefreshing(false);
        });

        loadUserProfile();

        setButtonClick(btnSteps, StepsOverviewActivity.class);
        setButtonClick(btnSleep, SleepOverviewActivity.class);
        setButtonClick(btnHeart, HeartRateOverviewActivity.class);
        setButtonClick(btnSpO2, SpO2OverviewActivity.class);
        setButtonClick(btnRecommendations, RecommendationsActivity.class);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_PROFILE_UPDATE);
        });

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues healthEntry = new ContentValues();
        healthEntry.put("user_uid", uid);
        healthEntry.put("recorded_at", System.currentTimeMillis());
        long healthDataId = db.insert("health_data", null, healthEntry);

        ContentValues spo2Entry = new ContentValues();
        spo2Entry.put("health_data_id", healthDataId);
        spo2Entry.put("spo2_value", 95);
        db.insert("spo2", null, spo2Entry);

        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROFILE_UPDATE && resultCode == RESULT_OK) {
            loadUserProfile();
        }
    }

    private void loadUserProfile() {
        UserProfile profile = dbHelper.getUserProfile(uid);

        if (profile != null) {
            String name = profile.name;
            String uri = profile.photoUri;

            welcomeText.setText((name != null && !name.isEmpty()) ? "Labdien, " + name + "!" : "Labdien!");

            if (uri != null && !uri.isEmpty() && !uri.equals("null")) {
                File imgFile = new File(uri);
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(uri);
                    profileImageView.setImageBitmap(bitmap);
                } else {
                    profileImageView.setImageResource(R.drawable.default_profile_icon);
                }
            } else {
                profileImageView.setImageResource(R.drawable.default_profile_icon);
            }
        } else {
            welcomeText.setText("Labdien!");
            profileImageView.setImageResource(R.drawable.default_profile_icon);
        }
    }

    private void setButtonClick(Button btn, Class<?> cls) {
        if (btn != null) {
            btn.setOnClickListener(v -> {
                Intent intent = new Intent(this, cls);
                intent.putExtra("uid", uid);
                startActivity(intent);
            });
        }
    }
}
