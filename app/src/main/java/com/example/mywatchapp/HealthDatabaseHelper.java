package com.example.mywatchapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HealthDatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_NOTIFICATIONS = "notifications";

    public static final String DB_NAME = "health_data.db";
    public static final int DB_VERSION = 9;

    public HealthDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void saveRecommendation(String userUid, String category, String text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_uid", userUid);
        values.put("category", category);
        values.put("text", text);
        db.insertWithOnConflict("recommendations", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id TEXT PRIMARY KEY, username TEXT, email TEXT, registered_at DATETIME)");
        db.execSQL("CREATE TABLE user_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT NOT NULL UNIQUE, name TEXT, gender TEXT, age TEXT, photo_uri TEXT)");
        db.execSQL("CREATE TABLE health_data (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT NOT NULL, recorded_at DATETIME)");
        db.execSQL("CREATE TABLE steps (id INTEGER PRIMARY KEY AUTOINCREMENT, health_data_id INTEGER, step_count INTEGER, distance_km FLOAT)");
        db.execSQL("CREATE TABLE sleep (id INTEGER PRIMARY KEY AUTOINCREMENT, health_data_id INTEGER, hours FLOAT, bedtime TIME, wakeup_time TIME)");
        db.execSQL("CREATE TABLE heart_rate (id INTEGER PRIMARY KEY AUTOINCREMENT, health_data_id INTEGER, bpm INTEGER)");
        db.execSQL("CREATE TABLE spo2 (id INTEGER PRIMARY KEY AUTOINCREMENT, health_data_id INTEGER, spo2_value INTEGER)");
        db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, goal_type TEXT, value INTEGER, status TEXT)");
        db.execSQL("CREATE TABLE activity_log (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, activity TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, message TEXT, status TEXT, sent_at DATETIME)");
        db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, receive_notifications BOOLEAN)");
        db.execSQL("CREATE TABLE step_recommendations (id INTEGER PRIMARY KEY AUTOINCREMENT, gender TEXT, min_age INTEGER, max_age INTEGER, min_steps INTEGER, max_steps INTEGER, recommendation TEXT)");
        db.execSQL("CREATE TABLE spo2_recommendations (id INTEGER PRIMARY KEY AUTOINCREMENT, gender TEXT, min_age INTEGER, max_age INTEGER, min_spo2 INTEGER, max_spo2 INTEGER, recommendation TEXT)");
        db.execSQL("CREATE TABLE heart_rate_recommendations (id INTEGER PRIMARY KEY AUTOINCREMENT, gender TEXT, min_age INTEGER, max_age INTEGER, min_bpm INTEGER, max_bpm INTEGER, recommendation TEXT)");
        db.execSQL("CREATE TABLE sleep_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, target_sleep REAL, bedtime TEXT, waketime TEXT)");
        db.execSQL("CREATE TABLE sleep_recommendations (id INTEGER PRIMARY KEY AUTOINCREMENT, gender TEXT, min_age INTEGER, max_age INTEGER, min_hours REAL, max_hours REAL, recommendation TEXT)");
        db.execSQL("CREATE TABLE recommendations (id INTEGER PRIMARY KEY AUTOINCREMENT, user_uid TEXT, category TEXT, text TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE(user_uid, category, text))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS user_profiles");
        db.execSQL("DROP TABLE IF EXISTS health_data");
        db.execSQL("DROP TABLE IF EXISTS steps");
        db.execSQL("DROP TABLE IF EXISTS sleep");
        db.execSQL("DROP TABLE IF EXISTS heart_rate");
        db.execSQL("DROP TABLE IF EXISTS spo2");
        db.execSQL("DROP TABLE IF EXISTS goals");
        db.execSQL("DROP TABLE IF EXISTS activity_log");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS settings");
        db.execSQL("DROP TABLE IF EXISTS step_recommendations");
        db.execSQL("DROP TABLE IF EXISTS spo2_recommendations");
        db.execSQL("DROP TABLE IF EXISTS heart_rate_recommendations");
        db.execSQL("DROP TABLE IF EXISTS sleep_goals");
        db.execSQL("DROP TABLE IF EXISTS sleep_recommendations");
        db.execSQL("DROP TABLE IF EXISTS recommendations");
        onCreate(db);
    }

    public UserProfile getUserProfile(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, gender, age, photo_uri FROM user_profiles WHERE user_uid = ?", new String[]{uid});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String gender = cursor.getString(1);
            String age = cursor.getString(2);
            String photoUri = cursor.getString(3);
            cursor.close();
            return new UserProfile(uid, name, gender, age, photoUri);
        }
        cursor.close();
        return null;
    }
}
