//
//  WidgetStyle.swift
//  LPCounterWidgetExtension
//
//  The app's LED display outside the app (SPEC §10): black, the unlit matrix behind, totals dot by dot.
//  Built from the app's own `LED.swift`, compiled into this target too.
//

import SwiftUI
import WidgetKit

enum WidgetStyle {
    /// The starting LP a duel resets to, for gauges.
    static var startingLifePoints: Int {
        let defaults = UserDefaults(suiteName: GameConstants.appGroup)
        return defaults?.object(forKey: GameConstants.startingLifePointsKey) as? Int ?? GameConstants.initialLifePoints
    }
}

/// A total on the widget's grid like the app's; red at 0. `size` caps its height.
struct LifePointsText: View {
    let value: Int
    var size: CGFloat = 40

    var body: some View {
        DotMatrixNumber(value: Double(value), color: value == 0 ? LED.red : LED.lit, glow: false)
            .frame(maxHeight: size)
            .widgetAccentable()
    }
}

/// How much of the starting LP is left, as a row of lit and unlit dots.
struct LifeBar: View {
    let fraction: Double
    @Environment(\.pixelStyle) private var pixels
    @Environment(\.ledPitch) private var pitch

    var body: some View {
        // One row of the grid, lit from the left.
        GridCanvas { gc, size, o, p in
            let n = Int((size.width - o.x) / p)
            let on = Int((min(1, max(0, fraction)) * Double(n)).rounded())
            var lit = Path()
            for i in 0..<on { lit.addPath(pixels.path(in: LED.cell(i, 0, o, p))) }
            gc.fill(lit, with: .color(fraction > 0.25 ? LED.lit : LED.red))
        }
        .frame(height: pitch * 2)
        .widgetAccentable()
        .accessibilityHidden(true)
    }
}

/// "LP1" / "LP2" in the dot font.
struct PlayerLabel: View {
    let player: Int

    var body: some View {
        Text(verbatim: "LP\(player)").led(13, color: LED.dim, glow: false)
    }
}

extension View {
    /// The matrix as widget background; in tinted or clear Home Screens the system replaces it.
    func lpWidgetBackground() -> some View {
        containerBackground(for: .widget) { LEDMatrixBackground() }
            .environment(\.colorScheme, .dark)
            // Widgets are small: a finer grid so a total still fits at full 5×7 resolution.
            .environment(\.ledPitch, 3)
    }
}
