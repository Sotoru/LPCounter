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
    /// LP a new duel starts from (SPEC §5): 8000, or 4000 for Speed Duel.
    private(set) var startingLifePoints: Int
    private(set) var timerMinutes: Int
    /// When the running round ends; nil when no timer was started (SPEC §9).
    private(set) var timerEndsAt: Date?
    /// Seconds left while the timer is paused; nil when it runs or there is none.
    private(set) var timerPausedRemaining: TimeInterval?
    /// A timer exists for this duel, running or paused.
    var hasTimer: Bool { timerEndsAt != nil || timerPausedRemaining != nil }
    /// Whether the custom timer sheet is open; lives here so the Home and the game share one editor.
    var isEditingTimer = false
    /// Bumped by every game reset, so the totals can tell it from any other change and play its effect.
    private(set) var resets = 0

    private let defaults: UserDefaults?
    private let onChange: @MainActor (PlayerScore, PlayerScore) -> Void
    private let now: () -> Date

    init(
        defaults: UserDefaults? = UserDefaults(suiteName: GameConstants.appGroup),
        now: @escaping () -> Date = Date.init,
        onChange: @escaping @MainActor (PlayerScore, PlayerScore) -> Void = { _, _ in }
    ) {
        self.defaults = defaults
        self.onChange = onChange
        self.now = now
        // Recent duels were dropped from the app: clear what an older version stored.
        defaults?.removeObject(forKey: "recentDuels")
        timerMinutes = defaults?.integer(forKey: GameConstants.timerMinutesKey) ?? 0
        timerEndsAt = defaults?.object(forKey: GameConstants.timerEndsAtKey) as? Date
        timerPausedRemaining = defaults?.object(forKey: GameConstants.timerPausedKey) as? TimeInterval
        startingLifePoints = defaults?.object(forKey: GameConstants.startingLifePointsKey) as? Int ?? GameConstants.initialLifePoints
        p1 = Self.load(.one, from: defaults)
        p2 = Self.load(.two, from: defaults)
    }

    func score(_ player: Player) -> PlayerScore { player == .one ? p1 : p2 }

    func changeBy(_ player: Player, _ delta: Int) { update(player) { $0.change(by: delta) } }

    func halve(_ player: Player) { update(player) { $0.halve() } }

    func undo(_ player: Player) { update(player) { $0.undo() } }

    func reset(_ player: Player) { update(player) { [startingLifePoints] in $0.reset(to: startingLifePoints) } }

    /// The duel is over when a player has no life points left (SPEC §4).
    var isOver: Bool { p1.lifePoints == 0 || p2.lifePoints == 0 }

    /// Applies at once if the duel hasn't started, otherwise from the next reset (SPEC §5).
    func setStartingLifePoints(_ value: Int) {
        let untouched = p1.lifePoints == startingLifePoints && p2.lifePoints == startingLifePoints
        startingLifePoints = value
        defaults?.set(value, forKey: GameConstants.startingLifePointsKey)
        if untouched { resetGame() }
    }

    /// A new duel: both totals back to the start and both histories cleared (SPEC §2).
    func resetGame() {
        p1 = PlayerScore(lifePoints: startingLifePoints)
        p2 = PlayerScore(lifePoints: startingLifePoints)
        resets += 1
        save(.one)
        save(.two)
        onChange(p1, p2)
    }

    /// Some life points changed since the duel started (SPEC §9).
    var inProgress: Bool { p1.lifePoints != startingLifePoints || p2.lifePoints != startingLifePoints }

    func setTimerMinutes(_ value: Int) {
        let value = value == 0 ? 0 : min(max(value, GameConstants.timerRange.lowerBound), GameConstants.timerRange.upperBound)
        timerMinutes = value
        defaults?.set(value, forKey: GameConstants.timerMinutesKey)
        // "Off" removes the timer at once; a new length waits for Play.
        if value == 0 { removeTimer() }
    }

    /// A fresh timer of the chosen length, from now.
    func restartTimer() {
        guard timerMinutes > 0 else { return removeTimer() }
        setTimer(endsAt: now().addingTimeInterval(Double(timerMinutes) * 60), paused: nil)
    }

    /// Freezes the countdown where it is (SPEC §9); `resumeTimer()` picks it up again.
    func pauseTimer() {
        guard let endsAt = timerEndsAt else { return }
        setTimer(endsAt: nil, paused: max(0, endsAt.timeIntervalSince(now())))
    }

    func resumeTimer() {
        guard let left = timerPausedRemaining else { return }
        setTimer(endsAt: now().addingTimeInterval(left), paused: nil)
    }

    /// Removes the timer for this duel, running or paused; the chosen length stays for the next Play.
    func removeTimer() { setTimer(endsAt: nil, paused: nil) }

    private func setTimer(endsAt: Date?, paused: TimeInterval?) {
        timerEndsAt = endsAt
        timerPausedRemaining = paused
        defaults?.set(endsAt, forKey: GameConstants.timerEndsAtKey)
        defaults?.set(paused, forKey: GameConstants.timerPausedKey)
    }

    /// On Play (SPEC §9): starts a new timer unless one is running or paused (a paused one resumes);
    /// "off" removes it.
    func startDuel() {
        if timerMinutes == 0 {
            removeTimer()
        } else if timerPausedRemaining != nil {
            resumeTimer()
        } else if timerEndsAt.map({ $0 <= now() }) ?? true {
            restartTimer()
        }
    }

    /// Totals, history and timer; settings stay (SPEC §2).
    func clearAll() {
        p1 = PlayerScore(lifePoints: startingLifePoints)
        p2 = PlayerScore(lifePoints: startingLifePoints)
        removeTimer()
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
