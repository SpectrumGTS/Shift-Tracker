# Shift Tracker & Overtime Calculator

An elegant, modern, offline-first Android application designed to track work shifts, log hours, and calculate regular and overtime earnings automatically. Built entirely using **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **Jetpack Room** database.

---

## 🎨 Visual Preview & Design Philosophy
Shift Tracker leverages **Material Design 3 (M3)** guidelines to provide a modern, colorful, and accessible user interface.
*   **Warm Neutral Palette**: Visually soothing dark and light schemes designed for readability at any hour.
*   **Fluid & Adaptive**: Seamlessly scales across multiple device formats from handheld phones to foldables.
*   **Accessibility First**: Features generous spacing, robust color contrasts, and fully tactile minimum touch targets of 48.dp.

---

## 🚀 Key Features

*   **📝 Quick Shift Logging**: Log your working hours, regular shifts, and notes in seconds.
*   **⏰ Automatic Calculations**: Real-time calculation of overtime hours, regular earnings, and total payouts based on customizable hourly rates and multipliers.
*   **📅 Customizable Default Schedules**: Define your standard working hours for each day of the week to simplify logging of recurring shifts.
*   **🔍 Rich History & Advanced Filtering**: Browse past shifts easily with an integrated search bar and a date-range filter using dynamic date-pickers.
*   **📊 Interactive Insights**: A dedicated statistics screen visualizing regular vs. overtime hours and aggregate earnings to track trends over time.
*   **💾 Backup & Restore**: Securely export shift logs and settings to local storage or restore them at any time to keep your data safe.
*   **⚙️ Custom Settings**: Configure your custom Hourly Rate, Overtime Multipliers, and Shift Cutoff Times to match any workplace policy.

---

## 🛠️ Technology Stack & Architecture

This project is built using modern Android development practices:

### 📱 Frontend & UI
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Declarative UI)
*   **Design System**: Material Design 3 (M3)
*   **Navigation**: Jetpack Navigation Compose with type-safe route keys
*   **Image Loading**: Coil Compose for local and asynchronous image rendering

### 🗄️ Backend & Persistence
*   **Local Database**: Jetpack Room with Kotlin Symbol Processing (KSP) and SQLite storage
*   **Concurrency**: Kotlin Coroutines and asynchronous Flow API for non-blocking reactive database queries
*   **Architecture**: Clean MVVM (Model-View-ViewModel) architecture ensuring complete separation of UI concerns and business logic

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
└────────────────────────────────────────────────────────┘
```

---

## 📂 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── dao/         # Data Access Objects (ShiftLogDao, AppSettingsDao, etc.)
│   ├── db/          # Room Database implementation (AppDatabase)
│   ├── model/       # Data Models & calculations (ShiftLog, AppSettings, OvertimeCalculation)
│   └── repository/  # Repository pattern implementation (OvertimeRepository)
├── ui/
│   ├── components/  # Shared Compose components (ShiftCard, M3TimePickerDialog)
│   ├── navigation/  # Navigation structures & sidebar layout (NavigationDrawerContent)
│   ├── screens/     # Application screens (Dashboard, ShiftHistory, Insights, Settings)
│   ├── theme/       # Application styling (Color, Type, Theme)
│   └── viewmodel/   # Core State Management (OvertimeViewModel)
└── MainActivity.kt  # Main entry point of the application
```

---

## ⚙️ Building and Running

1.  Clone this repository or import it into your workspace.
2.  Open the project in **Android Studio** (Koala or later recommended).
3.  Ensure you have **JDK 17+** configured in your Gradle settings.
4.  Sync Gradle dependencies.
5.  Press **Run** to install and run the app on your Android emulator or physical device.

### 🧪 Running Unit Tests
Execute the local JVM unit and Robolectric tests via Gradle:
```bash
gradle :app:testDebugUnitTest
```
