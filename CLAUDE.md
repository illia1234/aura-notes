# Notebook

Native Android notes app "Aura" (project folder and package still named Notebook; Kotlin, Jetpack Compose, Material 3, Room, MVVM). Target device: Pixel 8 / 8a.

- Requirements: `docs/PROJECT_INSTRUCTIONS.md`. Behavior spec (Russian): `docs/SPEC.md`. Visual design: `docs/REDESIGN_AURA.md`. v1.1 changes (calendar instead of favorites, note dates, styles, ru/en): `docs/V1_1_PLAN.md`. Later documents override earlier ones.
- Work in the 7 stages listed at the end of `docs/SPEC.md`; each stage ends with `./gradlew assembleDebug` and install on the phone.
- The user is learning Android: explain decisions in Russian, list files before changing them, ask before large architectural changes.
- adb: `C:/Users/illia/AppData/Local/Android/Sdk/platform-tools/adb.exe`; the phone connects over Wi‑Fi (serial starts with `adb-`), an emulator may also be running — always pass `-s`.
- Never use `fallbackToDestructiveMigration`; every Room schema change needs a migration.
- Builds: debug = `com.example.notebook.debug` ("Aura Debug"), release = `com.example.notebook`, signed with `keystore/notebook-release.jks` via `keystore.properties` (both gitignored, never print or commit them). Install/update steps: `docs/INSTALL.md`.
- Release uses R8 (`optimization { enable = true }`); do not add `packageScope` back — it caused an IllegalAccessError crash at startup. After touching serialization or navigation, test the release build too.
- The phone is the user's everyday phone (they may be in another app or a game): never launch the app, tap or take screenshots on it without asking. Do UI checks on the emulator (AVD `Medium_Phone_API_37.0`, can run headless). Installing an update with `adb install -r` is fine at the end of a stage.
