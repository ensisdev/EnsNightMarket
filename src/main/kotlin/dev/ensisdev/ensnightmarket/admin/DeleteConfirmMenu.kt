package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object DeleteConfirmMenu {
    fun openOffer(plugin: EnsNightMarket, player: Player, id: String) {
        if (!plugin.market.offerDefinitions.containsKey(id)) {
            OfferListMenu.open(plugin, player, 0)
            return
        }
        open(plugin, player, "offer", id, Lang.get("gui-delete-offer-info", mapOf("%id%" to id)))
    }

    fun openRarity(plugin: EnsNightMarket, player: Player, id: String) {
        if (!plugin.market.rarityDefinitions.containsKey(id)) {
            RarityListMenu.open(plugin, player, 0)
            return
        }
        val used = plugin.market.offerDefinitions.values.count { it.rarity == id }
        open(plugin, player, "rarity", id, Lang.get("gui-delete-rarity-info", mapOf("%id%" to id, "%used%" to "$used")))
    }

    private fun open(plugin: EnsNightMarket, player: Player, kind: String, id: String, info: String) {
        val holder = AdminHolder(AdminMenu.DELETE_CONFIRM, 0, "$kind:$id")
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 27, Lang.get("gui-delete-title-id", mapOf("%id%" to id)))
        holder.bind(inv)
        val filler = AdminItems.fillerOf(Material.RED_STAINED_GLASS_PANE)
        for (i in 0 until inv.size) inv.setItem(i, filler)
        inv.setItem(4, AdminItems.button(Material.BARRIER, Lang.get("gui-delete-question"), listOf(info, Lang.get("gui-delete-final")), "noop"))
        inv.setItem(12, AdminItems.button(Material.LIME_CONCRETE, Lang.get("gui-delete-yes"), emptyList(), "delete-yes:$kind:$id"))
        inv.setItem(14, AdminItems.button(Material.RED_CONCRETE, Lang.get("gui-delete-no"), emptyList(), "delete-no:$kind:$id"))
        player.openInventory(inv)
    }

    fun confirm(plugin: EnsNightMarket, player: Player, kind: String, id: String) {
        when (kind) {
            "offer" -> {
                YmlWriter.deleteOffer(plugin, id)
                Msgs.send(plugin, player, "admin-removed")
                OfferListMenu.open(plugin, player, 0)
            }
            "rarity" -> {
                if (plugin.market.offerDefinitions.values.any { it.rarity == id }) {
                    Msgs.send(plugin, player, "admin-rarity-used")
                    RarityListMenu.open(plugin, player, 0)
                    return
                }
                YmlWriter.deleteRarity(plugin, id)
                Msgs.send(plugin, player, "admin-removed")
                RarityListMenu.open(plugin, player, 0)
            }
            else -> MainMenu.open(plugin, player)
        }
    }

    fun cancel(plugin: EnsNightMarket, player: Player, kind: String, id: String) {
        Msgs.send(plugin, player, "admin-cancelled")
        when (kind) {
            "offer" -> OfferEditMenu.open(plugin, player, id)
            "rarity" -> RarityEditMenu.open(plugin, player, id)
            else -> MainMenu.open(plugin, player)
        }
    }
}
