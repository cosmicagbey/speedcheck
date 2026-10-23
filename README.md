# SpeedCheck 🇬🇭

**SpeedCheck** is an intelligent, road-safety Android application built with Kotlin and Jetpack Compose. Designed specifically for drivers in Ghana, it helps motorists stay aware of their vehicle speed and avoid speeding citations from the Ghana Police MTTD (Motor Traffic and Transport Department) automated speed camera system on major highways and urban corridors (such as the George Walker Bush Highway / N1, Accra-Tema Motorway, and Pokuase corridor).

---

## 🚀 Key Features

### 1. Speed Limit Setting & Ghana Presets
- **Quick Switch Presets**: One-tap speed limit selection matching Ghana Road Safety standards:
  - **30 km/h**: School zones, busy market corridors, hospital areas, and residential feeder roads.
  - **50 km/h**: Standard urban roads and town centers (Accra, Kumasi, Takoradi).
  - **80 km/h**: Inter-urban highways and single-lane regional roads.
  - **100 km/h**: National dual-carriageway highways (e.g. N1 George Walker Bush Highway).
  - **120 km/h**: Expressways and motorways (Accra-Tema Motorway).
- **Custom Limit Input**: Type any custom speed limit (e.g. 40, 60, 70, 90 km/h).
- **Persistent Preferences**: All selections persist across app restarts and phone reboots using Jetpack DataStore.

### 2. Proactive Voice Alerts (Text-to-Speech)
- **Early Approaching Alert**: Notifies drivers *before* reaching their target speed limit:
  - *Example*: When the limit is set to **30 km/h**, SpeedCheck triggers an alert when reaching **25 km/h**:
    > *"You are approaching 30 kilometers per hour."*
  - Approaching buffer is customizable (default 5 km/h before target).
- **Over-Limit Warning**: Triggers immediately if speed exceeds the limit:
  > *"Slow down. Current speed is above your limit of 80 kilometers per hour."*
- **Voice Customization**: Choose between Female, Male, or System voice styles with speed rate and pitch controls.
- **Anti-Spam Cooldown**: Alerts repeat at configurable intervals (10s, 15s, 20s, 30s) while over the limit to avoid spamming the driver.
- **Loud & Clear Audio**: Uses Android `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` to ensure alerts are loud and clearly heard over road and car noise.

### 3. Background Speed Monitoring
- **Foreground Service with GPS Tracking**: Continuous, high-accuracy speed tracking using Google Play Services `FusedLocationProviderClient`.
- **Works with Screen Off**: Protects the driver silently in the pocket or dashboard mount even when the screen is locked or another navigation app (Google Maps, Waze) is active.
- **Smart Vehicle Movement Detection**:
  - Automatically detects when the driver is in a moving vehicle (speed consistently > ~18 km/h).
  - Automatically transitions to high-frequency active tracking.
  - Pauses to low-power mode when stationary for extended periods to conserve battery.
- **Auto-Resume on Boot**: Automatically restores background monitoring after phone restart if configured.
- **Persistent Minimal Notification**: Compliant with Android foreground service guidelines with quick "Stop Monitoring" button.

### 4. High-Contrast Driving Dashboard
- Large digital speedometer readout designed for glanceability while driving.
- Custom Canvas arc gauge with color states:
  - 🟢 **Safe** (Emerald Green)
  - 🟡 **Approaching Limit** (Amber Yellow)
  - 🔴 **Over Speed Limit** (Pulsing Red)
- Real-time Trip Statistics: Max speed, average speed, and total trip distance.
- One-touch voice mute/unmute toggle on the top bar.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.3+
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15/16 (API 36)
- **UI Toolkit**: Jetpack Compose with Material 3 & Navigation3
- **Location Engine**: Google Play Services Location (`FusedLocationProviderClient`)
- **Speech Engine**: Android Text-to-Speech (`TextToSpeech`)
- **Persistence**: Jetpack DataStore Preferences
- **Concurrency**: Kotlin Coroutines & StateFlow
- **CI/CD**: GitHub Actions workflow for automatic APK building

---

## ☁️ Building the APK on GitHub (Automated CI/CD)

A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. Every time you push code to GitHub, GitHub builds the APK automatically in the cloud.

### Step 1: Push Project to GitHub

1. Initialize git (if not already done) and stage all files:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: SpeedCheck complete Android app"
   ```

2. Create a repository on [GitHub](https://github.com/new) named `SpeedCheck`.

3. Push your code:
   ```bash
   git remote add origin https://github.com/<your-username>/SpeedCheck.git
   git branch -M main
   git push -u origin main
   ```

### Step 2: Download the Built APK from GitHub

1. Go to your repository on GitHub.
2. Click the **Actions** tab at the top.
3. Select the latest **"Build Android APK"** workflow run.
4. Under **Artifacts** at the bottom of the page, download **`SpeedCheck-debug-apk`**.
5. Unzip the file and install `app-debug.apk` directly onto your Android device!

---

## 💻 Building Locally (Optional)

If you have Android Studio installed locally:

1. Open Android Studio and select **Open** -> Select `SpeedCheck` folder.
2. Let Gradle sync dependencies.
3. To build the APK from terminal:
   ```bash
   ./gradlew assembleDebug
   ```
4. The output APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

To run unit tests:
```bash
./gradlew test
```

---

## 📱 Permissions Used

| Permission | Purpose |
| :--- | :--- |
| `ACCESS_FINE_LOCATION` | Required for accurate GPS vehicle speed calculation. |
| `ACCESS_COARSE_LOCATION` | Fallback network location. |
| `ACCESS_BACKGROUND_LOCATION` | Allows tracking speed and giving voice warnings while screen is off. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION` | Keeps the speed monitoring service alive in the background. |
| `POST_NOTIFICATIONS` | Displays the ongoing speed status notification and heads-up warnings. |
| `RECEIVE_BOOT_COMPLETED` | Automatically resumes speed monitoring when device reboots (if enabled). |
| `WAKE_LOCK` | Prevents the CPU from sleeping during active driving sessions. |

---

## 🇬🇭 Ghana Road Safety & MTTD Speed Enforcement Tips

1. **George Walker Bush Highway (N1)**: 90–100 km/h speed cameras installed near footbridges and major interchanges (Dzorwulu, Abeka Lapaz, Mallam).
2. **Accra-Tema Motorway**: 100–120 km/h limit; cameras active for overspeeding enforcement.
3. **Pokuase Interchange - Amasaman Corridor**: 80–90 km/h cameras.
4. **School & Hospital Zones**: Strictly 30 km/h.
