package dev.ensisdev.ensnightmarket.animation

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import kotlin.math.pow

object SoundBank {
    private val pentatonic = floatArrayOf(0f, 2f, 4f, 7f, 9f, 12f, 14f, 16f, 19f, 21f, 24f)

    fun pitch(step: Int): Float {
        val semitones = pentatonic[((step % pentatonic.size) + pentatonic.size) % pentatonic.size]
        return 2.0.pow((semitones / 12.0).toDouble()).toFloat()
    }

    fun note(world: World, location: Location, step: Int, volume: Float = 0.5f) {
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, volume, pitch(step))
    }

    fun softHarp(world: World, location: Location, step: Int, volume: Float = 0.15f) {
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, pitch(step))
    }

    fun thud(world: World, location: Location, volume: Float = 0.6f) {
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_BASS, volume, pitch(0) * 0.5f)
    }

    fun shimmer(world: World, location: Location, step: Int, volume: Float = 0.4f) {
        world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch(step))
    }
}
