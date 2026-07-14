//
//  GameModel.swift
//  LPCounter
//
//  Created by paolo laddomada on 11/12/25.
//

import Foundation

class Game {

    var firstPlayerLp: Int
    var secondPlayerLp: Int

    init(firstPlayerLp: Int = 8000, secondPlayerLp: Int = 8000) {
        self.firstPlayerLp = firstPlayerLp
        self.secondPlayerLp = secondPlayerLp
    }
}


// MARK: - Constants
enum GameConstants {
    static let initialLifePoints = 8000
    static let appGroup = "group.it.sotoru.LPCounter"
    static let widgetKinds = ["GameWidget", "FirstPlayerWidget", "FirstPlayerLargeWidget"]

    // Chiavi condivise nello UserDefaults dell'App Group (app <-> widget)
    static let lifePointsP1Key = "lifePointsP1"
    static let lifePointsP2Key = "lifePointsP2"
}
