package dev.ensisdev.ensnightmarket.config

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class Configs(private val plugin: EnsNightMarket) {
    lateinit var rarities: FileConfiguration; private set
    lateinit var offers: FileConfiguration; private set

    fun loadAll() {
        plugin.saveDefaultConfig()
        listOf("rarities.yml", "offers.yml").forEach(::saveIfMissing)
        reload()
    }

    fun reload() {
        plugin.reloadConfig()
        rarities = YamlConfiguration.loadConfiguration(file("rarities.yml"))
        offers = YamlConfiguration.loadConfiguration(file("offers.yml"))
    }

    private fun file(name: String) = File(plugin.dataFolder, name)
    private fun saveIfMissing(name: String) { if (!file(name).exists()) plugin.saveResource(name, false) }
}
