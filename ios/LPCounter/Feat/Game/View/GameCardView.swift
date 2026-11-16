//
//  GameCard.swift
//  LPCounter
//
//  Created by paolo laddomada on 29/01/26.
//

import SwiftUI

/// One player's card: the counter, whose top area shows the total or its history; the clock button next
/// to Undo and Halve switches between them, and the buttons never move (SPEC §4).
struct GameCardView: View {
    let model: GameViewModel
    let player: Player
    let isLandscape: Bool
    @Environment(\.introStep) private var introStep

    var body: some View {
        VStack(spacing: 4) {
            Text(player == .one ? "Player 1" : "Player 2")
                .led(16, color: LED.lit.opacity(0.75), glow: false)
                .accessibilityAddTraits(.isHeader)
                .opacity(introStep >= 2 ? 1 : 0)
            // Not clipped: the floating delta and the shake run past the card's edges.
            CounterView(model: model, player: player, isLandscape: isLandscape)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        // One container per player, so VoiceOver reads Player 2's turned card as a unit, top to bottom.
        .accessibilityElement(children: .contain)
        // Players face each other across the device in portrait; in landscape both read upright.
        .environment(\.ledFlipped, !isLandscape && player == .two)
        .rotationEffect(.degrees(isLandscape || player == .one ? 0 : 180))
    }
}

#Preview {
    GameCardView(model: GameViewModel(defaults: nil), player: .one, isLandscape: false)
        .frame(height: 420)
        .background(LEDMatrixBackground())
}
