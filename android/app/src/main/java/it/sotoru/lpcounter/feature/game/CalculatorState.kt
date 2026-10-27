package it.sotoru.lpcounter.feature.game

enum class Operation { Increase, Decrease }

private const val MAX_DIGITS = 4

/** Keypad state; mirrors the rules of the iOS CalculatorView. */
data class CalculatorState(
    val input: Int = 0,
    val operation: Operation = Operation.Decrease,
) {
    private val digits get() = if (input == 0) 0 else input.toString().length

    /** 1-2 digits are shorthand for hundreds (5 -> 500, 25 -> 2500); 3+ digits are literal. */
    val value get() = if (digits >= 3) input else input * 100

    fun append(digit: Int): CalculatorState = when {
        input == 0 && digit == 0 -> this
        digits >= MAX_DIGITS -> this
        else -> copy(input = input * 10 + digit)
    }

    fun clear() = copy(input = 0)

    fun toggleOperation() =
        copy(operation = if (operation == Operation.Increase) Operation.Decrease else Operation.Increase)

    /** New life total after applying the pending operation, never below zero. */
    fun applyTo(lifePoints: Int): Int = (lifePoints + delta).coerceAtLeast(0)

    private val delta get() = if (operation == Operation.Increase) value else -value
}
