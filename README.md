# GeoКоваль — Android Java + Firebase

Java Android app with XML layouts, OpenStreetMap tiles (osmdroid), foreground GPS updates, Firebase Anonymous Authentication, Cloud Firestore realtime member/location data, and a callable Cloud Function that securely creates or joins password-protected rooms.

## Firebase setup (required once by the project owner)

1. Create a Firebase project in the Firebase Console and add an Android app with package ID `com.koval.app`.
2. Download `google-services.json` and place it in `app/google-services.json` (never commit a real config if it contains project-specific values you do not want public).
3. Enable **Authentication → Sign-in method → Anonymous**.
4. Create a **Cloud Firestore** database.
5. Install Node.js 20 and Firebase CLI; authenticate and select your Firebase project (`firebase login`, `firebase use --add`).
6. Deploy the room-join function and Firestore rules from the repository root: `firebase deploy --only functions,firestore:rules`.
7. In VS Code, install Extension Pack for Java and Gradle for Java. Install JDK 17, Android SDK Platform 34, and Android Build Tools. Open this folder, sync Gradle, start an emulator or connect a phone, then run `gradle installDebug` (or use Android Studio's Run button).

The deployed callable `createOrJoinRoom` is the trusted authority for room creation and password checks. It stores a salted scrypt password hash, not the room password. Room membership/location documents are protected by `firestore.rules`; only authenticated members can read room data, and a user can only write their own location document. Anonymous Firebase users are device/app identities, not verified people; production deployments should add account recovery and consider enabling Firebase App Check and rate limiting.

## Features

- Create a room on first use or join the same room from another phone with the room name and password.
- Anonymous Firebase sign-in, room membership updates, and realtime Firestore location listeners.
- OpenStreetMap map, foreground GPS/network location tracking, current-user marker, other members' markers, participant list, and leave-room action.
- Location sharing and GPS update preferences; stopping sharing removes the current location document.

## Privacy and limitations

Location is uploaded only while the map screen is open and the sharing switch is on. The app does not request background location. Other room members can see shared coordinates. Users can stop sharing or leave the room. OpenStreetMap tiles and Firebase require internet. The notice radius is saved as a preference; proximity notifications are not yet implemented. `google-services.json`, a Firebase project, enabled Anonymous Auth, Firestore, and deployed Functions are required before the app can connect.
