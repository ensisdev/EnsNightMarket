package dev.ensisdev.ensnightmarket.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object Text {
    private val mm = MiniMessage.miniMessage()
    private val plain = PlainTextComponentSerializer.plainText()
    private val legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
    fun component(s: String): Component = mm.deserialize(s.replace("\\n", "<newline>"))
    fun plain(s: String): String = plain.serialize(component(s))
    fun plainOf(c: Component): String = plain.serialize(c)
    /** Spigot legacy envanter baþlýklarý / setDisplayName için. */
    fun legacy(s: String): String = legacy.serialize(component(s))

    /** Converts legacy §/& color codes (e.g. from in-hand items) into MiniMessage tags. */
    fun fromLegacy(s: String): String {
        val parsed = runCatching { legacy.deserialize(s.replace('&', '§')) }.getOrNull()
            ?: return s
        return runCatching { mm.serialize(parsed) }.getOrDefault(s)
    }

    fun prefixed(prefix: String, raw: String, replacements: Map<String, String> = emptyMap()): Component {
        var s = raw.replace("<prefix>", prefix)
        replacements.forEach { (a, b) -> s = s.replace(a, b) }
        return component(s)
    }

    fun prefixedLegacy(prefix: String, raw: String, replacements: Map<String, String> = emptyMap()): String {
        var out = raw.replace("<prefix>", prefix)
        replacements.forEach { (a, b) -> out = out.replace(a, b) }
        return legacy(out)
    }
}
