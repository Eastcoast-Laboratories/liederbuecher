# Kultliederbuch

Kultliederbuch is an Android app for browsing and searching a local songbook collection based on the German songbook series **"Das Ding"**.

## What the app does

- Search songs by **title**, **artist**, and **lyrics**
- Show song details with lyrics and chord information
- Display the matching book and page references
- Mark songs as favorites
- Store user comments locally on the device
- Load the song data fully offline from bundled files

## Data source

The app uses local CSV and JSON data that are bundled with the app and loaded on startup. No external server is required.

## Screenshots

![Screenshot](/screenshot.png)

## Download APK

You can download the current APK here:

[Download kultliederbuch_v2.0.apk](download/kultliederbuch_v2.0.apk)

## Tech stack

- Kotlin
- Jetpack Compose
- Kotlin Multiplatform shared module
- SQLDelight
- AndroidX

## Development

Open the project in Android Studio and run the `app-android` module on an Android emulator or device.

## License

Unlicense
