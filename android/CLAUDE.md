# CLAUDE.md (android)

Jetpack Compose app, package `it.sotoru.lpcounter`. Behaviour is defined by `../SPEC.md`; read it before changing features.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug   # run from android/
```

## Architecture
- `data/GameRepository` — single source of truth (DataStore): life points **and** history. It takes a `DataStore` plus an `onChange` callback; the `GameRepository(context)` factory wires widgets and the notification. History is stored here so the widget's changes are undoable.
- `data/GameRepository` also stores the starting LP (`startingLifePoints`, SPEC §5): it is the fallback for missing totals and the value resets go back to.
- `data/Settings` — `SettingsRepository` (own DataStore `settings`): confirm reset, auto end, haptics, theme.
- `data/GameRepository` also stores recent duels (`recentDuels`, saved by `reset()`), the timer choice and the running round's end (SPEC §9); `clearAll()` wipes game data, not settings.
- `AppNavigation` — root: `NavigationBar` tabs Home / Settings; Play pushes `GameScreen` via `AnimatedContent` + `BackHandler`.
- `feature/home/HomeScreen` — Play, recent duels grouped by day. Starting LP and timer are in Settings.
- `feature/game/` — `GameViewModel` (thin, exposes `GameState` and `Settings`), `GameScreen`, `GameCard`, `CalculatorSheet`, `CalculatorState`.
- `feature/settings/SettingsScreen` — Settings tab (`LargeTopAppBar`, `Switch`, `SingleChoiceSegmentedButtonRow`, delete all data).
- `widget/` — Glance widgets (`GameWidget`, `FirstPlayerWidget`); `notification/LiveScoreNotifier` — ongoing notification (Android's Live Activity).
- Tests are plain JVM: `CalculatorStateTest`, `GameRepositoryTest` (real DataStore on a temp file).
