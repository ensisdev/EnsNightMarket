package dev.ensisdev.ensnightmarket.performance

import org.bukkit.Location
import org.bukkit.entity.Player

object LODSystem {

    data class LODLevel(
        val maxDistance: Double,
        val particleMultiplier: Float,
        val updateRate: Int
    )

    private val lodLevels = listOf(
        LODLevel(8.0, 1.0f, 1),
        LODLevel(16.0, 0.75f, 1),
        LODLevel(24.0, 0.5f, 2),
        LODLevel(32.0, 0.25f, 4)
    )

    fun getLODLevel(distance: Double): LODLevel {
        return lodLevels.firstOrNull { distance <= it.maxDistance }
            ?: lodLevels.last()
    }

    fun getLODLevelForPlayer(player: Player, target: Location): LODLevel {
        val distance = player.location.distance(target)
        return getLODLevel(distance)
    }

    fun calculateParticleCount(baseCount: Int, lodLevel: LODLevel): Int {
        return (baseCount * lodLevel.particleMultiplier).toInt().coerceAtLeast(1)
    }

    fun shouldUpdate(tick: Int, lodLevel: LODLevel): Boolean {
        return tick % lodLevel.updateRate == 0
    }
}

object PerformanceMonitor {
    private val animationTimes = mutableMapOf<String, Long>()
    private val particleCounts = mutableMapOf<String, Int>()
    private var totalParticles = 0
    private var totalAnimationTime = 0L
    private var frameCount = 0
    private var followTotal = 0L
    private var followFrames = 0
    /** Sliding window: lifetime averages go stale on long-running servers, so the
     *  counters reset automatically every 30 minutes. */
    private var windowStart = System.currentTimeMillis()
    private const val WINDOW_MS = 30 * 60_000L

    private fun window() {
        if (System.currentTimeMillis() - windowStart >= WINDOW_MS) reset()
    }

    fun startTracking(key: String) {
        animationTimes[key] = System.nanoTime()
    }

    fun endTracking(key: String) {
        window()
        val startTime = animationTimes[key] ?: return
        val elapsed = System.nanoTime() - startTime
        animationTimes[key] = elapsed
        totalAnimationTime = (totalAnimationTime + elapsed).coerceAtMost(Long.MAX_VALUE - 1_000_000_000L)
        frameCount++
    }

    fun recordParticles(key: String, count: Int) {
        window()
        particleCounts[key] = count
        totalParticles = (totalParticles + count).coerceAtMost(Int.MAX_VALUE - 1000)
    }

    fun recordFollow(nanos: Long) {
        window()
        followTotal = (followTotal + nanos).coerceAtMost(Long.MAX_VALUE - 1_000_000_000L)
        followFrames++
    }

    fun getAverageFollowTime(): Double {
        return if (followFrames > 0) followTotal.toDouble() / followFrames / 1_000_000.0 else 0.0
    }

    fun getAverageAnimationTime(): Double {
        return if (frameCount > 0) totalAnimationTime.toDouble() / frameCount / 1_000_000.0 else 0.0
    }

    fun getTotalParticles(): Int = totalParticles

    fun getParticleCounts(): Map<String, Int> = particleCounts.toMap()

    fun reset() {
        animationTimes.clear()
        particleCounts.clear()
        totalParticles = 0
        totalAnimationTime = 0L
        frameCount = 0
        followTotal = 0L
        followFrames = 0
        windowStart = System.currentTimeMillis()
    }

    fun getStats(): PerformanceStats {
        return PerformanceStats(
            averageAnimationTime = getAverageAnimationTime(),
            totalParticles = totalParticles,
            frameCount = frameCount,
            particleCounts = particleCounts.toMap(),
            followMs = getAverageFollowTime()
        )
    }

    data class PerformanceStats(
        val averageAnimationTime: Double,
        val totalParticles: Int,
        val frameCount: Int,
        val particleCounts: Map<String, Int>,
        val followMs: Double
    )
}
