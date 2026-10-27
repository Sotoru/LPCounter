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

    @Test func resetGameKeepsHistoryButResetPlayerClearsIt() {
        let model = GameViewModel(defaults: makeDefaults())
        model.changeBy(.one, -1000)
        model.changeBy(.two, -2000)

        model.resetGame()
        #expect(model.p1.lifePoints == 8000 && model.p2.lifePoints == 8000)
        #expect(model.p1.history == [-1000] && model.p2.history == [-2000])

        model.changeBy(.one, -300)
        model.reset(.one)
        #expect(model.p1.lifePoints == 8000)
        #expect(model.p1.history.isEmpty)
        #expect(model.p2.history == [-2000])
    }

    @Test func onChangeFiresOnEveryWriteAndOnlyOnExternalDifferences() {
        var calls = 0
        let defaults = makeDefaults()
        let model = GameViewModel(defaults: defaults) { _, _ in calls += 1 }

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
}
