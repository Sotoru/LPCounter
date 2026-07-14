//
//  LPCounterWidgetBundle.swift
//  LPCounterWidget
//
//  Created by paolo laddomada on 12/11/25.
//

import WidgetKit
import SwiftUI

@main
struct LPCounterWidgetBundle: WidgetBundle {
    var body: some Widget {
        GameWidget()
        FirstPlayerWidget()
        #if os(iOS)
        GameLiveActivity()
        #endif
    }
}

