//
//  CalculatorState.swift
//  LPCounter
//

import Foundation

nonisolated enum CalculatorOperation {
    case increase
    case decrease
}

/// Keypad rules (SPEC §3), kept out of the view so they can be tested.
nonisolated struct CalculatorState: Equatable {
    static let maxDigits = 4

    private(set) var input = 0
    var operation: CalculatorOperation = .decrease

    private var digits: Int { input == 0 ? 0 : String(input).count }

    /// 1-2 digits are shorthand for hundreds (5 -> 500, 25 -> 2500); 3+ digits are literal.
    var value: Int { digits >= 3 ? input : input * 100 }

    /// Signed change to apply to the life points.
    var delta: Int { operation == .increase ? value : -value }

    mutating func append(_ digit: Int) {
        if input == 0 && digit == 0 { return }
        guard digits < Self.maxDigits else { return }
        input = input * 10 + digit
    }

    mutating func clear() { input = 0 }

    mutating func toggleOperation() {
        operation = operation == .increase ? .decrease : .increase
    }
}
