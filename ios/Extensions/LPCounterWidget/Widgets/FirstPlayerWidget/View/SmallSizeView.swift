//
//  FirstPlayerEntryView.swift
//  LPCounterWidgetExtension
//
//  Created by Paolo on 18/11/25.
//

import SwiftUI
import WidgetKit
import AppIntents

struct SmallSizeView: View {
    var entry: FirstPlayerEntry
    #if os(iOS)
    @Environment(\.levelOfDetail) var levelOfDetail: LevelOfDetail
    #endif
    
    
    
    var body: some View {
        VStack {
            Text("LP1")
                .font(.title.bold())
            Text(String(entry.lifePointP1))
                .font(.largeTitle)
            HStack{
                Button(intent:
                        HandleGameIntent(lp: 1000,
                                         isSubtract: true
                                        )) {
                    Text("-")
                        .font(.title)
                }
                
                Button(intent: HandleGameIntent()) {
                    Text("+")
                        .font(.title)
                }
            }
        }
    }
}


#Preview(as: .systemSmall) {
    FirstPlayerWidget()
} timeline: {
    FirstPlayerEntry(date: .now, lifePointP1: 8000)
    FirstPlayerEntry(date: .distantFuture, lifePointP1: 1000)
}
