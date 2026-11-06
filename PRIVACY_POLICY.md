# Privacy Policy for SpeedCheck

**Last Updated:** November 6, 2026
**Effective Date:** November 6, 2026

Thank you for choosing **SpeedCheck** ("we", "our", or "the App"). SpeedCheck is a road safety and speed awareness application designed to assist drivers in Ghana with real-time speed monitoring and automated speed camera zone alerts.

This policy describes the Android app and the SpeedCheck web app. SpeedCheck does not upload GPS location data to its own servers, sell user data, or use advertising identifiers. The apps do store some location information locally as described below.

---

## 1. Summary of Core Privacy Principles

* 🔒 **On-Device Location Processing**: GPS location and speed calculations run on your device.
* 🚫 **No SpeedCheck Location Uploads**: SpeedCheck does not send GPS coordinates or driving history to SpeedCheck servers.
* 🚫 **No Personal Information Required**: SpeedCheck does not require an account, sign-in, name, phone number, email address, or vehicle details.
* 🚫 **No Data Selling**: SpeedCheck does not sell personal or location data.

---

## 2. Information Accessed and Permissions Requested

To provide its core safety features, SpeedCheck requires specific Android system permissions:

### A. Location Permissions (Fine, Coarse & Background Location)
* **Permissions**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
* **Purpose**:
  * To calculate your instantaneous vehicle speed in kilometers per hour (km/h) via GPS.
  * To detect whether you are traveling toward designated speed camera enforcement corridors (e.g., N1 Highway, Borteyman Sports Complex, Kawukudi, Kanda Highway).
  * To provide audio alerts before and when exceeding speed limits even when the screen is locked or while using other navigation software.
* **Storage & Transmission**: Live coordinates are processed on-device. Warning and violation events, including GPS coordinates, are saved in the Android app's local history database (up to 500 records). The history remains until you clear it or uninstall the app. It is not uploaded to SpeedCheck servers. You may explicitly export it using the system share sheet; information shared with another app is then subject to that app's privacy practices.

### B. Foreground Service (`FOREGROUND_SERVICE_LOCATION`)
* **Permission**: `FOREGROUND_SERVICE_LOCATION`
* **Purpose**: In compliance with Android operating system standards, SpeedCheck operates a foreground service with a persistent status notification to prevent the system from terminating speed monitoring while you are driving.

### C. Notifications
* **Permission**: `POST_NOTIFICATIONS`
* **Purpose**: Displays the active speed status and issues heads-up warnings if your vehicle exceeds the designated speed limit.

### D. Device Boot & Startup
* **Permission**: `RECEIVE_BOOT_COMPLETED`
* **Purpose**: Allows the background monitoring service to automatically resume after your phone restarts, only if you have explicitly enabled the "Resume on Phone Restart" setting.

---

## 3. Local Data Storage

SpeedCheck uses Android Jetpack DataStore to save your preferences locally and an on-device database for warning and violation history:
* User-selected speed limit presets (e.g., 30, 50, 80, 100, 120 km/h)
* Audio preferences (voice gender, speech pitch, speech rate, alert repeat cooldown)
* Feature toggles (Auto-Detect mode, voice alert on/off)
* Warning and violation events, including their time, speed, limit, and GPS coordinates (up to 500 records)

The Android app excludes its database and preferences from Android cloud backup and device transfer. Clearing app storage or uninstalling the app deletes this local data.

### Web app storage

The web app stores preferences and up to 100 warning/violation history records, including GPS coordinates, in browser `localStorage`. This information remains in that browser profile until you clear the history, clear the site's storage, or remove the browser profile. CSV export is user-initiated and shared files are subject to the receiving app's privacy practices. Browser storage is not uploaded to SpeedCheck servers.

---

## 4. Third-Party Services & Analytics

SpeedCheck does not embed advertising SDKs (such as AdMob), tracking cookies, behavioral analytics (such as Google Analytics or Firebase Analytics), or user profiling tools. 

The Android app utilizes Google Play Services for location resolution (`FusedLocationProviderClient`) and the Android system Text-to-Speech engine. The web app uses browser location, local storage, and speech APIs. These platform services may process data under their own terms; SpeedCheck does not send location data to them as a backend analytics or tracking service.

---

## 5. Privacy and Legal Notice

SpeedCheck aims to follow applicable privacy requirements and minimize data use. This section is not a certification or legal determination of compliance.

---

## 6. Children's Privacy

SpeedCheck is designed for vehicle operators and does not knowingly address or collect information from children under the age of 13.

---

## 7. Changes to This Privacy Policy

We may update this Privacy Policy periodically to reflect app updates or regulatory modifications. Any revisions will be published directly within the application and in the GitHub repository. Continued use of SpeedCheck after changes are posted constitutes acceptance of the revised policy.

---

## 8. Contact Information

If you have questions, feedback, or concerns regarding this Privacy Policy or SpeedCheck's privacy practices, please contact us:

* **Repository**: [https://github.com/cosmicagbey/speedcheck](https://github.com/cosmicagbey/speedcheck)
* **Issues / Inquiries**: kentbanks96@gmail.com
