//
//  GameCard.swift
//  LPCounter
//
//  Created by paolo laddomada on 29/01/26.
//

import SwiftUI




struct GameCardProps {
    @Binding var score: PlayerScore
    @Binding var isLandscape: Bool
    let direction: CounterDirection
    let playerNumber: Int
}


struct GameCardView: View {
    let props: GameCardProps

    var body: some View {
        TabView {
            CounterView(score: props.$score, isLandscape: props.$isLandscape, direction: props.direction )
            HistoryView(history: props.score.history)
                .padding(8)
        }
        #if os(iOS)
        .tabViewStyle(.page)
        #endif
        .glassCard(playerNumber: props.playerNumber, isLandscape: props.isLandscape)
    }
}


// MARK: - View Convenience
private extension View {
    func glassCard(cornerRadius: CGFloat = 32, playerNumber: Int = 1, isLandscape: Bool) -> some View {
        self
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .glassEffect(.clear, in: .rect(cornerRadius: cornerRadius))
            .padding()
            .rotationEffect(.degrees(isLandscape ? 360 : (playerNumber == 1 ? 0 : 180)))
    }
}

#Preview {
    @Previewable @State var score = PlayerScore()
    @Previewable @State var isLandscape = false
      
    let props = GameCardProps(
        score: $score,
        isLandscape: $isLandscape,
        direction: .normal,
        playerNumber: 1
    )
    
    GeometryReader { geometry in
        ZStack {
            LinearGradient(
                colors: [Color.red.opacity(0.5), Color.blue.opacity(0.5)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            GameCardView(props: props)
                .frame(height: geometry.size.height / 2 )
        }
    }
}

