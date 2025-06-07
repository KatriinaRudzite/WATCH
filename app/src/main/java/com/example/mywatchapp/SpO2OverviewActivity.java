package com.example.mywatchapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SpO2OverviewActivity extends AppCompatActivity {

    private WebView chartWebView;
    private TextView tvSpO2Value, tvSuggestion;
    private ImageButton btnBack;

    private HealthDatabaseHelper dbHelper;
    private String uid = "nezināms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spo2_overview);

        initializeViews();
        fetchUserUid();

        setupWebView();
        loadLatestSpO2DataWithRecommendation();
    }

    private void initializeViews() {
        chartWebView = findViewById(R.id.chartWebView);
        tvSpO2Value = findViewById(R.id.tvSpO2Value);
        tvSuggestion = findViewById(R.id.tvSuggestion);
        btnBack = findViewById(R.id.btnBack);
        dbHelper = new HealthDatabaseHelper(this);

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
        }
    }

    private void setupWebView() {
        WebSettings settings = chartWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        chartWebView.setWebViewClient(new WebViewClient());
        chartWebView.loadUrl("file:///android_asset/spo2_chart.html");
    }

    private void loadLatestSpO2DataWithRecommendation() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor spo2Cursor = db.rawQuery(
                    "SELECT s.spo2_value " +
                            "FROM spo2 s JOIN health_data h ON s.health_data_id = h.id " +
                            "WHERE h.user_uid = ? " +
                            "ORDER BY h.recorded_at DESC, s.id DESC LIMIT 1",
                    new String[]{uid}
            );

            if (spo2Cursor.moveToFirst()) {
                int spo2Value = spo2Cursor.getInt(0);
                tvSpO2Value.setText("SpO₂: " + spo2Value + "%");
                spo2Cursor.close();

                loadSuggestionForSpO2Value(db, spo2Value);
            } else {
                tvSpO2Value.setText("Nav pieejamu SpO₂ datu.");
                tvSuggestion.setText("Lūdzu sinhronizē datus no viedpulksteņa.");
                spo2Cursor.close();
            }
        } catch (Exception e) {
            tvSpO2Value.setText("Kļūda datu ielādē.");
            tvSuggestion.setText("Notikusi kļūda, mēģini vēlreiz.");
        } finally {
            db.close();
        }
    }

    private void loadSuggestionForSpO2Value(SQLiteDatabase db, int spo2) {
        Cursor profileCursor = db.rawQuery(
                "SELECT gender, age FROM user_profiles WHERE user_uid = ?",
                new String[]{uid}
        );

        if (profileCursor.moveToFirst()) {
            String gender = profileCursor.getString(0);
            int age;

            try {
                age = Integer.parseInt(profileCursor.getString(1));
            } catch (NumberFormatException e) {
                tvSuggestion.setText("Vecums nav derīgā formātā.");
                profileCursor.close();
                return;
            }
            profileCursor.close();

            Cursor recCursor = db.rawQuery(
                    "SELECT recommendation FROM spo2_recommendations " +
                            "WHERE gender = ? AND ? BETWEEN min_age AND max_age " +
                            "AND ? BETWEEN min_spo2 AND max_spo2 LIMIT 1",
                    new String[]{gender, String.valueOf(age), String.valueOf(spo2)}
            );

            if (recCursor.moveToFirst()) {
                tvSuggestion.setText(recCursor.getString(0));
            } else {
                tvSuggestion.setText("Nav īpašu ieteikumu šai SpO₂ vērtībai.");
            }
            recCursor.close();

        } else {
            tvSuggestion.setText("Lietotāja profils nav aizpildīts – ieteikums nav pieejams.");
            profileCursor.close();
        }
    }
}
