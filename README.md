# NothingMP3

An Android music player built with Jetpack Compose. It plays audio files stored on the device and provides search, playback queue, and playlist features. The project also includes YouTube playback support.

> This is an unofficial app and is not affiliated with or endorsed by Nothing.

## Features

- Browse and search music stored on the device
- Play, pause, skip tracks, shuffle, and seek within tracks
- Create and manage playlists
- Background playback and media notifications powered by AndroidX Media3
- Nothing-inspired interface and Ndot typeface
- YouTube video playback integration

## Requirements

- Android Studio
- JDK 17
- Android SDK 34
- Android 8.0 (API 26) or later device or emulator

## Build and run

1. Clone this repository and open it in Android Studio.
2. Wait for Gradle sync to finish.
3. Select an Android device or emulator and run the `app` configuration.
4. Grant access to audio files when prompted.

To build from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

## Technologies

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Media3 ExoPlayer and MediaSession
- Coil

## Font attribution

The app bundles **NDOT 47 (inspired by NOTHING)** by Interactivate, licensed under the SIL Open Font License 1.1. The copyright notice and license are included in [`licenses/NDOT-47-OFL.txt`](licenses/NDOT-47-OFL.txt) and in the APK at `assets/licenses/NDOT-47-OFL.txt`.

## Permissions

The app uses Android media permissions to read local audio files and play audio in the background. The YouTube playback integration requires network access.
