//
//  GameWidgetEntryView.swift
//  LPCounterWidgetExtension
//

import SwiftUI
import WidgetKit

/// Both totals, read only; a tap opens the app (SPEC §6). Small: one above the other like the board;
/// medium: side by side, Player 1 on the left like the landscape board.
struct GameWidgetEntryView: View {
    var entry: SimpleEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        Group {
            if family == .systemMedium {
                HStack(spacing: 0) {
                    total(1, entry.lifePointP1, size: 40)
                    Divider().overlay(LED.unlit).padding(.vertical, 8)
                    total(2, entry.lifePointP2, size: 40)
                }
            } else {
                VStack(spacing: 6) {
                    total(2, entry.lifePointP2, size: 34)
                    Divider().overlay(LED.unlit)
                    total(1, entry.lifePointP1, size: 34)
                }
            }
        }
        .lpWidgetBackground()
    }

    private func total(_ player: Int, _ value: Int, size: CGFloat) -> some View {
        VStack(spacing: 4) {
            PlayerLabel(player: player)
            LifePointsText(value: value, size: size)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview(as: .systemSmall) {
    GameWidget()
} timeline: {
    SimpleEntry(date: .now, lifePointP1: 6500, lifePointP2: 8000)
    SimpleEntry(date: .now, lifePointP1: 0, lifePointP2: 2400)
}

#Preview(as: .systemMedium) {
    GameWidget()
} timeline: {
    SimpleEntry(date: .now, lifePointP1: 6500, lifePointP2: 8000)
}
