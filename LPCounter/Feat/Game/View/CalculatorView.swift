//
//  CalculatorView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI


enum CalculatorOperation {
    case increase
    case decrease
}

struct CalculatorView: View {
    
    @Binding var lifePoints: Int
    @Binding var isLandscape: Bool
    @State private var numberInput: Int = 0
    @State private var operation: CalculatorOperation = .decrease
    
    private let btnSize: CGFloat = 80
    
    var body: some View {
        Group {
            if isLandscape {
                landscapeLayout()
            } else {
                portraitLayout()
            }
        }
        .padding()
    }
}

// MARK: - Layout Views
private extension CalculatorView {
    @ViewBuilder
    func portraitLayout() -> some View {
        VStack(spacing: 20) {
            lifePointsText()
            headerGrid()
            calculatorGrid(columns: 3)
        }
    }
    
    @ViewBuilder
    func landscapeLayout() -> some View {
        HStack(spacing: 40) {
            calculatorGrid(columns: 4)
                .frame(maxWidth: .infinity)
            
            VStack(spacing: 20) {
                lifePointsText()
                headerGrid()
            }
            .frame(maxWidth: .infinity)
            
           
        }
    }
    
    @ViewBuilder
    func lifePointsText() -> some View {
        Text(String(lifePoints))
            .font(.system(size: 100))
            .bold()
    }
    
    @ViewBuilder
    func headerGrid() -> some View {
        Grid(horizontalSpacing: 16, verticalSpacing: 0) {
            GridRow {
                // Colonna 1: simbolo operazione
                Group {
                    switch operation {
                    case .increase: Text("+")
                    case .decrease: Text("-")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .center)
                .bold()
                .font(.system(size: isLandscape ? 50 : 60))
                
                // Colonna 2: valore autocompletato o placeholder
                Group {
                    if numberInput != 0 {
                        Text(String(autocompletedValue))
                            .font(.system(.title, design: .monospaced))
                            .fontWeight(.regular)
                            .foregroundStyle(.secondary)
                            .transition(.opacity)
                    } else {
                        Text("-")
                            .font(.system(.title, design: .monospaced))
                            .foregroundStyle(.tertiary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .center)
                
                // Colonna 3: pulsante clear
                Button {
                    clearInput()
                } label: {
                    Image(systemName: "delete.left")
                }
                .buttonStyle(CalculatorButtonStyle(size: btnSize))
                .frame(maxWidth: .infinity, alignment: .center)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: numberInput)
    }
    
    @ViewBuilder
    func calculatorGrid(columns: Int) -> some View {
        LazyVGrid(columns: Array(repeating: .init(.flexible()), count: columns), spacing: 16) {
            ForEach(1...9, id: \.self) { number in
                Button("\(number)") {
                    appendDigit(number)
                }
                .buttonStyle(CalculatorButtonStyle(size: btnSize))
            }
            
            Button {
                operation = operation == .increase ? .decrease : .increase
            } label: {
                Image(systemName: "plus.forwardslash.minus")
            }
            .buttonStyle(CalculatorButtonStyle(size: btnSize))
            
            Button("0") {
                appendDigit(0)
            }
            .buttonStyle(CalculatorButtonStyle(size: btnSize))
            
            Button {
                submitValue()
            } label: {
                Image(systemName: "arrow.right")
            }
            .buttonStyle(CalculatorButtonStyle(size: btnSize, customColor: .orange))
        }
    }
    
    // MARK: - Actions
    
    // Aggiunge una cifra rispettando:
    // - nessuno 0 iniziale
    // - massimo 4 cifre effettive
    private func appendDigit(_ digit: Int) {
        // Evita 0 come prima cifra
        if numberInput == 0 && digit == 0 { return }
        
        // Conta le cifre correnti
        let currentDigits = digitsCount(of: numberInput)
        if currentDigits >= 4 { return } // massimo 4 cifre
        
        numberInput = numberInput == 0 ? digit : numberInput * 10 + digit
    }
    
    private func clearInput() {
        numberInput = 0
    }
    
    // Valore autocompletato in base alle regole:
    // - 1 cifra  -> * 1000
    // - 2 cifre -> * 100
    // - 3+ cifre -> così com'è
    private var autocompletedValue: Int {
        let digitsCount = digitsCount(of: numberInput)
        switch digitsCount {
        case 0:
            return 0
        case 1:
            return numberInput * 100
        case 2:
            return numberInput * 100
        default:
            return numberInput
        }
    }
    
    private func submitValue() {
        
        var newLifePoints = lifePoints
        
        if(operation == .decrease) {
            newLifePoints -= autocompletedValue
        }
        else {
            newLifePoints += autocompletedValue
        }
        
        if newLifePoints >= 0 {
            lifePoints = newLifePoints
        } else {
            lifePoints = 0
        }
        numberInput = 0
    }
    
    // MARK: - Helpers
    
    private func digitsCount(of value: Int) -> Int {
        if value == 0 { return 0 }
        return Int(floor(log10(Double(value)))) + 1
    }
    
}

// Stile riutilizzabile per i pulsanti della calcolatrice
private struct CalculatorButtonStyle: ButtonStyle {
    let size: CGFloat
    let customColor: Color?
    
    init(size: CGFloat, customColor: Color? = nil) {
        self.size = size
        self.customColor = customColor
    }
    
    func makeBody(configuration: Configuration) -> some View {
       configuration.label
            .font(.system(size: 28, weight: .semibold))
            .frame(width: size, height: size)
            .glassEffect(.regular.tint(customColor).interactive())
    }
}

// Preview dinamica
#Preview("Portrait",traits: .portrait) {
    @Previewable @State var lp: Int = 8000
    @Previewable @State var isLandscape: Bool = false
    CalculatorView(lifePoints: $lp, isLandscape: $isLandscape)
}

#Preview("Portrait Dark",traits: .portrait) {
    @Previewable @State var lp: Int = 8000
    @Previewable @State var isLandscape: Bool = false
    CalculatorView(lifePoints: $lp, isLandscape: $isLandscape)
        .preferredColorScheme(.dark)
}


#Preview("Landscape",traits: .landscapeLeft) {
    @Previewable @State var lp: Int = 8000
    @Previewable @State var isLandscape: Bool = true
    CalculatorView(lifePoints: $lp, isLandscape: $isLandscape)
}

