package dev.ensisdev.ensnightmarket.util

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.meta.ItemMeta

/**
 * Spigot + Paper uyumluluk katmaný.
 * Paper'da Component overload'larý kullanýlýr, Spigot'ta legacy String'e düþer.
 * NoSuchMethodError yakalanýr, asla crash vermez.
 */
object Compat {

    fun createInventory(holder: InventoryHolder?, size: Int, title: Component, legacy: String): Inventory {
        return runCatching { Bukkit.createInventory(holder, size, title) }
            .getOrElse { Bukkit.createInventory(holder, size, legacy.take(32)) }
    }

    /** MiniMessage string alır, Paper'da Component / Spigot'ta legacy başlık açar. */
    fun inv(holder: InventoryHolder?, size: Int, miniMessage: String): Inventory =
        createInventory(holder, size, Text.component(miniMessage), Text.legacy(miniMessage))

    fun setName(meta: ItemMeta, name: Component, legacy: String) {
        runCatching { meta.displayName(name) }
            .onFailure { runCatching { meta.setDisplayName(legacy) } }
    }

    fun setLore(meta: ItemMeta, lore: List<Component>, legacy: List<String>) {
        runCatching { meta.lore(lore) }
            .onFailure { runCatching { meta.setLore(legacy) } }
    }

    fun displayNameOf(meta: ItemMeta, fallback: String): String {
        runCatching {
            val c = meta.displayName() ?: return fallback
            return Text.plainOf(c).ifEmpty { fallback }
        }
        @Suppress("DEPRECATION")
        return runCatching { meta.displayName ?: fallback }.getOrDefault(fallback)
    }

    fun loreOf(meta: ItemMeta): List<String> {
        runCatching {
            val lines = meta.lore() ?: return emptyList()
            return lines.map { Text.plainOf(it) }
        }
        @Suppress("DEPRECATION")
        return runCatching { meta.lore ?: emptyList() }.getOrDefault(emptyList())
    }

    fun send(sender: CommandSender, message: Component, legacy: String) {
        runCatching { sender.sendMessage(message) }
            .onFailure { runCatching { sender.sendMessage(legacy) } }
    }

    fun title(player: Player, title: Component, subtitle: Component, legacyTitle: String, legacySub: String) {
        runCatching {
            player.showTitle(
                net.kyori.adventure.title.Title.title(
                    title, subtitle,
                    net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(1500),
                        java.time.Duration.ofMillis(500)
                    )
                )
            )
            return
        }
        @Suppress("DEPRECATION")
        runCatching { player.sendTitle(legacyTitle, legacySub, 5, 30, 10) }
    }
}
