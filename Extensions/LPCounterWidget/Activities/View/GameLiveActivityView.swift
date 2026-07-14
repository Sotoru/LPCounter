#if os(iOS)
// Extensions/LPCounterWidget/Widgets/GameWidget/View/GameLiveActivityView.swift
import SwiftUI
import ActivityKit
import WidgetKit

struct GameLiveActivityView: View {
    let context: ActivityViewContext<GameLiveAttributes>
    
    var body: some View {
        VStack(spacing: 8) {
//            Text(context.attributes.gameName)
//                .font(.headline)
            
            HStack {
                VStack {
                    Text("Player 1")
                        .font(.caption)
                    Text("\(context.state.firstPlayerLp)")
                        .font(.title)
                        .fontWeight(.bold)
                }
                
                Spacer()
                
                VStack {
                    Text("Player 2")
                        .font(.caption)
                    Text("\(context.state.secondPlayerLp)")
                        .font(.title)
                        .fontWeight(.bold)
                }
            }
            .padding()
        }
        .activityBackgroundTint(Color.black.opacity(0.8))
        .activitySystemActionForegroundColor(Color.white)
    }
}
#endif

