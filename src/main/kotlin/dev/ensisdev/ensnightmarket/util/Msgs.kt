package dev.ensisdev.ensnightmarket.util

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

object Msgs {
    fun prefix(plugin: EnsNightMarket): String =
        Lang.get("prefix")

    fun get(plugin: EnsNightMarket, key: String, replacements: Map<String, String> = emptyMap()): Component {
        return Text.prefixed(prefix(plugin), Lang.get(key, replacements))
    }

    fun getList(plugin: EnsNightMarket, key: String, replacements: Map<String, String> = emptyMap()): List<Component> {
        return Lang.list(key, replacements).map(Text::component)
    }

    fun send(plugin: EnsNightMarket, sender: CommandSender, key: String, replacements: Map<String, String> = emptyMap()) {
        Compat.send(sender, get(plugin, key, replacements), Text.prefixedLegacy(prefix(plugin), Lang.get(key, replacements), emptyMap()))
    }

    fun raw(plugin: EnsNightMarket, body: String, replacements: Map<String, String> = emptyMap()): Component =
        Text.prefixed(prefix(plugin), "<prefix>$body", replacements)

    fun sendRaw(plugin: EnsNightMarket, sender: CommandSender, body: String, replacements: Map<String, String> = emptyMap()) {
        Compat.send(sender, raw(plugin, body, replacements), Text.prefixedLegacy(prefix(plugin), "<prefix>$body", replacements))
    }

    fun broadcast(plugin: EnsNightMarket, key: String, replacements: Map<String, String> = emptyMap()) {
        val message = get(plugin, key, replacements)
        val legacy = Text.prefixedLegacy(prefix(plugin), Lang.get(key, replacements), emptyMap())
        plugin.server.onlinePlayers.forEach { Compat.send(it, message, legacy) }
    }
}
