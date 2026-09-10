package dev.ensisdev.ensnightmarket.animation

import kotlin.math.*

enum class EasingType {
    LINEAR,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_IN_QUART,
    EASE_OUT_QUART,
    EASE_IN_OUT_QUART,
    EASE_IN_QUINT,
    EASE_OUT_QUINT,
    EASE_IN_OUT_QUINT,
    EASE_IN_SINE,
    EASE_OUT_SINE,
    EASE_IN_OUT_SINE,
    EASE_IN_EXPO,
    EASE_OUT_EXPO,
    EASE_IN_OUT_EXPO,
    EASE_IN_CIRC,
    EASE_OUT_CIRC,
    EASE_IN_OUT_CIRC,
    EASE_IN_BACK,
    EASE_OUT_BACK,
    EASE_IN_OUT_BACK,
    EASE_IN_ELASTIC,
    EASE_OUT_ELASTIC,
    EASE_IN_OUT_ELASTIC,
    EASE_IN_BOUNCE,
    EASE_OUT_BOUNCE,
    EASE_IN_OUT_BOUNCE,
    EASE_OUT_ELASTIC_STRONG,
    EASE_OUT_BACK_STRONG;

    companion object {
        private const val PI = Math.PI.toFloat()
        private const val C1 = 1.70158f
        private const val C2 = C1 * 1.525f
        private const val C3 = C1 + 1f
        private const val C4 = (2f * PI) / 3f
        private const val C5 = (2f * PI) / 4.5f
        private const val N1 = 7.5625f
        private const val D1 = 2.75f

        fun ease(type: EasingType, t: Float): Float {
            val clamped = t.coerceIn(0f, 1f)
            return when (type) {
                LINEAR -> clamped
                EASE_IN_QUAD -> clamped * clamped
                EASE_OUT_QUAD -> 1f - (1f - clamped) * (1f - clamped)
                EASE_IN_OUT_QUAD -> if (clamped < 0.5f) 2f * clamped * clamped else 1f - (-2f * clamped + 2f).pow(2) / 2f
                EASE_IN_CUBIC -> clamped.pow(3)
                EASE_OUT_CUBIC -> 1f - (1f - clamped).pow(3)
                EASE_IN_OUT_CUBIC -> if (clamped < 0.5f) 4f * clamped.pow(3) else 1f - (-2f * clamped + 2f).pow(3) / 2f
                EASE_IN_QUART -> clamped.pow(4)
                EASE_OUT_QUART -> 1f - (1f - clamped).pow(4)
                EASE_IN_OUT_QUART -> if (clamped < 0.5f) 8f * clamped.pow(4) else 1f - (-2f * clamped + 2f).pow(4) / 2f
                EASE_IN_QUINT -> clamped.pow(5)
                EASE_OUT_QUINT -> 1f - (1f - clamped).pow(5)
                EASE_IN_OUT_QUINT -> if (clamped < 0.5f) 16f * clamped.pow(5) else 1f - (-2f * clamped + 2f).pow(5) / 2f
                EASE_IN_SINE -> 1f - cos((clamped * PI) / 2f)
                EASE_OUT_SINE -> sin((clamped * PI) / 2f)
                EASE_IN_OUT_SINE -> -(cos(PI * clamped) - 1f) / 2f
                EASE_IN_EXPO -> if (clamped == 0f) 0f else 2f.pow(10f * clamped - 10f)
                EASE_OUT_EXPO -> if (clamped == 1f) 1f else 1f - 2f.pow(-10f * clamped)
                EASE_IN_OUT_EXPO -> when {
                    clamped == 0f -> 0f
                    clamped == 1f -> 1f
                    clamped < 0.5f -> 2f.pow(20f * clamped - 10f) / 2f
                    else -> (2f - 2f.pow(-20f * clamped + 10f)) / 2f
                }
                EASE_IN_CIRC -> 1f - sqrt(1f - clamped.pow(2))
                EASE_OUT_CIRC -> sqrt(1f - (clamped - 1f).pow(2))
                EASE_IN_OUT_CIRC -> if (clamped < 0.5f) (1f - sqrt(1f - (2f * clamped).pow(2))) / 2f else (sqrt(1f - (-2f * clamped + 2f).pow(2)) + 1f) / 2f
                EASE_IN_BACK -> C3 * clamped.pow(3) - C1 * clamped.pow(2)
                EASE_OUT_BACK -> 1f + C3 * (clamped - 1f).pow(3) + C1 * (clamped - 1f).pow(2)
                EASE_IN_OUT_BACK -> if (clamped < 0.5f) ((2f * clamped).pow(2) * ((C2 + 1f) * 2f * clamped - C2)) / 2f else ((2f * clamped - 2f).pow(2) * ((C2 + 1f) * (clamped * 2f - 2f) + C2) + 2f) / 2f
                EASE_IN_ELASTIC -> when {
                    clamped == 0f -> 0f
                    clamped == 1f -> 1f
                    else -> -(2f.pow(10f * clamped - 10f)) * sin((clamped * 10f - 10.75f) * C4)
                }
                EASE_OUT_ELASTIC -> when {
                    clamped == 0f -> 0f
                    clamped == 1f -> 1f
                    else -> 2f.pow(-10f * clamped) * sin((clamped * 10f - 0.75f) * C4) + 1f
                }
                EASE_IN_OUT_ELASTIC -> when {
                    clamped == 0f -> 0f
                    clamped == 1f -> 1f
                    clamped < 0.5f -> -(2f.pow(20f * clamped - 10f) * sin((20f * clamped - 11.125f) * C5)) / 2f
                    else -> (2f.pow(-20f * clamped + 10f) * sin((20f * clamped - 11.125f) * C5)) / 2f + 1f
                }
                EASE_IN_BOUNCE -> 1f - ease(EASE_OUT_BOUNCE, 1f - clamped)
                EASE_OUT_BOUNCE -> when {
                    clamped < 1f / D1 -> N1 * clamped.pow(2)
                    clamped < 2f / D1 -> N1 * (clamped - 1.5f / D1).pow(2) + 0.75f
                    clamped < 2.5f / D1 -> N1 * (clamped - 2.25f / D1).pow(2) + 0.9375f
                    else -> N1 * (clamped - 2.625f / D1).pow(2) + 0.984375f
                }
                EASE_IN_OUT_BOUNCE -> if (clamped < 0.5f) (1f - ease(EASE_OUT_BOUNCE, 1f - 2f * clamped)) / 2f else (1f + ease(EASE_OUT_BOUNCE, 2f * clamped - 1f)) / 2f
                EASE_OUT_ELASTIC_STRONG -> when {
                    clamped == 0f -> 0f
                    clamped == 1f -> 1f
                    else -> 2f.pow(-15f * clamped) * sin((clamped * 15f - 0.75f) * C4) + 1f
                }
                EASE_OUT_BACK_STRONG -> {
                    val c1Strong = 2.70158f
                    val c3Strong = c1Strong + 1f
                    1f + c3Strong * (clamped - 1f).pow(3) + c1Strong * (clamped - 1f).pow(2)
                }
            }
        }
    }
}
