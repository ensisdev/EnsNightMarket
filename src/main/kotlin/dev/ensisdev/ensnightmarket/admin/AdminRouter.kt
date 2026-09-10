package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object AdminItems {
    private lateinit var actionKey: NamespacedKey

    fun init(plugin: EnsNightMarket) {
        actionKey = NamespacedKey(plugin, "ensnm_admin_action")
    }

    fun button(material: Material, name: String, lore: List<String>, action: String): ItemStack {
        return applyAction(ItemStack(material), name, lore, action)
    }

    fun applyAction(stack: ItemStack, name: String, lore: List<String>, action: String): ItemStack {
        val meta = stack.itemMeta ?: return stack
        dev.ensisdev.ensnightmarket.util.Compat.setName(meta, Text.component(name), Text.legacy(name))
        dev.ensisdev.ensnightmarket.util.Compat.setLore(meta, lore.map(Text::component), lore.map(Text::legacy))
        meta.addItemFlags(*ItemFlag.values())
        meta.persistentDataContainer.set(actionKey, PersistentDataType.STRING, action)
        stack.itemMeta = meta
        return stack
    }

    fun actionOf(stack: ItemStack?): String? {
        val meta = stack?.itemMeta ?: return null
        return meta.persistentDataContainer.get(actionKey, PersistentDataType.STRING)
    }

    fun filler(): ItemStack {
        return fillerOf(Material.GRAY_STAINED_GLASS_PANE)
    }

    fun fillerOf(material: Material): ItemStack {
        val stack = ItemStack(material)
        val meta = stack.itemMeta ?: return stack
        dev.ensisdev.ensnightmarket.util.Compat.setName(meta, Text.component("<dark_gray>"), " ")
        stack.itemMeta = meta
        return stack
    }
}

object AdminRouter {
    private lateinit var plugin: EnsNightMarket

    fun init(plugin: EnsNightMarket) {
        this.plugin = plugin
    }

    fun handle(e: InventoryClickEvent): Boolean {
        val holder = e.view.topInventory.holder as? AdminHolder ?: return false
        e.isCancelled = true
        if (e.clickedInventory != e.view.topInventory) return true
        val player = e.whoClicked as? Player ?: return true
        if (!player.hasPermission("ensnightmarket.admin")) {
            player.closeInventory()
            return true
        }
        val action = AdminItems.actionOf(e.currentItem) ?: return true
        dispatch(player, holder, action, e.click)
        return true
    }

    private fun dispatch(player: Player, holder: AdminHolder, action: String, click: ClickType) {
        val parts = action.split(":")
        when (parts[0]) {
            "noop" -> {}
            "close" -> player.closeInventory()
            "back-main" -> MainMenu.open(plugin, player)
            "back-offers" -> OfferListMenu.open(plugin, player, holder.page)
            "back-offer-edit" -> OfferEditMenu.open(plugin, player, holder.id, holder.page)
            "back-rarities" -> RarityListMenu.open(plugin, player, holder.page)
            "back-rarity-edit" -> RarityEditMenu.open(plugin, player, holder.id)
            "open-offers" -> OfferListMenu.open(plugin, player, 0)
            "open-rarities" -> RarityListMenu.open(plugin, player, 0)
            "open-settings" -> SettingsMenu.open(plugin, player)
            "open-player-market" -> PlayerMarketMenu.open(plugin, player, player.uniqueId)
            "page-prev" -> OfferListMenu.open(plugin, player, (holder.page - 1).coerceAtLeast(0))
            "page-next" -> OfferListMenu.open(plugin, player, holder.page + 1)
            "rarity-page-prev" -> RarityListMenu.open(plugin, player, (holder.page - 1).coerceAtLeast(0))
            "rarity-page-next" -> RarityListMenu.open(plugin, player, holder.page + 1)
            "offer-add-hand" -> OfferListMenu.addFromHand(plugin, player)
            "offer-add-manual" -> OfferListMenu.addManual(plugin, player)
            "offer-toggle" -> OfferListMenu.toggle(plugin, player, holder, parts.getOrNull(1) ?: "")
            "offer-delete" -> DeleteConfirmMenu.openOffer(plugin, player, parts.getOrNull(1) ?: "")
            "offer-open" -> OfferEditMenu.open(plugin, player, parts.getOrNull(1) ?: "", holder.page)
            "offer-field" -> OfferEditMenu.editField(plugin, player, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "", holder.page)
            "offer-rarity" -> RaritySelectMenu.open(plugin, player, parts.getOrNull(1) ?: "")
            "rarity-add" -> RarityListMenu.create(plugin, player)
            "rarity-pick" -> RaritySelectMenu.pick(plugin, player, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
            "rarity-select-prev" -> RaritySelectMenu.open(plugin, player, holder.id, (holder.page - 1).coerceAtLeast(0))
            "rarity-select-next" -> RaritySelectMenu.open(plugin, player, holder.id, holder.page + 1)
            "rarity-open" -> RarityEditMenu.open(plugin, player, parts.getOrNull(1) ?: "")
            "rarity-field" -> RarityEditMenu.editField(plugin, player, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
            "rarity-delete" -> DeleteConfirmMenu.openRarity(plugin, player, parts.getOrNull(1) ?: "")
            "delete-yes" -> DeleteConfirmMenu.confirm(plugin, player, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
            "delete-no" -> DeleteConfirmMenu.cancel(plugin, player, parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
            "setting" -> SettingsMenu.edit(plugin, player, parts.getOrNull(1) ?: "")
            "pm-refresh" -> PlayerMarketMenu.refresh(plugin, player, parts.getOrNull(1) ?: "")
            "pm-reveal" -> PlayerMarketMenu.revealAll(plugin, player, parts.getOrNull(1) ?: "")
            "pm-reset" -> PlayerMarketMenu.reset(plugin, player, parts.getOrNull(1) ?: "")
            "pm-back" -> {
                val target = holder.target ?: player.uniqueId
                if (target == player.uniqueId) MainMenu.open(plugin, player)
                else PlayerMarketMenu.open(plugin, player, target)
            }
        }
        if (holder.menu == AdminMenu.OFFERS || holder.menu == AdminMenu.RARITIES) {
            val id = parts.getOrNull(1) ?: ""
            if (parts[0] == "offer-open" && click == ClickType.MIDDLE) {
                OfferListMenu.toggle(plugin, player, holder, id)
            }
            if (parts[0] == "offer-open" && click == ClickType.RIGHT) {
                DeleteConfirmMenu.openOffer(plugin, player, id)
            }
            if (parts[0] == "rarity-open" && click == ClickType.RIGHT) {
                DeleteConfirmMenu.openRarity(plugin, player, id)
            }
        }
    }
}
