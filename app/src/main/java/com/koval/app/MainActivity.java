package com.koval.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText nameInput, roomInput, passwordInput;
    private Button continueButton;
    private boolean firebaseReady;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameInput = findViewById(R.id.inputName);
        roomInput = findViewById(R.id.inputRoom);
        passwordInput = findViewById(R.id.inputPassword);
        continueButton = findViewById(R.id.continueBtn);
        continueButton.setOnClickListener(v -> authenticateAndJoin());

        firebaseReady = FirebaseStatus.isConfigured(this);
        if (firebaseReady && FirebaseAuth.getInstance().getCurrentUser() == null) {
            setBusy(true);
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(result -> setBusy(false))
                    .addOnFailureListener(error -> {
                        setBusy(false);
                        showError("Не удалось подключиться к Firebase: " + safeMessage(error));
                    });
        } else if (!firebaseReady) {
            Toast.makeText(this, "Firebase не настроен: приложение запустится в локальном режиме. Для общих комнат добавьте google-services.json и настройте Firebase.", Toast.LENGTH_LONG).show();
        }
    }

    private void authenticateAndJoin() {
        String name = value(nameInput), room = value(roomInput), password = value(passwordInput);
        if (name.isEmpty() || room.length() < 4 || password.length() < 4) {
            showError("Укажите имя, название комнаты (не менее 4 символов) и пароль (не менее 4 символов).");
            return;
        }
        if (!firebaseReady) {
            saveSession(name, room, "local-" + room.toLowerCase(java.util.Locale.ROOT));
            startActivity(new Intent(this, MapActivity.class));
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showError("Подключение ещё не готово. Проверьте интернет и повторите попытку.");
            return;
        }

        setBusy(true);
        Map<String, Object> input = new HashMap<>();
        input.put("displayName", name);
        input.put("roomName", room);
        input.put("password", password);
        FirebaseFunctions.getInstance().getHttpsCallable("createOrJoinRoom").call(input)
                .addOnSuccessListener(result -> {
                    @SuppressWarnings("unchecked") Map<String, Object> response = (Map<String, Object>) result.getData();
                    if (response == null || response.get("roomId") == null) {
                        setBusy(false);
                        showError("Сервер вернул некорректный ответ.");
                        return;
                    }
                    saveSession(name, room, String.valueOf(response.get("roomId")));
                    setBusy(false);
                    startActivity(new Intent(this, MapActivity.class));
                })
                .addOnFailureListener(error -> {
                    setBusy(false);
                    showError(safeMessage(error));
                });
    }

    private void saveSession(String name, String room, String roomId) {
        // Never persist the password on the device. The callable validates it on the server.
        getSharedPreferences("geokoval", MODE_PRIVATE).edit()
                .putString("display_name", name)
                .putString("room_name", room)
                .putString("room_id", roomId)
                .putBoolean("firebase_room", firebaseReady)
                .apply();
    }

    private String value(CharSequence value) { return value == null ? "" : value.toString().trim(); }
    private String safeMessage(Exception error) { return error.getLocalizedMessage() == null ? "Не удалось выполнить запрос." : error.getLocalizedMessage(); }
    private void setBusy(boolean busy) { continueButton.setEnabled(!busy); continueButton.setText(busy ? "Подключение…" : "Продолжить"); }
    private void showError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
