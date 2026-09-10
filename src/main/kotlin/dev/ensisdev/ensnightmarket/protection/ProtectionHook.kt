package dev.ensisdev.ensnightmarket.protection

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * Reflection-only WorldGuard hook.
 *
 * The custom `ensnightmarket` state flag is read directly from each applicable
 * region via ProtectedRegion.getFlag() (identity-safe: the registered flag
 * instance is reused). A fail-closed default is used: if WorldGuard is present
 * and protection is enabled but the query itself throws, the market is denied
 * rather than silently allowed.
 */
class ProtectionHook(private val plugin: EnsNightMarket) {
    private var available = false
    private var flag: Any? = null

    fun refresh() {
        available = false
        flag = null
        runCatching {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return
            Class.forName("com.sk89q.worldguard.WorldGuard")
            Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
            val flagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag")
            val flagIface = Class.forName("com.sk89q.worldguard.protection.flags.Flag")
            val candidate = flagClass.getConstructor(String::class.java, Boolean::class.javaPrimitiveType)
                .newInstance("ensnightmarket", true)
            val wg = Class.forName("com.sk89q.worldguard.WorldGuard")
                .getMethod("getInstance").invoke(null)
            val registry = wg.javaClass.getMethod("getFlagRegistry").invoke(wg)
            val registered = runCatching {
                registry.javaClass.getMethod("register", flagIface).invoke(registry, candidate)
                candidate
            }.getOrElse {
                // Already registered (e.g. by another plugin or a reload): reuse the
                // registered instance, region flag lookups are identity-based.
                val existing = registry.javaClass.getMethod("get", String::class.java)
                    .invoke(registry, "ensnightmarket")
                if (existing == null || existing.javaClass != flagClass) throw it
                existing
            }
            flag = registered
            available = true
        }.onFailure {
            plugin.logger.warning("WorldGuard hook failed: ${it.message}")
        }
    }

    fun enabled(): Boolean = plugin.config.getBoolean("protection.enabled", false)

    fun allowedAt(location: Location): Boolean {
        if (!enabled() || !available) return true
        val mode = (plugin.config.getString("protection.mode", "BLACKLIST") ?: "BLACKLIST").uppercase()
        val listed = plugin.config.getStringList("protection.regions").map { it.lowercase() }.toSet()
        val result = runCatching { evaluate(location, listed) }.onFailure {
            plugin.logger.warning("WorldGuard region query failed, denying market to be safe: ${it.message}")
        }.getOrNull() ?: return false
        return if (mode == "WHITELIST") result.inListed && !result.denied
        else !result.inListed && !result.denied
    }

    private data class RegionResult(val inListed: Boolean, val denied: Boolean)

    private fun evaluate(location: Location, listed: Set<String>): RegionResult {
        val world = location.world ?: return RegionResult(false, false)
        val adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
        val wg = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null)
        val platform = wg.javaClass.getMethod("getPlatform").invoke(wg)
        val container = platform.javaClass.getMethod("getRegionContainer").invoke(platform)
        val weWorld = adapter.getMethod("adapt", org.bukkit.World::class.java).invoke(null, world)
        val manager: Any = container.javaClass.getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
            .invoke(container, weWorld) ?: return RegionResult(false, false)
        val weLocation = adapter.getMethod("adapt", Location::class.java).invoke(null, location)
        val set = manager.javaClass.methods.firstOrNull {
            it.name == "getApplicableRegions" && it.parameterTypes.size == 1
        }?.invoke(manager, weLocation) ?: return RegionResult(false, false)
        @Suppress("UNCHECKED_CAST")
        val regions = set.javaClass.getMethod("getRegions").invoke(set) as? Set<*> ?: emptySet<Any>()
        val flagIface = Class.forName("com.sk89q.worldguard.protection.flags.Flag")
        val stateFlag = flag
        var inListed = false
        var denied = false
        regions.forEach { region ->
            val id = runCatching { region?.javaClass?.getMethod("getId")?.invoke(region) as? String }
                .getOrNull()?.lowercase() ?: return@forEach
            if (listed.isNotEmpty() && id in listed) inListed = true
            if (stateFlag != null) {
                val state = runCatching {
                    region?.javaClass?.getMethod("getFlag", flagIface)?.invoke(region, stateFlag)
                }.getOrNull()?.toString()
                if (state == "DENY") denied = true
            }
        }
        return RegionResult(inListed, denied)
    }
}
