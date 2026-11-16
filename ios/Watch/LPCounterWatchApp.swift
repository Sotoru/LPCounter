//
//  LPCounterWatchApp.swift
//  LPCounterWatch
//
//  Apple Watch companion for Player 1 (SPEC §6b): the total on the dot matrix, quick ±100 / ±1000,
//  the Digital Crown for any amount, and undo. Changes go to the iPhone, which applies them with the
//  app's rules; the Watch shows what the iPhone sends back.
//

import SwiftUI

@main
struct LPCounterWatchApp: App {
    @State private var link = PhoneLink()

    init() {
        LED.registerFonts()
    }

    var body: some Scene {
        WindowGroup {
            PlayerOneView(link: link)
                .environment(\.pixelStyle, link.pixels)
                .environment(\.ledPitch, 3)
                .tint(LED.lit)
                .id(link.palette)
                .task { link.activate() }
        }
    }
}
