package com.example.mywatchapp;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HeartRateOverviewActivity extends AppCompatActivity {

    WebView chartWebView;
    TextView tvHeartRateValue, tvSuggestion;
    ImageButton btnBack;
    HealthDatabaseHelper dbHelper;
    String userUid = "nezināms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_overview);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userUid = currentUser.getUid();
        }

        chartWebView = findViewById(R.id.chartWebView);
        tvHeartRateValue = findViewById(R.id.tvHeartRateValue);
        tvSuggestion = findViewById(R.id.tvSuggestion);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        WebSettings webSettings = chartWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        chartWebView.setWebViewClient(new WebViewClient());
        chartWebView.loadUrl("file:///android_asset/heart_rate_chart.html");

        dbHelper = new HealthDatabaseHelper(this);

        insertTestHeartRateDataIfNeeded();
        loadHeartRateAndRecommendation();
    }

    private void insertTestHeartRateDataIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("test_data", MODE_PRIVATE);
        boolean inserted = prefs.getBoolean("test_heart_rate_inserted", false);
        if (inserted) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT id FROM health_data WHERE user_uid = ?", new String[]{userUid});
            int healthDataId = -1;
            if (cursor.moveToFirst()) {
                healthDataId = cursor.getInt(0);
            }
            cursor.close();

            if (healthDataId != -1) {
                ContentValues values = new ContentValues();
                values.put("health_data_id", healthDataId);
                values.put("bpm", 68); // Normāls pulss

                db.insert("heart_rate", null, values);
                prefs.edit().putBoolean("test_heart_rate_inserted", true).apply();

                Log.d("TEST_DATA", "Testa pulss ievietots: 72 bpm");
            } else {
                Log.e("TEST_DATA", "Nav atrasts health_data ieraksts lietotājam ar UID: " + userUid);
            }
        } catch (Exception e) {
            Log.e("TEST_DATA", "Kļūda ievietojot testa pulsu", e);
        } finally {
            db.close();
        }
    }

    private void loadHeartRateAndRecommendation() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT bpm FROM heart_rate " +
                            "JOIN health_data ON heart_rate.health_data_id = health_data.id " +
                            "WHERE health_data.user_uid = ? " +
                            "ORDER BY health_data.recorded_at DESC LIMIT 1",
                    new String[]{userUid});

            if (cursor.moveToFirst()) {
                int heartRate = cursor.getInt(0);
                tvHeartRateValue.setText("Pulss: " + heartRate + " bpm");
                cursor.close();

                Cursor userCursor = db.rawQuery(
                        "SELECT gender, age FROM user_profiles WHERE user_uid = ?",
                        new String[]{userUid});

                if (userCursor.moveToFirst()) {
                    String gender = userCursor.getString(0);
                    int age = userCursor.getInt(1);
                    userCursor.close();

                    Cursor rec = db.rawQuery(
                            "SELECT recommendation FROM heart_rate_recommendations " +
                                    "WHERE gender = ? AND ? BETWEEN min_age AND max_age AND ? BETWEEN min_bpm AND max_bpm LIMIT 1",
                            new String[]{gender, String.valueOf(age), String.valueOf(heartRate)});

                    if (rec.moveToFirst()) {
                        tvSuggestion.setText(rec.getString(0));
                        Log.d("RECOMMENDATION", "Ieteikums: " + rec.getString(0));
                    } else {
                        tvSuggestion.setText("Nav pieejama ieteikuma šai pulsa vērtībai.");
                        Log.d("RECOMMENDATION", "Nav ieteikuma šim pulsam.");
                    }
                    rec.close();

                } else {
                    tvSuggestion.setText("Lietotāja profils nav atrasts.");
                    Log.e("USER_PROFILE", "Nav atrasts profils UID: " + userUid);
                }
            } else {
                tvHeartRateValue.setText("Nav pulsa datu.");
                tvSuggestion.setText("Nevar sniegt ieteikumu.");
            }
        } catch (Exception e) {
            Log.e("HeartRateOverview", "Kļūda ielādējot datus", e);
            tvSuggestion.setText("Kļūda ielādējot ieteikumu.");
        } finally {
            db.close();
        }
    }


    @Override
    protected void onDestroy() {
        if (chartWebView != null) {
            chartWebView.loadUrl("about:blank");
            chartWebView.clearHistory();
            chartWebView.removeAllViews();
            chartWebView.destroy();
        }
        super.onDestroy();
    }
}
