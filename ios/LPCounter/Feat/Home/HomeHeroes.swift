//
//  HomeHeroes.swift
//  LPCounter
//
//  The timer button above Play.
//

import SwiftUI

/// The timer in the Home's bottom bar: just the icon and the length ("50 min", or the countdown if one is running);
/// off shows the icon alone. Opens the native menu to change it.
struct HomeTimerButton: View {
    let model: GameViewModel

    var body: some View {
        Menu {
            TimerMenuItems(model: model)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "timer")
                    .symbolVariant(model.timerMinutes == 0 ? .slash : .none)
                    .opacity(model.timerMinutes == 0 ? 0.6 : 1)
                label
            }
            .font(.subheadline.weight(.semibold))
            // Never squeezed: in the top bar it would wrap the countdown digit by digit.
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, model.timerMinutes == 0 && !model.hasTimer ? 0 : 12)
            // Off it is just the icon: a circle like the die and coin next to it.
            .frame(minWidth: 44, minHeight: 44)
        }
        .buttonStyle(.glass)
        .buttonBorderShape(model.timerMinutes == 0 && !model.hasTimer ? .circle : .capsule)
        .menuIndicator(.hidden)
        .accessibilityLabel("Timer")
        .accessibilityValue(model.timerMinutes == 0 ? Text("Off") : Text("\(model.timerMinutes) min"))
        .animation(.snappy, value: model.timerMinutes)
        .sheet(isPresented: Binding(get: { model.isEditingTimer }, set: { model.isEditingTimer = $0 })) {
            TimerEditor(model: model)
        }
    }

    @ViewBuilder
    private var label: some View {
        if model.hasTimer {
            TimerReadout(model: model, size: 14)
        } else if model.timerMinutes > 0 {
            Text("\(model.timerMinutes) min").contentTransition(.numericText(value: Double(model.timerMinutes)))
        }
    }
}
