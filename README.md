# GeoКоваль — Android Java MVP

Native Android application written in Java with XML layouts. Open this repository folder in Android Studio, or use VS Code with the Android SDK and Java/Gradle extensions.

## Run from VS Code

1. Install a JDK 17, Android Studio (for Android SDK/emulator), and VS Code extensions **Extension Pack for Java** and **Gradle for Java**.
2. In Android Studio's SDK Manager install Android SDK Platform 34 and Build Tools.
3. Set `ANDROID_HOME` to your Android SDK folder and accept SDK licenses (`sdkmanager --licenses`).
4. Open this folder in VS Code and run Gradle sync/build. If Gradle is not installed, install Gradle 8.7 or generate the wrapper from Android Studio (`gradle wrapper --gradle-version 8.7`).
5. Start an emulator or connect an Android phone with USB debugging, then run `gradle installDebug` (or use the Android extension's Run command).

## Included now

- Russian join/create-room form with basic input validation.
- OpenStreetMap map using osmdroid (no Google Maps API key needed).
- Runtime location permission request and foreground GPS/network location updates.
- Current-position marker and **Где я?** recenter action.
- Settings for GPS updates, location sharing preference, and a persisted notification-radius value.
- Room leave action and participant explanation.

## Limitations

This MVP obtains and displays the current device's location. Room name/password and preferences are stored locally with SharedPreferences. It does **not** yet authenticate users, synchronize rooms/locations between devices, display other participants, or send proximity notifications. Those features need a backend (for example, Firebase) and a secure room-membership design. The password is only a local demo value in this build; do not use it as production authentication.

OpenStreetMap tiles require internet. Location permission and the device's Location/GPS setting must be enabled. Location updates run only while the map screen is open, avoiding background tracking.
