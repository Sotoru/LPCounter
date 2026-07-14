//
//  FirstPlayerWidgetProvider.swift
//  LPCounterWidgetExtension
//
//  Created by Paolo on 18/11/25.
//

import SwiftUI
import WidgetKit

struct FirstPlayerEntry: TimelineEntry {
    let date: Date
    let lifePointP1: Int
}

struct FirstPlayerProvider: TimelineProvider {
    typealias Entry = FirstPlayerEntry
    
    let data = DataService()
    
    func placeholder(in context: Context) -> Entry {
        FirstPlayerEntry(date: Date(), lifePointP1: 8000)
    }
    
    func getSnapshot(in context: Context, completion: @escaping (Entry) -> ()) {
        let entry = FirstPlayerEntry(date: Date(), lifePointP1: data.progressP1())
        completion(entry)
    }
    
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        var entries: [Entry] = []
        
        let currentDate = Date()
        for hourOffset in 0 ..< 5 {
            let entryDate = Calendar.current.date(byAdding: .hour, value: hourOffset, to: currentDate)!
            let entry = FirstPlayerEntry(date: entryDate, lifePointP1: data.progressP1())
            entries.append(entry)
        }
        
        let timeline = Timeline(entries: entries, policy: .atEnd)
        completion(timeline)
    }
}
