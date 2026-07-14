//
//  LpCounterProvider.swift
//  LPCounterWidgetExtension
//
//  Created by Paolo on 17/11/25.
//

import SwiftUI
import WidgetKit

struct SimpleEntry: TimelineEntry {
    let date: Date
    let lifePointP1: Int
    let lifePointP2: Int
}

struct Provider: TimelineProvider {
    typealias Entry = SimpleEntry

    let data = DataService()

    func placeholder(in context: Context) -> Entry {
        SimpleEntry(date: Date(), lifePointP1: 8000, lifePointP2: 8000)
    }

    func getSnapshot(in context: Context, completion: @escaping (Entry) -> ()) {
        let entry = SimpleEntry(date: Date(), lifePointP1: data.progressP1(), lifePointP2: data.progressP2())
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        var entries: [Entry] = []

        let currentDate = Date()
        for hourOffset in 0 ..< 5 {
            let entryDate = Calendar.current.date(byAdding: .hour, value: hourOffset, to: currentDate)!
            let entry = SimpleEntry(date: entryDate, lifePointP1: data.progressP1(), lifePointP2: data.progressP2())
            entries.append(entry)
        }

        let timeline = Timeline(entries: entries, policy: .atEnd)
        completion(timeline)
    }
}
