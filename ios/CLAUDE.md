# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Part of the LPCounter monorepo: behaviour and UI parity with `../android` are defined by `../SPEC.md` (read it first; see "Divergenze da chiudere" for known gaps). Run the commands below from `ios/`.

## What this is

LPCounter is a SwiftUI iOS app: a life-point counter for Yu-Gi-Oh (games start at 8000 LP). It ships an app target plus a widget extension with interactive widgets and a Live Activity.

## Commands

```bash
# Build the app
xcodebuild -project LPCounter.xcodeproj -scheme LPCounter -destination 'generic/platform=iOS' build

# Build for simulator (usable destination)
xcodebuild -project LPCounter.xcodeproj -scheme LPCounter -destination 'platform=iOS Simulator,name=iPhone 16' build

# Build the widget extension
xcodebuild -project LPCounter.xcodeproj -scheme LPCounterWidgetExtension -destination 'platform=iOS Simulator,name=iPhone 16' build

# Run the unit tests (PlayerScore + CalculatorState + GameViewModel, Swift Testing)
xcodebuild -project LPCounter.xcodeproj -scheme LPCounter -destination 'platform=iOS Simulator,name=iPhone 18 Pro' test
```

Day-to-day work is done in Xcode (Cmd+R to run, Cmd+B to build, Cmd+U to test). There is no lint config.

## Architecture

**Shared state, not SwiftData.** Despite `import SwiftData`, no model container is used. The single source of truth is the App Group `group.it.sotoru.LPCounter`: life points under `lifePointsP1` / `lifePointsP2` and the history under `historyP1` / `historyP2` (signed changes, comma separated, max 200). The app (`GameViewModel`) and the widget extension (`DataService`) read/write these same keys — that's how they stay in sync across process boundaries.

**Rules live in `PlayerScore`** (`Feat/Game/Model/GameModel.swift`, compiled into both targets): clamp at 0, record the change actually applied, relative undo, halve, reset. Never mutate life points or history directly; go through `PlayerScore` so the app and the widget behave the same (SPEC §2). `CalculatorState` holds the keypad rules (SPEC §3) and is app-only. App-only preferences (SPEC §5) are `@AppStorage` keys in `AppSettings` (`Feat/Settings/SettingsView.swift`); the starting LP is in `GameViewModel` (`startingLifePoints`) because reset rules need it. Unit tests are in `LPCounterTests/`.

**`GameViewModel`** (`Feat/Game/Model/GameViewModel.swift`, `@Observable`, `@MainActor`) is the iOS counterpart of Android's `GameViewModel` + `GameRepository`: it loads both players from the store, applies `PlayerScore` rules, saves, and calls `onChange` on every write. `ContentView` owns it and wires `onChange` (`GameView.sync`) to the Live Activity and `WidgetCenter`; views below take `model` + `Player` (`.one` / `.two`) and never touch `UserDefaults`. It re-reads the store (`reload()`) when the scene becomes active, since the widget writes while the app is in the background. Tests take an injected `UserDefaults`.

**Two targets:**
- `LPCounter` — the app. Entry `App/LPCounterApp.swift` → `ContentView` (owns `GameViewModel`; one `NavigationStack` rooted at `HomeView`, Settings in a sheet from the gear) → `HomeView` pushes `GameView` (bars hidden). `GameViewModel` also holds the timer and `clearAll()` (SPEC §9). Feature code lives under `LPCounter/Feat/Game/` (Model/View split).
- `LPCounterWidgetExtension` — under `Extensions/LPCounterWidget/`. Contains the Live Activity, a `GameWidget`, and `FirstPlayerWidget`. Interactive widget buttons fire `HandleGameIntent` (AppKit `AppIntent`), which mutates the shared store via `DataService.handleLifePoints`.

**Keeping widgets & Live Activity fresh.** When LP change in the app, `GameViewModel`'s `onChange` (set in `GameView`) calls `LiveActivityManager.shared.updateActivity(...)` and reloads every widget kind via `WidgetCenter`. The widget kinds are centralized in `GameConstants.widgetKinds` — keep that list in sync with the actual widget `kind` strings (currently `GameWidget`, `FirstPlayerWidget`, `FirstPlayerLargeWidget`). The `AppIntent` path reloads the same kinds after mutating state.

**Orientation.** `GameView` adapts portrait/landscape by swapping `VStackLayout`/`HStackLayout` read in a `GeometryReader` in the same layout pass, so the first frame is already right. In portrait Player 2 sits on top, rotated 180°, facing Player 1; in landscape Player 1 is on the left and both read upright, as do the centre controls, which stack into a column along the seam.

**Platforms.** One target, `SDKROOT = auto`: iPhone, iPad (all orientations, Split View / Stage Manager) and native macOS (not Catalyst). Layouts switch on the shape of the space (`width > height`, `onGeometryChange`), never on the device, so phone landscape, iPad, Mac windows and dual-screen spans share one code path. The few APIs that differ are wrapped in `Utils/PlatformModifiers.swift` (`hidesBars`, `disablesSwipeBack`); On iPhone the duel is always landscape: `HomeView.play()` awaits `AppOrientation.lockForDuel()` (rotates first, at most once) before pushing, and the game's Home button awaits `AppOrientation.releaseForHome()` (turns back to how the device is held) before dismissing, so no rotation overlaps the push; the game turns off the swipe back (edge and full-width) so its swipes on the totals win; `unlock()` in `onDisappear` is the fallback; `AppDelegate` reports `AppOrientation.mask` to UIKit. The Home follows the device. `GameCardView` shows the counter; `CounterView` swaps its total for the history in the same fixed-height area with the clock button next to Undo and Halve, on every platform (no paging: swipes on the total change LP, `CounterView.swipeDelta`). Keyboard: Return plays, ⌘, settings, Esc home, ⌘R reset.

**iOS-only APIs are fenced.** ActivityKit / `UIApplication` usage is wrapped in `#if os(iOS)` (see `LiveActivityManager`, `GameView`).

## Look (SPEC §10)

`Design/LED.swift` is the whole style. One grid, anchored to the screen (`GridCanvas` snaps to global coordinates, `ledPitch` / `ledFlipped` in the environment): `LEDMatrixBackground` draws its unlit dots behind every screen and sheet, and `DotMatrixText` / `DotMatrixNumber` / `TypewriterText` light dots of that same grid with a 5×7 font (`DotFont`), so text and background are one matrix. Pitch: 5 pt everywhere in the app (the duel's totals use larger blocks of it), 3 pt in widgets. Small text (`.led(size)`) is system monospaced, too small for the grid; DSEG7 (`SegmentReadout`, `LED.segmentFont`) for readouts. `PixelStyle` (dot shape) is stored in the App Group and passed down the environment (`\.pixelStyle`, set in `ContentView`) so every dot redraws live, sheets included. Always dark, blue tint set in `ContentView`; native controls stay native. No splash: `ContentView` is the Home's `NavigationStack`. DSEG7 is in `Resources/Fonts` (OFL, licence alongside), registered at launch by `LED.registerFonts()` in the app and declared in the widget's `Info.plist` (`UIAppFonts`). `LED.swift` and the font are also members of the widget target.

## Apple Watch companion (SPEC §6b)

Target `LPCounterWatch` (watchOS 26, sources in `Watch/`, embedded in the iOS app only via "Embed Watch Content"). It also compiles `Design/LED.swift`, `Feat/Game/Model/GameModel.swift`, `Utils/WatchMessage.swift`, the DSEG7 font and the string catalog from the app folder (synchronized-group membership exceptions). WatchConnectivity: the iPhone (`Utils/WatchLink.swift`, started in `ContentView`) sends Player 1's total, history, palette and pixel style as the application context on every change (`GameView.sync`); the Watch (`PhoneLink`) sends `delta` / `undo` messages (queued with `transferUserInfo` when the iPhone is unreachable), which the iPhone applies to the App Group store through `PlayerScore`, then reloads the view model, widgets and Live Activity. Build: `xcodebuild -scheme LPCounterWatch -destination 'platform=watchOS Simulator,name=Apple Watch Series 12 (46mm)' build`.

## Shared constants

`GameConstants` in `LPCounter/Feat/Game/Model/GameModel.swift` holds `appGroup`, `initialLifePoints` (8000), and `widgetKinds`. The App Group string is also hardcoded in `DataService.swift` — if it ever changes, update both.

## Localization

String catalogs, `en` source + `it`: `LPCounter/Resources/Localizable.xcstrings` (app) and `Extensions/LPCounterWidget/Resources/Localizable.xcstrings` (widgets, Live Activity). Keys are the English strings; the Italian copy matches `android/.../values-it/strings.xml`. The widget catalog is listed in the `LPCounterWidgetExtension` membership exceptions in `project.pbxproj` (target has no synchronized group of its own) and excluded from the app target, otherwise Xcode errors on two catalogs in one target.

## Docs

Human-facing documentation goes in `Docs/`.
