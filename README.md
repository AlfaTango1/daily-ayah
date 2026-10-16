# Daily Ayah (Аят дня)

An Android app that shows one Quran verse a day, in the app and in a home-screen widget.
Arabic (Uthmani script) with an English (Sahih International) or Russian (Elmir Kuliev) translation, or both.

## Features
- Verse of the day: the same verse for everyone on a given date, changing at midnight
- Resizable home-screen widget with a refresh button; tap it to open the app
- Works offline after the first load (the last verse is cached)
- Random verse, copy, share
- English / Russian / both translations; app interface in English and Russian
- Light and dark mode

## Build and install
1. Install **Android Studio** (latest stable) from developer.android.com/studio.
2. Unzip this folder and choose **File → Open** → select the `DailyAyah` folder.
3. Wait for Gradle sync to finish (first time it downloads the tools, needs internet).
   If Android Studio offers to upgrade the Android Gradle Plugin, accept it.
4. Connect your phone with USB debugging on (Settings → About phone → tap *Build number* 7 times →
   Developer options → USB debugging), then press **Run ▶**.
5. To get an installable file: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   The APK appears in `app/build/outputs/apk/debug/`.

## Add the widget
In the app tap **Add widget to home screen**, or long-press the home screen → Widgets → *Daily Ayah*.

## How it works
| File | Role |
|---|---|
| `data/QuranApi.kt` | Downloads an ayah from api.alquran.cloud (Arabic + both translations in one request) |
| `data/VerseRepository.kt` | Picks the day's verse from the date, caches it, stores the translation setting |
| `work/DailyVerseWorker.kt` | Background job (every 3 h, when online) that refreshes the verse and widgets |
| `widget/VerseWidgetProvider.kt` | Draws the widget |
| `MainActivity.kt` | Main screen |

To change translations, edit `ED_ENGLISH` / `ED_RUSSIAN` in `QuranApi.kt`
(list of options: https://api.alquran.cloud/v1/edition/language/ru, `/en`, `/uz`).

## Before publishing on Google Play
- Change `applicationId` in `app/build.gradle.kts` to your own unique id.
- Create a signing key (Build → Generate Signed App Bundle).
- Check the licence terms of the translations you ship.
