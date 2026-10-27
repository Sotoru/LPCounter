// LPCounter/Utils/LiveActivityManager.swift
#if os(iOS)
import ActivityKit
import Foundation

@MainActor
final class LiveActivityManager {
    static let shared = LiveActivityManager()
    private init() {}

    func startActivity(game: Game) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            print("❌ Live Activities non abilitate")
            return
        }

        let content = ActivityContent(
            state: GameLiveAttributes.ContentState(
                firstPlayerLp: game.firstPlayerLp,
                secondPlayerLp: game.secondPlayerLp,
                lastUpdate: Date()
            ),
            staleDate: nil
        )

        do {
            _ = try Activity.request(attributes: GameLiveAttributes(), content: content)
        } catch {
            print("❌ Errore avvio Live Activity: \(error)")
        }
    }

    func updateActivity(game: Game) {
        // Cattura solo valori Sendable; l'activity viene recuperata fuori dal main actor.
        let p1 = game.firstPlayerLp
        let p2 = game.secondPlayerLp
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

    func endActivity() {
        Task.detached {
            let content = ActivityContent(
                state: GameLiveAttributes.ContentState(
                    firstPlayerLp: 0,
                    secondPlayerLp: 0,
                    lastUpdate: Date()
                ),
                staleDate: nil
            )
            for activity in Activity<GameLiveAttributes>.activities {
                await activity.end(content, dismissalPolicy: .immediate)
            }
        }
    }
}
#endif
