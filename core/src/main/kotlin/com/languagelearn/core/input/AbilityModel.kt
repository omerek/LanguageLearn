package com.languagelearn.core.input

import com.languagelearn.core.model.CefrLevel
import kotlin.math.exp

/** Elo-style ability estimate on the same 0-100 scale as sentence difficulty. */
object AbilityModel {
    const val SCALE = 8.0
    const val K_CALIBRATION = 4.0
    const val K_STEADY = 2.0
    const val CALIBRATION_EXERCISES = 30

    fun expected(theta: Double, difficulty: Double): Double =
        1.0 / (1.0 + exp(-(theta - difficulty) / SCALE))

    fun update(theta: Double, difficulty: Double, outcome: Double, exercisesDone: Int): Double {
        val k = if (exercisesDone < CALIBRATION_EXERCISES) K_CALIBRATION else K_STEADY
        return (theta + k * (outcome - expected(theta, difficulty))).coerceIn(0.0, 100.0)
    }

    /** Start just above the band so the first sentences of the chosen level fall inside the window. */
    fun initial(level: CefrLevel): Double = level.bandStart + 4.0
}
