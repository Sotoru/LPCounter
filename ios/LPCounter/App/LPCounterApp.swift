//
//  LPCounterApp.swift
//  LPCounter
//
//  Created by paolo laddomada on 29/10/25.
//

import SwiftUI

@main
struct LPCounterApp: App {
    #if os(iOS)
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    #endif

    init() {
        LED.registerFonts()
        #if os(iOS)
        // So `AppOrientation.unlock()` knows how the device is held.
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                #if os(macOS)
                // Room for two cards side by side; the board switches layout by window shape.
                .frame(minWidth: 720, minHeight: 480)
                #endif
        }
        #if os(macOS)
        .defaultSize(width: 1100, height: 720)
        .windowToolbarStyle(.unifiedCompact)
        #endif
    }
}
