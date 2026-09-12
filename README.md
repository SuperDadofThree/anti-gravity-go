# Antigravity Go (Android Web Interface Container) - Beta v0.0.1

[![GitHub Release](https://img.shields.io/github/v/release/michaelstillings-hash/anti-gravity-go?include_prereleases&label=release)](https://github.com/michaelstillings-hash/anti-gravity-go/releases)
[![Download APK](https://img.shields.io/badge/Download-AntigravityGo--v0.0.1--beta.apk-blue?logo=android)](https://github.com/michaelstillings-hash/anti-gravity-go/releases/download/v0.0.1-beta/AntigravityGo-v0.0.1-beta.apk)

**Antigravity Go** (`com.antigravity.go`) is a native Android application container for [antigravity.google.com](https://antigravity.google.com). It provides a full-screen, app-style experience optimized for Android phones with developer productivity tools.

---

## 📥 Direct Download

You can download the compiled APK directly to your Android device from the Releases page:
- **[Download AntigravityGo-v0.0.1-beta.apk](https://github.com/michaelstillings-hash/anti-gravity-go/releases/download/v0.0.1-beta/AntigravityGo-v0.0.1-beta.apk)** *(12.1 MB)*
- **[View GitHub Release v0.0.1-beta](https://github.com/michaelstillings-hash/anti-gravity-go/releases/tag/v0.0.1-beta)**

---

## 📱 Features

- **App-Style Launching & Full-Screen Immersion**: Launches directly from the home screen as an installed app. Removes browser URL bars, tab switchers, and clutter to maximize coding and chat canvas space.
- **Google Sign-In & OAuth Safe**: Eliminates Google OAuth `403: disallowed_useragent` blocks by utilizing a clean modern Chrome User-Agent and Chrome Custom Tabs fallback.
- **Floating Quick Menu (`✦`)**:
  - **Desktop / Mobile Mode Toggle**: Instantly switch between mobile responsive view and full 1280px desktop view.
  - **Text & Viewport Zoom**: Fine-grained zoom controls (`-`, `100%`, `+`) for comfortable reading of code, logs, and small web elements.
  - **Developer Soft Key Bar (`</>`)**: One-tap toggle for on-screen coding keys (`ESC`, `TAB`, `CTRL+C`, `CTRL+V`, `CTRL+Z`, `←`, `→`, `↑`, `↓`, `/`, `\`, `{`, `}`, `[`, `]`, `|`, `~`, `` ` ``, `$`).
  - **Endpoint Switcher**: Connect to `https://antigravity.google.com` or custom endpoints (e.g., local dev tunnels, Cloud Workstations, or `http://10.0.2.2:8080`).
  - **Clear Cache & Storage**: One-tap web cache and cookie reset.
  - **Version Badge**: Shows `Antigravity Go Beta v0.0.1`.
- **Smart Back Navigation**: The Android system back gesture navigates backward through web page history. Pressing back on the root page prompts "Press back again to exit" to prevent accidental exits.
- **File & Image Upload Support**: Native `WebChromeClient` file chooser integration allows attaching code files, logs, and screenshots directly from Android storage or camera.
- **Android Download Manager Integration**: Downloads from the web interface save straight to Android's `/Download` directory with progress notifications.

---

## 🚀 Installation on Android Phone

The compiled APK is ready in the project root:
- `AntigravityGo-v0.0.1-beta.apk` (12.1 MB)
- `AntigravityGo-debug.apk` (12.1 MB)

### Option 1: Install via ADB (USB / Wireless Debugging)
If your Android phone is connected to your computer with USB Debugging enabled:
```powershell
adb install -r AntigravityGo-v0.0.1-beta.apk
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
