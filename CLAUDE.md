# Notebook

Native Android notes app (Kotlin, Jetpack Compose, Material 3, Room, MVVM). Target device: Pixel 8 / 8a.

- Requirements: `docs/PROJECT_INSTRUCTIONS.md`. UI/behavior spec (Russian): `docs/SPEC.md`. Anything not in the spec is out of scope for v1.
- Work in the 7 stages listed at the end of `docs/SPEC.md`; each stage ends with `./gradlew assembleDebug` and install on the phone.
- The user is learning Android: explain decisions in Russian, list files before changing them, ask before large architectural changes.
- adb: `C:/Users/illia/AppData/Local/Android/Sdk/platform-tools/adb.exe`; the phone connects over Wi‑Fi (serial starts with `adb-`), an emulator may also be running — always pass `-s`.
- Never use `fallbackToDestructiveMigration`; every Room schema change needs a migration.
