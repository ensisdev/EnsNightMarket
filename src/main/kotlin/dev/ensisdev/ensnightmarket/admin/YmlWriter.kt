package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.market.OfferDef
import dev.ensisdev.ensnightmarket.market.RarityDef
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object YmlWriter {
    private fun file(plugin: EnsNightMarket, name: String): File {
        plugin.dataFolder.mkdirs()
        return File(plugin.dataFolder, name)
    }

    fun saveOffer(plugin: EnsNightMarket, def: OfferDef) {
        val f = file(plugin, "offers.yml")
        val yml = YamlConfiguration.loadConfiguration(f)
        val p = "offers.${def.id}."
        yml.set(p + "display-name", def.displayName)
        yml.set(p + "material", def.material)
        yml.set(p + "amount", def.amount)
        yml.set(p + "base-price", def.basePrice)
        yml.set(p + "discount-min", def.discountMin)
        yml.set(p + "discount-max", def.discountMax)
        yml.set(p + "stock-min", def.stockMin)
        yml.set(p + "stock-max", def.stockMax)
        yml.set(p + "rarity", def.rarity)
        yml.set(p + "permission", def.permission)
        yml.set(p + "max-purchases", def.maxPurchases)
        yml.set(p + "lore", def.lore)
        yml.set(p + "enabled", def.enabled)
        yml.save(f)
        afterDefinitions(plugin)
    }

    fun deleteOffer(plugin: EnsNightMarket, id: String) {
        val f = file(plugin, "offers.yml")
        val yml = YamlConfiguration.loadConfiguration(f)
        yml.set("offers.$id", null)
        yml.save(f)
        afterDefinitions(plugin)
    }

    fun saveRarity(plugin: EnsNightMarket, def: RarityDef) {
        val f = file(plugin, "rarities.yml")
        val yml = YamlConfiguration.loadConfiguration(f)
        val p = "rarities.${def.id}."
        yml.set(p + "display-name", def.displayName)
        yml.set(p + "weight", def.weight)
        yml.set(p + "color", def.color)
        yml.set(p + "head.type", def.headType)
        yml.set(p + "head.value", def.headValue)
        yml.set(p + "effects.particle", def.particle)
        yml.set(p + "effects.particle-count", def.particleCount)
        yml.set(p + "effects.sound", def.sound)
        yml.set(p + "effects.pitch", def.pitch.toDouble())
        yml.set(p + "floating.scale", def.floatingScale.toDouble())
        yml.set(p + "floating.aura", def.aura)
        yml.save(f)
        afterDefinitions(plugin)
    }

    fun deleteRarity(plugin: EnsNightMarket, id: String) {
        val f = file(plugin, "rarities.yml")
        val yml = YamlConfiguration.loadConfiguration(f)
        yml.set("rarities.$id", null)
        yml.save(f)
        afterDefinitions(plugin)
    }

    fun setConfig(plugin: EnsNightMarket, path: String, value: Any?) {
        plugin.config.set(path, value)
        plugin.saveConfig()
    }

    private fun afterDefinitions(plugin: EnsNightMarket) {
        plugin.configs.reload()
        plugin.market.loadDefinitions()
        plugin.market.dropCache()
    }
}
