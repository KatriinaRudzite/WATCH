package com.example.mywatchapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        HealthDatabaseHelper dbHelper = new HealthDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double stepTarget = 0, sleepTarget = 0, stepValue = 0, sleepValue = 0;

        try {
            Cursor c1 = db.rawQuery("SELECT target_steps FROM step_goals WHERE user_id = ? ORDER BY id DESC LIMIT 1", new String[]{uid});
            if (c1.moveToFirst()) stepTarget = c1.getDouble(0);
            c1.close();

            Cursor c2 = db.rawQuery("SELECT target_sleep FROM sleep_goals WHERE user_id = ? ORDER BY id DESC LIMIT 1", new String[]{uid});
            if (c2.moveToFirst()) sleepTarget = c2.getDouble(0);
            c2.close();

            Cursor c3 = db.rawQuery("SELECT s.steps FROM steps s JOIN health_data h ON s.health_data_id = h.id WHERE h.user_uid = ? ORDER BY h.recorded_at DESC LIMIT 1", new String[]{uid});
            if (c3.moveToFirst()) stepValue = c3.getDouble(0);
            c3.close();

            Cursor c4 = db.rawQuery("SELECT s.hours FROM sleep s JOIN health_data h ON s.health_data_id = h.id WHERE h.user_uid = ? ORDER BY h.recorded_at DESC LIMIT 1", new String[]{uid});
            if (c4.moveToFirst()) sleepValue = c4.getDouble(0);
            c4.close();

            if (stepTarget > 0) {
                if (stepValue >= stepTarget) {
                    send(context, "Soļu mērķis sasniegts!", "Apsveicam, Tu esi sasniedzis savu soļu mērķi!");
                } else {
                    send(context, "Soļu atgādinājums", "Tev vēl jānoiet " + (int)(stepTarget - stepValue) + " soļi.");
                }
            }

            if (sleepTarget > 0) {
                if (sleepValue >= sleepTarget) {
                    send(context, "Miega mērķis sasniegts!", "Tu esi izgulējies atbilstoši savam mērķim!");
                } else {
                    send(context, "Miega atgādinājums", String.format(Locale.getDefault(), "Tev vēl jāguļ %.1f stundas.", (sleepTarget - sleepValue)));
                }
            }

        } catch (Exception e) {
        } finally {
            db.close();
        }

        String msg = "Pārbaudīti mērķi " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("reminder_channel", "Atgādinājumi", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Pārbaude pabeigta")
                .setContentText(msg)
                .setAutoCancel(true);
        nm.notify((int) System.currentTimeMillis(), builder.build());

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        SQLiteDatabase db2 = dbHelper.getWritableDatabase();
        db2.execSQL("INSERT INTO " + HealthDatabaseHelper.TABLE_NOTIFICATIONS + " (user_uid, message, status, sent_at) VALUES (?, ?, ?, ?)",
                new Object[]{uid, msg, "new", time});
        db2.close();
    }

    private void send(Context context, String title, String text) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
