//
//  DataService.swift
//  LPCounterWidgetExtension
//
//  Created by paolo laddomada on 13/11/25.
//

import Foundation
import SwiftUI

struct DataService {
    @AppStorage(GameConstants.lifePointsP1Key, store: UserDefaults(suiteName: GameConstants.appGroup)) private var lifePointsP1: Int = GameConstants.initialLifePoints
    @AppStorage(GameConstants.lifePointsP2Key, store: UserDefaults(suiteName: GameConstants.appGroup)) private var lifePointsP2: Int = GameConstants.initialLifePoints
    
    
    //    func addLpP1(lp: Int, isPlayer1: Bool?) {
    //        lifePointsP1 += lp
    //    }
    //
    //    func subtractLpP1(lp: Int) {
    //        lifePointsP1 -= lp
    //    }
    //
    //
    //    func addLpP2() {
    //        lifePointsP2 += 1000
    //    }
    
    func handleLifePoints(lp: Int, isSubtract: Bool?, isFirstPlayer: Bool?) {
        let subtract = isSubtract ?? false
        let isP1 = isFirstPlayer ?? true
        let delta = subtract ? -lp : lp

        if isP1 {
            lifePointsP1 += delta
        } else {
            lifePointsP2 += delta
        }
    }
    
    func progressP1()-> Int {
        return lifePointsP1
    }
    
    func progressP2()-> Int {
        return lifePointsP2
    }
    
}
