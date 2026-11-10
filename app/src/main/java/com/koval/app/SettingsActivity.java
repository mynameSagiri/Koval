package com.koval.app;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "geokoval";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String roomId = prefs.getString("room_id", null);
        String uid = FirebaseAuth.getInstance().getUid();
        SeekBar radius = findViewById(R.id.radiusSeekBar);
        TextView radiusText = findViewById(R.id.radiusValue);
        SwitchCompat share = findViewById(R.id.shareSwitch), gps = findViewById(R.id.gpsSwitch);
        TextView location = findViewById(R.id.locationText), nearest = findViewById(R.id.nearestUserText);
        int savedRadius = prefs.getInt("notice_radius", 250);
        radius.setProgress(Math.max(0, Math.min(975, savedRadius - 25)));
        setRadiusText(radiusText, savedRadius);
        share.setChecked(prefs.getBoolean("share_location", true));
        gps.setChecked(prefs.getBoolean("gps_enabled", true));
        String lat = prefs.getString("last_latitude", null), lon = prefs.getString("last_longitude", null);
        if (lat != null && lon != null) location.setText("Широта: " + lat + ", долгота: " + lon);
        nearest.setText("Участники с включённым обменом отображаются на карте.");

        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int meters = progress + 25;
                setRadiusText(radiusText, meters);
                if (fromUser) prefs.edit().putInt("notice_radius", meters).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        share.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("share_location", checked).apply();
            if (roomId != null && uid != null) {
                if (!checked) FirebaseFirestore.getInstance().collection("rooms").document(roomId).collection("locations").document(uid).delete();
                else {
                    Map<String, Object> member = new HashMap<>();
                    member.put("sharingLocation", true);
                    member.put("updatedAt", FieldValue.serverTimestamp());
                    FirebaseFirestore.getInstance().collection("rooms").document(roomId).collection("members").document(uid).update(member);
                }
            }
        });
        gps.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean("gps_enabled", checked).apply());
    }

    private void setRadiusText(TextView view, int meters) { view.setText("Радиус уведомления: " + meters + " м"); }
}
