//
//  LPCounterWidgetCompact.swift
//  LPCounterWidget
//
//  Created by assistant on 14/11/25.
//

import WidgetKit
import SwiftUI
import AppIntents



struct FirstPlayerWidget: Widget {
    
    let kind: String = "FirstPlayerWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: FirstPlayerProvider()) { entry in
            WidgetView(entry: entry)
        }
        #if os(iOS)
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryInline, .accessoryCircular, .accessoryRectangular])
        .supportedMountingStyles([.elevated])
        #else
        .supportedFamilies([.systemSmall, .systemMedium])
        #endif
        .configurationDisplayName("LPCounter Player 1")
        .description("Add or subtract LP for Player 1")
        
        
    }
    
    
}

