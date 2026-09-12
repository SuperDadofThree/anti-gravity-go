# Antigravity Go (Android Web Interface Container) - Beta v0.0.3

[![GitHub Release](https://img.shields.io/github/v/release/SuperDadofThree/anti-gravity-go?include_prereleases&label=release)](https://github.com/SuperDadofThree/anti-gravity-go/releases)
[![Download APK](https://img.shields.io/badge/Download-AntigravityGo--v0.0.3--beta.apk-blue?logo=android)](https://github.com/SuperDadofThree/anti-gravity-go/releases/download/v0.0.3-beta/AntigravityGo-v0.0.3-beta.apk)

**Antigravity Go** (`com.antigravity.go`) is a native Android application container for [antigravity.google.com](https://antigravity.google.com). It provides a full-screen, app-style experience optimized for Android phones with developer productivity tools.

---

## 📥 Direct Download

You can download the compiled APK directly to your Android device from the Releases page:
- **[Download AntigravityGo-v0.0.3-beta.apk](https://github.com/SuperDadofThree/anti-gravity-go/releases/download/v0.0.3-beta/AntigravityGo-v0.0.3-beta.apk)** *(12.1 MB)*
- **[View GitHub Release v0.0.3-beta](https://github.com/SuperDadofThree/anti-gravity-go/releases/tag/v0.0.3-beta)**

---

## 📱 Features

- **App-Style Launching & Full-Screen Immersion**: Launches directly from the home screen as an installed app. Removes browser URL bars, tab switchers, and clutter to maximize coding and chat canvas space.
- **Robust URL & Window Handling (Crash-Proof)**:
  - Fixed crashes when tapping links opening in new windows/tabs (`target="_blank"` or `window.open`).
  - Safe URL dispatching: internal Antigravity and Google links stay inside the app; external links open safely in Custom Tabs or external browser with `FLAG_ACTIVITY_NEW_TASK`.
- **Integrated Diagnostics & Log Collection**:
  - **In-App Log Console**: Access via **Settings (`⚙`) → Collect & View Logs**.
  - **Live Capture**: Captures page navigation, Web Chrome console messages (`console.log`, `console.warn`, `console.error`), network failures, and uncaught crash traces.
  - **One-Tap Actions**: Copy full logs to clipboard, share logs via Android's share sheet, or dump device system `logcat`.
- **Draggable Floating Action Bubble (`✦`)**:
  - The quick menu trigger bubble can be dragged and repositioned anywhere around your phone screen so it never obstructs code, chat input, or buttons.
  - Automatically constrained within screen bounds so it never gets lost.
- **Redesigned & Enlarged Quick Menu**:
  - Large tap targets (48dp+ buttons) with modern elevated card styling.
  - **Desktop / Mobile Mode Toggle**: Instantly switch between mobile responsive view and full desktop view.
  - **Home / Reload**: Quickly reload the Antigravity session.
  - **Open in External Browser**: One-tap fallback to external Chrome/system browser.
  - **Google Account Switcher**: Easily switch or log in with any device account.
  - **Endpoint Switcher & Cache Control**: Connect to custom endpoints or clear web cache and cookies.
  - **Tap-to-Dismiss Scrim**: Tap anywhere outside the menu to dismiss.
- **Smart Back Navigation**: The Android system back gesture navigates backward through web page history. Pressing back on the root page prompts "Press back again to exit" to prevent accidental exits.
- **File & Image Upload Support**: Native `WebChromeClient` file chooser integration allows attaching code files, logs, and screenshots directly from Android storage or camera.
- **Android Download Manager Integration**: Downloads from the web interface save straight to Android's `/Download` directory with progress notifications.

---

## 🚀 Installation on Android Phone

The compiled APK is ready in the project root:
- `AntigravityGo-v0.0.3-beta.apk` (12.1 MB)
- `AntigravityGo-debug.apk` (12.1 MB)

### Option 1: Install via ADB (USB / Wireless Debugging)
If your Android phone is connected to your computer with USB Debugging enabled:
```powershell
adb install -r AntigravityGo-v0.0.3-beta.apk
```

### Option 2: Direct Sideloading (Transfer to Phone)
1. Transfer `AntigravityGo-debug.apk` to your phone via:
   - USB cable
   - Google Drive / Dropbox
   - Quick Share (Nearby Share)
   - Messaging / Email to yourself
2. On your phone, tap the `.apk` file in your Files or Downloads app.
3. If prompted, allow "Install unknown apps" for your file manager.
4. Tap **Install** and then **Open**.

---

## 🛠️ Building from Source

### Requirements
- JDK 17+
- Android SDK (API 36 / Android 14+)

### Build Command
```powershell
# Windows PowerShell
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
The output APK will be placed in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🏗️ Architecture

```
app/src/main/
├── AndroidManifest.xml             # Permissions, Intent filters for antigravity.google.com
├── java/com/antigravity/go/
│   ├── MainActivity.kt             # Activity, file picker launcher, edge-to-edge
│   ├── ui/
│   │   ├── ContainerScreen.kt      # Main Compose UI, WebView integration, controls
│   │   ├── DeveloperKeyBar.kt      # Soft keyboard coding toolbar (Esc, Tab, Ctrl...)
│   │   └── SettingsDialog.kt       # Target URL and cache management dialog
│   ├── web/
│   │   ├── AntigravityWebClients.kt# WebChromeClient & WebViewClient implementations
│   │   ├── UserAgentHelper.kt      # Chrome UA spoofing & Google OAuth compatibility
│   │   └── WebContainerState.kt    # Container state model & SharedPreferences keys
│   └── theme/
│       ├── Color.kt                # Antigravity dark/light color palette
│       ├── Theme.kt                # Material 3 Compose theme
│       └── Type.kt                 # Typography
└── res/
    ├── drawable/                   # Vector assets & Antigravity Go app launcher icons
    └── values/                     # Strings and app theme
```
