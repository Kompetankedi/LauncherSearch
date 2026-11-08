# LauncherSearch (crDroid / Launcher3 Search Enter Fix)

An Xposed / LSPosed module for crDroid 12 (Android 16 QPR2) and AOSP Launcher3 forks that launches the first matching application upon pressing **Enter** in the app drawer search bar instead of opening Google Web Search.

---

## 🧐 Problem
In crDroid's Launcher3 fork (`com.android.launcher3`), pressing the **Enter** key or the IME **Search** button in the app drawer unconditionally triggers `DefaultSearchAdapterProvider.performGoogleSearch(...)`. This launches Google Web Search (`android.intent.action.WEB_SEARCH`) even when apps matching the search query are already listed on screen.

## 💡 Solution
This module hooks `DefaultSearchAdapterProvider.performGoogleSearch` via LSPosed / Vector:
1. **Checks for Matching Apps:** Reads `mHighlightedView` (the first search result icon view) or the first child of `SearchRecyclerView`.
2. **Launches the App:** Calls `.performClick()` on the top matching app, triggering native app launch animations and statistics.
3. **Graceful Fallback:** If no installed app matches the query (e.g. searching a question or web query), it allows Google Web Search to execute normally.

---

## 🛠️ Requirements
* Rooted Android device with **KernelSU**, **APatch**, or **Magisk**
* **LSPosed** / **Vector Framework** / **Zygisk**
* crDroid 12.x / Launcher3 (Android 14, 15, 16)

---

## 🚀 Building & Installation

### Option 1: Build with Gradle
```bash
./gradlew assembleRelease
```
Or for debug:
```bash
./gradlew assembleDebug
```
The compiled APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: Enable Module in LSPosed / Vector CLI
```bash
# Enable module
/data/adb/lspd/cli modules enable com.crdroid.launchersearch

# Set scope to Launcher3
/data/adb/lspd/cli scope add com.crdroid.launchersearch com.android.launcher3/0

# Restart Launcher3
am force-stop com.android.launcher3
```

---

## 📄 License
MIT License
