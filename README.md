# Daily Ayah (Аят дня)

An Android app that shows a Quran verse on your home screen and renews it automatically
(every hour by default). Arabic (Uthmani script) with an English (Sahih International)
or Russian (Elmir Kuliev) translation, or both. Works fully offline.

## Features
- **New verse every hour** — or every 30 min / 3 h / 6 h / once a day (Settings in the app)
- **Four widget types**: Verse (Arabic + translation), Verse small (translation), Arabic calligraphy, My dhikr (your own text)
- **Every widget is editable**: tap ✎ on the widget (or *Edit* in the app) to change
  - content (Arabic, translation, both, or your own text)
  - Arabic font (Amiri, Scheherazade, Noto Naskh, Lateef, Noto Kufi, Reem Kufi, Ruqaa)
  - translation font (Sans, Serif, Light, Medium, Condensed, Mono, Casual, Cursive)
  - text sizes, bold, italic, alignment
  - 10 colour themes, background opacity, corner roundness
  - surah name and reference on/off
- Tap ↻ on a widget for another verse; tap the widget to open it in the app
- In the app: browse previous/next verses, random verse, copy, share; its own font/style settings
- English and Russian interface, light and dark mode

## Download
Every push to `main` is built by GitHub Actions; the APK is attached to the newest entry under **Releases**.

## Build it yourself
1. Install Android Studio, then **File → Open** this folder.
2. Press **Run ▶** (first build needs internet: it downloads the Quran text and fonts into `app/src/main/assets`).

## How it works
| File | Role |
|---|---|
| `app/build.gradle.kts` | `downloadQuranAssets` task: fetches the Quran text (api.alquran.cloud) and fonts (Google Fonts) before the build |
| `data/QuranData.kt` | Reads the bundled text; verse lookup by number |
| `data/VerseSchedule.kt` | Which verse is shown in the current hour/period |
| `widget/WidgetRenderer.kt` | Draws a widget (fonts, colours, auto-fit text) into an image |
| `widget/Widgets.kt`, `WidgetKind.kt` | The four widget types |
| `widget/RenewScheduler.kt` | Alarm at the start of each period to refresh all widgets |
| `WidgetEditorActivity.kt` | Style editor with live preview (widgets and app) |
| `MainActivity.kt` | Main screen |

## Before publishing on Google Play
- Change `applicationId` in `app/build.gradle.kts` to your own unique id and sign a release build.
- Check the licence terms of the translations you ship.
