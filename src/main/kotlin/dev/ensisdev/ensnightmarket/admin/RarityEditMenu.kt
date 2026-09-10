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

object RarityEditMenu {
    fun open(plugin: EnsNightMarket, player: Player, id: String, listPage: Int = 0) {
        val def = plugin.market.rarityDefinitions[id] ?: run {
            RarityListMenu.open(plugin, player, 0)
            return
        }
        val holder = AdminHolder(AdminMenu.RARITY_EDIT, listPage, id)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 45, Lang.get("gui-redit-title", mapOf("%id%" to def.id)))
        holder.bind(inv)
        val filler = AdminItems.fillerOf(Material.BLACK_STAINED_GLASS_PANE)
        for (i in 0 until inv.size) inv.setItem(i, filler)
        val preview = try {
            HeadFactory.create(def.headType, def.headValue)
        } catch (t: Throwable) {
            ItemStack(Material.PLAYER_HEAD)
        }
        inv.setItem(9, AdminItems.button(Material.ENDER_EYE, "<white>${def.headType}", emptyList(), "noop"))
        inv.setItem(18, AdminItems.applyAction(preview, def.displayName,
            Lang.list("gui-redit-preview", mapOf("%id%" to def.id, "%weight%" to "${def.weight}")), "noop"))
        inv.setItem(27, AdminItems.button(Material.TNT, Lang.get("gui-delete"), Lang.list("gui-delete-note"), "rarity-delete:$id"))
        inv.setItem(10, field(Material.NAME_TAG, Lang.get("gui-f-name"), def.displayName, "rarity-field:$id:name"))
        inv.setItem(11, field(Material.ANVIL, Lang.get("gui-f-weight"), "${def.weight}", "rarity-field:$id:weight"))
        inv.setItem(12, field(Material.WHITE_WOOL, Lang.get("gui-f-color"), def.color, "rarity-field:$id:color"))
        inv.setItem(13, field(Material.BLAZE_POWDER, Lang.get("gui-f-particle"), def.particle, "rarity-field:$id:particle"))
        inv.setItem(14, field(Material.GLOWSTONE_DUST, Lang.get("gui-f-particle-count"), "${def.particleCount}", "rarity-field:$id:particleCount"))
        inv.setItem(15, field(Material.NOTE_BLOCK, Lang.get("gui-f-sound"), def.sound, "rarity-field:$id:sound"))
        inv.setItem(16, field(Material.BELL, Lang.get("gui-f-pitch"), "${def.pitch}", "rarity-field:$id:pitch"))
        inv.setItem(19, field(Material.SLIME_BALL, Lang.get("gui-f-scale"), "${def.floatingScale}", "rarity-field:$id:scale"))
        inv.setItem(20, field(Material.TOTEM_OF_UNDYING, Lang.get(if (def.aura) "gui-f-aura-on" else "gui-f-aura-off"), Lang.get("gui-change-note"), "rarity-field:$id:aura"))
        inv.setItem(21, field(Material.PLAYER_HEAD, Lang.get("gui-f-head-texture"), Lang.get("gui-f-head-texture-value", mapOf("%type%" to def.headType)), "rarity-field:$id:headTexture"))
        inv.setItem(22, field(Material.SKELETON_SKULL, Lang.get("gui-f-head-hdb"), Lang.get("gui-f-head-hdb-value"), "rarity-field:$id:headHdb"))
        inv.setItem(23, field(Material.CREEPER_HEAD, Lang.get("gui-f-head-url"), Lang.get("gui-f-head-url-value"), "rarity-field:$id:headUrl"))
        inv.setItem(36, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "back-rarities"))
        inv.setItem(44, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    private fun field(icon: Material, name: String, value: String, action: String): ItemStack {
        return AdminItems.button(icon, "<white>$name", listOf("<gray>$value", "", Lang.get("common-hint-edit")), action)
    }

    fun editField(plugin: EnsNightMarket, player: Player, id: String, field: String, listPage: Int = 0) {
        val def = plugin.market.rarityDefinitions[id] ?: run {
            RarityListMenu.open(plugin, player, 0)
            return
        }
        if (field == "aura") {
            YmlWriter.saveRarity(plugin, def.copy(aura = !def.aura))
            Msgs.send(plugin, player, "admin-saved")
            open(plugin, player, id, listPage)
            return
        }
        val type = when (field) {
            "weight", "pitch", "scale" -> ChatInput.Type.DOUBLE
            "particleCount" -> ChatInput.Type.INT
            else -> ChatInput.Type.STRING
        }
        ChatInput.prompt(player, type, onValue = { input ->
            val updated = applyField(plugin, def, field, input.trim()) ?: run {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player, id, listPage)
                return@prompt
            }
            YmlWriter.saveRarity(plugin, updated)
            Msgs.send(plugin, player, "admin-saved")
            open(plugin, player, id, listPage)
        }, onCancel = { open(plugin, player, id, listPage) })
    }

    private fun applyField(plugin: EnsNightMarket, def: dev.ensisdev.ensnightmarket.market.RarityDef, field: String, input: String): dev.ensisdev.ensnightmarket.market.RarityDef? {
        if (input.isEmpty()) return null
        return when (field) {
            "name" -> def.copy(displayName = input)
            "weight" -> input.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)?.let { def.copy(weight = it) }
            "color" -> {
                val hex = input.trim()
                if (!hex.matches("#[0-9a-fA-F]{6}".toRegex())) null else def.copy(color = hex.uppercase())
            }
            "particle" -> try {
                org.bukkit.Particle.valueOf(input.uppercase())
                def.copy(particle = input.uppercase())
            } catch (t: IllegalArgumentException) { null }
            "particleCount" -> input.toIntOrNull()?.coerceIn(0, 100)?.let { def.copy(particleCount = it) }
            "sound" -> try {
                org.bukkit.Sound.valueOf(input.uppercase())
                def.copy(sound = input.uppercase())
            } catch (t: IllegalArgumentException) { null }
            "pitch" -> input.replace(",", ".").toDoubleOrNull()?.coerceIn(0.1, 2.0)?.let { def.copy(pitch = it.toFloat()) }
            "scale" -> input.replace(",", ".").toDoubleOrNull()?.coerceIn(0.2, 4.0)?.let { def.copy(floatingScale = it.toFloat()) }
            "headTexture" -> def.copy(headType = "TEXTURE_VALUE", headValue = input)
            "headHdb" -> def.copy(headType = "HDB", headValue = input)
            "headUrl" -> def.copy(headType = "URL", headValue = input)
            else -> null
        }
    }
}
