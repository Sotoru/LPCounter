//
//  DataService.swift
//  LPCounterWidgetExtension
//
//  Created by paolo laddomada on 13/11/25.
//

import Foundation

/// Reads and writes the shared App Group store with the same rules as the app (`PlayerScore`),
/// so widget changes are clamped, recorded in the history and undoable from the app.
struct DataService {
    private let defaults = UserDefaults(suiteName: GameConstants.appGroup)

    func handleLifePoints(lp: Int, isSubtract: Bool?, isFirstPlayer: Bool?) {
        let delta = (isSubtract ?? false) ? -lp : lp
        let isP1 = isFirstPlayer ?? true

        var score = score(isFirstPlayer: isP1)
        score.change(by: delta)
        defaults?.set(score.lifePoints, forKey: isP1 ? GameConstants.lifePointsP1Key : GameConstants.lifePointsP2Key)
        defaults?.set(score.historyRaw, forKey: isP1 ? GameConstants.historyP1Key : GameConstants.historyP2Key)
    }

    func progressP1() -> Int { score(isFirstPlayer: true).lifePoints }

    func progressP2() -> Int { score(isFirstPlayer: false).lifePoints }

    private func score(isFirstPlayer: Bool) -> PlayerScore {
        let lifeKey = isFirstPlayer ? GameConstants.lifePointsP1Key : GameConstants.lifePointsP2Key
        let historyKey = isFirstPlayer ? GameConstants.historyP1Key : GameConstants.historyP2Key
        return PlayerScore(
            lifePoints: defaults?.object(forKey: lifeKey) as? Int ?? GameConstants.initialLifePoints,
            historyRaw: defaults?.string(forKey: historyKey) ?? ""
        )
    }
}
