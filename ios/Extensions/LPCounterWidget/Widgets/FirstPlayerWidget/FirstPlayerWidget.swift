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
    
    @Environment(\.widgetFamily) var widgetFamily
    
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: FirstPlayerProvider()) { entry in
            WidgetView(entry: entry)
                .containerBackground(.white.gradient, for: .widget)
        }
        #if os(iOS)
        .supportedFamilies([.systemSmall, .systemLarge, .accessoryInline, .accessoryCircular, .accessoryRectangular])
        .supportedMountingStyles([.elevated])
        #else
        .supportedFamilies([.systemSmall, .systemLarge])
        #endif
        .configurationDisplayName("LifePointCounter Player1")
        .description("Widget per aggiungere o sottrare LP al P1")
        
        
    }
    
    
}

