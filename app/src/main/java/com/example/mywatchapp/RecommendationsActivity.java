package com.example.mywatchapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RecommendationsActivity extends AppCompatActivity {

    LinearLayout container;
    HealthDatabaseHelper dbHelper;
    String userId = "nezināms";
    String gender = "Unknown";
    int age = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        container = findViewById(R.id.recommendationsContainer);
        dbHelper = new HealthDatabaseHelper(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        loadProfile();
        checkRecommendations();
        loadRecommendations();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    void loadProfile() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT gender, age FROM user_profiles WHERE user_uid = ?", new String[]{userId});
        if (c.moveToFirst()) {
            gender = c.getString(0);
            age = c.getInt(1);
        }
        c.close();
    }

    void checkRecommendations() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        saveIfNeeded(db, "step_recommendations", "min_steps", "max_steps", "Soļu ieteikums");
        saveIfNeeded(db, "spo2_recommendations", "min_spo2", "max_spo2", "SpO₂ ieteikums");
        saveIfNeeded(db, "sleep_recommendations", "min_hours", "max_hours", "Miega ieteikums");
        saveIfNeeded(db, "heart_rate_recommendations", "min_bpm", "max_bpm", "Sirdsdarbības ieteikums");

        db.close();
    }

    void saveIfNeeded(SQLiteDatabase db, String table, String min, String max, String category) {
        if (age < 0) return;

        Cursor c = db.rawQuery(
                "SELECT recommendation FROM " + table + " WHERE gender = ? AND min_age <= ? AND max_age >= ?",
                new String[]{gender, String.valueOf(age), String.valueOf(age)});

        if (c.moveToFirst()) {
            do {
                String rec = c.getString(0);

                Cursor check = db.rawQuery(
                        "SELECT 1 FROM recommendations WHERE user_uid = ? AND category = ? AND text = ? LIMIT 1",
                        new String[]{userId, category, rec});

                boolean exists = check.moveToFirst();
                check.close();

                if (!exists) {
                    SQLiteDatabase wdb = dbHelper.getWritableDatabase();
                    wdb.execSQL("INSERT INTO recommendations (user_uid, category, text) VALUES (?, ?, ?)",
                            new Object[]{userId, category, rec});
                }
            } while (c.moveToNext());
        }

        c.close();
    }

    void loadRecommendations() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT category, text, created_at FROM recommendations WHERE user_uid = ? ORDER BY created_at DESC",
                new String[]{userId});

        if (c.moveToFirst()) {
            do {
                String category = c.getString(0);
                String text = c.getString(1);
                String time = c.getString(2);

                String msg = category + ":\n" + text + "\n(" + time + ")";
                addCard(msg);
            } while (c.moveToNext());
        }

        c.close();
    }

    void addCard(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.BLACK);
        view.setTextSize(16);
        view.setPadding(32, 24, 32, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(32);
        bg.setStroke(4, getResources().getColor(R.color.white));
        view.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 24, 0, 0);

        container.addView(view, p);
    }
}
