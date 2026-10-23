# Known Issues & Architectural Scope — Notes

## 1. Overview

**Notes** (branded as **Notes by StrangeInfinity**) is intentionally designed as an offline-first, local-only note-taking application. This document describes architectural scope boundaries, hardware considerations, and future feature roadmaps.

---

## 2. Hardware & Platform Considerations

### 2.1. Stylus Pressure Sensitivity & Hardware Digitizers
- **Current Behavior**: The handwriting and sketch canvas utilizes Quadratic Bézier curves with midpoint interpolation based on touch drag coordinates (`Offset(x, y)`). Four width presets (Fine, Medium, Bold, Marker) offer variable stroke weights.
- **Hardware Limitation**: Passive capacitive touchscreens (standard smartphone glass) report a constant touch pressure (`1.0f`). True variable-width pressure response requires an active stylus digitizer (e.g. Samsung S-Pen or Wacom EMR) listening for `MotionEvent.AXIS_PRESSURE`.
- **Planned Enhancement**: Add an optional pressure-curve multiplier for devices equipped with active stylus hardware while maintaining smooth software Bézier smoothing on standard capacitive screens.

### 2.2. Audio & Multi-Page PDF Attachments
- **Current Behavior**: The attachment subsystem natively supports image attachments (`image/*`) via the Android Storage Access Framework (SAF), persisting decoded binary bytes to app-sandboxed storage (`files/attachments/`) and packing them into encrypted backups via Base64.
- **Scope Boundary**: Voice memos, audio recordings, and multi-page PDF inline rendering are excluded from the initial release to maintain a minimal APK footprint (<20MB) and avoid heavy native PDF-rendering libraries.

### 2.3. Backup Envelope Memory Footprint
- **Current Behavior**: `BackupManager` decodes and decrypts JSON envelopes into RAM using Kotlinx Serialization with a 50MB safeguard threshold.
- **Consideration**: For standard user archives (thousands of text notes, checklists, vector drawings, and dozens of compressed photos), backups remain well within 5–20MB. For users attaching hundreds of high-resolution camera photos, future releases will introduce a chunked `CipherInputStream` / `CipherOutputStream` streaming pipeline.

---

## 3. Deliberate Privacy Boundaries (Non-Issues)

The following design decisions are deliberate security measures, not bugs:
1. **No Cloud Synchronization**: Backups are client-managed JSON files. There is no automated sync with Google Drive, OneDrive, or Dropbox. Users own their data and choose when and where to export their encrypted backups.
2. **Absence of Internet Permission**: `android.permission.INTERNET` is completely removed from the manifest. The app cannot make HTTP requests, transmit analytics, or communicate with remote servers.
3. **No Automatic Account Registration**: The app does not prompt for logins, emails, phone numbers, or passwords.
