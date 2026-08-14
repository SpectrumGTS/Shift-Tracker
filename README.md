# Shift Tracker

**You deserve a break.**

Working for longer hours doesn’t mean better performance. Even a short break can reduces stress and improve productivity in the long run. Don't wait for a vacation or a career change to start living in balance.
This app was built to help users visualize their working time and maintain a healthy balance between work and life. 
An elegant, modern, offline-first Android application designed to track work shifts, log hours, calculate buffer-adjusted overtime, and monitor working time trends. Built entirely using **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **Jetpack Room** database.

---

## 🎨 Visual Preview & Design Philosophy
Shift Tracker leverages **Material Design 3 (M3)** guidelines to provide a modern, colorful, and accessible user interface:
*   **Warm Neutral Palette**: Visually soothing dark and light schemes designed for readability at any hour.
*   **Fluid & Adaptive**: Seamlessly scales across multiple device formats from handheld phones to foldables.
*   **Accessibility First**: Features generous spacing, robust color contrasts, and fully tactile minimum touch targets of 48dp.

---

## 🚀 Key Features

*   **📝 Quick Shift Logging**: Log your working hours, clock-in/out times, and notes in seconds.
*   **⏰ Automatic Shift Calculations**: Real-time calculation of early and late overtime minutes, scheduled vs. actual worked hours, and optional lunch break deductions.
*   **⚡ Live Active Shift Tracker**: Dashboard banner featuring a real-time "shift ends in" countdown that dynamically updates every second.
*   **📅 Customizable Default Schedules**: Define standard working hours for each day of the week with 1-tap schedule presets.
*   **🔍 Rich History & Advanced Filtering**: Search through past shift notes and filter history by custom date ranges using Material 3 date pickers.
*   **📊 Yearly Insights**: Visualize monthly total working time trends across any selected year with breakdown cards and peak month highlights.
*   **🧘 Mental Wellbeing Support**: Dedicated health resources, shift-work fatigue prevention guides, sleep hygiene advice, and support contacts.
*   **💾 Backup & Restore**: Export complete shift logs and settings to local JSON files or restore them via the app or dedicated import activity.
*   **⚙️ Flexible Settings**: Customize pre/post shift grace buffers, night-shift day cutoff times, early clock-in rules, and lunch break deduction toggles.

---

## 🛠️ Technology Stack & Architecture

This project is built using modern Android development practices:

### 📱 Frontend & UI
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Declarative UI)
*   **Design System**: Material Design 3 (M3)
*   **Navigation**: Jetpack Navigation Compose
*   **Image Loading**: Coil Compose for image rendering

### 🗄️ Backend & Persistence
*   **Local Database**: Jetpack Room with Kotlin Symbol Processing (KSP) and SQLite storage
*   **Concurrency**: Kotlin Coroutines and asynchronous Flow API for non-blocking reactive queries
*   **Architecture**: Clean MVVM (Model-View-ViewModel) architecture ensuring complete separation of concerns

```
┌────────────────────────────────────────────────────────┐
│                        VIEW                            │
│    (Jetpack Compose Screens / AddEditShiftDialog)      │
└───────────────────────────┬────────────────────────────┘
                            │ Collects StateFlow / Triggers Events
                            ▼
┌────────────────────────────────────────────────────────┐
│                     VIEWMODEL                          │
│                (OvertimeViewModel)                     │
└───────────────────────────┬────────────────────────────┘
                            │ Requests Data / Saves Changes
                            ▼
┌────────────────────────────────────────────────────────┐
│                    REPOSITORY                          │
│                (OvertimeRepository)                    │
└───────────────────────────┬────────────────────────────┘
                            │ Room DB Access / DAO Calls
                            ▼
┌────────────────────────────────────────────────────────┐
│                    LOCAL SOURCE                        │
│             (Room Database / SQLite)                   │
└───────────────────────────┬────────────────────────────┘
```

---

## 📂 Project Structure

```
app/src/main/java/dev/spectrumgts/shifttracker/
├── data/
│   ├── dao/         # Room Data Access Objects (ShiftLogDao, AppSettingsDao, etc.)
│   ├── db/          # Room Database implementation (AppDatabase)
│   ├── model/       # Data Models & calculations (ShiftLog, AppSettings, OvertimeCalculation)
│   └── repository/  # Repository pattern implementation (OvertimeRepository)
├── ui/
│   ├── components/  # Shared Compose components (ShiftCard, M3TimePickerDialog)
│   ├── navigation/  # Navigation structures & drawer layout (NavigationDrawerContent)
│   ├── screens/     # Application screens (Dashboard, ShiftHistory, Insights, Settings, Wellbeing, etc.)
│   ├── theme/       # Application styling (Color, Type, Theme)
│   ├── viewmodel/   # Core State Management (OvertimeViewModel)
│   └── BackupImportActivity.kt  # Standalone backup import activity
└── ShiftTrackerMainActivity.kt  # Main entry point of the application
```

---

## ⚙️ Building and Running

1. Clone this repository or import it into your workspace.
2. Open the project in **Android Studio** (Koala or later recommended).
3. Ensure you have **JDK 17+** configured in your Gradle settings.
4. Sync Gradle dependencies.
5. Press **Run** to install and run the app on an Android emulator or physical device.

### 🧪 Running Unit Tests
Execute the local JVM unit and Robolectric tests via Gradle:
```bash
gradle :app:testDebugUnitTest
```
