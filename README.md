# Auto Group Poster

**Auto Group Poster** is a free, **100% open-source** Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, and Android's **AccessibilityService**. It helps you maintain a local checklist of your Facebook groups and automate publishing a post (text caption + photo or video) across your checked groups using the official Facebook app (`com.facebook.katana`).

[![Download APK](https://img.shields.io/badge/Download_APK-FacebookAutoGroupPoster.apk-1877F2?style=for-the-badge&logo=android&logoColor=white)](https://github.com/dopartner001-art/Facebook-Auto-Group-Poster-v1.0/releases/download/AutoGroupPoster_v1.0/FacebookAutoGroupPoster.apk)

> **Open-Source & Privacy Guarantee:** Every line of source code in this repository is public and auditable. The APK built by GitHub Actions comes directly from this exact source code.

---

## Key Features

- **My Facebook Groups Checklist (Local Room Database):**
  - Save Facebook group names or direct Facebook group URLs (`https://www.facebook.com/...`) locally on your device.
  - Check/uncheck specific groups, toggle **Select All / Deselect All**, or delete groups anytime.
- **Post Composer with Media Preview:**
  - Multi-line caption editor inserted directly into the post box via Accessibility `Bundle` (`ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE`) without touching your clipboard.
  - Pick a photo or video using Android's zero-permission `ActivityResultContracts.GetContent` with live thumbnail preview.
- **One-Click Automated Posting Engine:**
  - Opens checked groups in the official Facebook app (`com.facebook.katana`) via direct group URL deep-linking or search.
  - Attaches selected media, fills in the caption, and publishes automatically (supports both English and Bengali Facebook UI).
  - Applies a randomized **8–15 second human-like cooldown** between posts.
- **Safety & Real-Time Control:**
  - Floating progress overlay (`SYSTEM_ALERT_WINDOW`) displays `"Posting to X of Y groups..."`, the active step, and a red **Stop** button to halt automation immediately at any time.

---

## Security & Privacy Verification (Zero Suspicion)

Anyone can verify these facts directly in the source files below:

1. **No `INTERNET` Permission:**
   - Check [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) — this app does **not** request `android.permission.INTERNET`.
   - It **cannot** send your captions, photos, group list, or any device data to any external server. Everything stays 100% offline on your device.
2. **No Broad Storage Permissions:**
   - The app does **not** request `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, or `READ_MEDIA_VIDEO`. Media selection uses Android's standard system picker (`ActivityResultContracts.GetContent`).
3. **Strictly Scoped Accessibility Service:**
   - Check [`app/src/main/res/xml/accessibility_service_config.xml`](app/src/main/res/xml/accessibility_service_config.xml) — `android:packageNames="com.facebook.katana"` restricts the service strictly to the official Facebook app. It never inspects other apps, keyboards, or banking apps.
4. **No Ads, No Trackers, No Analytics:**
   - There are no ad SDKs, third-party analytics, or hidden background services.

---

## Where Everything Is Located (Project Map)

| Component | Exact Path in Repository | Purpose |
| :--- | :--- | :--- |
| **Manifest & Permissions** | [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) | Declares `SYSTEM_ALERT_WINDOW`, `com.facebook.katana` query, and `AutoPostAccessibilityService` |
| **Accessibility Scope Config** | [`app/src/main/res/xml/accessibility_service_config.xml`](app/src/main/res/xml/accessibility_service_config.xml) | Limits AccessibilityService strictly to `com.facebook.katana` |
| **App Entry Point** | [`app/src/main/java/com/example/MainActivity.kt`](app/src/main/java/com/example/MainActivity.kt) | Hosts Compose UI, Room setup, and media picker launcher |
| **Main UI & ViewModel** | [`app/src/main/java/com/example/ui/`](app/src/main/java/com/example/ui/) | Material 3 screens, group checklist, post composer, and permission banner |
| **Automation Engine** | [`app/src/main/java/com/example/automation/AutoPostAccessibilityService.kt`](app/src/main/java/com/example/automation/AutoPostAccessibilityService.kt) | Automates opening groups, attaching media, setting caption text, and posting |
| **Floating Stop Widget** | [`app/src/main/java/com/example/automation/FloatingOverlayController.kt`](app/src/main/java/com/example/automation/FloatingOverlayController.kt) | Draggable floating card showing progress and Stop button |
| **Local Database (Room)** | [`app/src/main/java/com/example/data/local/`](app/src/main/java/com/example/data/local/) | Stores saved Facebook groups and selection state locally |
| **GitHub APK Build Workflow** | [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) | Compiles the APK transparently on GitHub Actions |
| **Compiled APK Output Path** | `app/build/outputs/apk/debug/app-debug.apk` | Generated APK output location after Gradle build |

---

## Where to Download the App (APK)

Click the button below for an instant direct download (no GitHub login required):

[![Download APK](https://img.shields.io/badge/Download_APK-FacebookAutoGroupPoster.apk-1877F2?style=for-the-badge&logo=android&logoColor=white)](https://github.com/dopartner001-art/Facebook-Auto-Group-Poster-v1.0/releases/download/AutoGroupPoster_v1.0/FacebookAutoGroupPoster.apk)

1. **Direct Link / GitHub Releases (Recommended — 1-Click Download):**
   - Direct URL: [`https://github.com/dopartner001-art/Facebook-Auto-Group-Poster-v1.0/releases/download/AutoGroupPoster_v1.0/FacebookAutoGroupPoster.apk`](https://github.com/dopartner001-art/Facebook-Auto-Group-Poster-v1.0/releases/download/AutoGroupPoster_v1.0/FacebookAutoGroupPoster.apk)
   - Or open **Releases** on this repository page and tap **`FacebookAutoGroupPoster.apk`**.
2. **From GitHub Actions (Requires GitHub Login):**
   - Open the **Actions** tab -> click the latest green **"Build Debug APK"** workflow run -> scroll to **Artifacts** -> download **`auto-group-poster-debug-apk`** and extract `app-debug.apk`.

---

## How to Connect & Set Up the App (Step-by-Step)

1. **Install Prerequisites:**
   - Install the official **Facebook** app (`com.facebook.katana`) and log in to your account *(Facebook Lite is not supported)*.
   - Install **`FacebookAutoGroupPoster.apk`** on your Android phone.
2. **Unlock Restricted Settings (Android 13 / 14 / 15+):**
   - **Method A (Standard):** Long-press the **Auto Group Poster** icon -> tap **App info** (or **Settings > Apps > Auto Group Poster**) -> tap the top-right **`⋮` (three dots)** -> select **Allow restricted settings** -> confirm with your PIN or fingerprint.
   - **Method B (If "Allow restricted settings" is hidden):** Open **Auto Group Poster** -> tap **Enable Service** -> tap the grayed-out **Auto Group Poster Engine** once so Android registers the request -> return to **App info > `⋮`** (or scroll to the bottom of App info on ColorOS/Realme/Xiaomi devices) -> **Allow restricted settings** will now appear.
3. **Connect Permissions Inside the App:**
   - Tap **Enable Service** -> **Downloaded apps > Auto Group Poster Engine** -> turn **On**.
   - Tap **Allow Overlay** -> turn **On** **Allow display over other apps** (required for the floating progress & **Stop** widget).
   - Confirm both badges show **Accessibility Service Active** and **Floating Widget On**.
4. **Add Your Groups (Best Practice):**
   - **Recommended Method (Most Reliable):** Open the group in Facebook -> tap **Share > Copy link** -> paste the group URL (`https://www.facebook.com/groups/...` or `https://www.facebook.com/share/g/...`) into **My Facebook Groups**. The app will open each group directly without relying on search.
   - **Alternative Method:** Enter the **exact group name** and make sure your Facebook account has already joined the group.
5. **Compose & Start Posting:**
   - Check the groups you want to post to, write your caption, optionally select a photo or video, and tap **Post to Selected Groups**.

---

## Troubleshooting & Solutions for Common Issues

| Issue | Why It Happens | How to Fix It |
| :--- | :--- | :--- |
| **"Controlled by Restricted Setting" / Overlay toggle disabled** | Android 13+ locks Accessibility & Overlay for APKs installed outside Google Play. | Use **Method A** or **Method B** in Step 2 above to tap **Allow restricted settings** in **App info**, then re-enable both toggles. |
| **Search opens the wrong group or skips the group** | Multiple Facebook groups share similar names. | Delete the group name and add the **exact Facebook Group URL** (`https://www.facebook.com/...`) instead so it opens directly. |
| **Automation stops on "Add text" / "টেক্সট যোগ করুন"** | Older builds didn't tap the **Done / সম্পন্ন** button before returning to the main post screen. | Update to the latest APK from **Actions**; it automatically taps **Done / সম্পন্ন** and supports both English and Bengali Facebook UIs. |
| **Text posts, but photo/video does not attach** | Facebook's gallery wasn't granted media access, or the photo wasn't at the top of Recents. | Ensure you pick the photo inside **Auto Group Poster** right before posting (Android 10+ stages it to the top of Recents). If Facebook asks for storage/photo permission inside its picker, tap **Allow / Allow all** once. |
| **Floating Stop widget does not appear over Facebook** | **Display over other apps** is turned off or restricted. | Unlock **Allow restricted settings** first, then tap **Allow Overlay** in the app banner. |
| **Post button doesn't fire or account gets warned** | Network lag, non-joined group, or posting too many groups at once. | Make sure you are already a member of every checked group and post in small batches (5–10 groups per session) with the built-in 8–15s cooldown. |

---

## Build From Source Yourself

If you want to inspect and compile the app locally:

- **Requirements:** JDK 17, Android SDK (Compile SDK 36), Gradle 9.3.1
- **Clone & Build:**
  ```bash
  git clone <your-repository-url>
  cd <repository-folder>
  base64 -d debug.keystore.base64 > debug.keystore   # or generate a debug keystore with keytool
  gradle assembleDebug
  ```
- The compiled APK will be created at:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## Disclaimer & License

- Use responsibly and follow Facebook's community guidelines. Avoid posting to excessive numbers of groups back-to-back to prevent account restrictions.
- Licensed under the **MIT License** — see [`LICENSE`](LICENSE) for full details.
