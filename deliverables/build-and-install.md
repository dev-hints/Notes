# Build and Installation Guide — Notes

## 1. Prerequisites & Toolchain

To build and run **Notes** (branded as **Notes by StrangeInfinity**), ensure your environment satisfies the following requirements:

| Component | Minimum Required | Recommended / Tested |
| :--- | :--- | :--- |
| **Operating System** | Linux, macOS, or Windows | Linux (x86_64 / arm64) |
| **Java Development Kit (JDK)** | JDK 17 | JDK 21 (Eclipse Temurin / JetBrains Runtime) |
| **Android SDK Platform** | API 34 (Android 14) | API 34 |
| **Android Build Tools** | 34.0.0 | 34.0.0 |
| **Gradle** | 8.9+ | Gradle 8.10 (via included Gradle Wrapper) |
| **Android Gradle Plugin (AGP)** | 8.6.0 | 8.6.0 |
| **Kotlin / KSP** | 2.0.21 | 2.0.21 / KSP 2.0.21-1.0.28 |

---

## 2. Environment Configuration

Set the required environment variables pointing to your SDK and JDK installations:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export JAVA_HOME=/path/to/jdk-21
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

Verify your device is detected via ADB:

```bash
adb devices -l
```

Expected output:
```text
List of devices attached
<device-serial>            device product:generic model:Android_Device device:generic
```

---

## 3. Building the Application

### 3.1. Build Debug APK
Run the Gradle wrapper from the root of the project:

```bash
./gradlew assembleDebug
```

The compiled APK will be generated at:
- `app/build/outputs/apk/debug/app-debug.apk`
- Ready-to-install deliverable copy: `deliverables/NotesByStrangeInfinity-debug.apk` (18 MB)

### 3.2. Build Signed Release APK
Run the Gradle wrapper with the release assembly task:

```bash
./gradlew assembleRelease
```

The signed release APK will be generated at:
- `app/build/outputs/apk/release/app-release.apk`
- Ready-to-install deliverable copy: `deliverables/NotesByStrangeInfinity-release.apk` (12 MB)

Verify APK signing with Android `apksigner`:

```bash
apksigner verify --verbose deliverables/NotesByStrangeInfinity-release.apk
```

### 3.3. Clean Build (Optional)
If switching branches or cleaning build caches:

```bash
./gradlew clean assembleDebug assembleRelease
```

---

## 4. Running Automated Tests

### 4.1. Unit Tests (JVM)
Runs all unit tests, cryptographic envelope validation, and data model business logic:

```bash
./gradlew testDebugUnitTest
```

HTML test reports are generated at `app/build/reports/tests/testDebugUnitTest/index.html`.

### 4.2. Connected Instrumentation Tests (Real Android Device / Emulator)
Ensure a device is connected via ADB (`adb devices`), then execute:

```bash
./gradlew connectedDebugAndroidTest
```

Or invoke the AndroidJUnitRunner directly:

```bash
adb shell am instrument -w -m -e debug false \
  com.strangeinfinity.notes.test/androidx.test.runner.AndroidJUnitRunner
```

---

## 5. Device Installation via ADB

### 5.1. Install the APK
Install the debug APK directly to the connected device:

```bash
adb install -r deliverables/NotesByStrangeInfinity-debug.apk
```

If multiple devices are connected, target your device explicitly using the `-s` flag:

```bash
adb -s <device-serial> install -r deliverables/NotesByStrangeInfinity-debug.apk
```

### 5.2. Launch the Application
Start the primary activity:

```bash
adb shell am start -n com.strangeinfinity.notes/.MainActivity
```

### 5.3. Verify Zero-Internet & Security Isolation
Verify that the app requests zero internet permissions and has no network sockets:

```bash
# Verify no INTERNET permission is granted or requested
adb shell dumpsys package com.strangeinfinity.notes | grep "permission.INTERNET"
# (Outputs nothing, confirming complete absence of internet permission)

# Check active process and UID
adb shell "ps -ef | grep com.strangeinfinity.notes"
```

---

## 6. Capturing Screenshots via ADB

To capture verification screenshots from the physical device:

```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./screenshot.png
adb shell rm /sdcard/screenshot.png
```
