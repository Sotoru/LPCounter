//
//  LPCounterEntryAppIntent.swift
//  LPCounterWidgetExtension
//
//  Created by paolo laddomada on 14/11/25.
//

import Foundation
import AppIntents
import WidgetKit

struct HandleGameIntent: AppIntent {
    static var title: LocalizedStringResource = "Update LP for Player 1"
    static var description = IntentDescription("Add or subtract life points for Player 1.")
    
    @Parameter(title: "Amount")
    var lp: Int
    
    @Parameter(title: "Player 1?")
    var isPlayerOne: Bool
    
    @Parameter(title: "Subtract?")
    var isSubtract: Bool
    
    
    // AppIntent richiede un init() senza argomenti
    init() {
        self.lp = 1000
        self.isSubtract = false
        self.isPlayerOne = true
    }

    init(lp: Int, isSubtract: Bool = false, isPlayerOne: Bool = true) {
        self.lp = lp
        self.isSubtract = isSubtract
        self.isPlayerOne = isPlayerOne
    }

    func perform() async throws -> some IntentResult {
        let data = DataService()
        
        data.handleLifePoints(lp: lp, isSubtract: isSubtract, isFirstPlayer: isPlayerOne)
        
        // Aggiorna i widget prima del return
        WidgetCenter.shared.reloadTimelines(ofKind: "GameWidget")
        WidgetCenter.shared.reloadTimelines(ofKind: "FirstPlayerWidget")
        WidgetCenter.shared.reloadTimelines(ofKind: "FirstPlayerLargeWidget")
        
        return .result()
    }
}
