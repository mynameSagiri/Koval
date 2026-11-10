package com.koval.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private static final String PREFS = "geokoval";
    private MapView map;
    private LocationManager locationManager;
    private Location lastLocation;
    private Marker myMarker;
    private boolean locationUpdatesStarted;
    private boolean firebaseRoom;
    private String uid, roomId, displayName;
    private final Map<String, Marker> memberMarkers = new HashMap<>();
    private ListenerRegistration membersListener, locationsListener;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) startLocationUpdates();
                else Toast.makeText(this, "Для показа вашей позиции разрешите доступ к геолокации.", Toast.LENGTH_LONG).show();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);
        map = findViewById(R.id.osmMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(51.5336, 46.0343));

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        uid = FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getUid();
        roomId = prefs.getString("room_id", null);
        displayName = prefs.getString("display_name", "Участник");
        firebaseRoom = prefs.getBoolean("firebase_room", false) && FirebaseStatus.isConfigured(this) && uid != null;
        String roomName = prefs.getString("room_name", "");
        ((TextView) findViewById(R.id.roomNameText)).setText("Комната: " + roomName);

        findViewById(R.id.findMeBtn).setOnClickListener(v -> findMe());
        findViewById(R.id.leaveBtn).setOnClickListener(v -> leaveRoom());
        findViewById(R.id.settingsBtn).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.participantsBtn).setOnClickListener(v -> showParticipants());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (roomId == null) { leaveToLogin(); return; }
        if (firebaseRoom) listenForMembers();
        else Toast.makeText(this, "Локальный режим: другие телефоны не синхронизируются до настройки Firebase.", Toast.LENGTH_LONG).show();
        if (hasLocationPermission()) startLocationUpdates();
        else locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void listenForMembers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        membersListener = db.collection("rooms").document(roomId).collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { showFirestoreError(error.getLocalizedMessage()); return; }
                    if (snapshot == null) return;
                    Map<String, String> names = new HashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("displayName");
                        names.put(doc.getId(), name == null ? "Участник" : name);
                    }
                    listenForLocations(names);
                });
    }

    private void listenForLocations(Map<String, String> names) {
        if (locationsListener != null) locationsListener.remove();
        locationsListener = FirebaseFirestore.getInstance().collection("rooms").document(roomId).collection("locations")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) { if (error != null) showFirestoreError(error.getLocalizedMessage()); return; }
                    Set<String> active = new HashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Double lat = doc.getDouble("latitude"), lon = doc.getDouble("longitude");
                        if (lat == null || lon == null || doc.getId().equals(uid)) continue;
                        String memberId = doc.getId();
                        active.add(memberId);
                        Marker marker = memberMarkers.get(memberId);
                        if (marker == null) {
                            marker = new Marker(map);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            memberMarkers.put(memberId, marker);
                            map.getOverlays().add(marker);
                        }
                        marker.setPosition(new GeoPoint(lat, lon));
                        marker.setTitle(names.getOrDefault(memberId, "Участник"));
                    }
                    for (String id : new HashSet<>(memberMarkers.keySet())) {
                        if (!active.contains(id)) map.getOverlays().remove(memberMarkers.remove(id));
                    }
                    map.invalidate();
                });
    }

    private void showFirestoreError(String message) {
        Toast.makeText(this, "Ошибка Firebase: " + (message == null ? "проверьте настройки" : message), Toast.LENGTH_LONG).show();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (locationUpdatesStarted || locationManager == null || !hasLocationPermission()) return;
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("gps_enabled", true)) return;
        try {
            boolean requested = false;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this);
                requested = true;
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, this);
                requested = true;
            }
            Location initial = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (initial == null) initial = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (initial != null) onLocationChanged(initial);
            locationUpdatesStarted = requested;
            if (!requested) Toast.makeText(this, "Включите геолокацию в настройках устройства.", Toast.LENGTH_LONG).show();
        } catch (SecurityException exception) {
            Toast.makeText(this, "Не удалось получить GPS-разрешение.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationUpdatesStarted) {
            try { locationManager.removeUpdates(this); } catch (SecurityException ignored) { }
        }
        locationUpdatesStarted = false;
    }

    @Override public void onLocationChanged(Location location) {
        lastLocation = location;
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (myMarker == null) {
            myMarker = new Marker(map);
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            myMarker.setTitle(displayName + " (вы)");
            map.getOverlays().add(myMarker);
        }
        myMarker.setPosition(point);
        map.invalidate();
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString("last_latitude", String.valueOf(location.getLatitude()))
                .putString("last_longitude", String.valueOf(location.getLongitude())).apply();
        if (firebaseRoom && prefs.getBoolean("share_location", true)) {
            Map<String, Object> data = new HashMap<>();
            data.put("latitude", location.getLatitude());
            data.put("longitude", location.getLongitude());
            data.put("displayName", displayName);
            data.put("roomId", roomId);
            data.put("updatedAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("rooms").document(roomId).collection("locations")
                    .document(uid).set(data)
                    .addOnFailureListener(error -> showFirestoreError(error.getLocalizedMessage()));
        }
    }

    private void findMe() {
        if (!hasLocationPermission()) { locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}); return; }
        if (lastLocation != null) {
            map.getController().animateTo(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
            map.getController().setZoom(17.0);
        } else Toast.makeText(this, "Ожидаю GPS-координаты. Проверьте, что геолокация включена.", Toast.LENGTH_LONG).show();
    }

    private void showParticipants() {
        StringBuilder text = new StringBuilder("В комнате: ").append(memberMarkers.size() + 1).append(" (включая вас)");
        if (!memberMarkers.isEmpty()) {
            text.append("\n\nНа карте:");
            for (Marker marker : memberMarkers.values()) text.append("\n• ").append(marker.getTitle());
        }
        if (!firebaseRoom) text.append("\n\nЭто локальная демонстрационная комната.");
        new AlertDialog.Builder(this).setTitle("Участники").setMessage(text.toString()).setPositiveButton("Закрыть", null).show();
    }

    private void leaveRoom() {
        new AlertDialog.Builder(this).setTitle("Покинуть комнату?").setMessage("Ваша запись участника и геопозиция будут удалены.")
                .setNegativeButton("Отмена", null).setPositiveButton("Покинуть", (dialog, which) -> {
                    stopLocationUpdates();
                    if (firebaseRoom) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.batch()
                                .delete(db.collection("rooms").document(roomId).collection("locations").document(uid))
                                .delete(db.collection("rooms").document(roomId).collection("members").document(uid))
                                .commit().addOnCompleteListener(task -> leaveToLogin());
                    } else leaveToLogin();
                }).show();
    }

    private void leaveToLogin() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove("room_id").remove("room_name").remove("firebase_room").apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); startLocationUpdates(); }
    @Override protected void onPause() { stopLocationUpdates(); if (map != null) map.onPause(); super.onPause(); }
    @Override protected void onDestroy() {
        stopLocationUpdates();
        if (membersListener != null) membersListener.remove();
        if (locationsListener != null) locationsListener.remove();
        if (map != null) map.onDetach();
        super.onDestroy();
    }
}
