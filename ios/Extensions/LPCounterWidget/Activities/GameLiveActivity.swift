//
//  GameLiveActivity.swift
//  LPCounterWidgetExtension
//

import SwiftUI
import WidgetKit

#if os(iOS)
struct GameLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: GameLiveAttributes.self) { context in
            GameLiveActivityView(context: context)
        } dynamicIsland: { context in
            let p1 = context.state.firstPlayerLp
            let p2 = context.state.secondPlayerLp
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 4) {
                        PlayerLabel(player: 1)
                        LifePointsText(value: p1, size: 30)
                    }
                    .environment(\.ledPitch, 2)
                    .padding(.leading, 6)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    VStack(alignment: .trailing, spacing: 4) {
                        PlayerLabel(player: 2)
                        LifePointsText(value: p2, size: 30)
                    }
                    .environment(\.ledPitch, 2)
                    .padding(.trailing, 6)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    // Who is ahead, at a glance: Player 1's share of the LP left on the table.
                    LifeBar(fraction: Double(p1) / Double(max(1, p1 + p2)))
                        .environment(\.ledPitch, 3)
                        .padding(.horizontal, 6)
                        .padding(.top, 6)
                }
            } compactLeading: {
                compact(p1)
            } compactTrailing: {
                compact(p2)
            } minimal: {
                Image(systemName: "suit.heart.fill").foregroundStyle(LED.lit)
            }
            .keylineTint(LED.lit)
        }
    }

    private func compact(_ value: Int) -> some View {
        Text(verbatim: String(value))
            .font(LED.segmentFont(14))
            .contentTransition(.numericText(value: Double(value)))
            .foregroundStyle(value == 0 ? LED.red : LED.lit)
    }
}

#Preview("Lock Screen", as: .content, using: GameLiveAttributes.preview) {
    GameLiveActivity()
} contentStates: {
    GameLiveAttributes.ContentState.preview
}

#Preview("Compact", as: .dynamicIsland(.compact), using: GameLiveAttributes.preview) {
    GameLiveActivity()
} contentStates: {
    GameLiveAttributes.ContentState.preview
}

#Preview("Expanded", as: .dynamicIsland(.expanded), using: GameLiveAttributes.preview) {
    GameLiveActivity()
} contentStates: {
    GameLiveAttributes.ContentState.preview
}
#endif
