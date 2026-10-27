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
    
    @Binding var score: PlayerScore
    @Binding var isLandscape: Bool
    let direction: CounterDirection
    
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
         
            HStack {
                Button {
                    score.reset()
                }label: {
                    Image(systemName: "clear")
                }
                .disabled(isAtInitialLifePoints)
                .opacity(isAtInitialLifePoints ? 0.4 : 1)
                .animation(.easeInOut(duration: 0.2), value: isAtInitialLifePoints)
                .font(.title)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.red).interactive())
                
                Button{
                    score.undo()
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                }
                .font(.title)
                .disabled(score.history.isEmpty)
                .opacity(score.history.isEmpty ? 0.4 : 1)
                .saturation(score.history.isEmpty ? 0 : 1)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.gray).interactive())
                .shadow(radius: 4)
                
                
                Button{
                    score.halve()
                } label: {
                    Image(systemName: "percent")
                }
                .font(.title)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.pink).interactive())
            }
            
            
        }
        .sheet(isPresented: $isSheetPresented) {
            SheetContentView(
                score: $score,
                isPresented: $isSheetPresented,
                isLandscape: $isLandscape
            )
            .rotationEffect(.degrees(direction == .inverted && !isLandscape ? 180 : 0))
            .presentationDetents([.large])
        }

    }
}

private struct SheetContentView: View {
    @Binding var score: PlayerScore
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
                .glassEffect()
            }
            .padding(.horizontal)
            .padding(.top, 8)
            
            // Contenuto della calcolatrice
            CalculatorView(lifePoints: score.lifePoints, isLandscape: $isLandscape) { delta in
                let before = score.lifePoints
                score.change(by: delta)
                // Il foglio si chiude solo se il totale è cambiato (SPEC §3)
                if score.lifePoints != before { isPresented = false }
            }
        }
    }
}

#Preview {
    @Previewable @State var score = PlayerScore()
    @Previewable @State var isLandscape: Bool = false
    
    CounterView(score: $score, isLandscape: $isLandscape, direction: .inverted)
}
