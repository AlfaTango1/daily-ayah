# Daily Ayah (Аят дня)

An Android app that shows a Quran verse on your home screen and renews it automatically
(every hour by default). Arabic (Uthmani script) with Uzbek, Russian and English translations.
Interface in Uzbek, Russian and English. Works fully offline.

## Translation sources
| Language | Translation | Notes |
|---|---|---|
| Arabic | Uthmani text, Tanzil Project (tanzil.net) | |
| Uzbek | Shaykh Muhammad Sodiq Muhammad Yusuf (1952–2015) | Cyrillic original; Latin version converted automatically |
| Russian | Elmir Kuliev | |
| English | Saheeh International (Jeddah, 1997) | |

All texts come from the Al-Quran Cloud API (`uz.sodik`, `ru.kuliev`, `en.sahih`, `quran-uthmani`).
Uzbekistan has no single legally designated "official" translation; the app explains this on its
*About translations* screen.

## Features
- **New verse every hour** — or every 30 min / 3 h / 6 h / once a day (Settings in the app)
- **Four widget types**: Verse (Arabic + translation), Verse small (translation), Arabic calligraphy, My dhikr (your own text)
- **Every widget is editable**: tap ✎ on the widget (or *Edit* in the app) to change
  - content (Arabic, translation, both, or your own text)
  - 8 ready-made styles (Samarkand, Bukhara gold, Mihrab, Night prayer, Nastaliq, Ruby, Minimal, Paper)
  - 19 Arabic fonts (Amiri, Amiri Quran, Scheherazade, Naskh, Nastaliq, Ruqaa, Kufi styles and more)
  - 17 translation fonts (Lora, Merriweather, PT Serif, EB Garamond, Playfair, Montserrat, Marck Script…)
  - Islamic geometric patterns (8-point star, rosette, zellige, arabesque, lattice, dots) with adjustable strength
  - frames (line, double, ornate corners, mihrab arch), text shadow, line spacing, verse-number mark ۝
  - text sizes, bold, italic, alignment
  - 16 colour themes, background opacity, corner roundness
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
