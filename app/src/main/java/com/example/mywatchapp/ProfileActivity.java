package com.example.mywatchapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    EditText nameInput, ageEditText;
    Spinner genderSpinner;
    ImageView profileIcon;
    Button btnSave;
    ImageButton btnBack;

    HealthDatabaseHelper dbHelper;
    String uid;
    File profileImageFile;
    static final int REQUEST_IMAGE_PICK = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameInput = findViewById(R.id.profileName);
        genderSpinner = findViewById(R.id.genderSpinner);
        ageEditText = findViewById(R.id.ageEditText);
        profileIcon = findViewById(R.id.profileIcon);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new HealthDatabaseHelper(this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = user.getUid();
        profileImageFile = new File(getFilesDir(), "profile_" + uid + ".jpg");

        genderSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Dzimums", "Vīrietis", "Sieviete"}));

        profileIcon.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> {
            saveProfile();
            setResult(RESULT_OK);
            finish();
        });
        btnBack.setOnClickListener(v -> finish());

        loadProfile();
    }

    void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int code, int result, @Nullable Intent data) {
        super.onActivityResult(code, result, data);
        if (code == REQUEST_IMAGE_PICK && result == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();

                FileOutputStream out = new FileOutputStream(profileImageFile);
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();

                profileIcon.setImageBitmap(bmp);
            } catch (Exception e) {
                Toast.makeText(this, "Neizdevās ielādēt attēlu", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String ageStr = ageEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Lūdzu ievadiet vārdu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gender.equals("Dzimums")) {
            Toast.makeText(this, "Lūdzu izvēlieties dzimumu", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 18) {
                Toast.makeText(this, "Vecumam jābūt vismaz 18 gadiem", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lūdzu ievadiet derīgu vecumu", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("user_profiles", "user_uid = ?", new String[]{uid});

        ContentValues values = new ContentValues();
        values.put("user_uid", uid);
        values.put("name", name);
        values.put("gender", gender);
        values.put("age", age);
        values.put("photo_uri", profileImageFile.getAbsolutePath());

        db.insert("user_profiles", null, values);
        Toast.makeText(this, "Profils saglabāts", Toast.LENGTH_SHORT).show();
    }

    void loadProfile() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name, gender, age, photo_uri FROM user_profiles WHERE user_uid = ?", new String[]{uid});

        if (c.moveToFirst()) {
            nameInput.setText(c.getString(0));
            genderSpinner.setSelection(((ArrayAdapter<String>) genderSpinner.getAdapter()).getPosition(c.getString(1)));
            ageEditText.setText(c.getString(2));

            String path = c.getString(3);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(path);
                    profileIcon.setImageBitmap(bmp);
                } else {
                    profileIcon.setImageResource(R.drawable.default_profile_icon);
                }
            }
        }
        c.close();
    }
}
