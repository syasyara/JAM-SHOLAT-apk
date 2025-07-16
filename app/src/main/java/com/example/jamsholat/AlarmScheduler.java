// Tahap 4: Kembangkan lebih lanjut dengan alarm harian otomatis (repeating)

package com.example.jamsholat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleRepeatingAlarm(Context context, int hour, int minute, String prayerName, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Set repeating for next day if time already passed
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        intent.putExtra("prayerName", prayerName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    public static void cancelAlarm(Context context, int requestCode) {
        Intent intent = new Intent(context, PrayerReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void resetAllAlarms(Context context, double[] prayerTimes, String[] prayerNames) {
        for (int i = 0; i < prayerTimes.length; i++) {
            int hour = (int) prayerTimes[i];
            int minute = (int) ((prayerTimes[i] - hour) * 60) - 15;

            if (minute < 0) {
                hour -= 1;
                minute += 60;
            }

            scheduleRepeatingAlarm(context, hour, minute, prayerNames[i], i);
        }
    }

    public static void cancelAllAlarms(Context context, int totalPrayers) {
        for (int i = 0; i < totalPrayers; i++) {
            cancelAlarm(context, i);
        }
    }

    // Integrasi ke MainActivity
    public static void handleReminderSwitch(Context context, Switch switchReminder, double[] prayerTimes, String[] prayerNames) {
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                resetAllAlarms(context, prayerTimes, prayerNames);
            } else {
                cancelAllAlarms(context, prayerNames.length);
            }
        });
    }
} // end of AlarmScheduler
