//
//  ContentView.swift
//  LPCounter
//
//  Created by paolo laddomada on 29/10/25.
//

import SwiftUI
import SwiftData

struct ContentView: View {
    
    @State private var isLandscape = false
    
    var body: some View {
        GameView(isLandscape: $isLandscape)
            .detectDeviceOrientation(isLandscape: $isLandscape)
        
    }
    
    
}

#Preview {
    ContentView()
}
