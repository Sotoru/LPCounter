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
            GameCardView(props: GameCardProps(lifePoints: $lifePointsP2, isLandscape: $isLandscape, direction: .inverted,playerNumber: 2))
            GameCardView(props: GameCardProps(lifePoints: $lifePointsP1, isLandscape: $isLandscape, direction: .normal,playerNumber: 1))
        }
    }
    
    @ViewBuilder
    func resetButton() -> some View {
        Button(action: resetLifePoints) {
            Image(systemName: "arrow.counterclockwise.circle.fill")
                .font(.system(size: 44, weight: .bold))
                .foregroundStyle(.white)
                .padding(16)
        }
        .glassEffect(.regular.tint(.blue).interactive())
        .clipShape(Circle())
        .shadow(radius: 8)
        .rotationEffect(.degrees(isLandscape ? 270 : 0))
    }
}

// MARK: - Actions
private extension GameView {
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
