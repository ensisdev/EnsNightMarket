package dev.ensisdev.ensnightmarket.protection

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.Bukkit
import org.bukkit.Location

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
            val candidate = flagClass.getConstructor(String::class.java, Boolean::class.javaPrimitiveType)
                .newInstance("ensnightmarket", true)
            val wg = Class.forName("com.sk89q.worldguard.WorldGuard")
                .getMethod("getInstance").invoke(null)
            val registry = wg.javaClass.getMethod("getFlagRegistry").invoke(wg)
            runCatching {
                registry.javaClass.getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
                    .invoke(registry, candidate)
            }.onFailure {
                val existing = registry.javaClass.getMethod("get", String::class.java).invoke(registry, "ensnightmarket")
                if (existing == null || existing.javaClass != flagClass) throw it
            }
            flag = candidate
            available = true
        }
    }

    fun enabled(): Boolean = plugin.config.getBoolean("protection.enabled", false)

    fun allowedAt(location: Location): Boolean {
        if (!enabled() || !available) return true
        val mode = plugin.config.getString("protection.mode", "BLACKLIST")!!.uppercase()
        val listed = plugin.config.getStringList("protection.regions").map { it.lowercase() }.toSet()
        val result = runCatching { evaluate(location, listed) }.getOrNull() ?: return true
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
        val ids = regions.mapNotNull { region ->
            runCatching { region?.javaClass?.getMethod("getId")?.invoke(region) as? String }.getOrNull()
        }.map { it.lowercase() }
        val inListed = listed.isNotEmpty() && ids.any { it in listed }
        var denied = false
        val stateFlag = flag
        if (stateFlag != null) {
            val state = runCatching {
                val query = set.javaClass.methods.firstOrNull {
                    it.name == "queryState" && it.parameterTypes.size == 2
                } ?: return@runCatching null
                query.invoke(set, null, stateFlag)?.toString()
            }.getOrNull()
            denied = state == "DENY"
        }
        return RegionResult(inListed, denied)
    }
}
