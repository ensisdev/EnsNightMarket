package dev.ensisdev.ensnightmarket.particle

import dev.ensisdev.ensnightmarket.animation.ParticleShape
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import kotlin.math.*
import kotlin.random.Random

object ParticleShapes {

    /**
     * Version-proof dust particle: DUST on 1.20.5+, REDSTONE below.
     * Resolved by name so it compiles against the 1.20 API either way.
     */
    val DUST: Particle by lazy { runCatching { Particle.valueOf("DUST") }.getOrElse { Particle.REDSTONE } }

    /** FLASH with an END_ROD fallback if the enum ever disappears. */
    fun flash(world: World, at: Location) {
        runCatching { world.spawnParticle(Particle.FLASH, at, 3, 0.2, 0.2, 0.2, 0.0) }
            .onFailure { world.spawnParticle(Particle.END_ROD, at, 6, 0.2, 0.2, 0.2, 0.05) }
    }
    fun sphere(world: World, center: Location, particle: Particle, radius: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        val goldenRatio = (1 + sqrt(5.0)) / 2
        for (i in 0 until count) {
            val theta = 2.0 * Math.PI * i / goldenRatio
            val phi = acos(1.0 - 2.0 * (i + 0.5) / count)
            val x = radius * cos(theta) * sin(phi)
            val y = radius * cos(phi)
            val z = radius * sin(theta) * sin(phi)
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun ring(world: World, center: Location, particle: Particle, radius: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        val angleStep = (2 * Math.PI / count)
        for (i in 0 until count) {
            val angle = i * angleStep
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            world.spawnParticle(particle, center.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun spiral(world: World, center: Location, particle: Particle, radius: Double, height: Double, count: Int, rotations: Float = 2f, speed: Double = 0.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val angle = progress * rotations * 2 * Math.PI
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            val y = progress * height
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun helix(world: World, center: Location, particle: Particle, radius: Double, height: Double, count: Int, rotations: Float = 2f, speed: Double = 0.0) {
        if (count <= 0) return
        val half = count / 2
        for (i in 0 until half) {
            val progress = i.toFloat() / half
            val angle = progress * rotations * 2 * Math.PI
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            val y = progress * height
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, speed)
            world.spawnParticle(particle, center.clone().add(-x, y, -z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun burst(world: World, center: Location, particle: Particle, count: Int, speed: Double = 0.1, radius: Double = 2.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val direction = randomDirection()
            val distance = Random.nextDouble() * radius
            val offset = direction.multiply(distance.toDouble())
            world.spawnParticle(particle, center.clone().add(offset), 1, offset.x * speed, offset.y * speed, offset.z * speed, abs(speed))
        }
    }

    fun orbit(world: World, center: Location, particle: Particle, radius: Double, count: Int, speed: Double = 0.0, layers: Int = 1) {
        if (count <= 0) return
        for (layer in 0 until layers) {
            val layerOffset = layer * 0.3
            for (i in 0 until count) {
                val angle = (i.toFloat() / count) * 2 * Math.PI + layerOffset
                val x = radius * cos(angle)
                val z = radius * sin(angle)
                val y = sin(angle * 2) * 0.2
                world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, speed)
            }
        }
    }

    fun cone(world: World, center: Location, particle: Particle, radius: Double, height: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val angle = progress * 2 * Math.PI
            val currentRadius = radius * (1f - progress)
            val x = currentRadius * cos(angle)
            val z = currentRadius * sin(angle)
            val y = height * progress
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun disc(world: World, center: Location, particle: Particle, radius: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val angle = progress * 2 * Math.PI
            val currentRadius = Random.nextDouble() * radius
            val x = currentRadius * cos(angle)
            val z = currentRadius * sin(angle)
            world.spawnParticle(particle, center.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun wall(world: World, center: Location, particle: Particle, width: Double, height: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val x = (Random.nextFloat() - 0.5f) * width
            val y = Random.nextFloat() * height
            world.spawnParticle(particle, center.clone().add(x, y, 0.0), 1, 0.0, 0.0, 0.0, speed)
        }
    }

    fun explosion(world: World, center: Location, particle: Particle, radius: Double, count: Int, speed: Double = 0.1) {
        if (count <= 0) return
        for (i in 0 until count) {
            val direction = randomDirection()
            world.spawnParticle(particle, center, 1, direction.x * speed * radius, direction.y * speed * radius, direction.z * speed * radius, speed)
        }
    }

    fun impact(world: World, center: Location, particle: Particle, count: Int, speed: Double = 0.1) {
        burst(world, center, particle, count / 2, speed)
        ring(world, center, particle, 0.5, count / 4, speed)
        ring(world, center, particle, 1.0, count / 4, speed * 0.5)
    }

    fun shockwave(world: World, center: Location, particle: Particle, maxRadius: Double, count: Int, speed: Double = 0.0) {
        if (count <= 0) return
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            val radius = progress * maxRadius
            ring(world, center, particle, radius, 8, speed)
        }
    }

    fun dust(world: World, center: Location, hex: String, count: Int, size: Float = 1.2f, speed: Double = 0.15, viewer: Player? = null) {
        val color = parseColor(hex) ?: return
        if (count <= 0) return
        val options = Particle.DustOptions(color, size)
        for (i in 0 until count) {
            val direction = randomDirection()
            val distance = Random.nextDouble() * 0.9
            val offset = direction.multiply(distance)
            val at = center.clone().add(offset)
            if (viewer != null) viewer.spawnParticle(DUST, at, 1, offset.x * speed, offset.y * speed, offset.z * speed, speed, options)
            else world.spawnParticle(DUST, at, 1, offset.x * speed, offset.y * speed, offset.z * speed, speed, options)
        }
    }

    fun dustRing(world: World, center: Location, hex: String, radius: Double, count: Int, size: Float = 1.0f, viewer: Player? = null, spin: Double = 0.0) {
        val color = parseColor(hex) ?: return
        if (count <= 0) return
        val options = Particle.DustOptions(color, size)
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2 * Math.PI + spin
            val at = center.clone().add(radius * cos(angle), 0.0, radius * sin(angle))
            if (viewer != null) viewer.spawnParticle(DUST, at, 1, 0.0, 0.0, 0.0, 0.0, options)
            else world.spawnParticle(DUST, at, 1, 0.0, 0.0, 0.0, 0.0, options)
        }
    }

    fun dustColumn(world: World, base: Location, hex: String, height: Double, count: Int, size: Float = 1.0f, viewer: Player? = null) {
        val color = parseColor(hex) ?: return
        if (count <= 0) return
        val options = Particle.DustOptions(color, size)
        for (i in 0 until count) {
            val at = base.clone().add(0.0, (i.toDouble() / count) * height, 0.0)
            if (viewer != null) viewer.spawnParticle(DUST, at, 1, 0.0, 0.02, 0.0, 0.0, options)
            else world.spawnParticle(DUST, at, 1, 0.0, 0.02, 0.0, 0.0, options)
        }
    }

    fun parseColor(hex: String): org.bukkit.Color? {
        return runCatching {
            val clean = hex.trim().removePrefix("#")
            val rgb = clean.toInt(16)
            org.bukkit.Color.fromRGB((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
        }.getOrNull()
    }

    fun generate(world: World, center: Location, particle: Particle, shape: ParticleShape, count: Int, speed: Double = 0.0, data: Map<String, Any> = emptyMap()) {
        when (shape) {
            ParticleShape.POINT -> world.spawnParticle(particle, center, count, 0.1, 0.1, 0.1, speed)
            ParticleShape.SPHERE -> sphere(world, center, particle, data["radius"] as? Double ?: 1.0, count, speed)
            ParticleShape.RING -> ring(world, center, particle, data["radius"] as? Double ?: 1.0, count, speed)
            ParticleShape.SPIRAL -> spiral(world, center, particle, data["radius"] as? Double ?: 1.0, data["height"] as? Double ?: 2.0, count, (data["rotations"] as? Double ?: 2.0).toFloat(), speed)
            ParticleShape.HELIX -> helix(world, center, particle, data["radius"] as? Double ?: 1.0, data["height"] as? Double ?: 2.0, count, (data["rotations"] as? Double ?: 2.0).toFloat(), speed)
            ParticleShape.BURST -> burst(world, center, particle, count, speed)
            ParticleShape.ORBIT -> orbit(world, center, particle, data["radius"] as? Double ?: 1.0, count, speed)
            ParticleShape.CONE -> cone(world, center, particle, data["radius"] as? Double ?: 1.0, data["height"] as? Double ?: 2.0, count, speed)
            ParticleShape.DISC -> disc(world, center, particle, data["radius"] as? Double ?: 1.0, count, speed)
            ParticleShape.WALL -> wall(world, center, particle, data["width"] as? Double ?: 2.0, data["height"] as? Double ?: 2.0, count, speed)
            ParticleShape.EXPLOSION -> explosion(world, center, particle, data["radius"] as? Double ?: 2.0, count, speed)
            ParticleShape.IMPACT -> impact(world, center, particle, count, speed)
            ParticleShape.SHOCKWAVE -> shockwave(world, center, particle, data["maxRadius"] as? Double ?: 3.0, count, speed)
            ParticleShape.WAVE -> ring(world, center, particle, data["radius"] as? Double ?: 1.0, count, speed)
        }
    }

    private fun randomDirection(): org.bukkit.util.Vector {
        val theta = Random.nextDouble() * 2 * Math.PI
        val phi = acos(2 * Random.nextDouble() - 1)
        return org.bukkit.util.Vector(
            sin(phi) * cos(theta),
            sin(phi) * sin(theta),
            cos(phi)
        )
    }
}