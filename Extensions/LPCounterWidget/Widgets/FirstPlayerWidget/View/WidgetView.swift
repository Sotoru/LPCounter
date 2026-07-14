//
//  WidgetView.swift
//  WidgetKitCourse WidgetExtension
//
//  Created by Florian Schweizer on 06.12.21.
//

import WidgetKit
import SwiftUI

struct WidgetView: View {
    var entry: FirstPlayerEntry

    
    @Environment(\.widgetFamily) var widgetFamily
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        widgetContent
            .containerBackground(for: .widget) {
                colorScheme == .dark ? Color.black : Color.white
            }
    }
    
    @ViewBuilder
    private var widgetContent: some View {
        switch widgetFamily {
            //            case .systemMedium:
            //                MediumSizeView(entry: entry)
            
        case .systemSmall:
            SmallSizeView(entry: entry)
            
        case .systemLarge:
            LargeSizeView(entry: entry)
            
        case .accessoryInline:
            Text("LP P1 \(entry.lifePointP1)")
            
            //update this
        case .accessoryCircular:
            Gauge(value: 0.7) {
                Text("LP P1 \(entry.lifePointP1)")
            }
            .gaugeStyle(.accessoryCircular)
            
            //update this
        case .accessoryRectangular:
            Gauge(value: 0.7) {
                Text("LP P1 \(entry.lifePointP1)")
            }
            .gaugeStyle(.accessoryLinear)
            
        default:
            Text("Not implemented!")
        }
    }
}

