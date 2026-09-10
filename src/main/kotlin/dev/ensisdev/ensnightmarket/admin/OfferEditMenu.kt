package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.items.CustomItems
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object OfferEditMenu {
    fun open(plugin: EnsNightMarket, player: Player, id: String, listPage: Int = 0) {
        val def = plugin.market.offerDefinitions[id] ?: run {
            OfferListMenu.open(plugin, player, 0)
            return
        }
        val holder = AdminHolder(AdminMenu.OFFER_EDIT, listPage, id)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 45, Lang.get("gui-edit-title", mapOf("%id%" to def.id)))
        holder.bind(inv)
        val filler = AdminItems.fillerOf(Material.BLACK_STAINED_GLASS_PANE)
        for (i in 0 until inv.size) inv.setItem(i, filler)
        val preview = CustomItems.resolve(def.material, def.amount) ?: ItemStack(Material.STONE)
        inv.setItem(4, AdminItems.applyAction(
            preview, def.displayName,
            def.lore + Lang.list("gui-preview-price", mapOf("%price%" to "${def.basePrice}")) +
                Lang.list("gui-preview-discount", mapOf("%dmin%" to "${def.discountMin}", "%dmax%" to "${def.discountMax}")),
            "noop"
        ))
        inv.setItem(9, AdminItems.button(Material.GOLD_INGOT, Lang.get("gui-group-price"), emptyList(), "noop"))
        inv.setItem(10, field(Material.NAME_TAG, Lang.get("gui-f-name"), def.displayName, "offer-field:$id:name"))
        inv.setItem(11, field(Material.STONE, Lang.get("gui-f-material"), def.material, "offer-field:$id:material"))
        inv.setItem(12, field(Material.GOLD_INGOT, Lang.get("gui-f-price"), "${def.basePrice}", "offer-field:$id:price"))
        inv.setItem(13, field(Material.CHEST, Lang.get("gui-f-amount"), "${def.amount}", "offer-field:$id:amount"))
        inv.setItem(14, field(Material.ENDER_EYE, Lang.get("gui-f-rarity"), def.rarity, "offer-rarity:$id"))
        inv.setItem(18, AdminItems.button(Material.EMERALD, Lang.get("gui-group-sale"), emptyList(), "noop"))
        inv.setItem(19, field(Material.GREEN_DYE, Lang.get("gui-f-discount-min"), "%${def.discountMin}", "offer-field:$id:discountMin"))
        inv.setItem(20, field(Material.RED_DYE, Lang.get("gui-f-discount-max"), "%${def.discountMax}", "offer-field:$id:discountMax"))
        inv.setItem(21, field(Material.HOPPER, Lang.get("gui-f-stock-min"), "${def.stockMin}", "offer-field:$id:stockMin"))
        inv.setItem(22, field(Material.DROPPER, Lang.get("gui-f-stock-max"), "${def.stockMax}", "offer-field:$id:stockMax"))
        inv.setItem(27, AdminItems.button(Material.CHAIN, Lang.get("gui-group-rules"), emptyList(), "noop"))
        inv.setItem(28, field(Material.TRIPWIRE_HOOK, Lang.get("gui-f-permission"), def.permission ?: Lang.get("common-none"), "offer-field:$id:permission"))
        inv.setItem(29, field(Material.CLOCK, Lang.get("gui-f-limit"), "${def.maxPurchases}", "offer-field:$id:maxPurchases"))
        inv.setItem(30, field(Material.WRITABLE_BOOK, Lang.get("gui-f-lore"), Lang.get("gui-lore-lines", mapOf("%n%" to "${def.lore.size}")), "offer-field:$id:lore"))
        inv.setItem(31, field(if (def.enabled) Material.LIME_WOOL else Material.RED_WOOL, Lang.get(if (def.enabled) "gui-f-status-on" else "gui-f-status-off"), Lang.get("gui-change-note"), "offer-toggle:$id"))
        inv.setItem(33, AdminItems.button(Material.TNT, Lang.get("gui-delete"), Lang.list("gui-delete-note"), "offer-delete:$id"))
        inv.setItem(36, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "back-offers"))
        inv.setItem(44, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    private fun field(icon: Material, name: String, value: String, action: String): ItemStack {
        return AdminItems.button(icon, "<white>$name", listOf("<gray>$value", "", Lang.get("common-hint-edit")), action)
    }

    fun editField(plugin: EnsNightMarket, player: Player, id: String, field: String, listPage: Int = 0) {
        val def = plugin.market.offerDefinitions[id] ?: run {
            OfferListMenu.open(plugin, player, 0)
            return
        }
        if (field == "lore") {
            ChatInput.promptLore(player, def.lore, onDone = { lines ->
                YmlWriter.saveOffer(plugin, def.copy(lore = lines))
                Msgs.send(plugin, player, "admin-saved")
                open(plugin, player, id, listPage)
            }, onCancel = { open(plugin, player, id, listPage) })
            return
        }
        val type = when (field) {
            "price" -> ChatInput.Type.DOUBLE
            "amount", "discountMin", "discountMax", "stockMin", "stockMax", "maxPurchases" -> ChatInput.Type.INT
            else -> ChatInput.Type.STRING
        }
        ChatInput.prompt(player, type, onValue = { input ->
            val updated = applyField(def, field, input.trim()) ?: run {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player, id, listPage)
                return@prompt
            }
            YmlWriter.saveOffer(plugin, updated)
            Msgs.send(plugin, player, "admin-saved")
            open(plugin, player, id, listPage)
        }, onCancel = { open(plugin, player, id, listPage) })
    }

    private fun applyField(def: dev.ensisdev.ensnightmarket.market.OfferDef, field: String, input: String): dev.ensisdev.ensnightmarket.market.OfferDef? {
        return when (field) {
            "name" -> if (input.isEmpty()) null else def.copy(displayName = input)
            "material" -> if (CustomItems.resolve(input, 1) == null) null else def.copy(material = input)
            "price" -> input.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)?.let { def.copy(basePrice = it) }
            "amount" -> input.toIntOrNull()?.coerceIn(1, 64)?.let { def.copy(amount = it) }
            "discountMin" -> input.toIntOrNull()?.coerceIn(0, 100)?.let { def.copy(discountMin = it, discountMax = def.discountMax.coerceAtLeast(it)) }
            "discountMax" -> input.toIntOrNull()?.coerceIn(0, 100)?.let { def.copy(discountMax = it.coerceAtLeast(def.discountMin)) }
            "stockMin" -> input.toIntOrNull()?.coerceAtLeast(0)?.let { def.copy(stockMin = it, stockMax = def.stockMax.coerceAtLeast(it)) }
            "stockMax" -> input.toIntOrNull()?.coerceAtLeast(0)?.let { def.copy(stockMax = it.coerceAtLeast(def.stockMin)) }
            "permission" -> def.copy(permission = input.ifBlank { null }?.takeUnless { it.equals("yok", true) || it.equals("none", true) })
            "maxPurchases" -> input.toIntOrNull()?.coerceAtLeast(0)?.let { def.copy(maxPurchases = it) }
            else -> null
        }
    }
}
