package com.koval.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private static final String PREFS = "geokoval";
    private MapView map;
    private LocationManager locationManager;
    private Marker myMarker;
    private Location lastLocation;
    private boolean locationUpdatesStarted;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.osmMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(51.5336, 46.0343)); // Saratov until GPS provides a fix

        String room = getSharedPreferences(PREFS, MODE_PRIVATE).getString("room_name", "");
        TextView roomTitle = findViewById(R.id.roomNameText);
        roomTitle.setText("Комната: " + (room.isEmpty() ? "без названия" : room));

        findViewById(R.id.findMeBtn).setOnClickListener(v -> findMe());
        findViewById(R.id.leaveBtn).setOnClickListener(v -> leaveRoom());
        findViewById(R.id.settingsBtn).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.participantsBtn).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Участники")
                .setMessage("В этой версии на карте отображается ваше GPS-местоположение. Для обмена локациями между телефонами нужен сервер комнаты.")
                .setPositiveButton("Понятно", null).show());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (hasLocationPermission()) startLocationUpdates();
        else requestLocationPermission();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        if (hasLocationPermission()) startLocationUpdates();
        else Toast.makeText(this, "Разрешите доступ к геопозиции в настройках Android, чтобы показать GPS на карте.", Toast.LENGTH_LONG).show();
    }

    private void startLocationUpdates() {
        if (locationUpdatesStarted || locationManager == null || !hasLocationPermission()) return;
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("gps_enabled", true)) return;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 3f, this);
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, this);
            Location initial = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (initial == null) initial = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (initial != null) onLocationChanged(initial);
            locationUpdatesStarted = true;
        } catch (SecurityException exception) {
            Toast.makeText(this, "Не удалось получить разрешение на GPS.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationUpdatesStarted) {
            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }
        }
        locationUpdatesStarted = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (myMarker == null) {
            myMarker = new Marker(map);
            myMarker.setTitle(getSharedPreferences(PREFS, MODE_PRIVATE).getString("display_name", "Моя локация"));
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(myMarker);
        }
        myMarker.setPosition(point);
        map.invalidate();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("last_latitude", String.valueOf(location.getLatitude()))
                .putString("last_longitude", String.valueOf(location.getLongitude()))
                .apply();
    }

    private void findMe() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        if (lastLocation != null) {
            map.getController().animateTo(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
            map.getController().setZoom(17.0);
        } else {
            Toast.makeText(this, "Жду GPS-координаты. Проверьте, что геолокация включена.", Toast.LENGTH_LONG).show();
            startLocationUpdates();
        }
    }

    private void leaveRoom() {
        new AlertDialog.Builder(this)
                .setTitle("Покинуть комнату?")
                .setMessage("Вы вернётесь на экран входа.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Покинуть", (dialog, which) -> {
                    stopLocationUpdates();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }).show();
    }

    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); startLocationUpdates(); }
    @Override protected void onPause() { stopLocationUpdates(); if (map != null) map.onPause(); super.onPause(); }
    @Override protected void onDestroy() { stopLocationUpdates(); if (map != null) map.onDetach(); super.onDestroy(); }
}
