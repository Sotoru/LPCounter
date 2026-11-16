//
//  ContentView.swift
//  LPCounter
//
//  Created by paolo laddomada on 29/10/25.
//

import SwiftUI

/// One stack: Home pushes the full-screen game (Play) and Settings (top-right gear) (SPEC §9).
struct ContentView: View {
    @State private var model = GameViewModel(onChange: GameView.sync)
    @Environment(\.scenePhase) private var scenePhase
    /// Watched only to redraw: `LED` colours read the same key.
    @AppStorage(LEDPalette.key, store: LED.store) private var palette = LEDPalette.night
    @AppStorage(PixelStyle.key, store: LED.store) private var pixels = PixelStyle.round

    var body: some View {
        NavigationStack { HomeView(model: model) }
            // A new palette rebuilds the tree, so every `LED` colour is read again.
            .id(palette)
            // The dot shape travels down the environment: every grid redraws live, the open Settings too.
            .environment(\.pixelStyle, pixels)
            #if os(iOS)
            .containerBackground(LED.background, for: .navigation)
            #endif
            // Behind everything, so the push and the rotation never show the window's edge.
            .background(LED.background)
        // The LED display is always black and blue (SPEC §10: no theme).
        .preferredColorScheme(.dark)
        .tint(LED.lit)
        // The widget writes to the store while the app is in the background.
        .onChange(of: scenePhase) {
            if scenePhase == .active { model.reload() }
        }
        #if os(iOS)
        // The Watch companion (SPEC §6b): it changes Player 1 through the store, then the board follows.
        .task {
            WatchLink.shared.onStoreChange = { model.reload() }
            WatchLink.shared.activate()
        }
        // The look goes to the Watch too.
        .onChange(of: palette) { WatchLink.shared.push() }
        .onChange(of: pixels) { WatchLink.shared.push() }
        #endif
    }
}

#Preview {
    ContentView()
}
