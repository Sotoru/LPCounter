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
    
    @Binding var lifePoints: Int
    @Binding var isLandscape: Bool
    @Binding var history: [Int]
    let direction: CounterDirection
    
    private let buttonSize = CGSize(width: 90, height: 44)
   
    @State private var isSheetPresented = false
    
    var body: some View {
        VStack{
            
            Text(String(lifePoints))
                .font(.system(size: 100))
                .padding(.bottom,24)
                .onTapGesture {
                    isSheetPresented.toggle()
                }
         
            HStack {
                Button {
                    lifePoints = GameConstants.initialLifePoints
                    history.removeAll()
                }label: {
                    Image(systemName: "clear")
                }
                .disabled(lifePoints == 8000)
                .opacity(lifePoints == 8000 ? 0.4 : 1)
                .animation(.easeInOut(duration: 0.2), value: lifePoints == 8000)
                .font(.title)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.red).interactive())
                
                Button{
                    if(!history.isEmpty){
                        history.removeLast()
                    }
                    lifePoints = history.last ?? 8000
                    
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                }
                .font(.title)
                .disabled(history.isEmpty)
                .opacity(history.isEmpty ? 0.4 : 1)
                .saturation(history.isEmpty ? 0 : 1)
                .frame(width: buttonSize.width, height: buttonSize.height)
                .foregroundStyle(.white)
                .glassEffect(.regular.tint(.gray).interactive())
                .shadow(radius: 4)
                
                
                Button{
                    let _lpHalfed = lifePoints / 2
                    lifePoints = _lpHalfed
                    history.append(_lpHalfed)
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
                lifePoints: $lifePoints,
                isPresented: $isSheetPresented,
                lpHistory: $history,
                isLandscape: $isLandscape
            )
            .rotationEffect(.degrees(direction == .inverted && !isLandscape ? 180 : 0))
            .presentationDetents([.large])
        }

    }
}

private struct SheetContentView: View {
    @Binding var lifePoints: Int
    @Binding var isPresented: Bool
    @Binding var lpHistory: [Int]
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
            CalculatorView(lifePoints: $lifePoints, isLandscape: $isLandscape)
                .onChange(of: lifePoints) { _, newLp in
                    // registra in history il nuovo totale assoluto (coerente con l'undo)
                    lpHistory.append(newLp)
                    // chiudi la modale ad ogni cambio
                    isPresented = false
                }
        }
    }
}

#Preview {
    @Previewable @State var lp: Int = 8000
    @Previewable @State var isLandscape: Bool = false
    @Previewable @State var history: [Int] = []
    
    CounterView(lifePoints: $lp, isLandscape: $isLandscape, history: $history, direction: .inverted)
}
