# Notes by StrangeInfinity

A complete, production-quality, offline-first Android note-taking application engineered with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Android Room**.

Designed with inspiration from Samsung Notes and Google Keep, **Notes** pairs an original, refined design system with a 100% private, local-only architecture and client-side password-protected **AES-256-GCM** encrypted backups.

---

## Key Highlights

- **100% Local & Offline**: `android.permission.INTERNET` is completely absent from the Android manifest. Zero tracking, zero telemetry, zero advertisements, and zero cloud lock-in.
- **Client-Side Encrypted Backups**: Portable, versioned JSON backup format protected with authenticated **AES-256-GCM** encryption and **PBKDF2-HMAC-SHA256** key stretching (100,000 iterations).
- **Refined Color System & Readability**: Google Keep and Apple Notes-inspired soft pastel card palettes in Light Mode with deep ink typography (`#0F172A`), and an OLED-optimized Obsidian palette in Dark Mode (`#090D16`).
- **Rich Text & Checklists**: Bold, italic, underline, strikethrough, headings, bulleted lists, numbered lists, and interactive checklists with real-time completion progress.
- **Vector Handwriting & Sketch Canvas**: Touch drag gestures interpolate quadratic Bézier curves with vector precision, complete with stroke smoothing, customizable widths, color palette, and undo/redo.
- **Multi-Level Organization**: Folders with customizable color badges, tag taxonomy, note pinning, multi-select batch operations, and a recoverable recycle bin (Trash).
- **Responsive Adaptive Layout**: Optimized for compact smartphones, large phones, and tablets with edge-to-edge support and adaptive staggered grid layouts (`StaggeredGridCells.Adaptive`).

---

## Technology Stack

- **Platform**: Android (Min SDK 26, Target SDK 34)
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (BOM 2024.10.00) with Material Design 3
- **Local Database**: Android Room 2.6.1 with SQLite transactions
- **Preferences**: Jetpack DataStore Preferences 1.1.1
- **Navigation**: Navigation Compose 2.8.3
- **Serialization**: Kotlinx Serialization 1.7.3
- **Cryptography**: `AES/GCM/NoPadding`, `PBKDF2WithHmacSHA256`, `SecureRandom`
- **File Management**: Android Storage Access Framework (SAF)
- **Build System**: Gradle 8.10 with Kotlin DSL and KSP 2.0.21-1.0.28

---

## Project Architecture

```text
com.strangeinfinity.notes/
├── MainActivity.kt               # Root single-activity, edge-to-edge system insets
├── NotesApplication.kt          # Application lifecycle container & dependency provider
├── core/
│   ├── model/                  # Domain models (Note, Folder, Tag, Checklist, Drawing)
│   ├── database/               # Room entities, DAOs, NotesDatabase singleton
│   ├── security/               # BackupCrypto (AES-256-GCM, PBKDF2, tamper detection)
│   ├── backup/                 # BackupManager, schemas, staging, transactional restore
│   ├── data/                   # NotesRepository, PreferencesRepository, AttachmentManager
│   └── designsystem/           # Color palettes, high-contrast typography, components
├── feature/
│   ├── home/                   # Note grid/list, debounced search, multi-select, filter chips
│   ├── editor/                 # Note editor, formatting bar, checklist mode, autosave
│   ├── drawing/                # Handwriting & sketch canvas, smoothing, stroke controls
│   ├── folders/                # Folder management, custom color picker, safe cascade
│   ├── trash/                  # Soft deletion, note restoration, permanent deletion
│   ├── backup/                 # Encrypted/Plain JSON backup & restore with safe preview
│   └── settings/               # App theme (Light/Dark/System), layout, sorting, storage, about
└── ui/
    └── navigation/             # Type-safe screen navigation and routes
```

---

## Quick Start & Build

### Prerequisites
- JDK 17 or JDK 21
- Android SDK 34 with Build Tools 34.0.0
- An Android device or emulator with USB debugging enabled

### Build APK
```bash
./gradlew assembleDebug
```
The resulting debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Run Instrumentation Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Install via ADB
```bash
adb install -r deliverables/NotesByStrangeInfinity-debug.apk
adb shell am start -n com.strangeinfinity.notes/.MainActivity
```

---

## Project Deliverables

All project deliverables and technical specifications are organized within the `deliverables/` directory:

- [**Architecture & Technical Design**](deliverables/architecture.md): Complete architectural specifications, Clean Architecture principles, and subsystem workflows.
- [**Backup Specification & Format**](deliverables/backup-format.md): Versioned JSON backup envelope, AES-256-GCM cryptographic parameters, schema definition, and atomic transaction guarantees.
- [**Comprehensive Test Report**](deliverables/test-report.md): Hardware verification on Samsung Galaxy SM-E045F (Android 14 API 34), automated test logs, pass/fail counts, and offline security audit.
- [**Build & Installation Guide**](deliverables/build-and-install.md): Step-by-step instructions for environment configuration, building, test execution, and ADB device installation.
- [**Known Issues & Scope**](deliverables/known-issues.md): Hardware digitizer considerations, memory footprint limits, and feature roadmap.
- [**Ready-to-Install Debug APK**](deliverables/NotesByStrangeInfinity-debug.apk): Verified, compiled debug APK (18 MB).
- [**Real Device Screenshots**](deliverables/screenshots/): Complete collection of 12 full-resolution PNG screenshots captured directly from the physical Android 14 test device.

---

## License & Attribution

Designed and developed for **StrangeInfinity**.  
100% local, offline, and private.
