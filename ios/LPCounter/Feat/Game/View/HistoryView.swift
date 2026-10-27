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
        if(history.isEmpty){
            Text("No history")
        } else {
            List {
                // Newest first. Changes can repeat, so a row is identified by its position.
                ForEach(Array(history.enumerated().reversed()), id: \.offset) { _, number in
                    let isPositive = number > 0
                        
                    Text("\(isPositive ? "+" : "-") \(abs(number))")
                        .foregroundStyle(isPositive ? .green : .red)
                        .font(.headline)
                }
            }
            .listStyle(.plain)
        }
    }
}

#Preview {
    
    HistoryView(history: [-2400,-1100,1000,-300,-2800,-300])
}
