package dev.ensisdev.ensnightmarket.listener

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.GameMode
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MarketListener(private val plugin: EnsNightMarket) : Listener {
    private val clickCooldowns = ConcurrentHashMap<UUID, Long>()
    private val cooldownMs = 500L

    private fun msg(p: Player, key: String, replacements: Map<String, String> = emptyMap()) =
        Msgs.send(plugin, p, key, replacements)

    private fun throttled(player: UUID): Boolean {
        val now = System.currentTimeMillis()
        val last = clickCooldowns[player] ?: 0L
        if (now - last < cooldownMs) return true
        clickCooldowns[player] = now
        return false
    }

    private fun isMarketEntity(entity: org.bukkit.entity.Entity): Boolean =
        entity is ItemDisplay || entity is TextDisplay || entity is Interaction

    private fun itemDisplayOf(owner: UUID, slot: Int, clicked: org.bukkit.entity.Entity): ItemDisplay? =
        if (clicked is ItemDisplay) clicked else plugin.display.itemEntityOf(owner, slot)

    @EventHandler(priority = EventPriority.HIGHEST) fun clickGui(e: InventoryClickEvent) {
        dev.ensisdev.ensnightmarket.admin.AdminRouter.handle(e)
    }

    @EventHandler(priority = EventPriority.HIGHEST) fun dragGui(e: org.bukkit.event.inventory.InventoryDragEvent) {
        if (e.view.topInventory.holder is dev.ensisdev.ensnightmarket.admin.AdminHolder) e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false) fun leftClick(e: EntityDamageByEntityEvent) {
        val attacker = e.damager as? Player ?: return
        val entity = e.entity
        if (!isMarketEntity(entity)) return
        val data = plugin.display.find(entity) ?: return
        e.isCancelled = true
        if (data.first != attacker.uniqueId && !attacker.hasPermission("ensnightmarket.admin")) return
        if (throttled(attacker.uniqueId)) return
        val before = plugin.market.getOrCreate(data.first).offers.getOrNull(data.second) ?: return
        if (before.revealed) return
        revealOffer(attacker, data.first, data.second, entity)
    }

    private fun revealOffer(viewer: Player, owner: UUID, slot: Int, clicked: org.bukkit.entity.Entity?): Boolean {
        val offer = plugin.market.reveal(owner, slot) ?: return false
        val display = if (clicked is ItemDisplay) clicked else plugin.display.itemEntityOf(owner, slot)
        if (display != null) plugin.display.reveal(display, offer)
        plugin.display.updateOffer(owner, slot, offer)
        if (owner == viewer.uniqueId) {
            msg(viewer, "revealed", mapOf(
                "%rarity%" to Text.plain(offer.rarity.displayName),
                "%offer%" to Text.plain(offer.definition.displayName)
            ))
        }
        return true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) fun swing(e: PlayerAnimationEvent) {
        val p = e.player
        if (p.gameMode == GameMode.SPECTATOR) return
        val heads = plugin.display.headPositions(p.uniqueId)
        if (heads.isEmpty()) return
        val eye = p.eyeLocation
        val dir = eye.direction
        val eyeVec = eye.toVector()
        var bestSlot = -1
        var bestDist = 1.5
        heads.forEach { (slot, base) ->
            if (base.world?.uid != eye.world?.uid) return@forEach
            val toHead = base.toVector().subtract(eyeVec)
            val t = toHead.dot(dir)
            if (t < 0.0 || t > 6.5) return@forEach
            val closest = eyeVec.clone().add(dir.clone().multiply(t))
            val d = closest.distance(base.toVector())
            if (d < bestDist) { bestDist = d; bestSlot = slot }
        }
        if (bestSlot < 0) return
        val before = plugin.market.getOrCreate(p.uniqueId).offers.getOrNull(bestSlot) ?: return
        if (before.revealed) return
        if (throttled(p.uniqueId)) return
        revealOffer(p, p.uniqueId, bestSlot, null)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) fun clickDisplay(e: PlayerInteractAtEntityEvent) {
        val entity = e.rightClicked
        if (!isMarketEntity(entity)) return
        val data = plugin.display.find(entity) ?: return
        e.isCancelled = true
        val p = e.player
        if (data.first != p.uniqueId && !p.hasPermission("ensnightmarket.admin")) return
        if (throttled(p.uniqueId)) return

        val market = plugin.market.getOrCreate(data.first)
        val offer = market.offers.getOrNull(data.second) ?: run { msg(p, "not-found"); return }

        if (!offer.revealed) {
            plugin.market.reveal(data.first, data.second)
            itemDisplayOf(data.first, data.second, entity)?.let { plugin.display.reveal(it, offer) }
            plugin.display.updateOffer(data.first, data.second, offer)
            if (data.first == p.uniqueId) {
                msg(p, "revealed", mapOf(
                    "%rarity%" to Text.plain(offer.rarity.displayName),
                    "%offer%" to Text.plain(offer.definition.displayName)
                ))
            }
            return
        }

        if (data.first != p.uniqueId) return
        val result = plugin.market.purchase(p.uniqueId, data.second, { p.hasPermission(it) }, { plugin.economy.withdraw(p, it) }, { stack ->
            val before = p.inventory.contents.clone()
            val leftover = p.inventory.addItem(stack)
            if (leftover.isEmpty()) true else { p.inventory.contents = before; false }
        })
        if (!result.success) {
            val key = when (result.reason) {
                "sold-out" -> "sold-out"
                "insufficient" -> "insufficient"
                "permission" -> "no-permission"
                "limit" -> "purchase-limit"
                "inventory" -> "inventory-full"
                "busy" -> "busy"
                else -> "not-found"
            }
            msg(p, key)
            return
        }
        val updated = plugin.market.getOrCreate(p.uniqueId).offers[data.second]
        msg(p, "purchased", mapOf(
            "%offer%" to Text.plain(updated.definition.displayName),
            "%price%" to plugin.economy.format(updated.price),
            "%stock%" to updated.stock.toString()
        ))
        if (updated.stock <= 0) plugin.display.closeSlot(p.uniqueId, data.second)
        else {
            plugin.display.updateOffer(p.uniqueId, data.second, updated)
            plugin.display.spinSlot(p.uniqueId, data.second)
        }
        purchaseFeedback(p, updated)
    }

    private fun purchaseFeedback(p: Player, offer: dev.ensisdev.ensnightmarket.market.MarketOffer) {
        val threshold = plugin.config.getDouble("purchase-feedback.title-max-weight", 15.0)
        val exotic = offer.rarity.weight <= threshold
        if (plugin.config.getBoolean("purchase-feedback.sound-enabled", true)) {
            val sound = if (exotic) org.bukkit.Sound.ENTITY_PLAYER_LEVELUP
            else org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP
            p.playSound(p.location, sound, 0.7f, if (exotic) 1.2f else 1.0f)
        }
        if (plugin.config.getBoolean("cinematics.enabled", true) &&
            plugin.config.getBoolean("cinematics.purchase-confetti", true)) {
            val quality = runCatching {
                dev.ensisdev.ensnightmarket.animation.AnimationQuality.valueOf(
                    plugin.config.getString("performance.animation-quality", "HIGH")!!.uppercase()
                )
            }.getOrElse { dev.ensisdev.ensnightmarket.animation.AnimationQuality.HIGH }
            if (quality != dev.ensisdev.ensnightmarket.animation.AnimationQuality.LOW) {
                val at = p.location.clone().add(0.0, 1.0, 0.0)
                dev.ensisdev.ensnightmarket.particle.ParticleShapes.dust(p.world, at, offer.rarity.color, 18, 1.4f, 0.3)
                dev.ensisdev.ensnightmarket.particle.ParticleShapes.dust(p.world, at, "#FFD75E", 12, 1.2f, 0.45)
            }
        }
        if (!plugin.config.getBoolean("purchase-feedback.title-enabled", true)) return
        if (!exotic) return
        val subRaw = dev.ensisdev.ensnightmarket.lang.Lang.get("purchase-subtitle", mapOf(
            "%rarity%" to offer.rarity.displayName,
            "%price%" to plugin.economy.format(offer.price)
        ))
        dev.ensisdev.ensnightmarket.util.Compat.title(
            p,
            Text.component(offer.definition.displayName),
            Text.component(subRaw),
            Text.plain(offer.definition.displayName),
            Text.plain(subRaw)
        )
    }

    @EventHandler fun damage(e: EntityDamageEvent) {
        if (e is EntityDamageByEntityEvent) return
        if (plugin.display.find(e.entity) != null) e.isCancelled = true
    }
    @EventHandler fun quit(e: PlayerQuitEvent) { plugin.display.removeFor(e.player.uniqueId) }
    @EventHandler fun join(e: PlayerJoinEvent) { plugin.display.hideAllFrom(e.player) }
    @EventHandler fun teleport(e: PlayerTeleportEvent) { closeSession(e.player) }
    @EventHandler fun worldChange(e: PlayerChangedWorldEvent) { closeSession(e.player) }
    @EventHandler fun death(e: PlayerDeathEvent) { closeSession(e.entity) }

    private fun closeSession(p: Player) {
        if (plugin.display.hasSession(p.uniqueId)) {
            plugin.display.removeFor(p.uniqueId)
            Msgs.send(plugin, p, "showcase-closed")
        }
    }
}
