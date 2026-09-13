# TRACE — Human Digital Black Box (Privacy-Preserving Safety & Incident Reconstruction)

> **BTech Computer Science Final Year Project & Viva Presentation Guide**  
> *Production-Oriented Android Architecture, Hilt DI, SQLCipher Encryption, Keystore Cryptography, & AWS Serverless Contract Infrastructure*

---

## 1. Executive Summary & One-Line Pitch

**TRACE** is an Android safety application that maintains a rolling, encrypted, on-device buffer of the user's last 60 minutes of sensor activity. Upon detecting an emergency (automatic crash, fall, or manual SOS), it freezes the buffer, reconstructs a tamper-evident, human-readable timeline, signs the root hash using hardware-backed RSA keys, encrypts the evidence bundle client-side, and dispatches encrypted alerts to emergency contacts.

---

## 2. Android Platform & Build Configuration

- **Target SDK / Compile SDK:** `36` (Android 16 / Google Play Submission Target)
- **Minimum SDK:** `26` (Android 8.0 Oreo)
- **Programming Language:** Kotlin 2.x
- **UI Toolkit:** Jetpack Compose (Material 3 Precision Pitch-Black Theme)
- **Dependency Injection:** Hilt 2.x (`@HiltViewModel`, `@AndroidEntryPoint`)
- **Database Engine:** Room 2.x + SQLCipher (AES-256 encrypted database at rest)
- **Unit Test Suite:** 60 Unit Tests passing (`app:testDebugUnitTest`)

---

## 3. Implementation Taxonomy (Honest System Architecture)

To ensure full transparency during project evaluation and viva examination, system features are categorized by their exact deployment level:

### A. Fully Implemented On-Device (Local)
1. **Rolling 60-Minute Sensor Telemetry Buffer:** 50Hz accelerometer, gyroscope, GPS location, activity recognition, acoustic decibels, battery, and Wi-Fi snapshots recorded in SQLCipher Room DB (`sensor_events` table).
2. **SHA-256 Hash Chain & Boundary Anchors:** Frame-by-frame cryptographic linking (`prevHash` $\rightarrow$ `entryHash`). Old entries purged every 15 minutes update `BufferAnchor` checkpoints in `EncryptedSharedPreferences`.
3. **3-Stage Multi-Evidence Physics Detection Model:** On-device `TriggerDetector` analyzing free-fall weightlessness, impact spikes, and gyroscopic rotation deltas.
4. **Client-Side Envelope Encryption (Wrapped DEK):** Per-incident AES-256-GCM Data Encryption Key (DEK) wrapped using Android Keystore Master Key (`trace_dek_wrapper_key`).
5. **Hardware-Backed Digital Signing:** Merkle Root Hash signed with 2048-bit RSA key pair generated in Android KeyStore (`trace_asymmetric_key`).
6. **Solo Walk Safety Check-In Timer:** Configurable 15, 30, and 60-minute safety countdowns that trigger incident dispatch if uncancelled upon expiration.
7. **Personal Motion Calibration:** 10-second 50Hz baseline accelerometer sampling deriving personalized crash thresholds.
8. **Encrypted Medical ID Profile & Opt-in Emergency QR:** AES-256 encrypted medical profile with privacy-first minimal scannable QR code.
9. **Granular Data Wipe Scopes:** Isolated operations for *Clear Buffer* (telemetry only), *Delete Incidents* (reports only), *Delete Contacts*, and *Factory Reset* (complete KeyStore & DB wipe).
10. **Foreground Protection Service:** `BlackboxForegroundService` running as an Android Foreground Service with explicit state machine (`STOPPED`, `STARTING`, `ACTIVE`, `PAUSED`, `PERMISSION_LIMITED`, `ERROR`).
11. **Glance Home Widget & PDF Exporter:** One-tap SOS Android Jetpack Glance widget and printable PDF evidence exporter.

### B. Simulated / Debug Behavior
1. **`DebugScreen` & Synthetic Crash Sequence Injection:** Debug-build tool (`BuildConfig.DEBUG`) injecting a synthetic 30-second crash timeline (*Cruising 65 km/h $\rightarrow$ Braking $\rightarrow$ Impact 34.7 m/s² $\rightarrow$ Acoustic Spike*) for live demonstration purposes without risking physical hardware.

### C. Backend-Dependent Behavior (Contract Implemented, Target Endpoint Dependent)
1. **Remote Cloud Upload & SNS Emergency Dispatch:** Retrofit contracts for `POST /v1/incidents` and `POST /v1/incidents/{id}/notify` targeting AWS Serverless API Gateway (`https://api.blackbox-safety.aws/v1/`).
2. **Network Diagnostic & Queueing:** Integrated `NetworkDiagnostic` classifying DNS failures (`UnknownHostException`), connection timeouts, TLS errors, and HTTP 4xx/5xx status codes. Reports failing network dispatch are queued as `UploadStatus.PENDING` or `FAILED` for automatic retry without losing contact recipient snapshots.

---

## 4. 3-Stage Multi-Evidence Physics Detection Engine

`TriggerDetector` implements a deterministic 3-stage physics model to detect genuine emergencies while filtering out everyday bumps:

```
[ Stage 1: Free-Fall ]       [ Stage 2: Heavy Impact ]       [ Stage 3: Gyro Delta ]
 Accel < 3.5 m/s²          +   Accel > 20.0 m/s²           +   Gyro > 2.5 rad/s
 (150ms – 600ms)               (impactThresholdMs2)            (gyroThresholdRad)
         │                             │                               │
         └──────────┬──────────────────┘                               │
                    │                                                  │
            [ AUTO_FALL Trigger ]                               [ AUTO_CRASH Trigger ]
```

- **`AUTO_FALL`**: Triggered when Stage 1 (Free-fall) occurs within 1,500ms prior to Stage 2 (Heavy Impact).
- **`AUTO_CRASH`**: Triggered when Stage 2 (Heavy Impact) is accompanied or followed by Stage 3 (Angular Gyroscopic Rotation $\ge 2.5 \text{ rad/s}$).
- **Table Drop & Phone Bump Filtering:** A single high accelerometer spike ($> 20.0 \text{ m/s}^2$) WITHOUT free-fall AND WITHOUT gyroscopic rotation ($< 2.5 \text{ rad/s}$) is recognized as a hard table drop or phone bump, returning `false` without false alarms.
- **Confirmation Countdowns:** `AUTO_CRASH` and `AUTO_FALL` launch a 30-second confirmation countdown. `MANUAL_SOS` uses an independent, immediate 3-second quick path.

---

## 5. Cryptographic Hardening & Envelope Encryption

```
                                [ Incident Timeline JSON ]
                                            │
                                  AES-256-GCM Encryption
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    ▼                                               ▼
         [ Encrypted Bundle ]                             [ Per-Incident DEK ]
         (Stored in DB & Uploaded)                                 │
                                                        AES-256 Key Wrapping via
                                                        Keystore KEK (trace_dek_wrapper_key)
                                                                   │
                                                                   ▼
                                                       [ Wrapped DEK Key ]
                                                       (Stored in DB)
```

1. **Envelope Encryption:** Each incident generates a fresh per-incident AES-256 Data Encryption Key (DEK). The timeline JSON is encrypted with this DEK using AES-256-GCM.
2. **DEK Key Wrapping:** The DEK is wrapped (encrypted) using an Android Keystore-backed AES-256 Master Key (`trace_dek_wrapper_key`). Only the wrapped key is stored in the `incident_reports` database table.
3. **RSA Digital Signing:** Merkle Chain Root Hash is signed with a 2048-bit RSA private key stored in Android KeyStore (`trace_asymmetric_key`).
4. **Signature Verification States:**
   - `SIGNED`: Valid RSA signature verified via Keystore public key.
   - `UNSIGNED`: Missing digital signature or public key.
   - `SIGNATURE_INVALID`: Signature verification failed (tampered data or modified hash).

---

## 6. Performance & Battery Optimization

1. **98% I/O Batching Reduction:** Real-time 50Hz accelerometer/gyroscope evaluation runs in RAM for zero-latency crash detection, while telemetry disk writes are batched in memory and written to SQLCipher Room DB in single batch transactions every 50 samples (~1 second) or immediately upon impact spikes ($> 18.0 \text{ m/s}^2$).
2. **Periodic Buffer Purging:** Background maintenance purges events older than 60 minutes every 15 minutes (`purgeExpiredBuffer`), keeping database file size and memory footprint strictly bounded.
3. **Power Saver Mode:** When battery drops below 15% and is not charging, audio classification is paused and location updates adapt to low-power 60-second intervals.

---

## 7. Navigation Structure

The application uses a 4-tab bottom navigation bar with top-hairline borders and uppercase typography:

- **`STATUS` (`HomeScreen`):** Protection status hero, hardware & buffer specification sheet grid, primary `SOS` button, solo walk check-in timer, and recent motion graph.
- **`TIMELINE` (`TimelineScreen`):** Cryptographic buffer integrity header, session-grouped timeline items, and expandable forensic evidence details.
- **`INCIDENTS` (`IncidentReportScreen`):** Saved encrypted incident reports, upload status badges, PDF export, digital signature verification, and incident replay.
- **`MORE` (`MoreScreen`):** Grouped index page providing access to:
  - *Emergency Contacts* (`ContactsScreen`)
  - *Medical Profile & Emergency QR* (`MedicalQrScreen`)
  - *Local Safety Analytics* (`AnalyticsScreen`)
  - *Settings & Data Privacy* (`SettingsScreen`)
  - *Developer & Testing Tools* (`DebugScreen`, Debug builds only)

---

## 8. Viva Demonstration Guide

### Step 1: Onboarding & Protection Startup
1. Launch TRACE for the first time; note the pitch-black editorial onboarding workflow.
2. Grant required permissions (Location, Microphone, Physical Activity) and complete setup.
3. Observe `STATUS` screen showing `PROTECTION ACTIVE` and `HARDWARE & BUFFER SPECIFICATION` grid displaying real-time sensor status.

### Step 2: Emergency Contacts & Contact Snapshot
1. Navigate to **MORE > Emergency Contacts** and add a trusted contact.
2. Tap **Send Real Test Alert to Contact** or **Preview Local Alert on Phone**.

### Step 3: Rolling Buffer & Timeline
1. Navigate to **TIMELINE**.
2. Observe the `BUFFER INTEGRITY: VERIFIED` banner and session-grouped motion entries.
3. Tap **View Evidence Details →** on an entry to inspect the SHA-256 entry hash and raw JSON payload.

### Step 4: Motion Calibration & Sensitivity
1. Navigate to **MORE > Settings**.
2. Tap **Start 10s Sensor Calibration** and carry the phone naturally to measure baseline motion.
3. Adjust the **Crash Detection Sensitivity** slider and inspect advanced physics thresholds.

### Step 5: Incident Simulation & Cryptographic Verification
1. Navigate to **MORE > Developer & Testing Tools** (or Debug tab).
2. Tap **Inject Simulated Crash Sequence**.
3. Let the 30-second emergency countdown expire (or tap **Cancel** to test false-positive avoidance).
4. Go to **INCIDENTS** to inspect the frozen report, severity score, wrapped encryption key, and tap **Verify Digital Signature & Hash Chain** to confirm RSA authenticity.
5. Tap **Export PDF** to view or share the generated evidence PDF.

### Step 6: Granular Data Wipe & Factory Reset
1. Go to **MORE > Settings**.
2. Test **Clear Buffer** (deletes telemetry only, preserving reports and contacts).
3. Test **Factory Reset TRACE** (purges all DBs, encrypted preferences, KeyStore keys, and returns TRACE to first-run onboarding).

---

## 9. Test Suite Summary

Run unit tests via Gradle:
```bash
./gradlew test
```

**Unit Test Status:** `60 passed, 0 skipped, 0 failed`

### Test Suite Breakdown:
- **`SafetyLifecycleEndToEndTest`**: End-to-end testing from onboarding, service startup, physics trigger, countdown, envelope encryption, RSA signing, network upload, offline queuing, and factory reset.
- **`TriggerDetectorTest`**: 3-stage physics model, free-fall + impact, gyro rotation + impact, single-spike table drop filtering, running, vehicle vibration.
- **`AutoDetectionSettingTest`**: Persisted auto-detection toggle and manual SOS bypass.
- **`CryptoHardeningTest`**: AES-256-GCM DEK envelope encryption, Keystore key wrapping/unwrapping, RSA signature verification, tampered signature detection.
- **`DataWipeIsolationTest`**: Isolated scopes for Clear Buffer, Delete Incidents, Delete Contacts, and Factory Reset.
- **`FactoryResetTest`**: Complete database, encrypted preferences, and KeyStore key destruction.
- **`ProtectionStateManagerTest`**: State machine transitions (`STOPPED`, `STARTING`, `ACTIVE`, `PAUSED`, `PERMISSION_LIMITED`, `ERROR`).
- **`NetworkDiagnosticTest`**: Categorization of DNS, connection, TLS, and HTTP 4xx/5xx failures.
- **`RepeatedPauseResumeTest`**: Service pause/resume cycles, job leak prevention, and 98% I/O batching savings.
- **`HashChainManagerTest`**: SHA-256 chain continuity, Merkle root hash computation, boundary anchor verification, and tampering detection.
- **`CalibrationCalculatorTest`**: 10-second 50Hz baseline accelerometer sampling, P95 derivation, and custom impact threshold calculation.
