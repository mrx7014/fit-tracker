# Fit Tracker 🏋️

[![Release](https://img.shields.io/github/v/release/mrx7014/fit-tracker?display_name=tag&style=flat-square&label=release)](https://github.com/mrx7014/fit-tracker/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/mrx7014/fit-tracker/android.yml?style=flat-square&label=build)](https://github.com/mrx7014/fit-tracker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)

**Fit Tracker** is a modern Android app for logging workouts, tracking progress, and building a consistent fitness habit. It is built with Kotlin, Jetpack Compose, and Material 3, with Arabic and English support and a privacy-first local-storage approach.

[Download the latest release](https://github.com/mrx7014/fit-tracker/releases/latest) · [Changelog](CHANGELOG.md) · [النسخة العربية](README.md)

## ✨ Features

- Log exercises with date, weight, and reps.
- Create a weekly workout plan with exercises assigned to each day.
- View a home dashboard with key metrics, workout streak, and history.
- Review weekly and monthly statistics for sessions, reps, active days, and average weight.
- Explore a weight-versus-reps chart with exercise labels.
- Unlock achievements for consistency and milestones.
- Configure a daily workout reminder.
- Choose system, light, or dark theme with optional dynamic colors.
- Switch between kilograms and pounds.
- Use the app in Arabic or English, including RTL layout support for Arabic.
- Enter Arabic-Indic, Persian-Indic, and Arabic decimal digits.
- Export and restore workout data as JSON backups.
- Use the app without an account or backend; workout data is stored locally.

## 📱 App Screens

| Screen | Purpose |
| --- | --- |
| Home | Performance summary, workout history, and quick workout entry |
| Plan | Weekly workout scheduling |
| Statistics | Weekly/monthly analytics and weight-versus-reps chart |
| Achievements | Milestones and workout streak |
| Settings | Profile, appearance, language, reminders, and backups |
| About | App, developer, and release information |

> Screenshots are not currently included in the repository. Add images under `docs/images/` and link them here when available.

## 🧰 Tech Stack

- **Kotlin**
- **Android Jetpack**
- **Jetpack Compose**
- **Material 3**
- **Compose Material Icons**
- **ViewModel**
- **SharedPreferences** for current local storage
- **Gradle Kotlin DSL**
- **GitHub Actions** for builds and releases

## 📋 Requirements

- Android Studio Ladybug or newer.
- JDK 21.
- Android SDK API 35.
- Android 8.0 (API 26) or newer for running the app.
- Internet access for the first Gradle dependency sync.

## 🚀 Getting Started

```bash
git clone https://github.com/mrx7014/fit-tracker.git
cd fit-tracker
```

Open the project in Android Studio and wait for Gradle sync to complete. Build a debug APK from the terminal:

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
./gradlew.bat assembleDebug
```

The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also run the app directly from Android Studio on an emulator or connected Android device.

## 📦 Downloads

Download the latest APK from [GitHub Releases](https://github.com/mrx7014/fit-tracker/releases/latest).

The current version is **3.0.0** and the application ID is:

```text
com.fittracker.app
```

## 🤖 GitHub Actions

The project includes a manual workflow at `.github/workflows/android.yml`:

1. Open the repository's **Actions** tab.
2. Select **Build Fit Tracker app**.
3. Click **Run workflow**.
4. Choose whether to publish a release.
5. When publishing, provide a version such as `3.0.0` or `3.0.0-beta1`.

The workflow builds the APK and uploads it as an artifact. When release publishing is enabled, it also creates a GitHub Release and attaches the APK.

## 🔐 Data and Privacy

Profile, workout, and app settings are stored locally on the device. The app does not require an account or backend. Use the backup feature to export workout data to a JSON file and restore it later.

> Keep backup files safe. Uninstalling the app or clearing its data may remove locally stored data.

## 🗂️ Project Structure

```text
fit-tracker/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fittracker/app/MainActivity.kt
│       └── res/
├── .github/workflows/android.yml
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── CHANGELOG.md
├── README.md
└── README.en.md
```

## 🤝 Contributing

Contributions are welcome:

1. Fork the repository.
2. Create a branch for your feature or fix.
3. Make your changes and write a clear commit message.
4. Open a pull request with a short description of the changes.

## 📄 License

This project does not currently include a license file. Contact the repository owner before using or redistributing the code in a commercial or production project.

## 👤 Maintainer

- **MRX7014**
- [GitHub profile](https://github.com/mrx7014)
- [Fit Tracker repository](https://github.com/mrx7014/fit-tracker)
- [Releases](https://github.com/mrx7014/fit-tracker/releases)

## 📚 More Information

See [CHANGELOG.md](CHANGELOG.md) for the current release details and previous changes.

---

Made with ❤️ to help athletes log, stay consistent, and improve.
