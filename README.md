# Human Digital Black Box — Privacy-Preserving Incident Reconstruction

> **BTech Computer Science Final Year Project**  
> *Production-Quality Android Architecture & AWS Serverless Security Infrastructure*

---

## 1. One-Line Pitch
An Android application that maintains a rolling, encrypted, on-device buffer of the user's last 60 minutes of sensor activity. On detecting a likely emergency (crash, fall, or manual SOS), it freezes the buffer, reconstructs a tamper-evident, human-readable timeline of what happened, and securely alerts the user's emergency contacts — like an aviation black box, but for a person.

---

## 2. Research Framing (Viva Core Thesis)

### Research Question
*Can heterogeneous smartphone sensor data be fused, entirely on-device and in real time, into a privacy-preserving, tamper-evident chronological reconstruction of a person's activity immediately preceding an emergency — without continuously transmitting raw personal data off the device?*

### Computer Science Concepts Demonstrated
1. **Sensor Fusion & On-Device Signal Processing**: Real-time multi-sensor ingestion combining accelerometer vector magnitude, gyroscopic angular delta, adaptive GPS updates, activity recognition transitions, and frame-by-frame acoustic amplitude analysis.
2. **Applied Cryptography**:
   - **SHA-256 Hash Chaining**: Every rolling buffer entry incorporates the previous entry's hash (`entryHash = SHA-256(prevHash | payloadJson | timestamp)`).
   - **Merkle-Style Root Hash**: Generates a tamper-evident root hash over frozen incident windows to prove timeline authenticity post-hoc.
   - **Zero-Knowledge Architecture & Envelope Encryption**: Client-side AES-256-GCM encryption before AWS S3 upload; decryption keys are shared out-of-band so the cloud backend never holds plaintext data or decryption keys.
3. **Mobile Systems Engineering**:
   - **SQLCipher Encrypted Database**: Hardware-backed MasterKey generation via Android Keystore (`security-crypto`).
   - **WorkManager Retention Pruning**: Automated background workers continuously purging entries older than 60 minutes.
   - **Foreground Service**: Android 14+ compliant continuous collection with location, microphone, and special-use types.

---

## 3. Non-Negotiable Privacy Design Principles

1. **60-Minute Rolling Buffer (No Permanent Logs)**: Sensor events older than 60 minutes are continuously purged by WorkManager unless an incident trigger freezes the buffer.
2. **On-Device Processing First**: All fusion rules and crash detection algorithms execute locally in RAM/Room.
3. **Zero-Knowledge Backend**: Incident bundles are encrypted client-side using AES-256-GCM before uploading to AWS API Gateway/S3.
4. **No Raw Audio Storage Ever**: Microphone samples feed a real-time on-device acoustic classifier only (e.g. `LOUD_IMPACT`, `RAISED_VOICE`, ambient dB). Raw PCM buffers are zeroed out and discarded frame-by-frame within RAM.
5. **Consensual & Visible Operation**: Persistent Android notification with one-tap pause and one-tap complete data wipe.
6. **Tamper-Evidence**: Cryptographic hash-chaining guarantees post-hoc insertion/modification detection.

---

## 4. Pipeline & Architecture

```
[ Accelerometer + Gyro + Adaptive Location + Activity Recognition + Audio Classifier + Wifi ]
                                          |
                                          v (Structured Kotlin Flows)
                               Ingestion & Hash-Chaining
                                          |
                                          v
                      Room + SQLCipher Encrypted 60m Rolling Buffer
                                          |
                                          +-----------------------+
                                          |                       |
                                          v                       v
                               WorkManager 60m Pruner      Fusion Engine & Trigger
                                                                  |
                                                                  v (Impact / SOS)
                                                       30s Confirmation Countdown
                                                                  |
                                                                  v (On Expiry)
                                                       Freeze Buffer & Reconstruct
                                                                  |
                                                                  v
                                                       Client-Side AES-256-GCM
                                                                  |
                                                                  v
                                                       AWS Backend & Emergency Alerts
```

---

## 5. Technology Stack

- **Language & Framework**: Kotlin, Jetpack Compose (Material 3), Coroutines, StateFlow
- **Architecture**: MVVM with Unidirectional Data Flow
- **Encrypted Local Storage**: Room + SQLCipher + Android Keystore (`androidx.security:security-crypto`)
- **Background Operations**: WorkManager, Android 14+ Foreground Service
- **Sensors & Context**: `SensorManager`, `FusedLocationProviderClient`, `ActivityRecognitionClient`, `AudioRecord` acoustic classifier
- **Networking & Security**: Retrofit + OkHttp with Certificate Pinning
- **Cloud Backend Architecture (AWS Serverless)**:
  - API Gateway (REST API endpoint)
  - AWS Lambda (Node.js/Python business logic)
  - DynamoDB (Metadata & hash-chain manifests)
  - S3 + SSE-KMS (Encrypted incident bundle storage)
  - Cognito (User Auth)
  - SNS / SES (SMS & Email alerts to emergency contacts)

---

## 6. How to Demo for Viva Evaluation

### Step 1: Onboarding & Privacy Rationale
- Launch the app. Review the onboarding screen highlighting the 60-minute retention policy, zero-knowledge encryption, and no-raw-audio policy. Click **Enable Black Box Protection**.

### Step 2: Live Sensor Buffer & Hash Chain Verification
- Navigate to the **Status** tab to observe live "Buffered Events" counter.
- Switch to the **Timeline** tab to inspect real-time reconstructed event entries (location updates, activity state, acoustic classification).
- Note the green banner at top: `CRYPTO HASH CHAIN: INTACT & TAMPER-EVIDENT` (verifies SHA-256 prevHash links).

### Step 3: Viva "Simulate Incident" Mode
- Navigate to the **Debug** tab.
- Tap **Inject Simulated Crash Sequence**.
- Watch as synthetic pre-impact telemetry is injected:
  1. *Vehicle Cruising at 65 km/h*
  2. *Sudden Harsh Deceleration & Angular Shift*
  3. *Peak Impact Deceleration (34.7 m/s²)*
  4. *Acoustic Impact Audio Event (88.5 dB)*
  5. *Post-Impact Stillness*
- The app immediately triggers the full-screen **30-Second Emergency Confirmation Countdown**.

### Step 4: Freeze Buffer, Timeline Reconstruction & PDF Export
- Let the countdown expire (or trigger manual SOS).
- Navigate to the **Incidents** tab to inspect the frozen incident report.
- Examine the generated **Merkle Root Hash** proof.
- Tap **Export PDF** to view or share the officially generated printable PDF Incident Report.

### Step 5: Quick Settings SOS Tile
- Pull down the Android system notification shade.
- Add and tap the **Blackbox SOS** Quick Settings Tile to test immediate system-wide SOS activation.

### Step 6: One-Tap Data Wipe
- Return to **Status** or **Settings** and tap **One-Tap Wipe** to demonstrate complete instant data purging.
