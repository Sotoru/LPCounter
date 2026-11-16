//
//  SmallSizeView.swift
//  LPCounterWidgetExtension
//

import SwiftUI
import WidgetKit
import AppIntents

/// Player 1's total with − / + 1000 (SPEC §6). The buttons run `HandleGameIntent`, which goes through the
/// same rules as the app, so the change lands in the history and can be undone there.
struct SmallSizeView: View {
    var entry: FirstPlayerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if family == .systemMedium {
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    PlayerLabel(player: 1)
                    LifePointsText(value: entry.lifePointP1, size: 44)
                    LifeBar(fraction: progress).padding(.top, 8)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(spacing: 10) { buttons }
                    .frame(width: 110)
                    .controlSize(.large)
            }
            .padding(.horizontal, 4)
        } else {
            VStack(spacing: 6) {
                PlayerLabel(player: 1)
                LifePointsText(value: entry.lifePointP1, size: 36)
                HStack(spacing: 10) { buttons }
            }
        }
    }

    private var progress: Double {
        min(1, Double(entry.lifePointP1) / Double(max(1, WidgetStyle.startingLifePoints)))
    }

    @ViewBuilder
    private var buttons: some View {
        step("Subtract 1000", symbol: "minus", subtract: true, color: LED.red)
        step("Add 1000", symbol: "plus", subtract: false, color: LED.green)
    }

    /// Both buttons the same box: "minus" is a shorter glyph than "plus", so without a fixed height the
    /// two capsules come out different and misaligned.
    private func step(_ title: LocalizedStringKey, symbol: String, subtract: Bool, color: Color) -> some View {
        Button(intent: HandleGameIntent(lp: 1000, isSubtract: subtract)) {
            Image(systemName: symbol)
                .font(.body.weight(.bold))
                .frame(maxWidth: .infinity, minHeight: 20, maxHeight: 20)
        }
        .tint(color)
        .accessibilityLabel(title)
    }
}

#Preview(as: .systemSmall) {
    FirstPlayerWidget()
} timeline: {
    FirstPlayerEntry(date: .now, lifePointP1: 6500)
    FirstPlayerEntry(date: .now, lifePointP1: 0)
}

#Preview(as: .systemMedium) {
    FirstPlayerWidget()
} timeline: {
    FirstPlayerEntry(date: .now, lifePointP1: 6500)
}
