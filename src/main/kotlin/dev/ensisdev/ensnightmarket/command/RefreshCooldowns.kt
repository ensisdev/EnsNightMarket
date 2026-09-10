package dev.ensisdev.ensnightmarket.command

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Paid-reroll cooldowns that survive restarts (cooldowns.yml).
 * All access is expected from the main thread; the map itself is concurrent.
 */
class RefreshCooldowns(private val plugin: EnsNightMarket) {
    private val lastRefresh = ConcurrentHashMap<UUID, Long>()

    fun load() {
        lastRefresh.clear()
        val f = file()
        if (!f.exists()) return
        val yml = runCatching { YamlConfiguration.loadConfiguration(f) }.getOrNull() ?: return
        yml.getConfigurationSection("paid-refresh")?.getKeys(false)?.forEach { key ->
            runCatching { UUID.fromString(key) }.getOrNull()?.let { uuid ->
                lastRefresh[uuid] = yml.getLong("paid-refresh.$key", 0L)
            }
        }
    }

    fun last(uuid: UUID): Long = lastRefresh[uuid] ?: 0L

    fun mark(uuid: UUID) {
        lastRefresh[uuid] = System.currentTimeMillis()
        save()
    }

    fun clear(uuid: UUID) {
        if (lastRefresh.remove(uuid) != null) save()
    }

    /** Drops entries whose cooldown window has fully passed. */
    fun sweep(cooldownMs: Long) {
        if (lastRefresh.isEmpty()) return
        val now = System.currentTimeMillis()
        var changed = false
        lastRefresh.entries.toList().forEach { (uuid, at) ->
            if (at + cooldownMs < now) {
                lastRefresh.remove(uuid)
                changed = true
            }
        }
        if (changed) save()
    }

    private fun file(): File {
        plugin.dataFolder.mkdirs()
        return File(plugin.dataFolder, "cooldowns.yml")
    }

    private fun save() {
        runCatching {
            val yml = YamlConfiguration()
            lastRefresh.forEach { (uuid, at) -> yml.set("paid-refresh.$uuid", at) }
            yml.save(file())
        }.onFailure { plugin.logger.warning("Could not save cooldowns.yml: ${it.message}") }
    }
}
