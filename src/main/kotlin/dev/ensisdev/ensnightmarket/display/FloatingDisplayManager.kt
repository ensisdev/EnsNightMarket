package dev.ensisdev.ensnightmarket.display

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.animation.*
import dev.ensisdev.ensnightmarket.market.MarketOffer
import dev.ensisdev.ensnightmarket.particle.ParticleShapes
import dev.ensisdev.ensnightmarket.performance.LODSystem
import dev.ensisdev.ensnightmarket.performance.PerformanceMonitor
import dev.ensisdev.ensnightmarket.texture.HeadFactory
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID

class FloatingDisplayManager(private val plugin: EnsNightMarket) {
    data class PairState(
        val owner: UUID,
        val slot: Int,
        val itemId: UUID,
        val textId: UUID,
        val interactionId: UUID,
        var base: Location,
        var phase: Double = 0.0,
        var targetScale: Float,
        var motionPattern: MotionPattern = MotionPattern.BOB,
        var spawnDelay: Int = 0,
        var spawnAge: Int = 0,
        var spawned: Boolean = false,
        var announced: Boolean = false,
        var spin: Double = 0.0
    )

    private val ownerKey = NamespacedKey(plugin, "ensnm_owner")
    private val slotKey = NamespacedKey(plugin, "ensnm_slot")
    private val rootKey = NamespacedKey(plugin, "ensnm_display")
    private val states = mutableMapOf<UUID, PairState>()
    private val revealAnimator = RevealAnimator(plugin)
    private val closing = mutableMapOf<UUID, ClosingState>()
    private val sessionEnds = java.util.concurrent.ConcurrentHashMap<UUID, Long>()

    private data class ClosingState(val itemId: UUID, val targetScale: Float, var left: Int)

    private var cachedEnabled = true
    private var cachedAmbient = true
    private var cachedAmplitude = 0.12
    private var cachedSpeed = 0.07
    private var cachedDegrees = 1.2
    private var cachedTextHeight = 0.85
    private var cachedLod = true
    private var cachedAnimDistance = 32.0
    private var cachedPerPlayerCap = 60
    private var cachedQuality = AnimationQuality.HIGH
    private var cachedAuraMaxWeight = 4.0
    private var cachedFollowSpeed = 0.45
    private var cachedFollowSnap = 12.0
    private var cachedMoveThreshold = 0.2
    private val lastFollowPos = mutableMapOf<UUID, Location>()
    private val reflowing = mutableSetOf<UUID>()
    private var cacheTick = 0
    private var tickCounter = 0

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tickAmbient() }, 400L, 400L)
    }

    private fun tickAmbient() {
        if (!fx("ambient-sound") || states.isEmpty()) return
        states.values.map { it.owner }.toSet().forEach { uuid ->
            val owner = Bukkit.getPlayer(uuid) ?: return@forEach
            if (!owner.isOnline) return@forEach
            SoundBank.softHarp(owner.world, owner.location, kotlin.random.Random.nextInt(0, 8), 0.15f)
        }
    }

    fun showcase(player: Player) {
        if (plugin.config.getStringList("performance.world-blacklist").contains(player.world.name)) return
        if (!plugin.protection.allowedAt(player.location)) {
            dev.ensisdev.ensnightmarket.util.Msgs.send(plugin, player, "protection-denied")
            return
        }
        removeFor(player.uniqueId)
        val offers = plugin.market.getOrCreate(player.uniqueId).offers.take(6)
        if (offers.isEmpty()) return
        val stagger = if (fx("spawn-ceremony")) plugin.config.getInt("cinematics.spawn-stagger-ticks", 4).coerceIn(0, 20) else 0
        offers.forEachIndexed { index, offer ->
            spawn(player, layoutSlot(player, index, offers.size), offer, index * stagger)
        }
        val sessionMinutes = plugin.config.getLong("session.duration-minutes", 10).coerceAtLeast(0)
        sessionEnds[player.uniqueId] = if (sessionMinutes == 0L) Long.MAX_VALUE else System.currentTimeMillis() + sessionMinutes * 60_000L
    }

    fun hasSession(uuid: UUID): Boolean = sessionEnds.containsKey(uuid)

    fun sessionRemaining(uuid: UUID): Long {        val end = sessionEnds[uuid] ?: return 0L
        if (end == Long.MAX_VALUE) return Long.MAX_VALUE
        return (end - System.currentTimeMillis()).coerceAtLeast(0)
    }

    private fun tickFollow(owner: Player) {
        var ordered = states.values.filter { it.owner == owner.uniqueId }.sortedBy { it.slot }
        if (ordered.isEmpty()) return
        // Drop stale cross-world states instead of freezing the whole follow.
        val stale = ordered.filter { it.base.world?.uid != owner.world.uid }
        stale.forEach { removeState(it.itemId) }
        if (stale.isNotEmpty()) {
            ordered = ordered.filter { it.base.world?.uid == owner.world.uid }
            if (ordered.isEmpty()) return
        }
        val reflowingNow = reflowing.contains(owner.uniqueId)
        if (!reflowingNow) {
            val feet = owner.location.clone().apply { y = 0.0 }
            val last = lastFollowPos[owner.uniqueId]
            if (last != null && last.world?.uid == owner.world.uid) {
                val mx = feet.x - last.x
                val mz = feet.z - last.z
                if (kotlin.math.sqrt(mx * mx + mz * mz) < cachedMoveThreshold) return
            }
            lastFollowPos[owner.uniqueId] = owner.location.clone()
        }
        val probe = layoutSlot(owner, ordered.size / 2, ordered.size)
        if (!plugin.protection.allowedAt(probe)) return
        val speed = cachedFollowSpeed
        val snap = cachedFollowSnap
        var settled = true
        ordered.forEachIndexed { index, state ->
            val target = layoutSlot(owner, index, ordered.size)
            val dx = target.x - state.base.x
            val dy = target.y - state.base.y
            val dz = target.z - state.base.z
            val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (dist < 0.05) return@forEachIndexed
            settled = false
            if (dist > snap) {
                state.base.x = target.x
                state.base.y = target.y
                state.base.z = target.z
            } else {
                state.base.x += dx * speed
                state.base.y += dy * speed
                state.base.z += dz * speed
            }
            (Bukkit.getEntity(state.itemId) as? ItemDisplay)?.teleport(state.base)
            (Bukkit.getEntity(state.textId) as? TextDisplay)?.teleport(state.base.clone().add(0.0, cachedTextHeight, 0.0))
            Bukkit.getEntity(state.interactionId)?.teleport(state.base.clone().add(0.0, -0.9, 0.0))
        }
        if (reflowingNow && settled) reflowing.remove(owner.uniqueId)
    }

    private fun layoutSlot(player: Player, index: Int, count: Int): Location {
        val direction = player.location.direction.clone().setY(0).normalize()
        if (direction.lengthSquared() < 0.001) { direction.setX(0.0); direction.setY(0.0); direction.setZ(1.0) }
        val side = Vector3f(-direction.z.toFloat(), 0f, direction.x.toFloat())
        val forward = plugin.config.getDouble("floating.forward-distance", 4.5)
        val eye = plugin.config.getDouble("floating.eye-height", 1.45)
        val spacing = plugin.config.getDouble("floating.spacing", 2.2)
        val arcDepth = plugin.config.getDouble("floating.layout-arc-depth", 1.6).coerceIn(0.0, 4.0)
        val wave = plugin.config.getDouble("floating.layout-height-wave", 0.0).coerceIn(0.0, 2.0)
        val t = index - (count - 1) / 2.0
        val edge = if (count > 1) t / ((count - 1) / 2.0) else 0.0
        val reach = forward - arcDepth * edge * edge
        val pos = player.location.clone().add(direction.x * reach, 0.0, direction.z * reach)
        pos.add(side.x * (t * spacing), 0.0, side.z * (t * spacing))
        pos.y = player.location.y + eye + wave * kotlin.math.sin(index * 1.1)
        return pos
    }

    private fun tickSessions() {
        if (states.isEmpty()) return
        val followEnabled = plugin.config.getBoolean("session.follow-enabled", true)
        states.values.map { it.owner }.toSet().forEach { uuid ->
            val end = sessionEnds[uuid]
            if (end != null && end != Long.MAX_VALUE && System.currentTimeMillis() >= end) {
                removeFor(uuid, animated = true)
                Bukkit.getPlayer(uuid)?.let { dev.ensisdev.ensnightmarket.util.Msgs.send(plugin, it, "session-expired") }
                return@forEach
            }
            if (followEnabled) {
                val owner = Bukkit.getPlayer(uuid)
                if (owner != null && owner.isOnline) {
                    val start = System.nanoTime()
                    tickFollow(owner)
                    PerformanceMonitor.recordFollow(System.nanoTime() - start)
                }
            }
        }
    }

    fun spawn(owner: Player, location: Location, offer: MarketOffer, spawnDelay: Int = 0): ItemDisplay {
        val world = location.world ?: error("World missing")
        val item = world.spawn(location, ItemDisplay::class.java)
        item.setItemStack(HeadFactory.create(offer.rarity.headType, offer.rarity.headValue))
        item.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.FIXED
        item.isPersistent = false; item.isInvulnerable = true
        item.viewRange = plugin.config.getDouble("floating.view-range", 32.0).toFloat()
        item.interpolationDuration = plugin.config.getInt("floating.interpolation-ticks", 3)
        item.persistentDataContainer.set(rootKey, PersistentDataType.BYTE, 1)
        item.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, offer.player.toString())
        item.persistentDataContainer.set(slotKey, PersistentDataType.INTEGER, offer.slot)
        val scale = plugin.config.getDouble("floating.scale", offer.rarity.floatingScale.toDouble()).toFloat()
        item.transformation = transform(scale, 0f, 0f, 0f, 0f)

        val text = world.spawn(location.clone().add(0.0, plugin.config.getDouble("floating.text-height", 0.85), 0.0), TextDisplay::class.java)
        text.text(Text.component(buildText(offer)))
        text.billboard = Display.Billboard.CENTER
        text.isSeeThrough = true; text.isShadowed = false; text.isPersistent = false; text.isInvulnerable = true
        text.backgroundColor = Color.fromARGB(0, 0, 0, 0)
        text.viewRange = item.viewRange
        if (spawnDelay > 0) {
            item.transformation = transform(0.01f, 0f, 0f, 0f, 0f)
            text.transformation = Transformation(Vector3f(0f, 0f, 0f), AxisAngle4f(), Vector3f(0.01f, 0.01f, 0.01f), AxisAngle4f())
        }
        text.persistentDataContainer.set(rootKey, PersistentDataType.BYTE, 1)
        text.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, offer.player.toString())
        text.persistentDataContainer.set(slotKey, PersistentDataType.INTEGER, offer.slot)

        val clickBox = world.spawn(location.clone().add(0.0, -0.9, 0.0), Interaction::class.java)
        clickBox.interactionWidth = 1.6f
        clickBox.interactionHeight = 2.6f
        clickBox.isResponsive = true
        clickBox.isPersistent = false
        clickBox.persistentDataContainer.set(rootKey, PersistentDataType.BYTE, 1)
        clickBox.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, offer.player.toString())
        clickBox.persistentDataContainer.set(slotKey, PersistentDataType.INTEGER, offer.slot)

        hideFromOthers(owner, item, text, clickBox)

        states[item.uniqueId] = PairState(
            owner = offer.player,
            slot = offer.slot,
            itemId = item.uniqueId,
            textId = text.uniqueId,
            interactionId = clickBox.uniqueId,
            base = location.clone(),
            targetScale = scale,
            motionPattern = getMotionPattern(offer.rarity.id),
            spawnDelay = spawnDelay,
            spawned = spawnDelay <= 0
        )
        return item
    }

    private fun fx(kind: String): Boolean {
        if (!plugin.config.getBoolean("cinematics.enabled", true)) return false
        return plugin.config.getBoolean("cinematics.$kind", true)
    }

    private fun hideFromOthers(owner: Player, vararg entities: org.bukkit.entity.Entity) {
        for (viewer in Bukkit.getOnlinePlayers()) {
            if (viewer.uniqueId == owner.uniqueId) continue
            for (e in entities) viewer.hideEntity(plugin, e)
        }
        for (e in entities) owner.showEntity(plugin, e)
    }

    fun hideAllFrom(viewer: Player) {
        for (state in states.values) {
            if (state.owner == viewer.uniqueId) continue
            (Bukkit.getEntity(state.itemId))?.let { viewer.hideEntity(plugin, it) }
            (Bukkit.getEntity(state.textId))?.let { viewer.hideEntity(plugin, it) }
            (Bukkit.getEntity(state.interactionId))?.let { viewer.hideEntity(plugin, it) }
        }
    }

    fun previewReveal(location: Location, offer: MarketOffer) {
        revealEffects(location, offer)
        val world = location.world ?: return
        if (fx("reveal-flash")) {
            ParticleShapes.flash(world, location.clone().add(0.0, 0.5, 0.0))
            ParticleShapes.dust(world, location.clone().add(0.0, 0.6, 0.0), offer.rarity.color, 24, 1.4f, 0.25)
        }
        if (fx("reveal-sound-ladder")) {
            SoundBank.note(world, location, 2, 0.5f)
            SoundBank.note(world, location, 5, 0.5f)
            SoundBank.note(world, location, 7, 0.55f)
        }
    }

    private fun revealEffects(location: Location, offer: MarketOffer) {
        val world = location.world ?: return
        val sound = soundOf(offer.rarity.sound)
        val particle = particleOf(offer.rarity.particle)
        val shape = getRevealShape(offer.rarity.id)
        ParticleShapes.generate(world, location.clone().add(0.0, 0.5, 0.0), particle, shape, offer.rarity.particleCount * 3, 0.1)
        world.playSound(location, sound, 0.6f, offer.rarity.pitch)
        when (offer.rarity.id.lowercase()) {
            "legendary" -> {
                ParticleShapes.ring(world, location.clone().add(0.0, 0.3, 0.0), Particle.FLAME, 2.0, 20, 0.05)
                ParticleShapes.spiral(world, location.clone().add(0.0, 0.5, 0.0), Particle.TOTEM, 1.5, 3.0, 30, 3.0f, 0.02)
            }
            "mysterious" -> {
                ParticleShapes.helix(world, location.clone().add(0.0, 0.5, 0.0), Particle.SOUL_FIRE_FLAME, 1.0, 2.5, 40, 2.0f, 0.03)
                world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.2f)
            }
        }
    }

    fun reveal(entity: org.bukkit.entity.Entity, offer: MarketOffer) {
        val world = entity.world
        val sound = soundOf(offer.rarity.sound)
        val particle = particleOf(offer.rarity.particle)

        val itemDisplay = entity as? ItemDisplay
        val state = states[entity.uniqueId]
        val textDisplay = state?.let { Bukkit.getEntity(it.textId) as? TextDisplay }

        val rewardStack = if (offer.revealed) plugin.market.createReward(offer).apply {
            itemMeta = itemMeta?.apply { addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES) }
        } else null

        if (plugin.config.getBoolean("animation.enable-advanced-reveal", true)) {
            val easing = runCatching {
                EasingType.valueOf(plugin.config.getString("animation.reveal-easing", "EASE_OUT_ELASTIC")!!.uppercase())
            }.getOrElse { EasingType.EASE_OUT_ELASTIC }
            revealAnimator.startReveal(offer, entity.location, itemDisplay, textDisplay, rewardStack, state?.targetScale ?: 1.55f, easing)
        } else if (rewardStack != null) {
            itemDisplay?.setItemStack(rewardStack)
        }

        val shape = getRevealShape(offer.rarity.id)
        ParticleShapes.generate(world, entity.location.clone().add(0.0, 0.5, 0.0), particle, shape, offer.rarity.particleCount * 2, 0.15)
        world.playSound(entity.location, sound, 0.7f, offer.rarity.pitch)
    }

    fun updateOffer(owner: UUID, slot: Int, offer: MarketOffer) {
        val state = states.values.find { it.owner == owner && it.slot == slot } ?: return
        val text = Bukkit.getEntity(state.textId) as? TextDisplay ?: return
        text.text(Text.component(buildText(offer)))
        if (offer.revealed) {
            (Bukkit.getEntity(state.itemId) as? ItemDisplay)?.let {
                it.setItemStack(plugin.market.createReward(offer))
            }
        }
    }

    fun find(entity: org.bukkit.entity.Entity): Pair<UUID, Int>? {
        val owner = entity.persistentDataContainer.get(ownerKey, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        val slot = entity.persistentDataContainer.get(slotKey, PersistentDataType.INTEGER) ?: return null
        return owner to slot
    }

    fun itemEntityOf(owner: UUID, slot: Int): ItemDisplay? {
        val state = states.values.find { it.owner == owner && it.slot == slot } ?: return null
        return Bukkit.getEntity(state.itemId) as? ItemDisplay
    }

    fun spinSlot(owner: UUID, slot: Int) {
        states.values.find { it.owner == owner && it.slot == slot }?.let { it.spin += 180.0 }
    }

    fun headPositions(owner: UUID): Map<Int, Location> {
        return states.values
            .filter { it.owner == owner }
            .associate { it.slot to it.base.clone() }
    }

    private fun refreshCache() {
        cachedEnabled = plugin.config.getBoolean("floating.enabled", true)
        cachedAmbient = plugin.config.getBoolean("floating.ambient-particles", true)
        cachedAmplitude = plugin.config.getDouble("floating.bob-amplitude", 0.12)
        cachedSpeed = plugin.config.getDouble("floating.bob-speed", 0.07)
        cachedDegrees = plugin.config.getDouble("floating.rotation-speed", 1.2)
        cachedTextHeight = plugin.config.getDouble("floating.text-height", 0.85)
        cachedLod = plugin.config.getBoolean("performance.enable-lod", true)
        val quality = runCatching {
            AnimationQuality.valueOf(plugin.config.getString("performance.animation-quality", "HIGH")!!.uppercase())
        }.getOrElse { AnimationQuality.HIGH }
        cachedQuality = quality
        cachedPerPlayerCap = (plugin.config.getInt("performance.max-particles-per-player", 200) * quality.particleMultiplier).toInt().coerceAtLeast(0)
        cachedAnimDistance = plugin.config.getDouble("performance.animation-distance", 32.0).coerceAtLeast(4.0)
        cachedAuraMaxWeight = plugin.config.getDouble("cinematics.aura-max-weight", 4.0).coerceAtLeast(0.0)
        cachedFollowSpeed = plugin.config.getDouble("session.follow-speed", 0.45).coerceIn(0.05, 1.0)
        cachedFollowSnap = plugin.config.getDouble("session.follow-snap-distance", 12.0).coerceAtLeast(2.0)
        cachedMoveThreshold = plugin.config.getDouble("session.follow-move-threshold", 0.2).coerceIn(0.0, 5.0)
    }

    private fun tick() {
        if (cacheTick++ % 100 == 0) refreshCache()
        tickCounter++
        if (!cachedEnabled) return

        PerformanceMonitor.startTracking("floating_tick")

        val amplitude = cachedAmplitude
        val speed = cachedSpeed
        val degrees = cachedDegrees
        val textHeight = cachedTextHeight
        val enableLOD = cachedLod
        val animDistance = cachedAnimDistance
        val perPlayerCap = cachedPerPlayerCap

        var particleCount = 0
        val perPlayerParticles = mutableMapOf<UUID, Int>()

        tickClosing()
        tickSessions()

        states.values.toList().forEach { state ->
            val owner = Bukkit.getPlayer(state.owner)
            if (owner == null || !owner.isOnline) { removeState(state.itemId); return@forEach }
            if (owner.location.distance(state.base) > animDistance) return@forEach

            val item = Bukkit.getEntity(state.itemId) as? ItemDisplay
            val text = Bukkit.getEntity(state.textId) as? TextDisplay
            if (item == null || text == null || !item.isValid) { removeState(state.itemId); return@forEach }

            var growScale = 1.0
            if (!state.spawned) {
                state.spawnAge++
                if (state.spawnAge < state.spawnDelay) {
                    item.transformation = transform(0.01f, 0f, 0f, 0f, 0f)
                    return@forEach
                }
                if (!state.announced) {
                    state.announced = true
                    if (fx("spawn-sound")) SoundBank.note(item.world, state.base, state.slot + 2, 0.45f)
                    ParticleShapes.spiral(item.world, state.base.clone().add(0.0, -0.6, 0.0), particleOf("END_ROD"), 0.7, 1.6, 14, 1.5f, 0.02)
                }
                growScale = EasingType.ease(EasingType.EASE_OUT_BACK, ((state.spawnAge - state.spawnDelay) / 12.0).toFloat().coerceIn(0f, 1f)).toDouble().coerceAtLeast(0.01)
                if (state.spawnAge - state.spawnDelay >= 12) state.spawned = true
            }

            state.phase += speed

            val offset = MotionPatterns.calculateOffset(state.motionPattern, state.phase, amplitude, speed)
            val rotation = MotionPatterns.calculateRotation(state.motionPattern, state.phase, speed, state.phase * degrees) + state.spin
            state.spin *= 0.88
            if (kotlin.math.abs(state.spin) < 0.5) state.spin = 0.0
            val scale = (state.targetScale * MotionPatterns.calculateScaleMultiplier(state.motionPattern, state.phase, speed) * growScale).toFloat()

            item.transformation = transform(scale, offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat(), rotation.toFloat())
            val pulse = 1.0 + 0.03 * kotlin.math.sin(state.phase * 2.0)
            val textScale = (scale / state.targetScale * pulse).toFloat().coerceAtLeast(0.01f)
            text.transformation = Transformation(Vector3f(0f, 0f, 0f), AxisAngle4f(), Vector3f(textScale, textScale, textScale), AxisAngle4f())
            text.teleport(state.base.clone().add(offset.x, textHeight + offset.y, offset.z))

            val offer = plugin.market.cached(state.owner)?.offers?.getOrNull(state.slot)
            tickIdleFx(owner, state, offer, perPlayerParticles, perPlayerCap)

            val used = perPlayerParticles.getOrDefault(state.owner, 0)
            if (cachedAmbient && used < perPlayerCap && state.phase.toInt() % 12 == 0) {
                if (offer?.rarity?.aura == true) {
                    val particle = particleOf(offer.rarity.particle)
                    val at = item.location.clone().add(0.0, 0.45, 0.0)
                    if (enableLOD) {
                        val lodLevel = LODSystem.getLODLevelForPlayer(owner, item.location)
                        if (LODSystem.shouldUpdate(tickCounter, lodLevel)) {
                            val count = LODSystem.calculateParticleCount(1, lodLevel)
                            owner.spawnParticle(particle, at, count, 0.15, 0.15, 0.15, 0.0)
                            particleCount += count
                            perPlayerParticles[state.owner] = used + count
                        }
                    } else {
                        owner.spawnParticle(particle, at, 1, 0.15, 0.15, 0.15, 0.0)
                        particleCount++
                        perPlayerParticles[state.owner] = used + 1
                    }
                }
            }
        }

        PerformanceMonitor.recordParticles("floating_tick", particleCount)
        PerformanceMonitor.endTracking("floating_tick")
    }

    private fun tickIdleFx(owner: Player, state: PairState, offer: MarketOffer?, particles: MutableMap<UUID, Int>, cap: Int) {
        if (offer == null) return
        val world = state.base.world ?: return
        if (fx("idle-aura") && state.phase.toInt() % 24 == 0) {
            val used = particles.getOrDefault(state.owner, 0)
            if (used < cap && offer.rarity.weight <= cachedAuraMaxWeight && cachedQuality != AnimationQuality.LOW) {
                val ringR = 0.9 + 0.1 * kotlin.math.sin(state.phase)
                ParticleShapes.dustRing(world, state.base.clone().add(0.0, 0.05, 0.0), offer.rarity.color, ringR, 10, 1.0f, owner, state.phase * 0.5)
                var spent = used + 10
                if (offer.rarity.weight <= cachedAuraMaxWeight / 4.0) {
                    ParticleShapes.dustColumn(world, state.base.clone(), offer.rarity.color, 2.0, 6, 1.0f, owner)
                    spent += 6
                }
                particles[state.owner] = spent
            }
        }
        if (fx("idle-twinkle") && cachedQuality == AnimationQuality.ULTRA && kotlin.random.Random.nextDouble() < 0.02) {
            val used = particles.getOrDefault(state.owner, 0)
            if (used < cap) {
                owner.spawnParticle(Particle.END_ROD, state.base.clone().add((kotlin.random.Random.nextDouble() - 0.5), 0.6 + kotlin.random.Random.nextDouble() * 0.8, (kotlin.random.Random.nextDouble() - 0.5)), 1, 0.0, 0.0, 0.0, 0.0)
                particles[state.owner] = used + 1
            }
        }
    }

    private fun getMotionPattern(rarityId: String): MotionPattern {
        val patternName = plugin.config.getString("animation.motion-pattern.${rarityId.lowercase()}", null)?.uppercase()
        return runCatching { MotionPattern.valueOf(patternName ?: "BOB") }.getOrElse { MotionPatterns.getDefaultPattern(rarityId) }
    }

    private fun getRevealShape(rarityId: String): ParticleShape {
        val shapeName = plugin.config.getString("animation.reveal-shape.${rarityId.lowercase()}", null)?.uppercase()
        return runCatching { ParticleShape.valueOf(shapeName ?: "SPHERE") }.getOrElse { ParticleShape.SPHERE }
    }

    private fun transform(scale: Float, tx: Float, ty: Float, tz: Float, angleDeg: Float) = Transformation(
        Vector3f(tx, ty, tz),
        AxisAngle4f(Math.toRadians(angleDeg.toDouble()).toFloat(), 0f, 1f, 0f),
        Vector3f(scale, scale, scale),
        AxisAngle4f()
    )
    private fun particleOf(name: String): Particle = runCatching { Particle.valueOf(name.uppercase()) }.getOrElse { Particle.END_ROD }
    private fun soundOf(name: String): Sound = runCatching { Sound.valueOf(name.uppercase()) }.getOrElse { Sound.BLOCK_NOTE_BLOCK_PLING }

    private fun buildText(o: MarketOffer): String {
        val lang = dev.ensisdev.ensnightmarket.lang.Lang
        if (!o.revealed) {
            return lang.get("reveal-hint", mapOf("%rarity%" to o.rarity.displayName))
        }
        val stockLine = if (o.stock > 0) lang.get("holo-stock", mapOf("%stock%" to "${o.stock}")) else lang.get("holo-soldout")
        val priceLine = lang.get("holo-price", mapOf("%price%" to plugin.economy.format(o.price), "%discount%" to "${o.discount}"))
        return "${o.definition.displayName}<newline>$priceLine<newline>${o.rarity.displayName} <dark_gray>· $stockLine"
    }

    private fun removeState(id: UUID) {
        val s = states.remove(id) ?: return
        (Bukkit.getEntity(s.itemId) as? ItemDisplay)?.let { revealAnimator.cancelFor(it) }
        Bukkit.getEntity(s.itemId)?.remove(); Bukkit.getEntity(s.textId)?.remove(); Bukkit.getEntity(s.interactionId)?.remove()
    }
    fun closeSlot(owner: UUID, slot: Int, animated: Boolean = true) {
        val state = states.values.find { it.owner == owner && it.slot == slot } ?: return
        if (!animated || !fx("close-animation")) {
            removeState(state.itemId)
            return
        }
        states.remove(state.itemId)
        Bukkit.getEntity(state.textId)?.remove()
        Bukkit.getEntity(state.interactionId)?.remove()
        val world = state.base.world
        if (world != null) {
            world.spawnParticle(Particle.CLOUD, state.base, 8, 0.3, 0.3, 0.3, 0.02)
            SoundBank.thud(world, state.base, 0.4f)
        }
        closing[state.itemId] = ClosingState(state.itemId, state.targetScale, 10)
        if (states.values.any { it.owner == owner }) reflowing.add(owner)
    }
    fun removeFor(owner: UUID, animated: Boolean = false) {
        if (animated && fx("close-animation")) {
            states.values.filter { it.owner == owner }.forEach { state ->
                states.remove(state.itemId)
                Bukkit.getEntity(state.textId)?.remove()
                Bukkit.getEntity(state.interactionId)?.remove()
                val world = state.base.world
                if (world != null) {
                    world.spawnParticle(Particle.CLOUD, state.base, 8, 0.3, 0.3, 0.3, 0.02)
                    SoundBank.thud(world, state.base, 0.4f)
                }
                closing[state.itemId] = ClosingState(state.itemId, state.targetScale, 10)
            }
        } else {
            states.values.filter { it.owner == owner }.map { it.itemId }.toList().forEach(::removeState)
        }
        sessionEnds.remove(owner)
        lastFollowPos.remove(owner)
        reflowing.remove(owner)
    }
    fun removeAll() { states.keys.toList().forEach(::removeState); closing.keys.toList().forEach { Bukkit.getEntity(it)?.remove() }; closing.clear(); sessionEnds.clear(); lastFollowPos.clear(); reflowing.clear(); revealAnimator.stopAll() }

    private fun tickClosing() {
        if (closing.isEmpty()) return
        closing.entries.toList().forEach { (id, c) ->
            c.left--
            val item = Bukkit.getEntity(c.itemId) as? ItemDisplay
            if (item == null || !item.isValid || c.left <= 0) {
                item?.remove()
                closing.remove(id)
                return@forEach
            }
            val s = (c.targetScale * (c.left / 10.0f)).coerceAtLeast(0.01f)
            item.transformation = transform(s, 0f, 0f, 0f, c.left * 36f)
        }
    }
}
