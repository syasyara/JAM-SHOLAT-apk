package com.example.jamsholat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class QiblaActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private float[] gravity, geomagnetic;
    private TextView txtKiblat;
    private ImageView compassImage;
    private float currentAzimuth = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qibla);

        txtKiblat = findViewById(R.id.txtKiblat);
        compassImage = findViewById(R.id.compass_image);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                float kiblatDirection = getQiblaDirection();
                float difference = (kiblatDirection - azimuth + 360) % 360;

                // Rotate compass image toward Qibla
                RotateAnimation rotate = new RotateAnimation(
                        currentAzimuth,
                        -difference,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(500);
                rotate.setFillAfter(true);

                compassImage.startAnimation(rotate);
                currentAzimuth = -difference;

                txtKiblat.setText(String.format("Arah Kiblat: %.2f° dari Utara", difference));
            }
        }
    }

    private float getQiblaDirection() {
        double kaabaLat = 21.4225;
        double kaabaLon = 39.8262;
        double userLat = -6.9667;     // Ganti sesuai lokasi pengguna
        double userLon = 110.4167;    // Ganti sesuai lokasi pengguna

        double latRad = Math.toRadians(userLat);
        double dLon = Math.toRadians(kaabaLon - userLon);

        double y = Math.sin(dLon) * Math.cos(Math.toRadians(kaabaLat));
        double x = Math.cos(latRad) * Math.sin(Math.toRadians(kaabaLat)) -
                Math.sin(latRad) * Math.cos(Math.toRadians(kaabaLat)) * Math.cos(dLon);

        return (float) ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
