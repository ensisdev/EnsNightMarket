package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.texture.HeadFactory
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object RaritySelectMenu {
    private val ROW = listOf(19, 20, 21, 22, 23, 24, 25)
    private const val PER_PAGE = 7

    fun open(plugin: EnsNightMarket, player: Player, offerId: String, listPage: Int = 0) {
        val offer = plugin.market.offerDefinitions[offerId]
        val defs = plugin.market.rarityDefinitions.values.sortedBy { it.id }
        val maxPage = (defs.size / PER_PAGE).coerceAtLeast(0)
        val safePage = if (offer == null) 0 else listPage.coerceIn(0, maxPage)
        val holder = AdminHolder(AdminMenu.RARITY_SELECT, safePage, offerId)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 54, Lang.get("gui-select-title", mapOf("%offer%" to offerId)))
        holder.bind(inv)
        val filler = AdminItems.fillerOf(Material.CYAN_STAINED_GLASS_PANE)
        for (i in 0 until inv.size) inv.setItem(i, filler)
        val current = offer?.rarity ?: "random"
        inv.setItem(4, AdminItems.button(Material.PAPER,
            Lang.get("gui-select-title", mapOf("%offer%" to offerId)),
            Lang.list("gui-select-info", mapOf("%offer%" to offerId, "%current%" to current)),
            "noop"))
        if (offer != null) {
            defs.drop(safePage * PER_PAGE).take(PER_PAGE).forEachIndexed { index, def ->
                val icon = try {
                    HeadFactory.create(def.headType, def.headValue)
                } catch (t: Throwable) {
                    ItemStack(Material.PLAYER_HEAD)
                }
                val glow = def.id == current
                val stack = AdminItems.applyAction(
                    icon, (if (glow) "<yellow>" else "") + def.displayName,
                    Lang.list("gui-select-row", mapOf("%id%" to def.id)),
                    "rarity-pick:$offerId:${def.id}"
                )
                if (glow) {
                    stack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1)
                    stack.itemMeta = stack.itemMeta?.apply {
                        addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                    }
                }
                inv.setItem(ROW[index], stack)
            }
            if (maxPage > 0) {
                inv.setItem(ROW.first() - 9, AdminItems.button(Material.ARROW, Lang.get("gui-prev"), emptyList(), "rarity-select-prev"))
                inv.setItem(ROW.last() - 9, AdminItems.button(Material.ARROW, Lang.get("gui-next"), emptyList(), "rarity-select-next"))
                inv.setItem(ROW.first() + 9, AdminItems.button(Material.OAK_DOOR, Lang.get("gui-select-cancel"), Lang.list("gui-select-cancel-lore"), "back-offer-edit"))
                inv.setItem(ROW.last() + 9, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
            } else {
                inv.setItem(49, AdminItems.button(Material.OAK_DOOR, Lang.get("gui-select-cancel"), Lang.list("gui-select-cancel-lore"), "back-offer-edit"))
                inv.setItem(53, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
            }
        }
        player.openInventory(inv)
    }

    fun pick(plugin: EnsNightMarket, player: Player, offerId: String, rarityId: String) {
        val offer = plugin.market.offerDefinitions[offerId]
        if (offer == null || !plugin.market.rarityDefinitions.containsKey(rarityId)) {
            RarityListMenu.open(plugin, player, 0)
            return
        }
        YmlWriter.saveOffer(plugin, offer.copy(rarity = rarityId))
        Msgs.send(plugin, player, "admin-saved")
        OfferEditMenu.open(plugin, player, offerId)
    }
}
