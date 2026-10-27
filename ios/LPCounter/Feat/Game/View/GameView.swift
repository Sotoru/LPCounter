//
//  GameView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI
import WidgetKit



struct GameView: View {
    // MARK: - State & Storage
    @AppStorage(GameConstants.lifePointsP1Key, store: UserDefaults(suiteName: GameConstants.appGroup))
    private var lifePointsP1: Int = GameConstants.initialLifePoints

    @AppStorage(GameConstants.lifePointsP2Key, store: UserDefaults(suiteName: GameConstants.appGroup))
    private var lifePointsP2: Int = GameConstants.initialLifePoints
    
    @AppStorage(GameConstants.historyP1Key, store: UserDefaults(suiteName: GameConstants.appGroup))
    private var historyP1: String = ""

    @AppStorage(GameConstants.historyP2Key, store: UserDefaults(suiteName: GameConstants.appGroup))
    private var historyP2: String = ""

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
            let game = Game(firstPlayerLp: lifePointsP1, secondPlayerLp: lifePointsP2)
            LiveActivityManager.shared.startActivity(game: game)
            #endif
        }
        .onChange(of: lifePointsP1) {
            #if os(iOS)
            let game = Game(firstPlayerLp: lifePointsP1, secondPlayerLp: lifePointsP2)
            LiveActivityManager.shared.updateActivity(game: game)
            #endif
            reloadWidgets()
        }
        .onChange(of: lifePointsP2) {
            #if os(iOS)
            let game = Game(firstPlayerLp: lifePointsP1, secondPlayerLp: lifePointsP2)
            LiveActivityManager.shared.updateActivity(game: game)
            #endif
            reloadWidgets()
        }
        .onDisappear {
            #if os(iOS)
            UIApplication.shared.isIdleTimerDisabled = false
            #endif
        }
    }
}

// MARK: - Subviews
private extension GameView {
    @ViewBuilder
    func countersStack() -> some View {
        let stackLayout: AnyLayout = isLandscape ? AnyLayout(HStackLayout(spacing: 0)) : AnyLayout(VStackLayout(spacing: 0))
        stackLayout {
            GameCardView(props: GameCardProps(score: scoreP2, isLandscape: $isLandscape, direction: .inverted,playerNumber: 2))
            GameCardView(props: GameCardProps(score: scoreP1, isLandscape: $isLandscape, direction: .normal,playerNumber: 1))
        }
    }

    // Life points and history live in the shared store as separate keys; the rules live in PlayerScore.
    var scoreP1: Binding<PlayerScore> {
        Binding(
            get: { PlayerScore(lifePoints: lifePointsP1, historyRaw: historyP1) },
            set: { lifePointsP1 = $0.lifePoints; historyP1 = $0.historyRaw }
        )
    }

    var scoreP2: Binding<PlayerScore> {
        Binding(
            get: { PlayerScore(lifePoints: lifePointsP2, historyRaw: historyP2) },
            set: { lifePointsP2 = $0.lifePoints; historyP2 = $0.historyRaw }
        )
    }
    
    @ViewBuilder
    func resetButton() -> some View {
        Button(action: resetLifePoints) {
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

// MARK: - Actions
private extension GameView {
    // Resets the totals only; the history is intentionally left untouched (SPEC §2).
    func resetLifePoints() {
        lifePointsP1 = GameConstants.initialLifePoints
        lifePointsP2 = GameConstants.initialLifePoints
        reloadWidgets()
    }
    
    func reloadWidgets() {
        Task {
            for kind in GameConstants.widgetKinds {
                WidgetCenter.shared.reloadTimelines(ofKind: kind)
            }
        }
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
