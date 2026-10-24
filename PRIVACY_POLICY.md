# Privacy Policy for SpeedCheck

**Last Updated:** October 24, 2026  
**Effective Date:** October 24, 2026  

Thank you for choosing **SpeedCheck** ("we", "our", or "the App"). SpeedCheck is a road safety and speed awareness application designed to assist drivers in Ghana with real-time speed monitoring and automated speed camera zone alerts.

Your privacy is paramount. This Privacy Policy outlines what information the App accesses, how that information is used, and confirms that **SpeedCheck does not collect, store remotely, share, or sell any personal or location data.**

---

## 1. Summary of Core Privacy Principles

* 🔒 **100% On-Device Processing**: All GPS location and speed calculations occur strictly on your physical device in real time.
* 🚫 **No Remote Data Collection**: We do not own, maintain, or transmit data to remote servers or third-party databases.
* 🚫 **No Personal Information Required**: SpeedCheck does not require an account, sign-in, name, phone number, email address, or vehicle details.
* 🚫 **No Data Sharing or Selling**: None of your data is ever sold, leased, or shared with advertisers, government agencies, or commercial entities.

---

## 2. Information Accessed and Permissions Requested

To provide its core safety features, SpeedCheck requires specific Android system permissions:

### A. Location Permissions (Fine, Coarse & Background Location)
* **Permissions**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
* **Purpose**:
  * To calculate your instantaneous vehicle speed in kilometers per hour (km/h) via GPS.
  * To detect whether you are traveling toward designated speed camera enforcement corridors (e.g., N1 Highway, Borteyman Sports Complex, Kawukudi, Kanda Highway).
  * To provide audio alerts before and when exceeding speed limits even when the screen is locked or while using other navigation software.
* **Storage & Transmission**: Location data is processed ephemerally in RAM while the monitoring service runs. **No location coordinates, route histories, or GPS logs are ever recorded to permanent storage or sent over the Internet.**

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

SpeedCheck uses Android Jetpack DataStore to save your user preferences locally on your device:
* User-selected speed limit presets (e.g., 30, 50, 80, 100, 120 km/h)
* Audio preferences (voice gender, speech pitch, speech rate, alert repeat cooldown)
* Feature toggles (Auto-Detect mode, voice alert on/off)
* Anonymous session trip statistics (maximum speed, average speed, and session distance)

All saved preferences remain solely on your device. Clearing the app storage or uninstalling the app permanently deletes this data.

---

## 4. Third-Party Services & Analytics

SpeedCheck does not embed advertising SDKs (such as AdMob), tracking cookies, behavioral analytics (such as Google Analytics or Firebase Analytics), or user profiling tools. 

The App utilizes standard Google Play Services libraries exclusively for on-device GPS location resolution (`FusedLocationProviderClient`) and the Android system Text-to-Speech engine (`TextToSpeech`).

---

## 5. Compliance with Privacy Regulations

SpeedCheck is developed in accordance with international and domestic privacy standards:
* **Google Play Developer Program Policies**: Meets strict User Data and Background Location requirements.
* **Ghana Data Protection Act, 2012 (Act 843)**: Strictly adheres to data minimization and user autonomy principles.
* **General Data Protection Regulation (GDPR)**: No personal identification data is stored or processed.

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
* **Issues / Inquiries**: [https://github.com/cosmicagbey/speedcheck/issues](https://github.com/cosmicagbey/speedcheck/issues)
