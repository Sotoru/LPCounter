# CLAUDE.md (android)

Jetpack Compose app, package `it.sotoru.lpcounter`. Behaviour is defined by `../SPEC.md`; read it before changing features.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug   # run from android/
```

## Architecture
- `data/GameRepository` — single source of truth (DataStore): life points **and** history. It takes a `DataStore` plus an `onChange` callback; the `GameRepository(context)` factory wires widgets and the notification. History is stored here so the widget's changes are undoable.
- `feature/game/` — `GameViewModel` (thin, exposes `GameState`), `GameScreen`, `GameCard`, `CalculatorSheet`, `CalculatorState`.
- `widget/` — Glance widgets (`GameWidget`, `FirstPlayerWidget`); `notification/LiveScoreNotifier` — ongoing notification (Android's Live Activity).
- Tests are plain JVM: `CalculatorStateTest`, `GameRepositoryTest` (real DataStore on a temp file).
