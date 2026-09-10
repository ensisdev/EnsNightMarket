package dev.ensisdev.ensnightmarket.animation

import dev.ensisdev.ensnightmarket.animation.EasingType
import dev.ensisdev.ensnightmarket.market.MarketOffer
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.*

class RevealAnimator(private val plugin: JavaPlugin) {

    private val activeAnimations = mutableMapOf<UUID, RevealAnimation>()

    data class RevealAnimation(
        val offer: MarketOffer,
        val location: Location,
        val itemDisplay: ItemDisplay?,
        val textDisplay: TextDisplay?,
        val startTime: Long,
        val duration: Int,
        val rewardStack: ItemStack? = null,
        val finalScale: Float = 1.55f,
        val easing: EasingType = EasingType.EASE_OUT_ELASTIC,
        var currentTick: Int = 0,
        var phase: RevealPhase = RevealPhase.PRE_REVEAL,
        var cue: Int = 0,
        var task: BukkitRunnable? = null
    )

    enum class RevealPhase {
        PRE_REVEAL,
        EXPLOSION,
        SETTLE,
        IDLE
    }

    fun startReveal(
        offer: MarketOffer,
        location: Location,
        itemDisplay: ItemDisplay?,
        textDisplay: TextDisplay?,
        rewardStack: ItemStack? = null,
        finalScale: Float = 1.55f,
        easing: EasingType = EasingType.EASE_OUT_ELASTIC
    ) {
        val id = UUID.randomUUID()
        val duration = plugin.config.getInt("animation.reveal-duration-ticks", 24)

        val animation = RevealAnimation(
            offer = offer,
            location = location,
            itemDisplay = itemDisplay,
            textDisplay = textDisplay,
            startTime = System.currentTimeMillis(),
            duration = duration,
            rewardStack = rewardStack,
            finalScale = finalScale,
            easing = easing
        )

        activeAnimations[id] = animation

        if (fx("reveal-flash")) {
            location.world?.let { world ->
                dev.ensisdev.ensnightmarket.particle.ParticleShapes.flash(world, location.clone().add(0.0, 0.5, 0.0))
                world.spawnParticle(Particle.END_ROD, location.clone().add(0.0, 0.5, 0.0), 12, 0.4, 0.4, 0.4, 0.08)
            }
            textDisplay?.let { punchText(it, 1.35f) }
        }

        val task = object : BukkitRunnable() {
            override fun run() {
                tickAnimation(id)
            }
        }
        task.runTaskTimer(plugin, 0L, 1L)
        animation.task = task
    }

    private fun fx(kind: String): Boolean {
        if (!plugin.config.getBoolean("cinematics.enabled", true)) return false
        return plugin.config.getBoolean("cinematics.$kind", true)
    }

    private fun punchText(text: TextDisplay, scale: Float) {
        text.transformation = Transformation(
            Vector3f(0f, 0f, 0f),
            AxisAngle4f(),
            Vector3f(scale, scale, scale),
            AxisAngle4f()
        )
    }

    private fun ladder(animation: RevealAnimation, progress: Float) {
        if (!fx("reveal-sound-ladder")) return
        val world = animation.location.world ?: return
        val at = animation.location
        when {
            animation.cue == 0 && progress >= 0.2f -> { animation.cue = 1; SoundBank.note(world, at, 2, 0.5f) }
            animation.cue == 1 && progress >= 0.35f -> { animation.cue = 2; SoundBank.note(world, at, 4, 0.5f) }
            animation.cue == 2 && progress >= 0.5f -> { animation.cue = 3; SoundBank.note(world, at, 5, 0.55f) }
        }
    }
    private fun tickAnimation(id: UUID) {
        val animation = activeAnimations[id] ?: return
        if (animation.itemDisplay?.isValid == false) {
            cancel(id)
            return
        }
        val offer = animation.offer
        val rarity = offer.rarity

        animation.currentTick++

        val progress = animation.currentTick.toFloat() / animation.duration
        ladder(animation, progress)

        animation.phase = when {
            progress < 0.2f -> RevealPhase.PRE_REVEAL
            progress < 0.5f -> RevealPhase.EXPLOSION
            progress < 0.8f -> RevealPhase.SETTLE
            else -> RevealPhase.IDLE
        }

        when (animation.phase) {
            RevealPhase.PRE_REVEAL -> applyPreReveal(animation, progress)
            RevealPhase.EXPLOSION -> applyExplosion(animation, progress)
            RevealPhase.SETTLE -> applySettle(animation, progress)
            RevealPhase.IDLE -> applyIdle(animation, progress)
        }

        if (animation.currentTick >= animation.duration) {
            completeAnimation(id)
        }
    }

    private fun applyPreReveal(animation: RevealAnimation, progress: Float) {
        val phaseProgress = progress / 0.2f
        val easedProgress = EasingType.ease(EasingType.EASE_IN_BACK, phaseProgress)

        val scale = 0.3f + easedProgress * 0.2f
        val rotation = easedProgress * 360f

        animation.itemDisplay?.transformation = createTransformation(scale, 0f, rotation)
    }

    private fun applyExplosion(animation: RevealAnimation, progress: Float) {
        val phaseProgress = (progress - 0.2f) / 0.3f
        val easedProgress = EasingType.ease(animation.easing, phaseProgress)

        val scale = 0.5f + easedProgress * 0.8f
        val rotation = 360f + easedProgress * 180f

        animation.itemDisplay?.transformation = createTransformation(scale, 0f, rotation)

        val particleCount = (easedProgress * 20).toInt().coerceIn(0, 20)
        val particle = runCatching { Particle.valueOf(animation.offer.rarity.particle.uppercase()) }.getOrElse { Particle.END_ROD }
        val world = animation.location.world
        val spin = animation.currentTick * 0.6

        for (i in 0 until particleCount) {
            val angle = (i.toFloat() / particleCount.coerceAtLeast(1)) * 2 * PI + spin
            val radius = 0.5 + easedProgress * 1.5
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val y = easedProgress * 1.5

            animation.location.world?.spawnParticle(
                particle,
                animation.location.clone().add(x, y, z),
                1,
                0.0,
                0.0,
                0.0,
                0.05
            )
        }
        if (world != null && fx("reveal-flash")) {
            dev.ensisdev.ensnightmarket.particle.ParticleShapes.dustRing(
                world,
                animation.location.clone().add(0.0, 0.3, 0.0),
                animation.offer.rarity.color,
                (0.4 + phaseProgress.coerceIn(0f, 1f) * 2.0),
                8,
                1.0f
            )
        }
    }

    private fun applySettle(animation: RevealAnimation, progress: Float) {
        val phaseProgress = (progress - 0.5f) / 0.3f
        val easedProgress = EasingType.ease(EasingType.EASE_OUT_QUAD, phaseProgress)

        val scale = 1.3f - easedProgress * 0.3f
        val rotation = 540f - easedProgress * 180f

        animation.itemDisplay?.transformation = createTransformation(scale, 0f, rotation)
    }

    private fun applyIdle(animation: RevealAnimation, progress: Float) {
        val phaseProgress = (progress - 0.8f) / 0.2f
        val easedProgress = EasingType.ease(EasingType.EASE_OUT_SINE, phaseProgress)

        val scale = 1.0f + sin(easedProgress * PI.toFloat()) * 0.05f
        val rotation = 360f + easedProgress * 360f

        animation.itemDisplay?.transformation = createTransformation(scale, 0f, rotation)
    }

    private fun completeAnimation(id: UUID) {
        val animation = activeAnimations.remove(id) ?: return
        animation.task?.cancel()

        animation.itemDisplay?.transformation = createTransformation(animation.finalScale, 0f, 0f)
        animation.textDisplay?.let { punchText(it, 1.0f) }

        val reward = animation.rewardStack
        if (reward != null) animation.itemDisplay?.setItemStack(reward)

        val world = animation.location.world
        if (world != null) {
            if (fx("reveal-sound-ladder")) SoundBank.note(world, animation.location, 7, 0.6f)
            if (fx("reveal-flash")) {
                dev.ensisdev.ensnightmarket.particle.ParticleShapes.dust(
                    world,
                    animation.location.clone().add(0.0, 0.6, 0.0),
                    animation.offer.rarity.color,
                    24,
                    1.4f,
                    0.25
                )
            }
        }

        val sound = runCatching { Sound.valueOf(animation.offer.rarity.sound.uppercase()) }.getOrElse { Sound.BLOCK_NOTE_BLOCK_PLING }
        animation.location.world?.playSound(animation.location, sound, 0.7f, animation.offer.rarity.pitch)

        val particle = runCatching { Particle.valueOf(animation.offer.rarity.particle.uppercase()) }.getOrElse { Particle.END_ROD }
        animation.location.world?.spawnParticle(particle, animation.location.clone().add(0.0, 0.5, 0.0), 30, 0.5, 0.5, 0.5, 0.1)
    }

    private fun createTransformation(scale: Float, y: Float, angle: Float): Transformation {
        return Transformation(
            Vector3f(0f, y, 0f),
            AxisAngle4f(Math.toRadians(angle.toDouble()).toFloat(), 0f, 1f, 0f),
            Vector3f(scale, scale, scale),
            AxisAngle4f()
        )
    }

    fun stopAll() {
        activeAnimations.values.forEach { it.task?.cancel() }
        activeAnimations.clear()
    }

    /** Cancels every reveal bound to the given display (called when the display is removed). */
    fun cancelFor(itemDisplay: ItemDisplay?) {
        if (itemDisplay == null) return
        activeAnimations.entries.toList()
            .filter { it.value.itemDisplay?.uniqueId == itemDisplay.uniqueId }
            .forEach { cancel(it.key) }
    }

    private fun cancel(id: UUID) {
        activeAnimations.remove(id)?.task?.cancel()
    }
}
