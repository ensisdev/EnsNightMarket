package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.items.CustomItems
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.market.OfferDef
import dev.ensisdev.ensnightmarket.util.Msgs
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object OfferListMenu {
    private val GRID = listOf(
        0, 1, 2, 3, 4, 5, 6,
        9, 10, 11, 12, 13, 14, 15,
        18, 19, 20, 21, 22, 23, 24,
        27, 28, 29, 30, 31, 32, 33
    )

    fun open(plugin: EnsNightMarket, player: Player, page: Int) {
        val defs = plugin.market.offerDefinitions.values.sortedWith(compareBy({ !it.enabled }, { it.id }))
        val maxPage = (defs.size / GRID.size).coerceAtLeast(0)
        val safePage = page.coerceIn(0, maxPage)
        val holder = AdminHolder(AdminMenu.OFFERS, safePage)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 54, Lang.get("gui-offers-title", mapOf("%page%" to "${safePage + 1}", "%count%" to "${defs.size}")))
        holder.bind(inv)
        val filler = AdminItems.filler()
        for (i in 0 until inv.size) inv.setItem(i, filler)
        defs.drop(safePage * GRID.size).take(GRID.size).forEachIndexed { index, def ->
            inv.setItem(GRID[index], button(def))
        }
        if (defs.isEmpty()) {
            inv.setItem(22, AdminItems.button(Material.HOPPER, Lang.get("gui-empty-offers"), Lang.list("gui-empty-offers-lore"), "noop"))
        }
        val pageRep = mapOf("%page%" to "${safePage + 1}")
        inv.setItem(36, AdminItems.button(Material.ARROW, Lang.get("gui-prev"), Lang.list("gui-prev-lore", pageRep), "page-prev"))
        inv.setItem(40, AdminItems.button(Material.PAPER,
            Lang.get("gui-page-info", mapOf("%page%" to "${safePage + 1}", "%pages%" to "${maxPage + 1}")),
            Lang.list("gui-total-lore", mapOf("%count%" to "${defs.size}")), "noop"))
        inv.setItem(44, AdminItems.button(Material.ARROW, Lang.get("gui-next"), Lang.list("gui-next-lore", pageRep), "page-next"))
        inv.setItem(45, AdminItems.button(Material.LIME_DYE, Lang.get("gui-offer-add-hand"), Lang.list("gui-offer-add-hand-lore"), "offer-add-hand"))
        inv.setItem(46, AdminItems.button(Material.NAME_TAG, Lang.get("gui-offer-add-manual"), Lang.list("gui-offer-add-manual-lore"), "offer-add-manual"))
        inv.setItem(49, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "back-main"))
        inv.setItem(53, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    private fun button(def: OfferDef): ItemStack {
        val icon = CustomItems.resolve(def.material, 1) ?: ItemStack(Material.STONE)
        val state = Lang.get(if (def.enabled) "common-state-on" else "common-state-off")
        val lore = Lang.list("gui-offer-row", mapOf(
            "%id%" to def.id,
            "%price%" to "${def.basePrice}",
            "%dmin%" to "${def.discountMin}",
            "%dmax%" to "${def.discountMax}",
            "%smin%" to "${def.stockMin}",
            "%smax%" to "${def.stockMax}",
            "%rarity%" to def.rarity,
            "%state%" to state
        ))
        return AdminItems.applyAction(icon, def.displayName, lore, "offer-open:${def.id}")
    }

    fun toggle(plugin: EnsNightMarket, player: Player, holder: AdminHolder, id: String) {
        val def = plugin.market.offerDefinitions[id] ?: return
        YmlWriter.saveOffer(plugin, def.copy(enabled = !def.enabled))
        Msgs.send(plugin, player, "admin-saved")
        if (holder.menu == AdminMenu.OFFER_EDIT) OfferEditMenu.open(plugin, player, id, holder.page)
        else open(plugin, player, holder.page)
    }

    fun addFromHand(plugin: EnsNightMarket, player: Player) {
        val hand = player.inventory.itemInMainHand
        if (hand.type.isAir) {
            Msgs.send(plugin, player, "admin-no-item")
            return
        }
        if (plugin.market.rarityDefinitions.isEmpty()) {
            Msgs.send(plugin, player, "admin-rarity-first")
            return
        }
        val meta = hand.itemMeta
        val name = if (meta != null) dev.ensisdev.ensnightmarket.util.Compat.displayNameOf(meta, pretty(hand.type)) else pretty(hand.type)
        val lore = if (meta != null) dev.ensisdev.ensnightmarket.util.Compat.loreOf(meta) else emptyList()
        val id = uniqueId(plugin, hand.type.name.lowercase())
        val def = OfferDef(
            id = id, displayName = name, material = hand.type.name,
            amount = hand.amount.coerceIn(1, 64), basePrice = 100.0,
            discountMin = 10, discountMax = 50, stockMin = 1, stockMax = 2,
            rarity = "random", permission = null, maxPurchases = 0, lore = lore, enabled = true
        )
        YmlWriter.saveOffer(plugin, def)
        Msgs.send(plugin, player, "admin-added")
        RaritySelectMenu.open(plugin, player, id)
    }

    fun addManual(plugin: EnsNightMarket, player: Player) {
        if (plugin.market.rarityDefinitions.isEmpty()) {
            Msgs.send(plugin, player, "admin-rarity-first")
            return
        }
        ChatInput.prompt(player, ChatInput.Type.STRING, onValue = { input ->
            val spec = input.trim()
            if (CustomItems.resolve(spec, 1) == null) {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player, 0)
                return@prompt
            }
            val id = uniqueId(plugin, spec.lowercase().replace("[^a-z0-9_]+".toRegex(), "_"))
            val def = OfferDef(
                id = id, displayName = pretty(spec), material = spec,
                amount = 1, basePrice = 100.0,
                discountMin = 10, discountMax = 50, stockMin = 1, stockMax = 2,
                rarity = "random", permission = null, maxPurchases = 0, lore = emptyList(), enabled = true
            )
            YmlWriter.saveOffer(plugin, def)
            Msgs.send(plugin, player, "admin-added")
            RaritySelectMenu.open(plugin, player, id)
        }, onCancel = { open(plugin, player, 0) })
    }

    private fun uniqueId(plugin: EnsNightMarket, base: String): String {
        var clean = base.lowercase().replace("[^a-z0-9_]+".toRegex(), "_").trim('_').take(32)
        if (clean.isEmpty()) clean = "offer"
        var id = clean
        var n = 2
        while (plugin.market.offerDefinitions.containsKey(id)) {
            id = "${clean}_$n"
            n++
        }
        return id
    }

    private fun pretty(material: Material): String {
        return material.name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    private fun pretty(spec: String): String {
        val last = spec.substringAfterLast(':')
        return last.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }
}
