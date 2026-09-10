package dev.ensisdev.ensnightmarket

import dev.ensisdev.ensnightmarket.admin.AdminItems
import dev.ensisdev.ensnightmarket.admin.AdminRouter
import dev.ensisdev.ensnightmarket.admin.ChatInput
import dev.ensisdev.ensnightmarket.command.EnmCommand
import dev.ensisdev.ensnightmarket.config.Configs
import dev.ensisdev.ensnightmarket.display.FloatingDisplayManager
import dev.ensisdev.ensnightmarket.economy.EconomyManager
import dev.ensisdev.ensnightmarket.listener.MarketListener
import dev.ensisdev.ensnightmarket.market.MarketManager
import dev.ensisdev.ensnightmarket.items.CustomItems
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.placeholder.PlaceholderHook
import dev.ensisdev.ensnightmarket.protection.ProtectionHook
import dev.ensisdev.ensnightmarket.schedule.ScheduleManager
import dev.ensisdev.ensnightmarket.storage.MySqlStorage
import dev.ensisdev.ensnightmarket.storage.SqliteStorage
import dev.ensisdev.ensnightmarket.storage.Storage
import dev.ensisdev.ensnightmarket.storage.AsyncStorage
import org.bukkit.plugin.java.JavaPlugin

class EnsNightMarket : JavaPlugin() {
    lateinit var configs: Configs; private set
    lateinit var economy: EconomyManager; private set
    lateinit var storage: Storage; private set
    lateinit var market: MarketManager; private set
    lateinit var display: FloatingDisplayManager; private set
    lateinit var schedule: ScheduleManager; private set
    lateinit var protection: ProtectionHook; private set
    lateinit var updateChecker: dev.ensisdev.ensnightmarket.util.UpdateChecker; private set
    private var placeholder: PlaceholderHook? = null

    override fun onEnable() {
        saveDefaultConfig()
        mergeMissingConfigKeys()
        configs = Configs(this).also { it.loadAll() }
        Lang.init(this)
        economy = EconomyManager(this)
        storage = createStorage().also { it.start() }
        CustomItems.refresh()
        market = MarketManager(this, storage).also { it.loadDefinitions() }
        display = FloatingDisplayManager(this)
        AdminItems.init(this)
        AdminRouter.init(this)
        ChatInput.init(this)
        dev.ensisdev.ensnightmarket.util.PaperChatBridge.init(this)
        schedule = ScheduleManager(this).also { it.start() }
        protection = ProtectionHook(this).also { it.refresh() }

        server.pluginManager.registerEvents(MarketListener(this), this)
        server.pluginManager.registerEvents(ChatInput, this)
        getCommand("nightmarket")?.let { cmd ->
            val executor = EnmCommand(this)
            cmd.setExecutor(executor); cmd.tabCompleter = executor
        }
        dev.ensisdev.ensnightmarket.command.CommandRegistry.init(this)

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) placeholder = PlaceholderHook(this).also { it.register() }
        updateChecker = dev.ensisdev.ensnightmarket.util.UpdateChecker(this).also { it.start() }
        if (!economy.available()) logger.warning("No usable economy provider found. Configure Vault or PlayerPoints before purchases.")
        logger.info("EnsNightMarket ${description.version} enabled - ${market.offerDefinitions.size} offers - ${market.rarityDefinitions.size} rarities - ${economy.providerName()}")
        logger.info("Advanced animation system enabled with LOD support.")
    }

    /** Public so /nightmarket admin reload can pick up new keys without a restart. */
    fun mergeMissingConfigKeys() {
        val defaults = org.bukkit.configuration.file.YamlConfiguration()
        runCatching {
            getResource("config.yml")?.use { stream ->
                defaults.load(java.io.InputStreamReader(stream, Charsets.UTF_8))
            }
        }
        if (defaults.getKeys(false).isEmpty()) return
        var changed = false
        defaults.getKeys(true).forEach { key ->
            if (!defaults.isConfigurationSection(key) && !config.contains(key)) {
                config.set(key, defaults.get(key))
                changed = true
            }
        }
        if (changed) {
            saveConfig()
            logger.info("config.yml updated with missing default keys.")
        }
    }

    fun reloadStorage() {
        if (::storage.isInitialized) storage.close()
        storage = createStorage().also { it.start() }
        market.reloadStorage(storage)
    }

    private fun createStorage(): Storage {
        val baseStorage = when ((config.getString("storage.type", "SQLITE") ?: "SQLITE").uppercase()) {
            "MYSQL", "MARIADB" -> MySqlStorage(this)
            else -> SqliteStorage(this)
        }
        return if (config.getBoolean("performance.async-storage", true)) {
            AsyncStorage(baseStorage, this)
        } else baseStorage
    }


    override fun onDisable() {
        if (::schedule.isInitialized) schedule.stop()
        if (::display.isInitialized) display.removeAll()
        placeholder?.unregister()
        if (::storage.isInitialized) storage.close()
    }
}
