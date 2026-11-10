package com.koval.app;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "geokoval";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        android.content.SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        SeekBar radiusSeekBar = findViewById(R.id.radiusSeekBar);
        TextView radiusValue = findViewById(R.id.radiusValue);
        SwitchCompat shareSwitch = findViewById(R.id.shareSwitch);
        SwitchCompat gpsSwitch = findViewById(R.id.gpsSwitch);
        TextView locationText = findViewById(R.id.locationText);
        TextView nearestText = findViewById(R.id.nearestUserText);

        int radius = preferences.getInt("notice_radius", 250);
        radiusSeekBar.setProgress(Math.max(0, Math.min(975, radius - 25)));
        showRadius(radiusValue, radius);
        shareSwitch.setChecked(preferences.getBoolean("share_location", true));
        gpsSwitch.setChecked(preferences.getBoolean("gps_enabled", true));

        String lat = preferences.getString("last_latitude", null);
        String lon = preferences.getString("last_longitude", null);
        if (lat != null && lon != null) locationText.setText("Широта: " + lat + ", долгота: " + lon);
        if (!preferences.getBoolean("share_location", true)) nearestText.setText("Передача геопозиции выключена.");

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int meters = progress + 25;
                showRadius(radiusValue, meters);
                if (fromUser) preferences.edit().putInt("notice_radius", meters).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        shareSwitch.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean("share_location", checked).apply();
            nearestText.setText(checked ? "Участники появятся после подключения сервера комнаты." : "Передача геопозиции выключена.");
        });
        gpsSwitch.setOnCheckedChangeListener((button, checked) -> preferences.edit().putBoolean("gps_enabled", checked).apply());
    }

    private void showRadius(TextView text, int meters) {
        text.setText("Радиус уведомления: " + meters + " м");
    }
}
