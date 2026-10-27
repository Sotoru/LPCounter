//
//  GameViewModel.swift
//  LPCounter
//

import Foundation
import Observation

/// Owns the game: loads and saves both players in the shared App Group store and applies the rules of
/// `PlayerScore`. Widgets and the Live Activity are pushed through `onChange`, like Android's `GameRepository`.
@MainActor @Observable
final class GameViewModel {
    private(set) var p1: PlayerScore
    private(set) var p2: PlayerScore

    private let defaults: UserDefaults?
    private let onChange: @MainActor (PlayerScore, PlayerScore) -> Void

    init(
        defaults: UserDefaults? = UserDefaults(suiteName: GameConstants.appGroup),
        onChange: @escaping @MainActor (PlayerScore, PlayerScore) -> Void = { _, _ in }
    ) {
        self.defaults = defaults
        self.onChange = onChange
        p1 = Self.load(.one, from: defaults)
        p2 = Self.load(.two, from: defaults)
    }

    func score(_ player: Player) -> PlayerScore { player == .one ? p1 : p2 }

    func changeBy(_ player: Player, _ delta: Int) { update(player) { $0.change(by: delta) } }

    func halve(_ player: Player) { update(player) { $0.halve() } }

    func undo(_ player: Player) { update(player) { $0.undo() } }

    func reset(_ player: Player) { update(player) { $0.reset() } }

    /// Resets both totals only; the history is intentionally left untouched (SPEC §2).
    func resetGame() {
        p1 = PlayerScore(lifePoints: GameConstants.initialLifePoints, history: p1.history)
        p2 = PlayerScore(lifePoints: GameConstants.initialLifePoints, history: p2.history)
        save(.one)
        save(.two)
        onChange(p1, p2)
    }

    /// Re-reads the store: the widget writes to it while the app is in the background.
    func reload() {
        let new1 = Self.load(.one, from: defaults)
        let new2 = Self.load(.two, from: defaults)
        guard new1 != p1 || new2 != p2 else { return }
        p1 = new1
        p2 = new2
        onChange(p1, p2)
    }

    private func update(_ player: Player, _ mutate: (inout PlayerScore) -> Void) {
        var score = score(player)
        mutate(&score)
        if player == .one { p1 = score } else { p2 = score }
        save(player)
        onChange(p1, p2)
    }

    private func save(_ player: Player) {
        let score = score(player)
        defaults?.set(score.lifePoints, forKey: player.lifePointsKey)
        defaults?.set(score.historyRaw, forKey: player.historyKey)
    }

    private static func load(_ player: Player, from defaults: UserDefaults?) -> PlayerScore {
        PlayerScore(
            lifePoints: defaults?.object(forKey: player.lifePointsKey) as? Int ?? GameConstants.initialLifePoints,
            historyRaw: defaults?.string(forKey: player.historyKey) ?? ""
        )
    }
}

private extension Player {
    var lifePointsKey: String { self == .one ? GameConstants.lifePointsP1Key : GameConstants.lifePointsP2Key }
    var historyKey: String { self == .one ? GameConstants.historyP1Key : GameConstants.historyP2Key }
}
