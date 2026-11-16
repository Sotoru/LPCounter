//
//  WidgetView.swift
//  LPCounterWidgetExtension
//

import SwiftUI
import WidgetKit

/// Player 1 widget for every family: Home Screen with buttons, Lock Screen read only.
struct WidgetView: View {
    var entry: FirstPlayerEntry
    @Environment(\.widgetFamily) private var widgetFamily

    var body: some View {
        switch widgetFamily {
        case .systemSmall, .systemMedium:
            SmallSizeView(entry: entry)
                .buttonStyle(.bordered)
                .buttonBorderShape(.capsule)
                .lpWidgetBackground()

        case .accessoryInline:
            Label {
                Text(verbatim: "LP1 \(entry.lifePointP1)")
            } icon: {
                Image(systemName: "heart.fill")
            }
            .containerBackground(for: .widget) { Color.clear }

        case .accessoryCircular:
            Gauge(value: Double(entry.lifePointP1), in: 0...Double(max(entry.lifePointP1, WidgetStyle.startingLifePoints))) {
                Text(verbatim: "LP1")
            } currentValueLabel: {
                Text(verbatim: entry.lifePointP1 >= 10_000 ? "\(entry.lifePointP1 / 1000)k" : "\(entry.lifePointP1)")
                    .font(LED.segmentFont(11))
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .containerBackground(for: .widget) { AccessoryWidgetBackground() }

        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: "LP1").led(12, color: .secondary, glow: false)
                Text(verbatim: String(entry.lifePointP1))
                    .font(LED.segmentFont(22, relativeTo: .title2))
                    .widgetAccentable()
                Gauge(value: min(1, Double(entry.lifePointP1) / Double(max(1, WidgetStyle.startingLifePoints)))) { EmptyView() }
                    .gaugeStyle(.accessoryLinearCapacity)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .containerBackground(for: .widget) { Color.clear }

        default:
            EmptyView()
        }
    }
}
