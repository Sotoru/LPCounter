//
//  WatchMessage.swift
//  LPCounter
//
//  The few keys the iPhone app and the Watch app exchange (SPEC §6b). Compiled into both targets.
//

import Foundation

nonisolated enum WatchMessage {
    // iPhone → Watch, as the application context: Player 1's state and the look.
    static let lifePoints = "lp"
    static let history = "history"
    static let palette = "palette"
    static let pixels = "pixels"

    // Watch → iPhone, as a message (or queued user info when the iPhone is out of reach).
    static let delta = "delta"
    static let undo = "undo"
}
