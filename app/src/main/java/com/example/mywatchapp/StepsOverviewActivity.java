package com.example.mywatchapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;

import java.util.Locale;

public class StepsOverviewActivity extends AppCompatActivity {

    WebView chartWebView;
    TextView stepsText, suggestionText;
    EditText distanceInput, stepsInput;
    ImageButton btnBack;
    HealthDatabaseHelper dbHelper;
    String userUid = "nezināms";
    double STEP_LENGTH_KM = 0.00075;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_overview);

        chartWebView = findViewById(R.id.chartWebView);
        stepsText = findViewById(R.id.tvStepsCount);
        suggestionText = findViewById(R.id.tvSuggestion);
        distanceInput = findViewById(R.id.etDistance);
        stepsInput = findViewById(R.id.etStepsGoal);
        btnBack = findViewById(R.id.btnBack);
        dbHelper = new HealthDatabaseHelper(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userUid = user.getUid();
        }

        btnBack.setOnClickListener(v -> finish());

        WebSettings settings = chartWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        chartWebView.setWebViewClient(new WebViewClient());
        chartWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        chartWebView.loadUrl("file:///android_asset/chart.html");

        int steps = 6773;
        stepsInput.setText(String.valueOf(steps));
        distanceInput.setText(String.format(Locale.US, "%.2f", steps * STEP_LENGTH_KM));
        stepsText.setText("Soļi šodien: " + steps + " (" + String.format(Locale.US, "%.2f", steps * STEP_LENGTH_KM) + " km)");

        loadSuggestion(steps);
    }

    private void loadSuggestion(int steps) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String gender = "any";
        int age = 0;

        Cursor c = db.rawQuery("SELECT gender, age FROM user_profiles WHERE user_uid = ?", new String[]{userUid});
        if (c.moveToFirst()) {
            gender = c.getString(0);
            age = Integer.parseInt(c.getString(1));
        }
        c.close();

        Cursor cur = db.rawQuery("SELECT recommendation FROM step_recommendations WHERE ? BETWEEN min_steps AND max_steps AND (? = gender OR gender = 'any') AND ? BETWEEN min_age AND max_age LIMIT 1",
                new String[]{String.valueOf(steps), gender, String.valueOf(age)});

        if (cur.moveToFirst()) {
            suggestionText.setText(cur.getString(0));
        } else {
            suggestionText.setText("Nav piemērota ieteikuma.");
        }

        cur.close();
    }

    public class WebAppInterface {
        Context context;
        WebAppInterface(Context c) {
            context = c;
        }

        @JavascriptInterface
        public String getStepsData() {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT step_count FROM steps ORDER BY id DESC LIMIT 7", null);
                JSONArray arr = new JSONArray();
                while (cursor.moveToNext()) {
                    arr.put(cursor.getInt(0));
                }
                cursor.close();
                return arr.toString();
            } catch (Exception e) {
                Log.e("WebAppInterface", "Kļūda ielādējot soļu datus", e);
                return "[4500, 6500, 7200, 8000, 4000, 9000, 7777]";
            }
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
