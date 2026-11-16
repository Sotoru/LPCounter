//
//  GameViewModelTests.swift
//  LPCounterTests
//
//  Mirrors android/GameViewModel + GameRepository: persistence, change callbacks and SPEC §2 at the model level.
//

import Foundation
import Testing
@testable import LPCounter

@MainActor
struct GameViewModelTests {
    private func makeDefaults() -> UserDefaults {
        let suite = "GameViewModelTests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        return defaults
    }

    @Test func startsAtInitialForBothPlayers() {
        let model = GameViewModel(defaults: makeDefaults())
        #expect(model.p1.lifePoints == 8000)
        #expect(model.p2.lifePoints == 8000)
    }

    @Test func changesAreIndependentPerPlayer() {
        let model = GameViewModel(defaults: makeDefaults())
        model.changeBy(.one, -1000)
        model.changeBy(.two, 500)
        #expect(model.p1.lifePoints == 7000)
        #expect(model.p2.lifePoints == 8500)
        #expect(model.p1.history == [-1000])
        #expect(model.p2.history == [500])
    }

    @Test func stateSurvivesRecreation() {
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults)
        model.changeBy(.one, -2400)
        model.changeBy(.one, 300)

        let reloaded = GameViewModel(defaults: defaults)
        #expect(reloaded.p1.lifePoints == 5900)
        #expect(reloaded.p1.history == [-2400, 300])
    }

    @Test func undoIsRelativeAfterAWidgetStyleWrite() {
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults)
        model.changeBy(.one, -500)

        // The widget writes through PlayerScore into the same keys while the app is in the background.
        var score = PlayerScore(lifePoints: model.p1.lifePoints, history: model.p1.history)
        score.change(by: -1000)
        defaults.set(score.lifePoints, forKey: GameConstants.lifePointsP1Key)
        defaults.set(score.historyRaw, forKey: GameConstants.historyP1Key)

        model.reload()
        #expect(model.p1.lifePoints == 6500)
        model.undo(.one)
        #expect(model.p1.lifePoints == 7500)
        #expect(model.p1.history == [-500])
    }

    @Test func resetGameClearsBothHistories() {
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults)
        model.changeBy(.one, -1000)
        model.changeBy(.two, -2000)

        model.resetGame()
        #expect(model.p1.lifePoints == 8000 && model.p2.lifePoints == 8000)
        #expect(model.p1.history.isEmpty && model.p2.history.isEmpty)
        // Cleared in the store too, so the widget and a relaunch agree.
        #expect(GameViewModel(defaults: defaults).p1.history.isEmpty)

        model.changeBy(.one, -300)
        model.reset(.one)
        #expect(model.p1.lifePoints == 8000)
        #expect(model.p1.history.isEmpty)
    }

    @Test func startingLifePointsApplyNowOnlyIfTheDuelHasNotStarted() {
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults)
        model.setStartingLifePoints(4000)
        #expect(model.p1.lifePoints == 4000 && model.p2.lifePoints == 4000)

        model.changeBy(.one, -500)
        model.setStartingLifePoints(8000)
        #expect(model.p1.lifePoints == 3500)
        model.resetGame()
        #expect(model.p1.lifePoints == 8000 && model.p2.lifePoints == 8000)
        #expect(GameViewModel(defaults: defaults).startingLifePoints == 8000)
    }

    @Test func duelIsOverWhenAPlayerReachesZero() {
        let model = GameViewModel(defaults: makeDefaults())
        #expect(!model.isOver)
        model.changeBy(.two, -9900)
        #expect(model.isOver)
        model.resetGame()
        #expect(!model.isOver)
    }

    @Test func onChangeFiresOnEveryWriteAndOnlyOnExternalDifferences() {
        var calls = 0
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults, onChange: { _, _ in calls += 1 })

        model.changeBy(.one, -100)
        model.halve(.two)
        model.resetGame()
        #expect(calls == 3)

        model.reload()
        #expect(calls == 3)

        defaults.set(1234, forKey: GameConstants.lifePointsP2Key)
        model.reload()
        #expect(calls == 4)
        #expect(model.p2.lifePoints == 1234)
    }

    @Test func startDuelStartsTimerOnlyWhenNoneIsRunning() {
        var clock = Date(timeIntervalSince1970: 1000)
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults, now: { clock })
        model.startDuel()
        #expect(model.timerEndsAt == nil)
        model.setTimerMinutes(50)
        model.startDuel()
        let endsAt = clock + 50 * 60
        #expect(model.timerEndsAt == endsAt)
        clock += 60
        model.startDuel()
        #expect(model.timerEndsAt == endsAt)
        #expect(GameViewModel(defaults: defaults).timerEndsAt == endsAt)
        clock = endsAt
        model.startDuel()
        #expect(model.timerEndsAt == clock + 50 * 60)
        model.setTimerMinutes(0)
        model.startDuel()
        #expect(model.timerEndsAt == nil)
    }

    @Test func clearAllKeepsSettings() {
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults)
        model.setStartingLifePoints(4000)
        model.setTimerMinutes(50)
        model.changeBy(.one, -500)
        model.resetGame()
        model.startDuel()
        model.clearAll()
        let reloaded = GameViewModel(defaults: defaults)
        #expect(reloaded.p1 == PlayerScore(lifePoints: 4000) && reloaded.p2 == PlayerScore(lifePoints: 4000))
        #expect(reloaded.timerEndsAt == nil)
        #expect(reloaded.timerMinutes == 50)
    }

    @Test func timerLengthIsClampedAndOffStopsTheRound() {
        let model = GameViewModel(defaults: makeDefaults())
        model.setTimerMinutes(45)
        #expect(model.timerMinutes == 45)
        model.setTimerMinutes(500)
        #expect(model.timerMinutes == GameConstants.timerRange.upperBound)
        model.setTimerMinutes(-3)
        #expect(model.timerMinutes == GameConstants.timerRange.lowerBound)
        model.startDuel()
        #expect(model.timerEndsAt != nil)
        model.setTimerMinutes(0)
        #expect(model.timerEndsAt == nil)
    }

    @Test func pauseKeepsTheTimeLeftAndOnlyRemoveDeletesIt() {
        var clock = Date(timeIntervalSince1970: 1000)
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults, now: { clock })
        model.setTimerMinutes(45)
        model.startDuel()
        clock += 600
        model.pauseTimer()
        #expect(model.timerEndsAt == nil)
        #expect(model.timerPausedRemaining.map { abs($0 - 35 * 60) < 0.001 } == true, "got \(String(describing: model.timerPausedRemaining))")
        #expect(model.hasTimer)
        let reloaded = GameViewModel(defaults: defaults).timerPausedRemaining
        #expect(reloaded.map { abs($0 - 35 * 60) < 0.001 } == true, "got \(String(describing: reloaded))")

        clock += 3600  // a paused timer does not run down
        model.startDuel()  // Play resumes it instead of starting over
        #expect(model.timerEndsAt.map { abs($0.timeIntervalSince(clock) - 35 * 60) < 0.001 } == true)

        model.removeTimer()
        #expect(!model.hasTimer)
        #expect(model.timerMinutes == 45)  // the chosen length stays for the next Play
    }
}
