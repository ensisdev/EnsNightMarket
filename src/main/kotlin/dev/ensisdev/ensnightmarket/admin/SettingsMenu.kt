package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SettingsMenu {
    fun open(plugin: EnsNightMarket, player: Player) {
        val holder = AdminHolder(AdminMenu.SETTINGS)
        val inv = dev.ensisdev.ensnightmarket.util.Compat.inv(holder, 45, Lang.get("gui-settings-title"))
        holder.bind(inv)
        val filler = AdminItems.filler()
        for (i in 0 until inv.size) inv.setItem(i, filler)
        val c = plugin.config
        inv.setItem(9, AdminItems.button(Material.CHEST, Lang.get("gui-cat-market"), emptyList(), "noop"))
        inv.setItem(10, num(Material.CHEST, Lang.get("gui-s-slots"), "${c.getInt("market.slots", 6)}", "setting:slots"))
        inv.setItem(11, num(Material.SUNFLOWER, Lang.get("gui-s-refresh-hours"), "${c.getLong("market.refresh-hours", 72)}", "setting:refreshHours"))
        inv.setItem(12, toggle(Lang.get("gui-s-unique"), c.getBoolean("market.unique-offers", true), "setting:unique"))
        inv.setItem(18, AdminItems.button(Material.CLOCK, Lang.get("gui-cat-time"), emptyList(), "noop"))
        inv.setItem(19, toggle(Lang.get("gui-s-schedule"), c.getBoolean("schedule.enabled", false), "setting:schedule"))
        inv.setItem(20, num(Material.REPEATER, Lang.get("gui-s-open-tick"), "${c.getLong("schedule.open-tick", 13000)}", "setting:openTick"))
        inv.setItem(21, num(Material.REPEATER, Lang.get("gui-s-close-tick"), "${c.getLong("schedule.close-tick", 23000)}", "setting:closeTick"))
        inv.setItem(27, AdminItems.button(Material.GOLD_INGOT, Lang.get("gui-cat-economy"), emptyList(), "noop"))
        inv.setItem(28, num(Material.EMERALD, Lang.get("gui-s-economy"), c.getString("economy.provider", "VAULT") ?: "VAULT", "setting:economy"))
        inv.setItem(29, toggle(Lang.get("gui-s-paid-refresh"), c.getBoolean("refresh.enabled", true), "setting:paidRefresh"))
        inv.setItem(30, num(Material.GOLD_NUGGET, Lang.get("gui-s-refresh-price"), "${c.getDouble("refresh.price", 500.0)}", "setting:refreshPrice"))
        inv.setItem(31, num(Material.CLOCK, Lang.get("gui-s-refresh-cooldown"), "${c.getLong("refresh.cooldown-minutes", 30)}", "setting:refreshCooldown"))
        inv.setItem(36, AdminItems.button(Material.REDSTONE, Lang.get("gui-cat-system"), emptyList(), "noop"))
        inv.setItem(37, num(Material.CLOCK, Lang.get("gui-s-session-duration"), "${c.getLong("session.duration-minutes", 10)}", "setting:sessionDuration"))
        inv.setItem(38, num(Material.DIAMOND, Lang.get("gui-s-quality"), c.getString("performance.animation-quality", "HIGH") ?: "HIGH", "setting:quality"))
        inv.setItem(39, toggle(Lang.get("gui-s-follow"), c.getBoolean("session.follow-enabled", true), "setting:follow"))
        inv.setItem(40, AdminItems.button(Material.OAK_DOOR, Lang.get("common-back"), emptyList(), "back-main"))
        inv.setItem(43, AdminItems.button(Material.BARRIER, Lang.get("common-close"), emptyList(), "close"))
        player.openInventory(inv)
    }

    private fun num(icon: Material, name: String, value: String, action: String): ItemStack {
        return AdminItems.button(icon, "<white>$name", listOf("<gray>$value", "", "<yellow>Tıkla <gray>düzenle"), action)
    }

    private fun toggle(name: String, on: Boolean, action: String): ItemStack {
        val mat = if (on) Material.LIME_WOOL else Material.RED_WOOL
        val state = if (on) "<green>Açık" else "<red>Kapalı"
        return AdminItems.button(mat, "<white>$name", listOf("<gray>$state", "", "<yellow>Tıkla <gray>değiştir"), action)
    }

    fun edit(plugin: EnsNightMarket, player: Player, key: String) {
        when (key) {
            "unique" -> flip(plugin, player, "market.unique-offers")
            "schedule" -> { flip(plugin, player, "schedule.enabled"); plugin.schedule.reload() }
            "paidRefresh" -> flip(plugin, player, "refresh.enabled")
            "follow" -> flip(plugin, player, "session.follow-enabled")
            "economy" -> {
                val next = if ((plugin.config.getString("economy.provider", "VAULT") ?: "VAULT").uppercase() == "VAULT") "PLAYER_POINTS" else "VAULT"
                YmlWriter.setConfig(plugin, "economy.provider", next)
                plugin.economy.refresh()
                saved(plugin, player)
            }
            "quality" -> {
                val order = listOf("LOW", "MEDIUM", "HIGH", "ULTRA")
                val current = (plugin.config.getString("performance.animation-quality", "HIGH") ?: "HIGH").uppercase()
                val next = order[(order.indexOf(current).coerceAtLeast(0) + 1) % order.size]
                YmlWriter.setConfig(plugin, "performance.animation-quality", next)
                saved(plugin, player)
            }
            "slots" -> askLong(plugin, player, 1, 54) { YmlWriter.setConfig(plugin, "market.slots", it.toInt()) }
            "refreshHours" -> askLong(plugin, player, 1, 720) { YmlWriter.setConfig(plugin, "market.refresh-hours", it) }
            "openTick" -> askLong(plugin, player, 0, 23999) { YmlWriter.setConfig(plugin, "schedule.open-tick", it); plugin.schedule.reload() }
            "closeTick" -> askLong(plugin, player, 0, 23999) { YmlWriter.setConfig(plugin, "schedule.close-tick", it); plugin.schedule.reload() }
            "refreshPrice" -> askDouble(plugin, player, 0.0) { YmlWriter.setConfig(plugin, "refresh.price", it) }
            "refreshCooldown" -> askLong(plugin, player, 0, 10080) { YmlWriter.setConfig(plugin, "refresh.cooldown-minutes", it) }
            "sessionDuration" -> askLong(plugin, player, 0, 10080) { YmlWriter.setConfig(plugin, "session.duration-minutes", it) }
            else -> open(plugin, player)
        }
    }

    private fun flip(plugin: EnsNightMarket, player: Player, path: String) {
        YmlWriter.setConfig(plugin, path, !plugin.config.getBoolean(path))
        saved(plugin, player)
    }

    private fun askLong(plugin: EnsNightMarket, player: Player, min: Long, max: Long, apply: (Long) -> Unit) {
        ChatInput.prompt(player, ChatInput.Type.LONG, onValue = { input ->
            val value = input.toLongOrNull()?.coerceIn(min, max)
            if (value == null) {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player)
                return@prompt
            }
            apply(value)
            saved(plugin, player)
        }, onCancel = { open(plugin, player) })
    }

    private fun askDouble(plugin: EnsNightMarket, player: Player, min: Double, apply: (Double) -> Unit) {
        ChatInput.prompt(player, ChatInput.Type.DOUBLE, onValue = { input ->
            val value = input.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(min)
            if (value == null) {
                Msgs.send(plugin, player, "admin-invalid")
                open(plugin, player)
                return@prompt
            }
            apply(value)
            saved(plugin, player)
        }, onCancel = { open(plugin, player) })
    }

    private fun saved(plugin: EnsNightMarket, player: Player) {
        Msgs.send(plugin, player, "admin-saved")
        open(plugin, player)
    }
}
