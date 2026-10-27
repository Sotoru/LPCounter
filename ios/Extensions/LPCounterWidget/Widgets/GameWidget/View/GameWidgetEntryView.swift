//
//  LPCounterWidgetEntryView.swift
//  LPCounterWidgetExtension
//
//  Created by Paolo on 17/11/25.
//

import SwiftUI
import WidgetKit

extension View {
    func containerBackgroundStyle(colorScheme: ColorScheme) -> some View {
        self
            .containerBackground(for: .widget) {
                colorScheme == .dark
                ? Color.black : Color.white
            }
    }
}

struct GameWidgetEntryView: View {
    var entry: SimpleEntry
    #if os(iOS)
    @Environment(\.levelOfDetail) var levelOfDetail: LevelOfDetail
    #endif

    @Environment(\.widgetRenderingMode) var renderingMode

    @Environment(\.colorScheme) private var colorScheme

    
    var body: some View {
        VStack {
            Spacer()
            Text("LP2")
            Text(String(entry.lifePointP2))
            Spacer()
            Text("LP1")
            Text(String(entry.lifePointP1))
            Spacer()
        }
        .containerBackground(for: .widget) {
            colorScheme == .dark
            ? Color.black : Color.white
        }
    }
}


#Preview(as: .systemSmall) {
    GameWidget()
} timeline: {
    SimpleEntry(date: Date(), lifePointP1: 8000, lifePointP2: 8000)
    SimpleEntry(date: .distantFuture, lifePointP1: 1000, lifePointP2: 2000)
}

#Preview(as: .systemMedium) {
    GameWidget()
} timeline: {
    SimpleEntry(date: Date(), lifePointP1: 9000, lifePointP2: 9000)
}
