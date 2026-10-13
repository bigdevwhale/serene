<div align="center">

<img src="docs/logo.svg" width="96" alt="Serene logo" />

# Serene

### A quiet place for your mind

*Guided meditations and visualizations read aloud, affirmations, a vision board, breathing and soft soundscapes. Free, no ads, no premium, no account, works offline.*

[![Android CI](https://github.com/bigdevwhale/serene/actions/workflows/android.yml/badge.svg)](https://github.com/bigdevwhale/serene/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/bigdevwhale/serene?color=6B5BD2&label=download&logo=android)](https://github.com/bigdevwhale/serene/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-6B5BD2?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Languages](https://img.shields.io/badge/lang-EN%20%7C%20RU-E39A7B)
[![License: MIT](https://img.shields.io/badge/license-MIT-ECE8FB)](LICENSE)

**English** · [Русский](README.ru.md)

</div>

---

## The idea

Calm is a skill, and like any skill it grows with a few minutes of practice a day.

Serene gives those minutes a gentle shape: a kind thought in the morning, a breath when everything is too much, a guided journey to a quiet place, a moment to picture the life you want, and a few lines about what went well before sleep. Meditation apps usually lock most of this behind a subscription. Here everything is unlocked, there is nothing to sign up for, and nothing leaves your phone.

## What's inside

| | Feature | How it works |
|---|---|---|
| 🧘 | **10 guided meditations** | Calm in 3 minutes, breath awareness, body scan, loving-kindness, letting go, inner harmony, gratitude, morning intention, self-compassion and a wind-down for sleep. |
| 🌅 | **10 guided visualizations** | Your safe place, a forest walk, the ocean shore, a mountain lake, healing light, meeting your future self, releasing worries, an inner garden, a goal come true and a starry sky. |
| 🗣️ | **A calm narrator, offline** | Every meditation, visualization and affirmation is narrated by a natural neural voice, recorded ahead of time and shipped with the app, so it works without internet. You can switch to your phone's own voice in settings. |
| 🌊 | **17 soundscapes** | Real recordings of rain, a thunderstorm, the ocean, a stream, wind, forest birds, a summer night, a fireplace, a boat, a city, a café, a train, white and pink noise, played as endlessly reshuffled segments so they never audibly loop; plus generative soft music, a warm tone and deep noise. Pick any of them, and the volume, inside every session. A singing bowl opens and closes each one. |
| ✨ | **240 affirmations** | Twelve themes from calm and self-love to courage and restful sleep. Favourites, your own affirmations, an affirmation of the day, and a daily mix read aloud with room to repeat each one. |
| 🖼️ | **Vision board** | Add what you want to bring into your life with a photo, how it looks and feels, and a line to say to yourself. *Visualize my board* turns it into a personal guided session. |
| 🎙️ | **Your own voice** | For your own affirmations and visions, record yourself in the app or upload an audio file, or let the phone read your words. |
| 🌬️ | **Breathing** | Calming 4-6, box breathing, 4-7-8, coherent breathing, the physiological sigh and equal breathing, with a glowing orb, soft tones and a light vibration at each change. |
| ⏳ | **Silent meditation timer** | 3 to 60 minutes, any soundscape, an optional interval bell. |
| 📔 | **Journal** | Mood check-ins, three good things, notes, a two-week mood chart, a practice calendar, streaks and minutes. |
| ☯️ | **Wheel of balance** | Rate body, mind, relationships, work, rest and joy; see where life feels full and where it asks for care, against your previous check. |
| 🔔 | **Gentle reminders** | A morning affirmation and an evening invitation to unwind, at times you choose, plus a home-screen widget with the affirmation of the day. |

## English & Русский

Every screen, meditation, visualization and affirmation is written natively in both languages, not machine-translated. The Russian texts are gender-neutral, so they fit everyone. Switch in **Settings → Language**; on Android 13+ the app also shows up in the system's per-app language settings.

Light and dark themes; sessions always play on a dark, slowly moving scene.

## Download

Get the APK from **[Releases](https://github.com/bigdevwhale/serene/releases/latest)**, or the debug APK attached to any [CI run](https://github.com/bigdevwhale/serene/actions/workflows/android.yml).
Android 8.0 (API 26) or newer.

> [!TIP]
> For the voice, the phone needs an offline text-to-speech voice for your language (Google Speech Services usually has one). **Settings → Voice → Speech settings** opens the right screen.
> On Xiaomi (MIUI / HyperOS), Huawei and some other phones, allow **Autostart** so reminders arrive on time.

## Privacy

No accounts, no servers, no analytics, no ads, no internet permission. The journal, favourites and visions live in a local database on the phone; vision photos are copied into the app's private storage.
**Settings → Backup** exports everything (except photos) to a JSON file you keep.

## Under the hood

- **Kotlin + Jetpack Compose**, Material 3 with a custom palette, the Inter and Lora typefaces.
- **Soundscapes**: recordings from [Blanket](https://github.com/rafaelmardojai/blanket), prepared by `tools/ambience.py`, decoded once and mixed with the generative sounds in one `AudioTrack` stream (`audio/Soundscape.kt`, `audio/Recording.kt`); the singing bowl is a set of slowly beating partials (`audio/Chimes.kt`).
- **Narration** recorded with open neural voices by `tools/narrate.py` ([Silero](https://github.com/snakers4/silero-models) for Russian, [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) for English) and shipped as Opus files keyed by a hash of each line; the phone's `TextToSpeech` speaks anything without a recording. The soundscape ducks while the voice speaks.
- Animated scenes (dawn, ocean, forest, night, mountain, meadow, light, space) are drawn on a `Canvas`.
- **Room** for sessions, journal, balance checks, own affirmations, favourites and visions · **DataStore** for settings · **AlarmManager** for reminders.

```
app/src/main/java/app/serene/
├── content/     meditations, visualizations, affirmations (EN + RU), vision-board script
├── audio/       soundscape synth, singing bowl and breath tones, text-to-speech
├── domain/      streaks and stats, breathing patterns (pure Kotlin, unit-tested)
├── data/        Room database, DataStore settings, JSON backup
├── notify/      reminders, boot receiver
├── widget/      affirmation of the day
└── ui/          today · practice · affirmations · journal · visions · players · settings
```

## Build from source

Requirements: JDK 17+ (Android Studio's bundled JBR works) and the Android SDK.

```bash
git clone https://github.com/bigdevwhale/serene.git
cd serene
./gradlew testDebugUnitTest                  # unit tests
./gradlew testDebugUnitTest -Ppreview=wav    # also writes each soundscape as a WAV to listen to
./gradlew installDebug                       # app.serene.dev, next to the real app
./gradlew installDebug -Pdemo                # app.serene.demo with a made-up journal and visions
```

### CI / releases

- **Android CI** (`.github/workflows/android.yml`): unit tests and lint (missing translations fail the build) on every push, and a debug APK.
- **Release** (`.github/workflows/release.yml`): push a tag like `v1.0.0` to build a signed APK and publish a GitHub Release.

## License

[MIT](LICENSE) · Inter and Lora typefaces under the SIL Open Font License 1.1 · Material Symbols under the Apache License 2.0.
Russian narration made with Silero TTS models (CC BY-NC-SA 4.0, non-commercial use) · English narration made with Kokoro-82M (Apache License 2.0).

### Sound credits

Recordings collected and edited by the [Blanket](https://github.com/rafaelmardojai/blanket) project:

| Sound | Author | License |
|---|---|---|
| [Rain](https://freesound.org/people/alex36917/sounds/524605/) | alex36917 | CC BY |
| [Thunderstorm](https://freesound.org/people/digifishmusic/sounds/41739/) | Digifish music | CC BY |
| [Ocean](https://freesound.org/people/Luftrum/sounds/48412/) | Luftrum | CC BY |
| [Stream](https://freesound.org/people/gluckose/sounds/333987/) | gluckose | CC0 |
| [Wind](https://freesound.org/people/felix.blume/sounds/217506/) | felix.blume | CC0 |
| [Forest birds](https://freesound.org/people/kvgarlic/sounds/156826/) | kvgarlic | CC0 |
| [Summer night](https://soundbible.com/2083-Crickets-Chirping-At-Night.html) | Lisa Redfern | Public domain |
| [Fireplace](https://soundbible.com/1543-Fireplace.html) | ezwa | Public domain |
| [Boat](https://freesound.org/people/Falcet/sounds/439365/) | Falcet | CC0 |
| [City](https://freesound.org/people/gezortenplotz/sounds/44796/) | gezortenplotz | CC BY |
| [Café](https://soundbible.com/1664-Restaurant-Ambiance.html) | stephan | Public domain |
| [Train](https://freesound.org/people/SDLx/sounds/259988/) | SDLx | CC BY 3.0 |
| [White noise](https://commons.wikimedia.org/w/index.php?title=File%3AWhite-noise-sound-20sec-mono-44100Hz.ogg) | Jorge Stolfi | CC BY-SA |
| [Pink noise](https://es.wikipedia.org/wiki/Archivo:Pink_noise.ogg) | Omegatron | CC BY-SA |

Serene is for relaxation and well-being. It is not a medical or psychological treatment.
