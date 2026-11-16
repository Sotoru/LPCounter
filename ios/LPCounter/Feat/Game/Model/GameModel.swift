//
//  GameModel.swift
//  LPCounter
//
//  Created by paolo laddomada on 11/12/25.
//

import Foundation

// MARK: - Rules (SPEC §2)

/// One player's life points and history. Pure value type: app, widget and tests share these rules.
/// `history` holds signed changes (oldest first), never absolute totals.
nonisolated struct PlayerScore: Equatable {
    private(set) var lifePoints: Int
    private(set) var history: [Int]

    init(lifePoints: Int = GameConstants.initialLifePoints, history: [Int] = []) {
        self.lifePoints = lifePoints
        self.history = history
    }

    /// Storage form of `history`: comma separated, same as Android.
    init(lifePoints: Int, historyRaw: String) {
        self.init(lifePoints: lifePoints, history: historyRaw.split(separator: ",").compactMap { Int($0) })
    }

    var historyRaw: String { history.map(String.init).joined(separator: ",") }

    /// Applies `delta`, clamped at 0, and records the change actually applied.
    mutating func change(by delta: Int) {
        let new = max(0, lifePoints + delta)
        record(new - lifePoints)
        lifePoints = new
    }

    /// Halves rounding up, as the official rulings do (2425 → 1213).
    mutating func halve() { change(by: (lifePoints + 1) / 2 - lifePoints) }

    /// Reverts the last change relative to the *current* total, so changes made elsewhere (widget) survive.
    mutating func undo() {
        guard let last = history.popLast() else { return }
        lifePoints = max(0, lifePoints - last)
    }

    mutating func reset(to startingLifePoints: Int = GameConstants.initialLifePoints) {
        lifePoints = startingLifePoints
        history = []
    }

    private mutating func record(_ change: Int) {
        guard change != 0 else { return }
        history.append(change)
        history.removeFirst(max(0, history.count - GameConstants.maxHistory))
    }
}

nonisolated enum Player {
    case one, two

    var number: Int { self == .one ? 1 : 2 }
}

// MARK: - Constants
nonisolated enum GameConstants {
    static let initialLifePoints = 8000
    /// Choices for the starting life points: standard and Speed Duel.
    static let startingLifePointsOptions = [8000, 4000]
    static let maxHistory = 200
    /// Timer presets in minutes (SPEC §9): 45 and 50-minute rounds; any other length can be set by hand.
    static let timerPresets = [45, 50]
    /// Range for a custom round length, in minutes.
    static let timerRange = 1...180
    static let appGroup = "group.it.sotoru.LPCounter"
    static let widgetKinds = ["GameWidget", "FirstPlayerWidget", "FirstPlayerLargeWidget"]

    // Chiavi condivise nello UserDefaults dell'App Group (app <-> widget)
    static let lifePointsP1Key = "lifePointsP1"
    static let lifePointsP2Key = "lifePointsP2"
    static let historyP1Key = "historyP1"
    static let historyP2Key = "historyP2"
    static let startingLifePointsKey = "startingLifePoints"
    static let timerMinutesKey = "timerMinutes"
    static let timerEndsAtKey = "timerEndsAt"
    /// Seconds left while the timer is paused (no `timerEndsAt` then).
    static let timerPausedKey = "timerPausedRemaining"
}
