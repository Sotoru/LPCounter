package it.sotoru.lpcounter

import it.sotoru.lpcounter.feature.game.CalculatorState
import it.sotoru.lpcounter.feature.game.Operation
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorStateTest {
    private fun type(vararg digits: Int) = digits.fold(CalculatorState()) { s, d -> s.append(d) }

    @Test fun shortInputIsHundreds() {
        assertEquals(500, type(5).value)
        assertEquals(2500, type(2, 5).value)
        assertEquals(300, type(3, 0, 0).value)
        assertEquals(1234, type(1, 2, 3, 4).value)
    }

    @Test fun leadingZeroAndMaxDigitsIgnored() {
        assertEquals(0, type(0).input)
        assertEquals(1234, type(1, 2, 3, 4, 5).input)
    }

    @Test fun appliesOperationAndClampsAtZero() {
        assertEquals(7500, type(5).applyTo(8000))
        assertEquals(0, type(9, 9).applyTo(8000))
        assertEquals(8500, type(5).toggleOperation().applyTo(8000))
        assertEquals(Operation.Decrease, CalculatorState().operation)
    }
}
