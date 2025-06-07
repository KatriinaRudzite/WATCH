package com.example.mywatchapp;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SleepOverviewActivity extends AppCompatActivity {

    private WebView chartWebView;
    private TextView sleepDuration, suggestionText;
    private EditText targetSleep, bedtimeInput, wakeInput;
    private ImageButton btnBack;

    private HealthDatabaseHelper dbHelper;
    private String uid = "nezināms";
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_overview);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
        }

        dbHelper = new HealthDatabaseHelper(this);

        chartWebView = findViewById(R.id.chartWebView);
        sleepDuration = findViewById(R.id.tvSleepDuration);
        suggestionText = findViewById(R.id.tvSuggestion);
        targetSleep = findViewById(R.id.etTargetSleep);
        bedtimeInput = findViewById(R.id.etBedtime);
        wakeInput = findViewById(R.id.etWakeTime);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        WebSettings webSettings = chartWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        chartWebView.setWebViewClient(new WebViewClient());
        chartWebView.loadUrl("file:///android_asset/sleep_chart.html");

        TextWatcher sleepWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                try {
                    String sleepStr = targetSleep.getText().toString().trim();
                    String bedStr = bedtimeInput.getText().toString().trim();

                    if (!sleepStr.isEmpty() && bedStr.matches("\\d{1,2}:\\d{2}")) {
                        double sleepHours = Double.parseDouble(sleepStr);
                        String[] parts = bedStr.split(":");
                        int bedHour = Integer.parseInt(parts[0]);
                        int bedMin = Integer.parseInt(parts[1]);
                        int bedTotalMin = bedHour * 60 + bedMin;

                        int sleepTotalMin = (int) (sleepHours * 60);
                        int wakeTotalMin = (bedTotalMin + sleepTotalMin) % 1440;
                        int wakeHour = wakeTotalMin / 60;
                        int wakeMin = wakeTotalMin % 60;

                        String wakeTimeFormatted = String.format(Locale.US, "%02d:%02d", wakeHour, wakeMin);
                        wakeInput.setText(wakeTimeFormatted);

                        saveSleepGoal(sleepHours, bedStr, wakeTimeFormatted);
                        loadSleepSuggestion();
                    }

                } catch (Exception e) {
                    Log.e("SleepOverview", "Kļūda apstrādājot miega ievades datus", e);
                }

                isUpdating = false;
            }
        };

        targetSleep.addTextChangedListener(sleepWatcher);
        bedtimeInput.addTextChangedListener(sleepWatcher);

        insertTestSleepDataIfNeeded();
        loadSleepSuggestion();
        loadSleepChartData();
    }

    private void insertTestSleepDataIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("csv_import", MODE_PRIVATE);
        boolean testSleepInserted = prefs.getBoolean("test_sleep_inserted", false);
        if (testSleepInserted) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            Cursor cursor = db.rawQuery("SELECT id FROM health_data WHERE user_uid = ?", new String[]{uid});
            int healthDataId = -1;
            if (cursor.moveToFirst()) {
                healthDataId = cursor.getInt(0);
            }
            cursor.close();

            if (healthDataId != -1) {
                ContentValues cv = new ContentValues();
                cv.put("health_data_id", healthDataId);
                cv.put("hours", 9.0);
                cv.put("bedtime", "22:30");
                cv.put("wakeup_time", "07:30");

                db.insert("sleep", null, cv);
                prefs.edit().putBoolean("test_sleep_inserted", true).apply();
                Log.d("TEST_DATA", "Testa miega dati ievietoti tabulā 'sleep'.");
            } else {
                Log.e("TEST_DATA", "Nav atrasts health_data ieraksts lietotājam ar UID: " + uid);
            }

        } catch (Exception e) {
            Log.e("TEST_DATA", "Kļūda ievietojot testa datus miega tabulā", e);
        }
    }

    private void saveSleepGoal(double hours, String bedtime, String wakeTime) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL(
                    "INSERT INTO sleep_goals (user_id, target_sleep, bedtime, waketime) VALUES (?, ?, ?, ?)",
                    new Object[]{uid, hours, bedtime, wakeTime}
            );
            Log.d("SleepOverview", "Saglabāts: " + hours + "h, guļ: " + bedtime + ", ceļas: " + wakeTime);
        } catch (Exception e) {
            Log.e("SleepOverview", "Kļūda saglabājot miega mērķus", e);
        }
    }

    private void loadSleepSuggestion() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor sleepCursor = db.rawQuery(
                    "SELECT s.hours FROM sleep s " +
                            "JOIN health_data h ON s.health_data_id = h.id " +
                            "WHERE h.user_uid = ? ORDER BY h.recorded_at DESC LIMIT 1",
                    new String[]{uid}
            );

            if (!sleepCursor.moveToFirst()) {
                suggestionText.setText("Nav pieejamu miega datu.");
                sleepCursor.close();
                return;
            }

            double sleepHours = sleepCursor.getDouble(0);
            sleepDuration.setText("Miega ilgums: " + sleepHours + "h");
            sleepCursor.close();

            Cursor profile = db.rawQuery("SELECT gender, age FROM user_profiles WHERE user_uid = ?", new String[]{uid});
            String gender = "any";
            int age = 0;

            if (profile.moveToFirst()) {
                gender = profile.getString(0);
                age = profile.getInt(1);
            }
            profile.close();

            Cursor rec = db.rawQuery(
                    "SELECT recommendation FROM sleep_recommendations " +
                            "WHERE (? BETWEEN min_hours AND max_hours) " +
                            "AND (? = gender OR gender = 'any') " +
                            "AND (? BETWEEN min_age AND max_age)",
                    new String[]{
                            String.valueOf(sleepHours),
                            gender,
                            String.valueOf(age)
                    }
            );

            if (rec.moveToFirst()) {
                suggestionText.setText(rec.getString(0));
            } else {
                suggestionText.setText("Nav piemērota ieteikuma šiem datiem.");
            }

            rec.close();
        } catch (Exception e) {
            Log.e("SleepOverview", "Kļūda ielādējot ieteikumu", e);
            suggestionText.setText("Kļūda ielādējot ieteikumu.");
        }
    }

    private void loadSleepChartData() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT s.hours FROM sleep s " +
                            "JOIN health_data h ON s.health_data_id = h.id " +
                            "WHERE h.user_uid = ? ORDER BY h.recorded_at DESC LIMIT 7",
                    new String[]{uid}
            );

            List<Double> values = new ArrayList<>();
            while (cursor.moveToNext()) {
                values.add(cursor.getDouble(0));
            }
            cursor.close();

            JSONArray labels = new JSONArray();
            JSONArray data = new JSONArray();

            String[] dayLabels = {"P", "O", "T", "C", "Pk", "S", "Sv"};
            for (int i = 0; i < values.size(); i++) {
                labels.put(dayLabels[i % dayLabels.length]);
                data.put(values.get(i));
            }

            String jsCall = String.format("drawSleepChart(%s, %s);", labels.toString(), data.toString());
            chartWebView.evaluateJavascript(jsCall, null);

        } catch (Exception e) {
            Log.e("SleepOverview", "Kļūda ielādējot miega datus grafikam", e);
        }
    }

    @Override
    protected void onDestroy() {
        if (chartWebView != null) {
            chartWebView.loadUrl("about:blank");
            chartWebView.clearHistory();
            chartWebView.removeAllViews();
            chartWebView.destroy();
            chartWebView = null;
        }
        super.onDestroy();
    }
}
