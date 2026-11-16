//
//  CalculatorView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI

/// The keypad sheet (SPEC §3), drawn on the app's dot matrix: the total and the result on the grid,
/// Subtract / Add as one big two-way switch, then the digits.
struct CalculatorView: View {
    let player: Player
    let lifePoints: Int
    let isLandscape: Bool
    /// Receives the signed change to apply.
    let onApply: (Int) -> Void
    @State private var calc = CalculatorState()

    /// Spacing on the matrix's pitch (5 pt), so keys, switch and gaps line up with the dots behind.
    private enum Metrics {
        static let gap: CGFloat = 15
        static let row: CGFloat = 55
        static let margin: CGFloat = 20
        static let width: CGFloat = 420
    }

    var body: some View {
        Group {
            if isLandscape {
                // Two equal columns; the switch sits on the keypad's last row, same height and gap.
                HStack(alignment: .bottom, spacing: Metrics.gap * 2) {
                    VStack(spacing: Metrics.gap) {
                        display
                        operationSwitch
                    }
                    .frame(maxWidth: Metrics.width)
                    keypad(columns: 4)
                        .frame(maxWidth: Metrics.width)
                }
                .frame(maxWidth: .infinity)
                .frame(maxHeight: .infinity)
            } else {
                // Scrolls only when it doesn't fit (large Dynamic Type sizes).
                ScrollView {
                    VStack(spacing: Metrics.gap) {
                        display
                        // Switch and keys share one column width, so their edges line up.
                        VStack(spacing: Metrics.gap) {
                            operationSwitch
                            keypad(columns: 3)
                        }
                        .frame(maxWidth: Metrics.width)
                    }
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        }
        .padding(Metrics.margin)
        .sensoryFeedback(.selection, trigger: calc.input)
        .sensoryFeedback(.impact(weight: .light), trigger: calc.operation)
    }

    private var adding: Bool { calc.operation == .increase }
    private var opColor: Color { adding ? LED.green : LED.red }
    private var result: Int { max(0, lifePoints + calc.delta) }

    // MARK: Display

    /// On the matrix, like the board: the total now, the change typed, and what it leads to.
    private var display: some View {
        VStack(spacing: Metrics.gap) {
            Text(player == .one ? "Player 1" : "Player 2").led(15, color: LED.dim, glow: false)
            DotMatrixNumber(value: Double(lifePoints), glow: false, maxScale: 2)
            HStack(spacing: Metrics.gap) {
                DotMatrixText(text: adding ? "+" : "-", color: opColor)
                DotMatrixText(text: calc.input == 0 ? "    " : String(calc.value), color: opColor)
                    .frame(minWidth: 0)
                DotMatrixText(text: "=", color: LED.dim, glow: false)
                DotMatrixText(text: calc.input == 0 ? "    " : String(result), color: result == 0 ? LED.red : LED.lit)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Metrics.gap)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(displayLabel)
        .animation(.snappy(duration: 0.15), value: calc)
    }

    /// The panel read as one sentence: "Player 1, 8000 life points, subtract 500, result 7500".
    private var displayLabel: Text {
        let who = Text(player == .one ? "Player 1" : "Player 2")
        let total = Text("\(lifePoints) life points")
        guard calc.input != 0 else { return who + Text(verbatim: ", ") + total }
        let op = adding ? Text("add \(calc.value)") : Text("subtract \(calc.value)")
        return who + Text(verbatim: ", ") + total + Text(verbatim: ", ") + op + Text(verbatim: ", ") + Text("result \(result)")
    }

    // MARK: Subtract / Add

    /// Two big halves, the active one lit in its colour: one tap on the side you want, no toggling blind.
    private var operationSwitch: some View {
        HStack(spacing: Metrics.gap) {
            side("Subtract", symbol: "minus", on: !adding, color: LED.red)
            side("Add", symbol: "plus", on: adding, color: LED.green)
        }
    }

    private func side(_ title: LocalizedStringKey, symbol: String, on: Bool, color: Color) -> some View {
        Button {
            if !on { calc.toggleOperation() }
        } label: {
            Label(title, systemImage: symbol)
                .font(.title3.weight(.bold))
                .foregroundStyle(on ? LED.background : color)
                // Exactly a key's height: no extra control-size padding.
                .frame(maxWidth: .infinity, minHeight: Metrics.row)
        }
        .tint(color)
        .modifier(SideStyle(on: on))
        .buttonBorderShape(.capsule)
        .accessibilityAddTraits(on ? .isSelected : [])
    }

    // MARK: Keypad

    private func keypad(columns: Int) -> some View {
        VStack(spacing: Metrics.gap) {
            GlassEffectContainer {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: Metrics.gap), count: columns),
                          spacing: Metrics.gap) {
                    ForEach(1...9, id: \.self) { number in
                        key { calc.append(number) } label: { digit(number) }
                    }
                    key { calc.clear() } label: { Image(systemName: "xmark") }
                        .accessibilityLabel("Clear input")
                        .disabled(calc.input == 0)
                    key { calc.append(0) } label: { digit(0) }
                    // Apply: filled in the operation's colour, the key that ends the entry.
                    Button {
                        onApply(calc.delta)
                        calc.clear()
                    } label: {
                        Image(systemName: "checkmark")
                            .font(.title2.weight(.bold))
                            .foregroundStyle(LED.background)
                            .frame(maxWidth: .infinity, minHeight: Metrics.row)
                    }
                    .buttonStyle(.glassProminent)
                    .buttonBorderShape(.capsule)
                    .tint(opColor)
                    .accessibilityLabel("Apply")
                    .keyboardShortcut(.defaultAction)
                    .disabled(calc.input == 0)
                }
            }
        }
    }

    private func digit(_ number: Int) -> some View {
        Text(verbatim: String(number)).font(LED.segmentFont(28, relativeTo: .title))
    }

    private func key<Label: View>(_ action: @escaping () -> Void, @ViewBuilder label: () -> Label) -> some View {
        Button(action: action) {
            label()
                .font(.title2.weight(.semibold))
                .frame(maxWidth: .infinity, minHeight: Metrics.row)
        }
        .buttonStyle(.glass)
        .buttonBorderShape(.capsule)
    }
}

/// The lit side is filled in its colour; the other stays glass.
private struct SideStyle: ViewModifier {
    let on: Bool

    func body(content: Content) -> some View {
        if on { content.buttonStyle(.glassProminent) } else { content.buttonStyle(.glass) }
    }
}

#Preview {
    CalculatorView(player: .one, lifePoints: 8000, isLandscape: false, onApply: { _ in })
        .background(LEDMatrixBackground())
        .tint(LED.lit)
        .preferredColorScheme(.dark)
}
