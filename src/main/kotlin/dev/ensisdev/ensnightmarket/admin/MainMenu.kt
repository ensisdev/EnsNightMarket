package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object MainMenu {
    fun open(plugin: EnsNightMarket, player: Player) {
        val offers = plugin.market.offerDefinitions.size
        val rarities = plugin.market.rarityDefinitions.size
        val holder = AdminHolder(AdminMenu.MAIN)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 27, Lang.get("gui-main-title"))
        holder.bind(inv)
        val filler = AdminItems.fillerOf(Material.PURPLE_STAINED_GLASS_PANE)
        for (i in 0 until inv.size) inv.setItem(i, filler)
        inv.setItem(11, AdminItems.button(
            Material.CHEST, Lang.get("gui-main-offers"),
            Lang.list("gui-main-offers-lore", mapOf("%count%" to "$offers")),
            "open-offers"
        ))
        inv.setItem(12, AdminItems.button(
            Material.DIAMOND, Lang.get("gui-main-rarities"),
            Lang.list("gui-main-rarities-lore", mapOf("%count%" to "$rarities")),
            "open-rarities"
        ))
        inv.setItem(14, AdminItems.button(
            Material.PLAYER_HEAD, Lang.get("gui-main-player"),
            Lang.list("gui-main-player-lore"),
            "open-player-market"
        ))
        inv.setItem(15, AdminItems.button(
            Material.COMPARATOR, Lang.get("gui-main-settings"),
            Lang.list("gui-main-settings-lore"),
            "open-settings"
        ))
        inv.setItem(22, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }
}
