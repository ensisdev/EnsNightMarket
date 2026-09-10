package dev.ensisdev.ensnightmarket.util

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.entity.Player
import java.net.HttpURLConnection
import java.net.URI

/**
 * Lightweight GitHub-releases update check. Async, silent on failure,
 * toggleable via `updates.check`. Notifies console once and ops on join.
 */
class UpdateChecker(private val plugin: EnsNightMarket) {
    @Volatile private var latest: String? = null

    fun start() {
        if (!plugin.config.getBoolean("updates.check", true)) return
        plugin.server.scheduler.runTaskLaterAsynchronously(plugin, Runnable { check() }, 100L)
    }

    fun notify(player: Player) {
        val v = latest ?: return
        if (!player.isOp && !player.hasPermission("ensnightmarket.admin")) return
        Msgs.send(plugin, player, "update-available", mapOf(
            "%version%" to v,
            "%current%" to plugin.description.version
        ))
    }

    private fun check() {
        val current = plugin.description.version
        val body = runCatching {
            val conn = URI("https://api.github.com/repos/ensisdev/EnsNightMarket/releases/latest")
                .toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "EnsNightMarket/$current")
            conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
        }.getOrNull() ?: return
        val remote = Regex("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").find(body)
            ?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return
        if (isNewer(current, remote)) {
            latest = remote
            plugin.logger.info("A new version of EnsNightMarket is available: $remote (you run $current).")
        }
    }

    private fun isNewer(current: String, remote: String): Boolean {
        if (current.equals(remote, true)) return false
        val c = current.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        val r = remote.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        if (c.isEmpty() || r.isEmpty()) return current != remote
        val n = maxOf(c.size, r.size)
        for (i in 0 until n) {
            val diff = (r.getOrElse(i) { 0 }) - (c.getOrElse(i) { 0 })
            if (diff != 0) return diff > 0
        }
        return false
    }
}
