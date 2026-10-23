# Architecture & Technical Design — Notes

## 1. Executive Summary

**Notes** (branded as **Notes by StrangeInfinity**) is a high-performance, production-quality Android note-taking application built exclusively using modern Android development practices, Kotlin, Jetpack Compose, Material 3, and Room. 

The application is engineered strictly around an **offline-first, zero-cloud, privacy-preserving paradigm**:
- **Zero Internet Permissions**: `android.permission.INTERNET` is completely absent from the Android manifest. The app is physically incapable of making network requests.
- **Sandboxed Local Persistence**: SQLite database managed via Android Room with strict relational integrity, transactions, and foreign-key isolation.
- **Client-Side Cryptographic Backups**: Portable, versioned JSON backup subsystem protected with authenticated **AES-256-GCM** encryption and **PBKDF2-HMAC-SHA256** key derivation.
- **Universal Responsiveness**: Fully adaptive UI for both compact smartphones and large-screen tablets/foldables.

---

## 2. Technology Stack

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | 2.0.21 | Primary application code, type safety, coroutines |
| **UI Toolkit** | Jetpack Compose | BOM 2024.10.00 | Declarative UI, Material 3 theming, animations |
| **Architecture** | MVVM + Clean Architecture | AndroidX Lifecycle 2.8.6 | Unidirectional Data Flow (UDF), StateFlow |
| **Local Database** | Room | 2.6.1 | Local relational persistence, transaction management |
| **Preferences** | Jetpack DataStore | 1.1.1 | Key-value settings persistence (theme, view mode, sort) |
| **Navigation** | Navigation Compose | 2.8.3 | Type-safe screen routing and deep navigation |
| **Serialization** | Kotlinx Serialization | 1.7.3 | JSON serialization for backups, checklists, drawings |
| **Cryptography** | Standard Java/Android Crypto | JVM / Android Provider | AES-256-GCM, PBKDF2WithHmacSHA256, SecureRandom |
| **Storage Access**| Android SAF | Android Framework | Document creation and opening without broad storage permissions |

---

## 3. High-Level Architecture & Clean Modular Structure

The codebase is organized into clear domain, data, and presentation boundaries:

```text
com.strangeinfinity.notes/
├── MainActivity.kt               # Single activity, edge-to-edge root, theme provider
├── NotesApplication.kt          # Application lifecycle, dependency container
├── core/
│   ├── model/                  # Domain entities (Note, Folder, Tag, Checklist, Drawing)
│   ├── database/               # Room entities, DAOs, NotesDatabase singleton
│   ├── security/               # BackupCrypto (AES-GCM, PBKDF2, tamper detection)
│   ├── backup/                 # BackupManager, schemas, staging, transactional restore
│   ├── data/                   # NotesRepository, PreferencesRepository, AttachmentManager
│   └── designsystem/           # Color palettes, high-contrast typography, components
├── feature/
│   ├── home/                   # Note grid/list, live debounced search, multi-select, filters
│   ├── editor/                 # Rich text formatting, interactive checklists, autosave
│   ├── drawing/                # Handwriting & vector stroke sketch canvas, smoothing
│   ├── folders/                # Folder organization, safe detachment on deletion
│   ├── trash/                  # Recoverable recycle bin, permanent deletion
│   ├── backup/                 # Backup & Restore UI, password management, preview modal
│   └── settings/               # Themes, sort preferences, storage usage, about & privacy
└── ui/
    └── navigation/             # Screen routes, NavHost, argument passing
```

---

## 4. Key Architectural Subsystems

### 4.1. Unidirectional Data Flow (UDF) & State Management
- Every screen is backed by an independent `ViewModel` exposing an immutable `StateFlow<UiState>`.
- Composables observe UI state and dispatch user intents/events back to the ViewModel.
- No composable accesses Room database instances directly; all interactions traverse `NotesRepository`.
- ViewModels maintain zero references to Android Activity or Context objects.

### 4.2. Autosave Engine
- `EditorViewModel` coordinates debounced background persistence:
  1. Keystrokes update UI state immediately for responsive 120Hz/60Hz rendering.
  2. A debouncing coroutine job waits 800ms after the last edit before invoking `saveImmediately()`.
  3. UI displays active save state (`Saving...`, `Saved`, `Save error`).
  4. On screen exit or lifecycle disposal (`DisposableEffect`), changes are immediately persisted to disk synchronously, eliminating data loss during rotation or navigation.

### 4.3. Drawing & Handwriting Vector Engine
- Strokes are recorded as floating-point vector coordinates (`DrawingPoint(x, y)`).
- Touch events (`detectDragGestures`) produce smooth quadratic Bézier curves interpolating midpoints.
- Vector data is serialized to JSON (`DrawingData`), meaning drawings never suffer from raster degradation or pixelation when resized or viewed on high-density displays.
- Full stroke-level Undo/Redo stack with eraser mode and canvas clearing.

### 4.4. Adaptive Responsive Design (Smartphones & Tablets)
- **Status Bar & Display Cutouts**: Top bars declare `.statusBarsPadding()`, ensuring icons, titles, and menus are never obscured by camera notches or system status bars.
- **Adaptive Staggered Grid**: Uses `StaggeredGridCells.Adaptive(minSize = 175.dp)`:
  - Narrow phones: 1 column
  - Standard smartphones: 2 columns
  - Tablets and landscape foldables: 3–4 columns
- **Constrained Content Width**: On tablets, editor and settings views center within a maximum reading width of 800–840dp, preventing wide, unwieldy text lines.

---

## 5. Security & Manifest Audit

Inspection of `app/src/main/AndroidManifest.xml` verifies:
- `android.permission.INTERNET` is **completely absent**.
- No background sync services or broadcast receivers for telemetry.
- Storage operations rely strictly on the system Storage Access Framework (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`), eliminating any need for invasive storage permissions (`READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`).
