//
//  PlayerScoreTests.swift
//  LPCounterTests
//
//  Mirrors android/GameRepositoryTest: the rules come from SPEC §2.
//

import Testing
@testable import LPCounter

struct PlayerScoreTests {
    @Test func startsAtInitialWithEmptyHistory() {
        let score = PlayerScore()
        #expect(score.lifePoints == 8000)
        #expect(score.history.isEmpty)
    }

    @Test func changeRecordsSignedChanges() {
        var score = PlayerScore()
        score.change(by: -500)
        score.change(by: 1000)
        #expect(score.history == [-500, 1000])
        #expect(score.lifePoints == 8500)
    }

    @Test func changeClampsAtZeroAndRecordsAppliedChange() {
        var score = PlayerScore()
        score.change(by: -9000)
        #expect(score.lifePoints == 0)
        #expect(score.history == [-8000])
    }

    @Test func noOpChangeIsNotRecorded() {
        var score = PlayerScore()
        score.change(by: 0)
        #expect(score.history.isEmpty)
    }

    @Test func halveRecordsTheChange() {
        var score = PlayerScore(lifePoints: 7500)
        score.halve()
        #expect(score.lifePoints == 3750)
        #expect(score.history == [-3750])
    }

    @Test func halveAtZeroIsNotRecorded() {
        var score = PlayerScore(lifePoints: 0)
        score.halve()
        #expect(score.history.isEmpty)
    }

    @Test func undoIsRelativeToCurrentTotal() {
        // A change that bypassed the history (e.g. older data) must survive the undo.
        var score = PlayerScore(lifePoints: 9000, history: [-500])
        score.undo()
        #expect(score.lifePoints == 9500)
        #expect(score.history.isEmpty)
    }

    @Test func undoWithEmptyHistoryDoesNothing() {
        var score = PlayerScore()
        score.undo()
        #expect(score == PlayerScore())
    }

    @Test func resetClearsLifePointsAndHistory() {
        var score = PlayerScore(lifePoints: 100, history: [-7900])
        score.reset()
        #expect(score == PlayerScore())
    }

    @Test func historyIsBounded() {
        var score = PlayerScore()
        for _ in 0..<250 { score.change(by: 1) }
        #expect(score.history.count == 200)
    }

    @Test func historyRoundTripsThroughStorageForm() {
        let score = PlayerScore(lifePoints: 7300, history: [-500, 1000, -300, -300])
        #expect(score.historyRaw == "-500,1000,-300,-300")
        #expect(PlayerScore(lifePoints: 7300, historyRaw: score.historyRaw) == score)
        #expect(PlayerScore(lifePoints: 8000, historyRaw: "").history.isEmpty)
    }
}
