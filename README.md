# 🚛 Driver Shift Tracker

**An Android app for tracking work shifts of long-haul drivers** with EU Regulation 561/2006 compliance monitoring.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)](https://apilevels.com)
[![License](https://img.shields.io/badge/License-Private-lightgrey)]()

---

## 📋 About

Driver Shift Tracker helps long-haul drivers maintain a daily work shift log, monitor driving and rest times, and generate reports for dispatchers. The app automatically checks compliance with the key rules of EU Regulation 561/2006 and warns about potential violations.

## ✨ Features

### 📊 Dashboard
- Next shift start time calculated with **regular** (11 h) and **reduced** (9 h) rest — displayed in different colors
- Weekly summary: total work hours, driving hours, expenses
- Visual limit indicators (red / green)

### 📝 Shift Log
- Add, edit, and delete shift records
- **Search by date** and **filter by date range**
- Auto-save draft when the form is closed
- Auto-fill based on the previous shift

### 📈 Reports
- Generate text reports for any custom period
- Copy report to clipboard to send to a dispatcher
- Week-by-week breakdown with hour and expense totals

### ⚙️ Settings
- **Profile**: change driver name
- **Language**: Russian 🇷🇺 / English 🇬🇧
- **Work mode**:
  - _Extended_ — enter both regular shift and tachograph times
  - _Simplified_ — tachograph times only
- **Regulations**: EU Regulation 561/2006 reference guide
- **About**: version and build number

### ✅ EU Regulation Compliance Checks
| Rule | Limit |
|------|-------|
| Maximum tachograph shift | ≤ 13 hours (max 3 times/week) |
| Maximum daily driving | ≤ 9 hours (extension to 10 h max 2 times/week) |
| Reduced daily rest | < 11 hours (max 3 times/week) |

---

## 🏗️ Architecture

```
app/src/main/java/com/example/
├── MainActivity.kt              # Single Activity, entire UI in Jetpack Compose
├── data/
│   ├── Database.kt              # Room DB: DriverEntity, ShiftEntity, DAOs
│   └── Repository.kt            # Data repository
├── domain/
│   └── ComplianceCalculator.kt  # Business logic for regulation checks
└── ui/
    ├── MainViewModel.kt         # MVVM ViewModel, state management
    └── Localization.kt          # Translation dictionary (RU/EN)
```

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Database | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Networking | Retrofit + OkHttp + Moshi |
| Build | Gradle (Kotlin DSL) |
| Testing | JUnit + Robolectric + Roborazzi |

---

## 🚀 Build & Run

### Requirements
- **Android Studio** Ladybug (2024.2) or newer
- **JDK 11+** (bundled with Android Studio)
- **Android SDK** with compileSdk 36

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/KindFest/driver-shift-tracker.git
   cd driver-shift-tracker
   ```

2. **Open in Android Studio**
   - File → Open → select the project folder
   - Wait for Gradle sync to complete

3. **Run**
   - Select a device or emulator
   - Press ▶️ Run

### Build APK from the Command Line

```bash
# Debug APK
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Environment Variables (for release builds)

```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

---

## 📱 Compatibility

- **Minimum Android version**: 7.0 (API 24)
- **Target Android version**: 14 (API 36)
- **Supported languages**: Russian, English

---

## 🗺️ Roadmap

- [ ] Weekly driving limit of 56 hours
- [ ] Bi-weekly driving limit of 90 hours (rolling)
- [ ] 45-minute break after 4.5 hours of continuous driving
- [ ] Weekly rest ≥ 45 hours (reduced ≥ 24 h)
- [ ] Maximum of 6 daily driving periods between weekly rests
- [ ] Export reports to PDF
- [ ] Notifications when approaching limits
- [ ] Cloud backup

---

## 📄 License

This project is private. All rights reserved.

---

<p align="center">
  <sub>Made with ❤️ for long-haul drivers</sub>
</p>
