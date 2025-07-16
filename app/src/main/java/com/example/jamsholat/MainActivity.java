package com.example.jamsholat;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView textClock, nextPrayerName, nextPrayerTime;
    private LinearLayout layoutPrayerTimes;
    private Spinner spinnerLocation;
    private SwitchCompat switchReminder;
    private Button buttonQibla, buttonTestAdzan;
    private final Handler handler = new Handler();

    private final Map<String, double[]> locations = new HashMap<>() {{
        put("Jakarta", new double[]{-6.2088, 106.8456});
        put("Bandung", new double[]{-6.9175, 107.6191});
        put("Semarang", new double[]{-6.9667, 110.4167});
        put("Surabaya", new double[]{-7.2504, 112.7688});
        put("Yogyakarta", new double[]{-7.7956, 110.3695});
    }};

    private final String[] prayerNames = {"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};
    private final String[] prayerDescriptions = {
            "Imsak: 04:15",
            "Matahari terbit",
            "Mulai matahari tergelincir",
            "Panjang bayangan = tinggi benda",
            "Setelah matahari terbenam",
            "Cahaya merah hilang di ufuk barat"
    };

    private final List<TextView> prayerTimeViews = new ArrayList<>();
    private double[] prayerTimes;

    private static final String PREFS_NAME = "SholatPrefs";
    private static final String KEY_SELECTED_CITY = "selectedCity";
    private static final String KEY_REMINDER_STATUS = "reminderStatus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinner();
        startClock();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isReminderOn = prefs.getBoolean(KEY_REMINDER_STATUS, false);
        switchReminder.setChecked(isReminderOn);

        if (isReminderOn) {
            setPrayerAlarms();
        }

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_REMINDER_STATUS, isChecked);
            editor.apply();

            if (isChecked) {
                setPrayerAlarms();
            }
        });

        buttonQibla.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QiblaActivity.class);
            startActivity(intent);
        });

        // 🔊 Listener untuk tombol "Tes Putar Adzan"
        buttonTestAdzan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdzanService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        });
    }

    private void initViews() {
        textClock = findViewById(R.id.textClock);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        layoutPrayerTimes = findViewById(R.id.layoutPrayerTimes);
        nextPrayerName = findViewById(R.id.nextPrayerName);
        nextPrayerTime = findViewById(R.id.nextPrayerTime);
        switchReminder = findViewById(R.id.switchReminder);
        buttonQibla = findViewById(R.id.buttonQibla);
        buttonTestAdzan = findViewById(R.id.buttonTestAdzan); // 🆕 Tambahan tombol test
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new ArrayList<>(locations.keySet()));
        spinnerLocation.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastSelectedCity = prefs.getString(KEY_SELECTED_CITY, "Jakarta");
        int position = new ArrayList<>(locations.keySet()).indexOf(lastSelectedCity);
        if (position >= 0) {
            spinnerLocation.setSelection(position);
        }

        spinnerLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(KEY_SELECTED_CITY, selectedCity);
                editor.apply();

                calculatePrayerTimes();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void calculatePrayerTimes() {
        String location = spinnerLocation.getSelectedItem().toString();
        double[] coords = locations.get(location);

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        prayerTimes = PrayerCalculator.calcPrayerTimes(year, month, day,
                coords[1], coords[0], 7, -18, -18);

        updatePrayerLayout();
        updateNextPrayer();

        if (switchReminder.isChecked()) {
            setPrayerAlarms();
        }
    }

    private void updatePrayerLayout() {
        layoutPrayerTimes.removeAllViews();
        prayerTimeViews.clear();

        for (int i = 0; i < prayerNames.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(0, 8, 0, 8);

            TextView label = new TextView(this);
            label.setText(prayerNames[i] + "\n" + prayerDescriptions[i]);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            label.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            TextView time = new TextView(this);
            time.setText(formatTime(prayerTimes[i]));
            time.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            time.setTextAlignment(TextView.TEXT_ALIGNMENT_VIEW_END);
            time.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            item.addView(label);
            item.addView(time);
            layoutPrayerTimes.addView(item);
            prayerTimeViews.add(time);
        }
    }

    private void updateNextPrayer() {
        Calendar now = Calendar.getInstance();
        double currentTime = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;

        for (int i = 0; i < prayerTimes.length; i++) {
            if (currentTime < prayerTimes[i]) {
                nextPrayerName.setText(prayerNames[i]);
                double diff = prayerTimes[i] - currentTime;
                int h = (int) diff;
                int m = (int) ((diff - h) * 60);
                nextPrayerTime.setText(String.format("%s (dalam %d jam %d menit)", formatTime(prayerTimes[i]), h, m));
                return;
            }
        }

        nextPrayerName.setText("Subuh (besok)");
        nextPrayerTime.setText("04:30 (dalam beberapa jam)");
    }

    private void startClock() {
        Runnable updateTime = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, EEEE - HH:mm:ss", new Locale("id", "ID"));
                textClock.setText(format.format(calendar.getTime()));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTime);
    }

    private String formatTime(double time) {
        int hour = (int) time;
        int minute = (int) ((time - hour) * 60);
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private void setPrayerAlarms() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null || !alarmManager.canScheduleExactAlarms()) return;

        Calendar now = Calendar.getInstance();
        String location = spinnerLocation.getSelectedItem().toString();
        double[] coords = locations.get(location);

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);

        double[] times = PrayerCalculator.calcPrayerTimes(year, month, day,
                coords[1], coords[0], 7, -18, -18);

        for (int i = 0; i < prayerNames.length; i++) {
            int hour = (int) times[i];
            int minute = (int) ((times[i] - hour) * 60) - 15;
            if (minute < 0) {
                hour -= 1;
                minute += 60;
            }

            Calendar alarmTime = Calendar.getInstance();
            alarmTime.set(Calendar.HOUR_OF_DAY, hour);
            alarmTime.set(Calendar.MINUTE, minute);
            alarmTime.set(Calendar.SECOND, 0);

            if (alarmTime.after(Calendar.getInstance())) {
                Intent intent = new Intent(this, PrayerReminderReceiver.class);
                intent.putExtra("prayerName", prayerNames[i]);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, i, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
