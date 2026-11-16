//
//  PhoneLink.swift
//  LPCounterWatch
//
//  The Watch side of the companion: receives Player 1's state and the look from the iPhone, sends it
//  changes. While the iPhone is out of reach the Watch shows the change at once and queues it; the iPhone
//  applies it when they meet again.
//

import Observation
import WatchConnectivity

@MainActor @Observable
final class PhoneLink: NSObject, WCSessionDelegate {
    private(set) var score = PlayerScore()
    private(set) var palette = LEDPalette.night
    private(set) var pixels = PixelStyle.round
    private(set) var isReachable = false

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func change(by delta: Int) {
        guard delta != 0 else { return }
        score.change(by: delta)
        send([WatchMessage.delta: delta])
    }

    func undo() {
        guard !score.history.isEmpty else { return }
        score.undo()
        send([WatchMessage.undo: true])
    }

    /// Live when the iPhone answers; otherwise queued, delivered in order when it is back.
    private func send(_ message: [String: Any]) {
        let session = WCSession.default
        if session.isReachable {
            session.sendMessage(message, replyHandler: nil) { _ in session.transferUserInfo(message) }
        } else {
            session.transferUserInfo(message)
        }
    }

    /// What the iPhone sends, as plain values so it can cross to the main actor.
    private struct State: Sendable {
        var lp: Int?
        var history: String
        var palette: String?
        var pixels: String?

        nonisolated init(_ context: [String: Any]) {
            lp = context[WatchMessage.lifePoints] as? Int
            history = context[WatchMessage.history] as? String ?? ""
            palette = context[WatchMessage.palette] as? String
            pixels = context[WatchMessage.pixels] as? String
        }
    }

    private func receive(_ state: State) {
        if let lp = state.lp { score = PlayerScore(lifePoints: lp, historyRaw: state.history) }
        if let raw = state.palette, let value = LEDPalette(rawValue: raw) {
            palette = value
            // `LED` colours read the shared store; on the Watch that is this app's own defaults.
            LED.store?.set(raw, forKey: LEDPalette.key)
        }
        if let raw = state.pixels, let value = PixelStyle(rawValue: raw) { pixels = value }
    }

    // MARK: WCSessionDelegate

    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let state = State(session.receivedApplicationContext), reachable = session.isReachable
        Task { @MainActor in
            self.receive(state)
            self.isReachable = reachable
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        let state = State(context)
        Task { @MainActor in self.receive(state) }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        let reachable = session.isReachable
        Task { @MainActor in self.isReachable = reachable }
    }
}
