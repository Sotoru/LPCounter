//
//  CounterView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI


enum CounterDirection {
    case normal
    case inverted
}

struct CounterView: View {
    
    let model: GameViewModel
    let player: Player
    @Binding var isLandscape: Bool
    let direction: CounterDirection

    private var score: PlayerScore { model.score(player) }
    
    private let buttonSize = CGSize(width: 90, height: 44)
   
    @State private var isSheetPresented = false

    private var isAtInitialLifePoints: Bool { score.lifePoints == GameConstants.initialLifePoints }
    
    var body: some View {
        VStack{
            
            Text(String(score.lifePoints))
                .font(.system(size: 100))
                .padding(.bottom,24)
                .onTapGesture {
                    isSheetPresented.toggle()
                }
                .accessibilityLabel("Edit life points")
                .accessibilityValue(String(score.lifePoints))
                .accessibilityAddTraits(.isButton)
         
            HStack {
                Button {
                    model.reset(player)
                }label: {
                    Image(systemName: "clear")
                }
                .accessibilityLabel("Reset life points")
                .disabled(isAtInitialLifePoints)
                .opacity(isAtInitialLifePoints ? 0.4 : 1)
                .animation(.easeInOut(duration: 0.2), value: isAtInitialLifePoints)
                .font(.title)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.red).interactive())
                
                Button{
                    model.undo(player)
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                }
                .accessibilityLabel("Undo")
                .font(.title)
                .disabled(score.history.isEmpty)
                .opacity(score.history.isEmpty ? 0.4 : 1)
                .saturation(score.history.isEmpty ? 0 : 1)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.gray).interactive())
                .shadow(radius: 4)
                
                
                Button{
                    model.halve(player)
                } label: {
                    Image(systemName: "percent")
                }
                .accessibilityLabel("Halve life points")
                .font(.title)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.pink).interactive())
            }
            
            
        }
        .sheet(isPresented: $isSheetPresented) {
            SheetContentView(
                model: model,
                player: player,
                isPresented: $isSheetPresented,
                isLandscape: $isLandscape
            )
            .rotationEffect(.degrees(direction == .inverted && !isLandscape ? 180 : 0))
            .presentationDetents([.large])
        }

    }
}

private struct SheetContentView: View {
    let model: GameViewModel
    let player: Player
    @Binding var isPresented: Bool
    @Binding var isLandscape: Bool
    
    var body: some View {
        VStack(spacing: 0) {
            // Header con pulsante di chiusura
            HStack {
                Spacer()
                Button {
                    isPresented = false
                } label: {
                    Image(systemName: "xmark")
                        .font(.headline)
                        .padding(12)
                        .contentShape(Rectangle())
                    
                }
                .accessibilityLabel("Close")
                .glassEffect()
            }
            .padding(.horizontal)
            .padding(.top, 8)
            
            // Contenuto della calcolatrice
            CalculatorView(lifePoints: model.score(player).lifePoints, isLandscape: $isLandscape) { delta in
                let before = model.score(player).lifePoints
                model.changeBy(player, delta)
                // Il foglio si chiude solo se il totale è cambiato (SPEC §3)
                if model.score(player).lifePoints != before { isPresented = false }
            }
        }
    }
}

#Preview {
    @Previewable @State var model = GameViewModel(defaults: nil)
    @Previewable @State var isLandscape: Bool = false
    
    CounterView(model: model, player: .one, isLandscape: $isLandscape, direction: .inverted)
}
