package dev.ensisdev.ensnightmarket.animation

enum class ParticleShape {
    POINT,
    SPHERE,
    RING,
    SPIRAL,
    HELIX,
    BURST,
    WAVE,
    ORBIT,
    CONE,
    DISC,
    WALL,
    EXPLOSION,
    IMPACT,
    SHOCKWAVE
}

enum class MotionPattern {
    STATIC,
    BOB,
    WAVE,
    ORBIT,
    FIGURE_EIGHT,
    WOBBLE,
    PULSE,
    SWAY,
    DRIFT,
    VORTEX
}

enum class AnimationQuality(val particleMultiplier: Float) {
    LOW(0.35f),
    MEDIUM(0.6f),
    HIGH(1.0f),
    ULTRA(1.5f)
}
