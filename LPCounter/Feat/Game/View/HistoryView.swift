//
//  HistoryView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/01/26.
//

import SwiftUI

struct HistoryView: View {
    
    @Binding var history: [Int]
    
    var body: some View {
        if(history.isEmpty){
            Text("No history")
        } else {
            List {
                ForEach(history.reversed(), id: \.self) { number in
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
    
    @Previewable @State var history: [Int] = [-2400,-1100,1000,-300,-2800]
    
    HistoryView(history: $history)
}
