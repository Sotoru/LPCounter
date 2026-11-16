//
//  WatchLink.swift
//  LPCounter
//
//  The iPhone side of the Watch companion (SPEC §6b): sends Player 1's state to the Watch and applies
//  the Watch's changes to the shared store with the same rules as the app and the widgets, so they land
//  in the history and can be undone here. Works with the app in the background too.
//

#if os(iOS)
import WatchConnectivity
import WidgetKit

@MainActor
final class WatchLink: NSObject, WCSessionDelegate {
    static let shared = WatchLink()

    /// Set by the app's root: re-reads the store into the open view model after a Watch change.
    var onStoreChange: () -> Void = {}

    private let defaults = UserDefaults(suiteName: GameConstants.appGroup)

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    /// Pushes Player 1's state and the look; the Watch keeps the latest one only.
    func push() {
        let session = WCSession.default
        guard WCSession.isSupported(), session.activationState == .activated, session.isWatchAppInstalled else { return }
        try? session.updateApplicationContext(state())
    }

    private func state() -> [String: Any] {
        let score = load()
        return [
            WatchMessage.lifePoints: score.lifePoints,
            WatchMessage.history: score.historyRaw,
            WatchMessage.palette: LED.palette.rawValue,
            WatchMessage.pixels: PixelStyle.stored.rawValue,
        ]
    }

    private func load() -> PlayerScore {
        PlayerScore(
            lifePoints: defaults?.object(forKey: GameConstants.lifePointsP1Key) as? Int ?? GameConstants.initialLifePoints,
            historyRaw: defaults?.string(forKey: GameConstants.historyP1Key) ?? ""
        )
    }

    /// A change from the Watch, through `PlayerScore` like the widget's buttons.
    private func apply(delta: Int?, undo: Bool) {
        var score = load()
        if undo { score.undo() } else if let delta { score.change(by: delta) } else { return }
        defaults?.set(score.lifePoints, forKey: GameConstants.lifePointsP1Key)
        defaults?.set(score.historyRaw, forKey: GameConstants.historyP1Key)
        for kind in GameConstants.widgetKinds { WidgetCenter.shared.reloadTimelines(ofKind: kind) }
        onStoreChange()
        push()
    }

    // MARK: WCSessionDelegate

    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        Task { @MainActor in self.push() }
    }

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    /// After switching to another Watch: start again with it.
    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let delta = message[WatchMessage.delta] as? Int, undo = message[WatchMessage.undo] as? Bool ?? false
        Task { @MainActor in self.apply(delta: delta, undo: undo) }
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        let delta = userInfo[WatchMessage.delta] as? Int, undo = userInfo[WatchMessage.undo] as? Bool ?? false
        Task { @MainActor in self.apply(delta: delta, undo: undo) }
    }
}
#endif
