// LPCounter/Utils/LiveActivityManager.swift
#if os(iOS)
import ActivityKit
import Foundation

@MainActor
final class LiveActivityManager {
    static let shared = LiveActivityManager()
    private init() {}

    func startActivity(_ p1: Int, _ p2: Int) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        // One activity per duel: re-entering the game updates the one already on the Lock Screen.
        guard Activity<GameLiveAttributes>.activities.isEmpty else {
            updateActivity(p1, p2)
            return
        }

        let content = ActivityContent(
            state: GameLiveAttributes.ContentState(
                firstPlayerLp: p1,
                secondPlayerLp: p2,
                lastUpdate: Date()
            ),
            staleDate: nil
        )

        // Fails only if the system refuses (budget, permissions): the duel goes on without it.
        _ = try? Activity.request(attributes: GameLiveAttributes(), content: content)
    }

    func updateActivity(_ p1: Int, _ p2: Int) {
        Task.detached {
            let content = ActivityContent(
                state: GameLiveAttributes.ContentState(
                    firstPlayerLp: p1,
                    secondPlayerLp: p2,
                    lastUpdate: Date()
                ),
                staleDate: nil
            )
            for activity in Activity<GameLiveAttributes>.activities {
                await activity.update(content)
            }
        }
    }
}
#endif
