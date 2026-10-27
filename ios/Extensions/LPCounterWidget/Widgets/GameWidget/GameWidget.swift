//
//  LPCounterWidget.swift
//  LPCounterWidget
//
//  Created by paolo laddomada on 12/11/25.
//

import WidgetKit
import SwiftUI


struct GameWidget: Widget {
    let kind: String = "GameWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            GameWidgetEntryView(entry: entry)            
        }
        .supportedFamilies([.systemSmall])
        #if os(iOS)
        .supportedMountingStyles([.elevated])
        #endif
        .configurationDisplayName("Game")
        .description("Preview of the current game")
    }
}
