//
//  CalculatorView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI


struct CalculatorView: View {
    
    let lifePoints: Int
    @Binding var isLandscape: Bool
    /// Receives the signed change to apply.
    let onApply: (Int) -> Void
    @State private var calc = CalculatorState()
    
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
                    switch calc.operation {
                    case .increase: Text("+")
                    case .decrease: Text("-")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .center)
                .bold()
                .font(.system(size: isLandscape ? 50 : 60))
                
                // Colonna 2: valore autocompletato o placeholder
                Group {
                    if calc.input != 0 {
                        Text(String(calc.value))
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
                    calc.clear()
                } label: {
                    Image(systemName: "delete.left")
                }
                .accessibilityLabel("Clear input")
                .buttonStyle(CalculatorButtonStyle(size: btnSize))
                .frame(maxWidth: .infinity, alignment: .center)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: calc.input)
    }
    
    @ViewBuilder
    func calculatorGrid(columns: Int) -> some View {
        LazyVGrid(columns: Array(repeating: .init(.flexible()), count: columns), spacing: 16) {
            ForEach(1...9, id: \.self) { number in
                Button("\(number)") {
                    calc.append(number)
                }
                .buttonStyle(CalculatorButtonStyle(size: btnSize))
            }
            
            Button {
                calc.toggleOperation()
            } label: {
                Image(systemName: "plus.forwardslash.minus")
            }
            .accessibilityLabel("Switch between add and subtract")
            .buttonStyle(CalculatorButtonStyle(size: btnSize))
            
            Button("0") {
                calc.append(0)
            }
            .buttonStyle(CalculatorButtonStyle(size: btnSize))
            
            Button {
                onApply(calc.delta)
                calc.clear()
            } label: {
                Image(systemName: "arrow.right")
            }
            .accessibilityLabel("Apply")
            .buttonStyle(CalculatorButtonStyle(size: btnSize, customColor: .orange))
        }
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
    @Previewable @State var isLandscape: Bool = false
    CalculatorView(lifePoints: 8000, isLandscape: $isLandscape, onApply: { _ in })
}

#Preview("Portrait Dark",traits: .portrait) {
    @Previewable @State var isLandscape: Bool = false
    CalculatorView(lifePoints: 8000, isLandscape: $isLandscape, onApply: { _ in })
        .preferredColorScheme(.dark)
}


#Preview("Landscape",traits: .landscapeLeft) {
    @Previewable @State var isLandscape: Bool = true
    CalculatorView(lifePoints: 8000, isLandscape: $isLandscape, onApply: { _ in })
}

