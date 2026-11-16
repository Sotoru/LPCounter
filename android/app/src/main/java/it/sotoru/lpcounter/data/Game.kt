package it.sotoru.lpcounter.data

const val INITIAL_LIFE_POINTS = 8000

/** Starting life points the user can pick (SPEC §5): standard and Speed Duel. */
val STARTING_LIFE_POINTS_OPTIONS = listOf(INITIAL_LIFE_POINTS, 4000)

/** Timer choices in minutes (SPEC §9): off, or a 50-minute tournament round. */
val TIMER_OPTIONS = listOf(0, 50)

/** A finished duel (SPEC §1): when it ended and both final totals. */
data class Duel(val endedAt: Long, val one: Int, val two: Int)

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
    val startingLifePoints: Int = INITIAL_LIFE_POINTS,
    /** Newest first. */
    val duels: List<Duel> = emptyList(),
    val timerMinutes: Int = 0,
    /** Epoch millis the running round ends at; null when no timer was started. */
    val timerEndsAt: Long? = null,
) {
    /** The duel is over when a player has no life points left (SPEC §4). */
    val isOver: Boolean get() = lifePoints.one == 0 || lifePoints.two == 0
}
