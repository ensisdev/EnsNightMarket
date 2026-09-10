package dev.ensisdev.ensnightmarket.placeholder

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.entity.Player

/**
 * PlaceholderAPI'ye compile-time bagimlilik olmadan baglanir.
 * PAPI yoksa veya sürümü eskiyse sessizce kayit yapmaz, hata fýrlatmaz.
 * Destek: PAPI 2.10+ (1.20 ve 1.21 sunucular).
 */
class PlaceholderHook(private val plugin: EnsNightMarket) {
    private var expansion: Any? = null

    fun register(): Boolean = runCatching {
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) return false
        val clazz = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion")
        val handler = java.lang.reflect.Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz)
        ) { _, method, args ->
            when (method.name) {
                "getIdentifier" -> "ensnightmarket"
                "getAuthor" -> "EnsisDev"
                "getVersion" -> plugin.description.version
                "persist" -> true
                "canRegister" -> true
                "onPlaceholderRequest" -> onRequest(args?.getOrNull(0) as? Player, args?.getOrNull(1) as? String ?: "")
                "onRequest" -> onRequest(args?.getOrNull(0) as? Player, args?.getOrNull(1) as? String ?: "")
                "equals" -> (args?.getOrNull(0) === this)
                "hashCode" -> System.identityHashCode(this)
                "toString" -> "EnsNightMarketExpansion"
                else -> method.defaultValue()
            }
        }
        expansion = handler
        (clazz.getMethod("register").invoke(handler) as? Boolean) ?: false
    }.getOrDefault(false)

    fun unregister(): Boolean = runCatching {
        val exp = expansion ?: return false
        exp.javaClass.interfaces.firstOrNull()
            ?.getMethod("unregister")?.invoke(exp)
        expansion = null
        true
    }.getOrDefault(false)

    private fun java.lang.reflect.Method.defaultValue(): Any? = when (returnType.name) {
        "boolean" -> false
        "int" -> 0
        "long" -> 0L
        "void" -> null
        else -> null
    }

    fun onRequest(player: Player?, params: String): String {
        if (player == null) return ""
        val market = plugin.market.getOrCreate(player.uniqueId)
        return when (params.lowercase()) {
            "refresh" -> formatDuration((market.expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
            "refresh_seconds" -> ((market.expiresAt - System.currentTimeMillis()).coerceAtLeast(0) / 1000).toString()
            "refresh_hours" -> (((market.expiresAt - System.currentTimeMillis()).coerceAtLeast(0) / 1000) / 3600).toString()
            "offers" -> market.offers.size.toString()
            "revealed" -> market.offers.count { it.revealed }.toString()
            "unrevealed" -> market.offers.count { !it.revealed }.toString()
            "remaining" -> market.offers.sumOf { it.stock }.toString()
            "purchased" -> market.offers.count { it.purchased }.toString()
            "purchases" -> market.offers.sumOf { it.purchases }.toString()
            "economy" -> plugin.economy.providerName()
            "balance" -> plugin.economy.format(plugin.economy.balance(player))
            "schedule_open" -> plugin.schedule.isOpen(player.world.name).toString()
            "schedule_enabled" -> plugin.schedule.enabled().toString()
            "session_active" -> plugin.display.hasSession(player.uniqueId).toString()
            "session_remaining" -> sessionRemainingText(player)
            "session_remaining_seconds" -> sessionRemainingSeconds(player)
            "refresh_price" -> plugin.economy.format(plugin.config.getDouble("refresh.price", 500.0))
            "refresh_enabled" -> plugin.config.getBoolean("refresh.enabled", true).toString()
            else -> ""
        }
    }

    private fun sessionRemainingText(player: Player): String {
        val left = plugin.display.sessionRemaining(player.uniqueId)
        if (!plugin.display.hasSession(player.uniqueId)) return ""
        if (left == Long.MAX_VALUE) return "∞"
        return formatDuration(left)
    }

    private fun sessionRemainingSeconds(player: Player): String {
        val left = plugin.display.sessionRemaining(player.uniqueId)
        if (!plugin.display.hasSession(player.uniqueId)) return ""
        if (left == Long.MAX_VALUE) return "-1"
        return (left / 1000).toString()
    }

    private fun formatDuration(ms: Long): String {
        val total = ms / 1000
        val d = total / 86400; val h = (total % 86400) / 3600; val m = (total % 3600) / 60; val s = total % 60
        val u = dev.ensisdev.ensnightmarket.lang.Lang
        return "$d${u.get("unit-day")} $h${u.get("unit-hour")} $m${u.get("unit-min")} $s${u.get("unit-sec")}"
    }
}
