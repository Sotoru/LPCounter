package it.sotoru.lpcounter.data

const val INITIAL_LIFE_POINTS = 8000

enum class Player(val number: Int) { One(1), Two(2) }

data class LifePoints(
    val one: Int = INITIAL_LIFE_POINTS,
    val two: Int = INITIAL_LIFE_POINTS,
) {
    operator fun get(player: Player): Int = if (player == Player.One) one else two
}

/** [history] holds each player's signed changes, oldest first. */
data class GameState(
    val lifePoints: LifePoints,
    val history: Map<Player, List<Int>>,
)
