package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

object PlayerMarketMenu {
    fun open(plugin: EnsNightMarket, player: Player, targetId: UUID) {
        val target = Bukkit.getPlayer(targetId) ?: Bukkit.getOfflinePlayer(targetId)
        val name = target.name ?: targetId.toString().take(8)
        val market = plugin.market.getOrCreate(targetId)
        val revealed = market.offers.count { it.revealed }
        val holder = AdminHolder(AdminMenu.PLAYER_MARKET, 0, "", targetId)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 27, Lang.get("gui-pm-title", mapOf("%name%" to name, "%revealed%" to "$revealed", "%total%" to "${market.offers.size}")))
        holder.bind(inv)
        val filler = AdminItems.filler()
        for (i in 0 until inv.size) inv.setItem(i, filler)
        inv.setItem(4, AdminItems.button(
            Material.PLAYER_HEAD, "<white>$name",
            Lang.list("gui-pm-info", mapOf("%offers%" to "${market.offers.size}", "%revealed%" to "$revealed", "%stock%" to "${market.offers.sumOf { it.stock }}")),
            "noop"
        ))
        inv.setItem(11, AdminItems.button(Material.CLOCK, Lang.get("gui-pm-refresh"), Lang.list("gui-pm-refresh-lore"), "pm-refresh:$targetId"))
        inv.setItem(12, AdminItems.button(Material.ENDER_EYE, Lang.get("gui-pm-reveal"), Lang.list("gui-pm-reveal-lore"), "pm-reveal:$targetId"))
        inv.setItem(13, AdminItems.button(Material.BARRIER, Lang.get("gui-pm-reset"), Lang.list("gui-pm-reset-lore"), "pm-reset:$targetId"))
        inv.setItem(22, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "pm-back"))
        inv.setItem(26, AdminItems.button(Material.BLACK_CONCRETE, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    private fun targetOf(plugin: EnsNightMarket, player: Player, raw: String): UUID? {
        val uuid = runCatching { UUID.fromString(raw) }.getOrNull()
        if (uuid == null) {
            Msgs.send(plugin, player, "not-found")
            MainMenu.open(plugin, player)
            return null
        }
        return uuid
    }

    fun refresh(plugin: EnsNightMarket, player: Player, raw: String) {
        val uuid = targetOf(plugin, player, raw) ?: return
        plugin.market.refresh(uuid)
        plugin.display.removeFor(uuid)
        Msgs.send(plugin, player, "admin-saved")
        open(plugin, player, uuid)
    }

    fun revealAll(plugin: EnsNightMarket, player: Player, raw: String) {
        val uuid = targetOf(plugin, player, raw) ?: return
        val market = plugin.market.getOrCreate(uuid)
        market.offers.forEach { it.revealed = true }
        plugin.market.save(market)
        market.offers.forEachIndexed { index, offer -> plugin.display.updateOffer(uuid, index, offer) }
        Msgs.send(plugin, player, "admin-saved")
        open(plugin, player, uuid)
    }

    fun reset(plugin: EnsNightMarket, player: Player, raw: String) {
        val uuid = targetOf(plugin, player, raw) ?: return
        plugin.market.reset(uuid)
        plugin.display.removeFor(uuid)
        Msgs.send(plugin, player, "admin-saved")
        open(plugin, player, uuid)
    }
}
