package com.example.jamsholat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class PrayerReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayerName");
        if (prayerName == null) prayerName = "Sholat";

        // Tampilkan toast
        Toast.makeText(context, "Waktu " + prayerName + " telah tiba", Toast.LENGTH_LONG).show();

        // Tampilkan notifikasi
        showNotification(context, prayerName);

        // Mulai AdzanService
        Intent serviceIntent = new Intent(context, AdzanService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // Android 8+
        } else {
            context.startService(serviceIntent);
        }
    }

    private void showNotification(Context context, String prayerName) {
        String channelId = "sholat_channel";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notifikasi Waktu Sholat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel untuk pengingat waktu sholat harian");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Waktu Sholat")
                .setContentText("Waktunya " + prayerName + " sekarang.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(prayerName.hashCode(), builder.build());
        }
    }
}
