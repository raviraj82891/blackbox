# Human Digital Black Box — Privacy-Preserving Incident Reconstruction

> **BTech Computer Science Final Year Project**  
> *Production-Quality Android Architecture, Hilt DI, & AWS Serverless Security Infrastructure*

---

## 1. One-Line Pitch
An Android application that maintains a rolling, encrypted, on-device buffer of the user's last 60 minutes of sensor activity. On detecting a likely emergency (crash, fall, or manual SOS), it freezes the buffer, reconstructs a tamper-evident, human-readable timeline of what happened, and securely alerts the user's emergency contacts — like an aviation black box, but for a person.

---

## 2. Research Framing & Architectural Enhancements

### Key Computer Science Concepts & Refinements
1. **Hilt Dependency Injection**: Clean separation of concerns with modular Hilt bindings (`DatabaseModule`, `AppModule`) and focused `@HiltViewModel`s (`HomeViewModel`, `TimelineViewModel`, `IncidentsViewModel`, `ContactsViewModel`, `SettingsViewModel`).
2. **Room Persisted Emergency Contacts & Schema Migration (v1 -> v2)**: Emergency contacts are stored in SQLCipher Room database with a formal Room `Migration(1, 2)`. Requires at least 1 saved contact for "FULL PROTECTION" status.
3. **Honest Co-Occurrence Audio Classification**: Acoustic amplitude (>85 dB) is logged as `LOUD_ACOUSTIC_NOISE` unless co-occurring within ±2000 ms with an accelerometer/gyro kinetic spike, in which case it elevates to `LOUD_IMPACT`.
4. **Immediate 3-Second Path for Manual SOS**: Deliberate manual SOS triggers bypass the 30-second false-positive filter and run a quick 3-second undo window for immediate activation.
5. **WorkManager Periodic Hash-Chain Verification**: Moved off ViewModel lifecycle into `HashChainVerificationWorker`.
6. **Real-time Accelerometer Sparkline Chart**: Live Compose `Canvas` sparkline displaying 60 points of motion magnitude.
7. **Session-Based Timeline Grouping**: Timeline entries are grouped into collapsible sessions bounded by Activity Recognition transitions (*Walking*, *In Vehicle*, *Stationary*) displaying start/end times, max speed, and peak severity level.
8. **Personal Calibration & Adaptive Sensitivity**: 10-second personal baseline motion calibration + automatic adaptive sensitivity prompt when countdowns are cancelled repeatedly.
9. **Glance Home-Screen Widget**: Android Jetpack Glance widget providing live status and direct one-tap emergency SOS activation.
10. **Battery-Aware Power Saver Mode**: Automatically pauses audio classification and relaxes location updates when battery drops below 15% to preserve emergency power.

---

## 3. Technology Stack

- **Architecture**: MVVM + Hilt DI + Jetpack Compose (Material 3)
- **Database**: Room (Migration 1->2) + SQLCipher + Android Keystore MasterKey
- **Sensors & Collectors**: `SensorManager`, `FusedLocationProviderClient`, `ActivityRecognitionClient`, `AudioRecord` (no raw audio stored)
- **Widgets & Backgrounding**: Jetpack Glance Home Widget, WorkManager (`HiltWorker`), Android 14+ Foreground Service
- **Networking**: Retrofit + OkHttp Certificate Pinning
- **Cloud Backend Architecture (AWS Serverless Intent)**: API Gateway -> Lambda -> S3 (SSE-KMS) -> DynamoDB -> SNS/SES

---

## 4. How to Demo for Viva Evaluation

### Step 1: Onboarding & Room Emergency Contacts
1. Launch the app and complete onboarding.
2. Go to the **Contacts** tab and add an emergency contact (*persisted in Room DB v2*).
3. Tap **Send Test Alert Preview** to fire a local notification previewing the alert that contact would receive.
4. Verify the **Status** screen now displays **FULL BLACK BOX PROTECTION ACTIVE**.

### Step 2: Live Motion Sparkline & Situational Status
1. Observe the live Compose `Canvas` **Sparkline Chart** moving with phone motion.
2. Note the dynamic **Situational Status** string (e.g. *"In Vehicle — Actively Monitoring High-Speed Motion"*).

### Step 3: Session-Based Timeline
1. Switch to the **Timeline** tab.
2. Examine the **Collapsible Sessions** grouped by Activity Recognition transitions (*Walking Session*, *In Vehicle Session*).
3. Expand a session to view individual event entries, max speed, and SHA-256 entry hashes.

### Step 4: Personal Motion Calibration & Adaptive Thresholds
1. Navigate to **Settings** and tap **Start 10s Personal Calibration**.
2. Carry the phone normally for 10 seconds to let the system measure your baseline motion and derive a personalized impact threshold.
3. On frequent false-positive cancellations, note the adaptive prompt offering to raise the sensitivity threshold.

### Step 5: Immediate Manual SOS & Home Widget
1. Tap **MANUAL EMERGENCY SOS** on the Home screen or use the **Glance Home-Screen Widget**.
2. Verify it triggers the fast 3-second emergency countdown for immediate activation.

### Step 6: Viva "Simulate Incident" Mode & PDF Export
1. Go to **Debug** tab and tap **Inject Simulated Crash Sequence**.
2. Let the emergency countdown expire.
3. Open the **Incidents** tab to inspect the frozen report and its Merkle Root Hash.
4. Tap **Export PDF** to view or share the generated printable PDF report.
