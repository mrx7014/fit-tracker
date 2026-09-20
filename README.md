# Fit Tracker

Fit Tracker is a modern Android fitness tracking app built with Kotlin and Jetpack Compose. It helps you log workouts, plan weekly routines, monitor training progress, and stay consistent with reminders, achievements, and local analytics.

This project is designed for personal use and emphasizes a clean, lightweight experience with realistic workout tracking, Arabic/English localization, and privacy-first local storage.

## Features

- Workout logging by exercise, date, weight, and reps
- Weekly workout plan with per-day exercise scheduling
- Home overview with key metrics like streak, volume, and body stats
- Weekly and monthly statistics with activity trends
- Weight-vs-reps chart for progress tracking
- Achievement system for milestones and consistency
- Reminder support for daily workouts
- Dark mode, dynamic colors, theme choices, and language options
- Arabic and English interface support
- JSON backup and restore of workout data
- Local-only storage with no backend required

## Tech Stack

- Kotlin
- Android Jetpack
- Jetpack Compose
- Material 3
- DataStore Preferences
- Gradle Kotlin DSL

## Screenshots

This repository does not currently include app screenshots, but the app includes:

- Home dashboard
- Workout plan
- Statistics
- Achievements
- Settings
- About section
- First-run onboarding and profile setup

## Project Structure

```text
fit-tracker/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/fittracker/app/
│           └── res/
├── gradle/
├── .github/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── CHANGELOG.md
└── README.md
```

## Requirements

- Android Studio Ladybug or newer
- JDK 21
- Android SDK with API 35 support
- Gradle wrapper included in the repo

## Getting Started

1. Clone the repository:

```bash
git clone https://github.com/mrx7014/fit-tracker.git
cd fit-tracker
```

2. Open the project in Android Studio.

3. Let Gradle sync the project.

4. Build and run the app:

```bash
./gradlew assembleDebug
```

Or run directly from Android Studio on an emulator or connected device.

## App Identity

- Application ID: `com.fittracker.app`
- Namespace: `com.fittracker.app`
- Minimum SDK: 26
- Target SDK: 35
- Version: `3.0.0`

## Local Data Behavior

Fit Tracker stores workout data locally on the device using SharedPreferences and DataStore. This keeps the app lightweight and private, while still supporting backup and restore through export/import of JSON files.

## Contributing

Contributions are welcome. If you want to improve the app, add features, or fix issues:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a pull request

## License

This project does not currently include a license file. Please check the repository settings or contact the maintainer before using it in production or redistributing it.

## Maintainer

- MRX7014
- GitHub: https://github.com/mrx7014
- Repository: https://github.com/mrx7014/fit-tracker

## Release Notes

The project has a changelog in `CHANGELOG.md` with detailed highlights for version `3.0.0` and earlier updates.
