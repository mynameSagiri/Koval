package com.koval.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "geokoval";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextInputEditText nameInput = findViewById(R.id.inputName);
        TextInputEditText roomInput = findViewById(R.id.inputRoom);
        TextInputEditText passwordInput = findViewById(R.id.inputPassword);
        Button continueButton = findViewById(R.id.continueBtn);

        continueButton.setOnClickListener(view -> {
            String name = valueOf(nameInput.getText());
            String room = valueOf(roomInput.getText());
            String password = valueOf(passwordInput.getText());
            if (name.isEmpty() || room.length() < 4 || password.isEmpty()) {
                Toast.makeText(this, "Укажите имя, комнату (не менее 4 символов) и пароль", Toast.LENGTH_LONG).show();
                return;
            }
            // Store only on this device for the offline MVP. Never store this password on a real server as plain text.
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("display_name", name)
                    .putString("room_name", room)
                    .putString("room_password", password)
                    .apply();
            startActivity(new Intent(this, MapActivity.class));
        });
    }

    private String valueOf(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }
}
