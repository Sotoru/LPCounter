//
//  CalculatorStateTests.swift
//  LPCounterTests
//
//  Mirrors android/CalculatorStateTest: the examples come from SPEC §3.
//

import CoreGraphics
import Testing
@testable import LPCounter

struct CalculatorStateTests {
    private func type(_ digits: Int...) -> CalculatorState {
        var state = CalculatorState()
        digits.forEach { state.append($0) }
        return state
    }

    @Test func shortInputIsHundreds() {
        #expect(type(5).value == 500)
        #expect(type(2, 5).value == 2500)
        #expect(type(3, 0, 0).value == 300)
        #expect(type(1, 2, 3, 4).value == 1234)
    }

    @Test func leadingZeroAndMaxDigitsIgnored() {
        #expect(type(0).input == 0)
        #expect(type(1, 2, 3, 4, 5).input == 1234)
    }

    @Test func deltaFollowsOperation() {
        var state = type(5)
        #expect(state.delta == -500)
        state.toggleOperation()
        #expect(state.delta == 500)
        #expect(CalculatorState().operation == .decrease)
    }

    @Test func appliedToAScoreMatchesTheSpecTable() {
        func apply(_ state: CalculatorState) -> Int {
            var score = PlayerScore()
            score.change(by: state.delta)
            return score.lifePoints
        }
        var add = type(5)
        add.toggleOperation()

        #expect(apply(type(5)) == 7500)
        #expect(apply(add) == 8500)
        #expect(apply(type(9, 9)) == 0)
        #expect(apply(type(3, 0, 0)) == 7700)
    }
}

/// SPEC §4: swipes on the total.
@MainActor
struct SwipeTests {
    @Test func directionsMapToSteps() {
        #expect(CounterView.swipeDelta(CGSize(width: 80, height: 10)) == 100)
        #expect(CounterView.swipeDelta(CGSize(width: -80, height: -10)) == -100)
        #expect(CounterView.swipeDelta(CGSize(width: 10, height: -80)) == 1000)
        #expect(CounterView.swipeDelta(CGSize(width: -10, height: 80)) == -1000)
    }
}
