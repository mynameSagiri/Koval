package com.koval.app;

import android.content.Context;
import com.google.firebase.FirebaseApp;

/** Safely detects whether Firebase has been configured in this checkout. */
public final class FirebaseStatus {
    private FirebaseStatus() { }

    public static boolean isConfigured(Context context) {
        try {
            return FirebaseApp.initializeApp(context.getApplicationContext()) != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
