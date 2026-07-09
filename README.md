# 🟡 Pac-Man HD — Run, Chase, Eat It All (Android)

> A native Android remake of the 1980 arcade classic, built with Kotlin and Jetpack Compose — a glowing neon maze, four ghosts with real personalities, and a chase that never lets up.

Pac-Man HD is a native Android game built with **Kotlin** and **Jetpack Compose** — no third-party game engine, no external dependencies. Guide Pac-Man through a 28×28 neon-glowing maze, gobble every dot and power pellet, and outrun four ghosts before they corner you. Each ghost hunts differently — Blinky chases directly, Pinky ambushes ahead of you, Inky flanks unpredictably, and Clyde loses interest up close — and a power pellet flips the hunt for a few precious seconds, turning every ghost into prey.

## 📦 What's Inside

- 🧩 A full 28×28 classic-style maze rendered on a Compose `Canvas`, with glowing walls, pulsing pac-dots and animated power pellets
- 🟡 Smooth, continuously-animated Pac-Man movement (not grid-snapped) with a rotating, chomping mouth that follows the travel direction
- 👻 Four ghosts — Blinky, Pinky, Inky, Clyde — each with authentic chase logic (direct pursuit, ambush, flank, and distance-based shyness) plus scatter/chase phases that alternate over time
- 😱 Power pellets trigger a frightened state — ghosts turn blue and flee, with an escalating 200 → 400 → 800 → 1600 combo for eating them in a row, plus floating score popups
- 👀 Eaten ghosts race home as a pair of eyes and re-emerge from the ghost house after a short respawn
- 🌀 Classic tunnel wraparound on the side corridor, exactly like the arcade original
- ♾️ Endless level progression — clear the maze and the next one begins with faster ghosts, same lives and score
- 🎯 Three difficulty levels — Easy (4 lives), Normal (3 lives), Hard (2 lives) — each tuning ghost speed, frightened duration and release timing
- 🏆 Best score tracked per difficulty, saved on-device
- 🔊 Fully synthesized retro sound effects via a custom `AudioTrack` synth — chomping, power-up siren, ghost-eaten blips, death sequence, level fanfare — no audio files
- 📳 Haptic feedback on power pellets, eating ghosts, dying and clearing a level
- 🇵🇹 🇬🇧 One-tap language toggle between European Portuguese and English
- 📱 ⌨️ On-screen D-pad plus hardware keyboard support (arrows/WASD, Space, P) for Bluetooth keyboards and Chromebooks
- ⏸️ Pause/resume at any time, with a dimmed overlay
- 🎬 An animated intro presentation on launch — Pac-Man chomps across the title screen before handing off to the main menu
- 🖥️ Neon glow styling throughout for an authentic arcade-cabinet feel

## 🛠️ Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=flat&logo=androidstudio&logoColor=white)

## 🏗️ Project Structure

```
DroidPacManHD/
├── settings.gradle.kts / build.gradle.kts   # Gradle project — open this folder in Android Studio
├── gradlew / gradlew.bat                     # Gradle wrapper — no local Gradle install needed
└── app/
    ├── build.gradle.kts                      # App module config (Compose, minSdk 26, compileSdk 36)
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                               # Launcher icon, theme, strings
        └── java/com/vidi/droidpacmanhd/
            ├── MainActivity.kt                 # Activity entry point, wires up Compose content
            ├── game/
            │   ├── GameConfig.kt                # Maze layout, difficulty presets, ghost/phase definitions
            │   ├── Entities.kt                  # Pac-Man/Ghost models and the shared movement engine
            │   ├── GameAI.kt                     # Ghost AI targeting, direction picking, speed tuning
            │   └── GameEngine.kt                 # Game state + tick loop + gameplay events (the simulation)
            ├── sound/SoundEngine.kt              # Synthesized chiptune sound effects over AudioTrack
            ├── haptics/Haptics.kt                # Haptic feedback helpers (Vibrator/VibrationEffect)
            ├── i18n/Localization.kt              # PT/EN strings
            └── ui/
                ├── GameScreen.kt                  # Frame loop, layout, keyboard input
                ├── MazeCanvas.kt                  # Maze/entity rendering (Compose Canvas)
                ├── Hud.kt                          # Score/level/lives/best, lang/sound/pause buttons
                ├── Overlays.kt                     # Start (difficulty picker), pause, game-over, toast
                ├── SplashScreen.kt                 # Intro presentation
                └── DPad.kt                         # On-screen D-pad + held-direction input stack
```

## ⚙️ Game Mechanics

```
New Game (per difficulty):
  lives, ghost speed multiplier, frightened duration = difficulty preset
  maze dots and pellets fully restocked, score = 0, level = 1

Each Frame:
  1. Move Pac-Man along the grid, turning at tile centers when a direction is queued
  2. Move each ghost according to its current state:
       house      → bobs in the ghost house until its release timer fires
       leaving    → paths to the door and out into the maze
       scatter    → retreats toward its own maze corner
       chase      → hunts Pac-Man using its own personality logic
       frightened → flees on random turns while power pellet is active
       eaten      → races home as eyes, then re-enters the house
  3. Eating a dot scores 10pts; a power pellet scores 50pts and frightens every active ghost
  4. Colliding with a frightened ghost eats it (200/400/800/1600 combo); colliding with
     a normal ghost costs a life, unless Pac-Man just respawned (brief invulnerability)
  5. Maze fully cleared → next level, faster ghosts, dots and pellets reset
  6. Lives exhausted → game over, compare score to the saved per-difficulty best
```

## 👻 Ghost Personalities

```
Blinky (red)   — always targets Pac-Man's current tile: a direct, relentless chase
Pinky  (pink)  — targets four tiles ahead of Pac-Man's facing direction: an ambusher
Inky   (cyan)  — targets a point mirrored through Blinky's position: an unpredictable flanker
Clyde  (orange)— chases directly when far away, but retreats to his corner up close
```

## 🚀 How to Run

```bash
# 1. Clone the repository
git clone https://github.com/VidiPT89/DroidPacManHD.git
cd DroidPacManHD

# 2. Open the project folder in Android Studio (it syncs automatically — no
#    generator step needed, the Gradle files are already checked in), or build
#    from the command line:
./gradlew assembleDebug

# 3. Run on an emulator or physical device (API 26+) from Android Studio's
#    Run button, or install the built APK directly:
./gradlew installDebug
```

Requires Android Studio (Koala or newer) and a JDK 17 toolchain (Android Studio bundles one). No third-party dependencies or package managers involved beyond AndroidX/Compose.

## 📝 Notes

- This is the native Android port of [pacmanHD](https://github.com/VidiPT89/pacmanHD), the browser-based Canvas/JavaScript version of the game — same maze, ghost AI and rules, rebuilt from scratch on Jetpack Compose for a native, touch-first experience. It's a sibling to [iPacManHD](https://github.com/VidiPT89/iPacManHD), the SpriteKit-based iOS port.
- Best-score, language and sound preferences are stored with `SharedPreferences`, so they persist between launches.

---

Developed by **David Arsénio Martins**
🌐 [ividi.dev](https://ividi.dev/)
