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

object RarityListMenu {
    private val GRID = listOf(
        0, 1, 2, 3, 4, 5, 6,
        9, 10, 11, 12, 13, 14, 15,
        18, 19, 20, 21, 22, 23, 24,
        27, 28, 29, 30, 31, 32, 33
    )

    fun open(plugin: EnsNightMarket, player: Player, page: Int) {
        val defs = plugin.market.rarityDefinitions.values.sortedBy { it.id }
        val maxPage = (defs.size / GRID.size).coerceAtLeast(0)
        val safePage = page.coerceIn(0, maxPage)
        val holder = AdminHolder(AdminMenu.RARITIES, safePage)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 54, Lang.get("gui-rarities-title", mapOf("%page%" to "${safePage + 1}", "%count%" to "${defs.size}")))
        holder.bind(inv)
        val filler = AdminItems.filler()
        for (i in 0 until inv.size) inv.setItem(i, filler)
        defs.drop(safePage * GRID.size).take(GRID.size).forEachIndexed { index, def ->
            val icon = try {
                HeadFactory.create(def.headType, def.headValue)
            } catch (t: Throwable) {
                ItemStack(Material.PLAYER_HEAD)
            }
            inv.setItem(GRID[index], AdminItems.applyAction(
                icon, def.displayName,
                Lang.list("gui-rarity-card", mapOf("%id%" to def.id, "%weight%" to "${def.weight}", "%sound%" to def.sound)),
                "rarity-open:${def.id}"
            ))
        }
        if (defs.isEmpty()) {
            inv.setItem(22, AdminItems.button(Material.HOPPER, Lang.get("gui-empty-rarities"), Lang.list("gui-empty-rarities-lore"), "noop"))
        }
        val pageRep = mapOf("%page%" to "${safePage + 1}")
        inv.setItem(36, AdminItems.button(Material.ARROW, Lang.get("gui-prev"), Lang.list("gui-prev-lore", pageRep), "rarity-page-prev"))
        inv.setItem(40, AdminItems.button(Material.PAPER,
            Lang.get("gui-page-info", mapOf("%page%" to "${safePage + 1}", "%pages%" to "${maxPage + 1}")),
            Lang.list("gui-total-lore", mapOf("%count%" to "${defs.size}")), "noop"))
        inv.setItem(44, AdminItems.button(Material.ARROW, Lang.get("gui-next"), Lang.list("gui-next-lore", pageRep), "rarity-page-next"))
        inv.setItem(45, AdminItems.button(Material.LIME_DYE, Lang.get("gui-rarity-add"), Lang.list("gui-rarity-add-lore"), "rarity-add"))
        inv.setItem(49, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "back-main"))
        inv.setItem(53, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    fun create(plugin: EnsNightMarket, player: Player) {
        ChatInput.prompt(player, ChatInput.Type.STRING, onValue = { input ->
            var id = input.trim().lowercase().replace("[^a-z0-9_]+".toRegex(), "_").trim('_')
            if (id.isEmpty()) {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player, 0)
                return@prompt
            }
            if (plugin.market.rarityDefinitions.containsKey(id)) {
                Msgs.send(plugin, player, "admin-exists")
                open(plugin, player, 0)
                return@prompt
            }
            YmlWriter.saveRarity(plugin, dev.ensisdev.ensnightmarket.market.RarityDef(
                id = id, displayName = "<gray>$id", weight = 10.0, color = "#AAAAAA",
                headType = "TEXTURE_VALUE", headValue = "",
                particle = "END_ROD", particleCount = 3,
                sound = "BLOCK_NOTE_BLOCK_PLING", pitch = 1f,
                floatingScale = 1.55f, aura = true
            ))
            Msgs.send(plugin, player, "admin-added")
            RarityEditMenu.open(plugin, player, id)
        }, onCancel = { open(plugin, player, 0) })
    }
}
