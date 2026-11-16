#if os(iOS)
import SwiftUI
import ActivityKit
import WidgetKit

/// Lock Screen / banner: both totals on the app's LED matrix, Player 1 on the left like the landscape board.
struct GameLiveActivityView: View {
    let context: ActivityViewContext<GameLiveAttributes>

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                DotMatrixText(text: "LP·COUNTER")
                    .accessibilityLabel(Text(verbatim: "LP Counter"))
                Spacer()
                Text(context.state.lastUpdate, style: .time)
                    .led(13, color: LED.dim, glow: false)
            }
            HStack(alignment: .bottom) {
                total(1, context.state.firstPlayerLp, alignment: .leading)
                Spacer()
                Text(verbatim: "VS")
                    .led(14, color: LED.dim, glow: false)
                    .padding(.bottom, 12)
                Spacer()
                total(2, context.state.secondPlayerLp, alignment: .trailing)
            }
        }
        .padding(16)
        .background { LEDMatrixBackground() }
        .environment(\.ledPitch, 3)
        .environment(\.colorScheme, .dark)
        .activityBackgroundTint(LED.background)
        .activitySystemActionForegroundColor(.white)
    }

    private func total(_ player: Int, _ value: Int, alignment: HorizontalAlignment) -> some View {
        VStack(alignment: alignment, spacing: 4) {
            PlayerLabel(player: player)
            LifePointsText(value: value, size: 36)
        }
    }
}
#endif
