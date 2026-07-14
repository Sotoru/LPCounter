# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
```

Day-to-day work is done in Xcode (Cmd+R to run, Cmd+B to build). There is no test target and no lint config.

## Architecture

**Shared state, not SwiftData.** Despite `import SwiftData`, no model container is used. The single source of truth for life points is `@AppStorage` backed by the App Group `group.it.sotoru.LPCounter`, keys `lifePointsP1` / `lifePointsP2`. Both the app (`GameView`) and the widget extension (`DataService`) read/write these same keys — that's how they stay in sync across process boundaries. Any code touching life points must go through this App Group store.

**Two targets:**
- `LPCounter` — the app. Entry `App/LPCounterApp.swift` → `ContentView` → `GameView`. Feature code lives under `LPCounter/Feat/Game/` (Model/View split).
- `LPCounterWidgetExtension` — under `Extensions/LPCounterWidget/`. Contains the Live Activity, a `GameWidget`, and `FirstPlayerWidget`. Interactive widget buttons fire `HandleGameIntent` (AppKit `AppIntent`), which mutates the shared store via `DataService.handleLifePoints`.

**Keeping widgets & Live Activity fresh.** When LP change in the app, `GameView.onChange` calls `LiveActivityManager.shared.updateActivity(...)` and reloads every widget kind via `WidgetCenter`. The widget kinds are centralized in `GameConstants.widgetKinds` — keep that list in sync with the actual widget `kind` strings (currently `GameWidget`, `FirstPlayerWidget`, `FirstPlayerLargeWidget`). The `AppIntent` path reloads the same kinds after mutating state.

**The `Game` class is only a transport for the Live Activity.** `GameView` builds a throwaway `Game` from the AppStorage values to feed `LiveActivityManager`; it is not the app's state.

**Orientation.** `GameView` adapts portrait/landscape by swapping `VStackLayout`/`HStackLayout` via the custom `.detectDeviceOrientation(isLandscape:)` modifier (`Utils/OrientationManager.swift`). Landscape rotates the second player's card so two players face each other across the device.

**iOS-only APIs are fenced.** ActivityKit / `UIApplication` usage is wrapped in `#if os(iOS)` (see `LiveActivityManager`, `GameView`).

## Shared constants

`GameConstants` in `LPCounter/Feat/Game/Model/GameModel.swift` holds `appGroup`, `initialLifePoints` (8000), and `widgetKinds`. The App Group string is also hardcoded in `DataService.swift` — if it ever changes, update both.

## Docs

Human-facing documentation goes in `Docs/`.
