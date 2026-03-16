# Development Guide

## Environment

- Android Studio Koala or newer is recommended (AGP 8.13, Kotlin 2.2).
- JDK 21 is required (`compileOptions`/`jvmTarget` are set to 21).
- Android SDK 36; `minSdk` is 34 (Android 14), so use an emulator/device on Android 14+.

## Building and running

- IDE: open the project in Android Studio, let Gradle sync, then press **Run** with an Android 14+ device selected.
- CLI: run `./gradlew assembleDebug` to build; use `./gradlew installDebug` with a connected device/emulator to deploy.
- The Compose launcher (`HomeActivity`) starts first; it forwards the selected difficulty to `MainActivity`.
- If Gradle picks JDK 25 from your shell environment, force JDK 21 before running CLI tasks. On macOS, `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` is the safest option.

## Testing

- JVM tests live under `app/src/test` and run with `./gradlew test`.
- Instrumented/device tests live under `app/src/androidTest` and run with `./gradlew connectedAndroidTest`.
- Current JVM coverage includes restore/endgame regressions around `SudokuViewModel`; logic-heavy additions should keep favoring local JVM tests where possible.

## Code style and patterns

- Kotlin uses the official style; Java follows standard Android/Jetpack conventions.
- UI logic should stay in Activities/Composables, while validation, scoring, and history live in `SudokuViewModel` and `SudokuBoard`.
- Keep long-running work (puzzle generation or validation copies) off the main thread; post results back through LiveData.

## Troubleshooting

- Puzzle generation is cancelable; if you see slow starts when switching difficulties rapidly, verify that new game requests cancel the previous `Future`.
- Timer issues usually come from missing `startTimerIfNotRunning()` calls after restores—check `restoreState` and `checkGameStatus`.
- If highlights look misaligned after layout changes, ensure the grid container width is non-zero before computing `cellSize`.
