#if os(iOS)
import ActivityKit
import Foundation

nonisolated struct GameLiveAttributes: ActivityAttributes {
    public nonisolated struct ContentState: Codable, Hashable {
        var firstPlayerLp: Int
        var secondPlayerLp: Int
        var lastUpdate: Date
    }
}

// Preview data
extension GameLiveAttributes {
    static var preview: GameLiveAttributes {
        GameLiveAttributes()
    }
}

extension GameLiveAttributes.ContentState {
    static var preview: GameLiveAttributes.ContentState {
        GameLiveAttributes.ContentState(
            firstPlayerLp: 8000,
            secondPlayerLp: 7500,
            lastUpdate: Date()
        )
    }
}
#endif

