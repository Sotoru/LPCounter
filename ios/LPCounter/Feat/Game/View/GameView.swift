//
//  GameView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI
import WidgetKit



struct GameView: View {
    @State private var model = GameViewModel(onChange: GameView.sync)
    @Environment(\.scenePhase) private var scenePhase
    @Binding var isLandscape: Bool

    // MARK: - Gradients
    private static let backgroundGradient = LinearGradient(
        gradient: Gradient(colors: [Color.red.opacity(0.4), Color.blue.opacity(0.4)]),
        startPoint: .top,
        endPoint: .bottom
    )

    var body: some View {
        ZStack {
            countersStack()
                .background(Self.backgroundGradient)
            resetButton()
        }
        .onAppear {
            #if os(iOS)
            UIApplication.shared.isIdleTimerDisabled = true
            LiveActivityManager.shared.startActivity(game: Game(firstPlayerLp: model.p1.lifePoints, secondPlayerLp: model.p2.lifePoints))
            #endif
        }
        .onChange(of: scenePhase) {
            if scenePhase == .active { model.reload() }
        }
        .onDisappear {
            #if os(iOS)
            UIApplication.shared.isIdleTimerDisabled = false
            #endif
        }
    }

    // Widgets and the Live Activity live outside the app UI, so they are pushed on every change.
    private static func sync(_ p1: PlayerScore, _ p2: PlayerScore) {
        #if os(iOS)
        LiveActivityManager.shared.updateActivity(game: Game(firstPlayerLp: p1.lifePoints, secondPlayerLp: p2.lifePoints))
        #endif
        for kind in GameConstants.widgetKinds {
            WidgetCenter.shared.reloadTimelines(ofKind: kind)
        }
    }
}

// MARK: - Subviews
private extension GameView {
    @ViewBuilder
    func countersStack() -> some View {
        let stackLayout: AnyLayout = isLandscape ? AnyLayout(HStackLayout(spacing: 0)) : AnyLayout(VStackLayout(spacing: 0))
        stackLayout {
            GameCardView(props: GameCardProps(model: model, player: .two, isLandscape: $isLandscape, direction: .inverted))
            GameCardView(props: GameCardProps(model: model, player: .one, isLandscape: $isLandscape, direction: .normal))
        }
    }

    @ViewBuilder
    func resetButton() -> some View {
        Button(action: model.resetGame) {
            Image(systemName: "arrow.counterclockwise.circle.fill")
                .font(.system(size: 44, weight: .bold))
                .foregroundStyle(.white)
                .padding(16)
        }
        .accessibilityLabel("Reset game")
        .glassEffect(.regular.tint(.blue).interactive())
        .clipShape(Circle())
        .shadow(radius: 8)
        .rotationEffect(.degrees(isLandscape ? 270 : 0))
    }
}

// MARK: - Previews
#Preview("Portrait Light",traits: .portrait) { GameView(isLandscape: .constant(false)
)
    .preferredColorScheme(.light) }
#Preview("Portrait Dark",traits: .portrait) { GameView(isLandscape: .constant(false)
)
    .preferredColorScheme(.dark) }
#Preview("Landscape", traits: .landscapeLeft) { GameView(isLandscape: .constant(true)) }
