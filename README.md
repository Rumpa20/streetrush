# AR NIT STREET RUSH

A complete, original 3D endless runner for Android. Built with **pure Java + the Android SDK + a hand-written OpenGL ES 2.0 mini-engine** - zero external libraries, zero binary assets (all art, textures, audio and the font are generated procedurally at runtime).

- Package: `online.arnitcreation.streetrush`
- Version: 1.0.0 (minSdk 21, target/compileSdk 34)
- Orientation: portrait, fullscreen immersive
- 100% offline, no permissions except `VIBRATE`
- Designed and optimised for **mobile-only development with CodeOnTheGo**

---

## 1. Why this technology (engine evaluation)

| Option | Verdict | Why |
|---|---|---|
| Unity / Godot | Rejected | Requires a desktop editor or heavy export tooling; not buildable on a phone. |
| LibGDX | Rejected | Adds ~20 MB of jars plus per-CPU native `.so` files that must be downloaded by Gradle on the device; heavy, fragile, and mostly unnecessary for a runner. |
| Canvas 2D | Rejected | No true 3D. |
| **Native Java + OpenGL ES 2.0 (chosen)** | Chosen | Compiles anywhere any Android app compiles (including CodeOnTheGo's on-device ARM Gradle), tiny APK (~2 MB), no dependencies to download, full control of 3D performance, and the whole project is plain text files that are easy to edit on a phone. |

The game uses a Crossy-Road-style low-poly look: everything is built from coloured cubes/prisms, so it runs fast on budget phones while still looking stylised and colourful.

## 2. Feature list

- 3-lane auto-running world with swipe controls: left/right lane change, up = jump, down = slide; tap = nothing, double-tap = hoverboard; Back button handled properly
- Jump physics, slide timer, ground detection, lane smoothing, invulnerability blink, fair obstacle spawning (never an impossible row)
- 6 obstacle families: low barriers (jump), overhead bars (slide), crate walls (lane change), parked/moving vehicles, long trains, ground gaps (jump)
- 5 themes that blend smoothly as you travel: Neon City, Indian Street, Sunset Highway, Futuristic Metro, Jungle Run
- Chunked world: 40-unit sections generated ahead and recycled behind the player with fixed-capacity object pools (no GC churn)
- Coins in lines, arcs, zigzag and risk-reward formations; persistent coin balance
- 6 power-ups (Magnet, Jetpack, Super Jump, X2 Score, Shield, Speed Boost), each with icon, duration, HUD bar, sound and a 3-level upgrade path
- 6 unlockable characters, 4 hoverboards, 4 cosmetic trails (coins only, two-step purchase confirm)
- Hoverboard system: activation via double-tap or HUD button, timed duration, break-on-crash protection, cooldown
- 12 missions + 8 achievement badges, persisted with reward payouts
- 7-day daily rewards with streak reset logic (cannot be farmed by restarting)
- Full UI: splash, menu, HUD, pause, game over with 5-second coin revive, characters, shop, missions/badges, daily, settings, privacy, credits
- Settings: music, SFX, vibration, graphics quality (Auto/High/Low), FPS (60/30), swipe sensitivity, volume 25% steps, reset with confirmation
- Procedural audio: 9 synthesized sound effects + a chiptune music loop (AudioTrack) - no audio files needed
- Visual FX: particles (dust, coin sparks, crashes, power-up aura), screen shake, boost speed lines, fog, glow windows, moving headlights
- Save system: one JSON blob in SharedPreferences, written after every run, every purchase, every settings change, and on app pause

## 3. Project structure

```
settings.gradle                Gradle settings (standard, unmodified)
build.gradle                   root build file (AGP 8.5.2)
gradle.properties              JVM args + flags
gradle/wrapper/gradle-wrapper.properties
app/build.gradle               app module (namespace, SDK levels)
app/proguard-rules.pro
app/src/main/
    AndroidManifest.xml
    java/online/arnitcreation/streetrush/
        MainActivity.java      fullscreen activity host
        StreetRushGame.java    game class: screens, HUD, menus
        engine/                mini OpenGL engine (Cfg, ShaderLib, Mesh, Primitives,
                               Camera3D, MatrixStack, Font, Textures, UiBatch, Particles)
        input/Touch.java       swipe/tap gesture analysis
        audio/Audio.java       procedural SFX + music synthesis
        world/                 Themes, Obstacle, Chunk, World, ChunkDraw
        game/                  Player, Humanoid
        meta/                  Save, Catalog, Missions, Achievements, Daily
    res/                       icons, splash, themes, colors, strings
```

## 4. Building with CodeOnTheGo (phone only)

### Option A - open as a project (try this first)

1. Move or clone this project so CodeOnTheGo can see the folder (from GitHub: use the app's clone/import feature, or download the ZIP and extract it).
2. Open the **project root folder** (the one containing `settings.gradle`), not the `app` folder.
3. Let Gradle sync, then **Build APK** (usually `Build > Assemble Debug`).
4. Install the APK from `app/build/outputs/apk/debug/` (allow "Install from this app" when asked), or use `adb install` via a terminal app if available.

### Option B - if the AGP version fails to resolve

CodeOnTheGo bundles its own Android Gradle Plugin (newer builds default to AGP 9.x). If the sync fails with a message like *"com.android.application 8.5.2 not found"*, do one of these, all doable on the phone:

1. **Change the version number:** open `build.gradle` (root) and replace `8.5.2` with the AGP version CodeOnTheGo reports (e.g. the version it uses in one of its own templates). Keep everything else.
2. **Template merge (most reliable):** create a fresh empty project inside CodeOnTheGo with package **`online.arnitcreation.streetrush`**, then copy this repo's files over the template's `app/src/main/java/`, `app/src/main/res/`, and `AndroidManifest.xml`, then build. All source files are plain text with no external dependencies, so this always works.
3. If asked whether to use a wrapper, let CodeOnTheGo manage Gradle itself - `gradle/wrapper/gradle-wrapper.properties` is provided but the app can ignore it.

### Notes for phone builds

- `gradle.properties` caps the Gradle JVM at 1536 MB heap so on-device builds do not run out of RAM. If a build dies mid-compile, close background apps and retry.
- Java level 11 is used (`compileOptions`) for maximum toolchain compatibility.
- **There are no libraries to import**: `dependencies {}` is intentionally empty and the game intentionally avoids AndroidX, so no Maven downloads are needed beyond the Android Gradle Plugin itself - important for offline builds.

## 5. How to play

- Swipe LEFT/RIGHT: change lane
- Swipe UP: jump (over barriers, cones, gaps)
- Swipe DOWN: slide (under overhead bars)
- Double-tap (or BOARD button): activate the equipped hoverboard
- Pause button or Back key: pause
- Back key on menus: go back / confirm exit

Coins purchase characters, hoverboards, trails and power-up upgrades in the shop. Every run earns coins; the daily calendar and missions pay bonuses.

## 6. Assets

None required. All textures (logo, coin, power-up icons, rounded UI shapes), the glyph font, every sound effect and the music loop are procedurally generated at startup. The launcher icon lives in `app/src/main/res/mipmap-*` and `drawable/ic_launcher_foreground.xml`; the splash is a vector drawable plus `SplashTheme` - no image files to source or license.

## 7. Tuning cheat-sheet

Most feel-of-game constants are in one file: `app/src/main/java/online/arnitcreation/streetrush/engine/Cfg.java` (lanes, speeds, jump/gravity, fog, chunk size). Character/shop data is in `meta/Catalog.java`; missions in `meta/Missions.java`; daily rewards in `meta/Daily.java`. Change values, rebuild, done.

## 8. Licensing

Original code and assets created for AR NIT CREATION. Game mechanics are inspired by but do not copy any existing product's characters, artwork, logos, maps or audio.
