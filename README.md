# Attendance Tracker

An Android app that helps students stay above their attendance target (the classic
**75% rule**). Upload a photo of your timetable and your college's holiday sheet —
on-device AI (Google Gemini) reads them into a structured schedule — then mark each
class present / absent / suspended and watch a live read-out of exactly how many
classes you can still skip, or must attend, to stay on target.

> Built with Kotlin, Jetpack Compose, and Room. Bring your own free Gemini API key.

## Features

- **AI timetable & holiday import** — snap a picture (or PDF) of your timetable or
  holiday list; Gemini extracts classes (name, day, time) and holidays automatically.
- **Daily checklist** — per-day list of scheduled classes; mark each Attended / Absent.
- **Live 75% math** — see your current percentage plus *"attend N more in a row"* or
  *"you can skip S more"* to hit your goal. Target is configurable (default 75%).
- **Suspensions** — cancel a single session so it's excluded from your percentage.
- **Holidays** — auto-imported or added manually; excluded from attendance.
- **Calendar month view** — color-coded overview of attended / absent / holiday days.
- **Bulk tools** — mass-mark a date range, or clear a whole month of logs.
- **Local-first & private** — all data stays in an on-device Room database. The only
  network call is to the Gemini API, using *your* key.

## Install (no build required)

Grab the latest signed APK from the [**Releases**](https://github.com/SushreeSudiptaJena/Attendance-Tracker/releases)
page and open it on your phone. You may need to allow "install from unknown sources".

After installing, open the app and tap the **key icon** (top-right) to paste a free
Gemini API key from [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
The key is stored only on your device and is never bundled into the app.

## Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM (ViewModel + StateFlow) |
| Persistence | Room |
| Networking | OkHttp |
| AI | Google Gemini REST API |
| Build | Gradle 9.3 + AGP 9.1, JDK 17+ |

## Build from source

**Prerequisites:** JDK 17+ and the Android SDK (platform 36, build-tools 36).
Android Studio installs both; the project's Gradle wrapper handles the rest.

```bash
git clone https://github.com/SushreeSudiptaJena/Attendance-Tracker.git
cd Attendance-Tracker

# Point Gradle at your SDK (or let Android Studio create this on import)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug      # outputs app/build/outputs/apk/debug/app-debug.apk
```

No API key is needed to build — users supply it at runtime in the app.

## Building a signed release

Release builds read signing credentials from a gitignored `keystore.properties`
(or environment variables in CI). To make your own signed build:

```bash
# 1. Generate a keystore (run in a real terminal so the password prompt works)
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias upload

# 2. Create keystore.properties from the template and fill in your passwords
cp keystore.properties.template keystore.properties

# 3. Build
./gradlew assembleRelease     # outputs app/build/outputs/apk/release/app-release.apk
```

Never commit `keystore.properties` or the `.jks` file — both are gitignored.

## License

Released under the [MIT License](LICENSE).

---

Made by [Sushree Sudipta Jena](https://github.com/SushreeSudiptaJena).
