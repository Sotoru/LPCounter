//
//  PlayerOneView.swift
//  LPCounterWatch
//
//  One screen: the total on the matrix; turn the Digital Crown to pick an amount (in hundreds), then
//  − or + applies it. Without an amount, − / + are 1000; a long press on them is 100. Undo below.
//

import SwiftUI

struct PlayerOneView: View {
    let link: PhoneLink
    @State private var crown = 0.0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// The amount picked with the crown, in steps of 100; 0 when none.
    private var amount: Int { Int(crown.rounded()) * 100 }
    private var lp: Int { link.score.lifePoints }

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(verbatim: "LP1").led(13, color: LED.dim, glow: false)
                Spacer()
                if !link.isReachable {
                    Image(systemName: "iphone.slash").font(.footnote).foregroundStyle(LED.dim)
                        .accessibilityLabel("iPhone not reachable")
                }
            }
            DotMatrixNumber(value: Double(lp), color: lp == 0 ? LED.red : LED.lit, glow: false, maxScale: 3)
                .animation(reduceMotion ? nil : .snappy, value: lp)
                .frame(maxHeight: .infinity)
                .accessibilityLabel("Player 1 life points")
                .accessibilityValue(String(lp))
            // The amount the crown has picked, on a seven-segment readout.
            SegmentReadout(text: amount == 0 ? "1000" : String(amount), digits: 4, size: 24,
                           color: amount == 0 ? LED.dim : LED.lit)
                .accessibilityLabel(amount == 0 ? Text("Amount 1000") : Text("Amount \(amount)"))
            HStack(spacing: 8) {
                step(sign: -1)
                step(sign: 1)
            }
            Button("Undo", systemImage: "arrow.uturn.backward") { link.undo() }
                .labelStyle(.iconOnly)
                .disabled(link.score.history.isEmpty)
        }
        .padding(.horizontal, 4)
        .background { LEDMatrixBackground() }
        .focusable()
        .digitalCrownRotation($crown, from: 0, through: 40, by: 1, sensitivity: .medium,
                              isContinuous: false, isHapticFeedbackEnabled: true)
        .sensoryFeedback(.impact(weight: .light), trigger: lp)
    }

    /// − or +: the crown's amount, or 1000; a long press applies 100.
    private func step(sign: Int) -> some View {
        Button {
            link.change(by: sign * (amount == 0 ? 1000 : amount))
            crown = 0
        } label: {
            Image(systemName: sign < 0 ? "minus" : "plus")
                .font(.title3.weight(.bold))
                .foregroundStyle(LED.background)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .tint(sign < 0 ? LED.red : LED.green)
        .simultaneousGesture(LongPressGesture(minimumDuration: 0.5).onEnded { _ in
            link.change(by: sign * 100)
            crown = 0
        })
        .accessibilityLabel(sign < 0 ? "Subtract" : "Add")
        .accessibilityHint("Long press for 100")
        .disabled(sign < 0 && lp == 0)
    }
}
