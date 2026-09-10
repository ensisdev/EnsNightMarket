package dev.ensisdev.ensnightmarket.animation

import org.bukkit.util.Vector

object MotionPatterns {

    fun calculateOffset(pattern: MotionPattern, time: Double, amplitude: Double, speed: Double): Vector {
        val t = time * speed
        return when (pattern) {
            MotionPattern.STATIC -> Vector(0.0, 0.0, 0.0)
            MotionPattern.BOB -> Vector(0.0, kotlin.math.sin(t) * amplitude, 0.0)
            MotionPattern.WAVE -> Vector(kotlin.math.sin(t * 0.5) * amplitude * 0.3, kotlin.math.sin(t) * amplitude, kotlin.math.cos(t * 0.5) * amplitude * 0.3)
            MotionPattern.ORBIT -> Vector(kotlin.math.cos(t) * amplitude, kotlin.math.sin(t * 2) * amplitude * 0.2, kotlin.math.sin(t) * amplitude)
            MotionPattern.FIGURE_EIGHT -> Vector(kotlin.math.sin(t) * amplitude, kotlin.math.sin(t * 2) * amplitude * 0.3, kotlin.math.sin(t * 2) * amplitude * 0.5)
            MotionPattern.WOBBLE -> Vector(kotlin.math.sin(t * 1.3) * amplitude * 0.2, kotlin.math.sin(t) * amplitude, kotlin.math.cos(t * 0.7) * amplitude * 0.2)
            MotionPattern.PULSE -> {
                val pulse = (kotlin.math.sin(t * 2) + 1) * 0.5
                Vector(0.0, pulse * amplitude * 0.5, 0.0)
            }
            MotionPattern.SWAY -> Vector(kotlin.math.sin(t * 0.8) * amplitude * 0.4, kotlin.math.sin(t) * amplitude, kotlin.math.cos(t * 0.6) * amplitude * 0.4)
            MotionPattern.DRIFT -> Vector(kotlin.math.sin(t * 0.3) * amplitude * 0.5, kotlin.math.sin(t) * amplitude, kotlin.math.cos(t * 0.4) * amplitude * 0.5)
            MotionPattern.VORTEX -> {
                val radius = amplitude * (0.5 + kotlin.math.sin(t * 0.5) * 0.5)
                Vector(kotlin.math.cos(t * 2) * radius, kotlin.math.sin(t) * amplitude, kotlin.math.sin(t * 2) * radius)
            }
        }
    }

    fun calculateRotation(pattern: MotionPattern, time: Double, speed: Double, baseRotation: Double): Double {
        val t = time * speed
        return when (pattern) {
            MotionPattern.STATIC -> baseRotation
            MotionPattern.BOB -> baseRotation + kotlin.math.sin(t * 0.5) * 10.0
            MotionPattern.WAVE -> baseRotation + kotlin.math.sin(t * 0.3) * 15.0
            MotionPattern.ORBIT -> baseRotation + t * 20.0
            MotionPattern.FIGURE_EIGHT -> baseRotation + kotlin.math.sin(t) * 20.0
            MotionPattern.WOBBLE -> baseRotation + kotlin.math.sin(t * 1.5) * 25.0
            MotionPattern.PULSE -> baseRotation + kotlin.math.sin(t * 2) * 10.0
            MotionPattern.SWAY -> baseRotation + kotlin.math.sin(t * 0.7) * 12.0
            MotionPattern.DRIFT -> baseRotation + kotlin.math.sin(t * 0.4) * 8.0
            MotionPattern.VORTEX -> baseRotation + t * 30.0
        }
    }

    fun calculateScaleMultiplier(pattern: MotionPattern, time: Double, speed: Double): Double {
        val t = time * speed
        return when (pattern) {
            MotionPattern.STATIC -> 1.0
            MotionPattern.BOB -> 1.0 + kotlin.math.sin(t * 2) * 0.05
            MotionPattern.WAVE -> 1.0 + kotlin.math.sin(t * 1.5) * 0.08
            MotionPattern.ORBIT -> 1.0 + kotlin.math.sin(t) * 0.06
            MotionPattern.FIGURE_EIGHT -> 1.0 + kotlin.math.sin(t * 2) * 0.07
            MotionPattern.WOBBLE -> 1.0 + kotlin.math.sin(t * 1.3) * 0.04
            MotionPattern.PULSE -> 1.0 + (kotlin.math.sin(t * 3) + 1) * 0.1
            MotionPattern.SWAY -> 1.0 + kotlin.math.sin(t * 0.8) * 0.05
            MotionPattern.DRIFT -> 1.0 + kotlin.math.sin(t * 0.5) * 0.06
            MotionPattern.VORTEX -> 1.0 + kotlin.math.sin(t * 2) * 0.1
        }
    }

    fun getDefaultPattern(rarityId: String): MotionPattern {
        return when (rarityId.lowercase()) {
            "common" -> MotionPattern.BOB
            "uncommon" -> MotionPattern.WAVE
            "rare" -> MotionPattern.ORBIT
            "legendary" -> MotionPattern.VORTEX
            "mysterious" -> MotionPattern.FIGURE_EIGHT
            else -> MotionPattern.BOB
        }
    }
}
