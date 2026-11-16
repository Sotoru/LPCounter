//
//  HistoryView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/01/26.
//

import SwiftUI

struct HistoryView: View {
    let history: [Int]

    var body: some View {
        if history.isEmpty {
            Text("No history").led(18, color: LED.dim, glow: false)
        } else {
            List {
                // Newest first. Changes can repeat, so a row is identified by its position.
                ForEach(Array(history.enumerated().reversed()), id: \.offset) { _, number in
                    Text(verbatim: "\(number > 0 ? "+" : "−") \(abs(number))")
                        .led(22, color: number > 0 ? LED.green : LED.red)
                        // Centred under the player's name and buttons, like the total it replaces.
                        .frame(maxWidth: .infinity)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .contentMargins(.horizontal, 0, for: .scrollContent)
        }
    }
}

#Preview {
    HistoryView(history: [-2400, -1100, 1000, -300, -2800, -300])
        .background(LEDMatrixBackground())
}
