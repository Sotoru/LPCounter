# CLAUDE.md (android)

Jetpack Compose app, package `it.sotoru.lpcounter`. Behaviour is defined by `../SPEC.md`; read it before changing features.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug   # run from android/
```

## Architecture
- `data/GameRepository` — single source of truth (DataStore): life points **and** history. It takes a `DataStore` plus an `onChange` callback; the `GameRepository(context)` factory wires widgets and the notification. History is stored here so the widget's changes are undoable.
- `data/GameRepository` also stores the starting LP (`startingLifePoints`, SPEC §5): it is the fallback for missing totals and the value resets go back to.
- `data/Settings` — `SettingsRepository` (own DataStore `settings`): confirm reset, auto end, haptics, dim after, LED palette and pixel style (a look change redraws widgets and notification).
- `data/GameRepository` also stores the timer: chosen length, the running round's end and the time left while paused (SPEC §9). `reset()` clears both histories; `clearAll()` wipes game data, not settings.
- `ui/led/Led.kt` — the whole LED style (SPEC §10), port of iOS `Design/LED.swift`: palettes, `PixelStyle`, `LedMatrix` (the grid, anchored to the window via `onGloballyPositioned`), `DotMatrixText` / `DotMatrixNumber` (5×7 font lighting the grid's own dots), `LedText`, `SegmentReadout` (DSEG7 in `res/font`, OFL licence in `assets`), and `ledBitmap` for widgets and the notification. `ui/theme/Theme.kt` is always dark, tinted by the palette.
- `AppNavigation` — root is the Home, no tabs: an enum picks Home / Settings / Game in `AnimatedContent`.
- `feature/home/HomeScreen` — catchphrase on the grid, totals of a duel in progress, DUEL / Resume, die and coin, timer menu.
- `feature/game/` — `GameViewModel` (thin, exposes `GameState`, `Settings` and a reset counter for the lamp test), `GameScreen` (intro, top bar, dimmer, leave dialog), `GameCard` (total or history, swipes `swipeDelta`, effects), `CalculatorSheet`, `CalculatorState`. `feature/chance` — die and coin overlay; `feature/timer` — readout, menu, custom length.
- `feature/settings/SettingsScreen` — full screen from the gear (`TopAppBar`, `Switch`, `DropdownMenu`, `SingleChoiceSegmentedButtonRow`, delete all data).
- `widget/` — Glance widgets (`GameWidget`, `FirstPlayerWidget`) drawn as a matrix bitmap; `notification/LiveScoreNotifier` — ongoing notification with the matrix as big picture (Android's Live Activity).
- Tests are plain JVM: `CalculatorStateTest` (also swipes), `GameRepositoryTest` (real DataStore on a temp file).
