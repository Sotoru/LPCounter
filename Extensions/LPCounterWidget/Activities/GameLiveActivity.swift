//
//  LPCounterActivity.swift
//  LPCounterWidgetExtension
//
//  Created by paolo laddomada on 10/12/25.
//

import SwiftUI
import WidgetKit

#if os(iOS)
struct GameLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: GameLiveAttributes.self) { context in
            // Lock Screen view
            GameLiveActivityView(context: context)
        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded
                DynamicIslandExpandedRegion(.leading) {
                    VStack {
                        Text("P1")
                            .font(.caption)
                        Text("\(context.state.firstPlayerLp)")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    VStack {
                        Text("P2")
                            .font(.caption)
                        Text("\(context.state.secondPlayerLp)")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("TEST TO DO")
                        .font(.caption)
                }
            } compactLeading: {
                Text("\(context.state.firstPlayerLp)")
                    .fontWeight(.bold)
            } compactTrailing: {
                Text("\(context.state.secondPlayerLp)")
                    .fontWeight(.bold)
            } minimal: {
                Text("LP")
            }
        }
    }
}


#Preview("GameLiveActivity", as: .dynamicIsland(.compact), using: GameLiveAttributes.preview) {
    GameLiveActivity()
} contentStates: {
    GameLiveAttributes.ContentState.preview
}


#Preview("GameLiveActivity", as: .dynamicIsland(.expanded), using: GameLiveAttributes.preview) {
    GameLiveActivity()
} contentStates: {
    GameLiveAttributes.ContentState.preview
}
#endif
