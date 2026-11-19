package it.sotoru.lpcounter.data

const val INITIAL_LIFE_POINTS = 8000

/** Starting life points the user can pick (SPEC §5): standard and Speed Duel. */
val STARTING_LIFE_POINTS_OPTIONS = listOf(INITIAL_LIFE_POINTS, 4000)

/** Timer presets in minutes (SPEC §9); any other length in [TIMER_RANGE] can be set by hand. */
val TIMER_PRESETS = listOf(45, 50)
val TIMER_RANGE = 1..180

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
    val timerMinutes: Int = 0,
    /** Epoch millis the running round ends at; null when no timer runs. */
    val timerEndsAt: Long? = null,
    /** Millis left while the timer is paused; null when it runs or there is none. */
    val timerPausedRemaining: Long? = null,
) {
    /** The duel is over when a player has no life points left (SPEC §4). */
    val isOver: Boolean get() = lifePoints.one == 0 || lifePoints.two == 0

    /** Some life points changed since the duel started (SPEC §9). */
    val inProgress: Boolean get() = lifePoints != LifePoints(startingLifePoints, startingLifePoints)

    /** A timer exists for this duel, running or paused. */
    val hasTimer: Boolean get() = timerEndsAt != null || timerPausedRemaining != null
}
