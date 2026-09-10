package dev.ensisdev.ensnightmarket.lang

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Lang {
    private lateinit var plugin: EnsNightMarket
    private var data: FileConfiguration = YamlConfiguration()
    private var fallback: FileConfiguration = YamlConfiguration()
    private var code: String = "tr_tr"

    fun init(plugin: EnsNightMarket) {
        this.plugin = plugin
        reload()
    }

    fun reload() {
        ensure("tr_tr")
        ensure("en_en")
        code = plugin.config.getString("language", "tr_tr")!!.lowercase()
        data = load(code)
        fallback = load("tr_tr")
        reportMissing()
    }

    fun current(): String = code

    fun available(): List<String> = listOf("tr_tr", "en_en")

    fun setLanguage(raw: String): String? {
        val normalized = when (raw.lowercase()) {
            "tr", "tr_tr", "turkish", "türkçe" -> "tr_tr"
            "en", "en_en", "english", "ingilizce" -> "en_en"
            else -> return null
        }
        plugin.config.set("language", normalized)
        plugin.saveConfig()
        reload()
        return normalized
    }

    fun get(key: String, replacements: Map<String, String> = emptyMap()): String {
        var s = raw(key)
        replacements.forEach { (a, b) -> s = s.replace(a, b) }
        return s
    }

    fun list(key: String, replacements: Map<String, String> = emptyMap()): List<String> {
        return strings(key).map { line ->
            var s = line
            replacements.forEach { (a, b) -> s = s.replace(a, b) }
            s
        }
    }

    fun words(key: String): List<String> {
        val fromData = data.getStringList(key)
        if (fromData.isNotEmpty()) return fromData
        return fallback.getStringList(key)
    }

    private fun raw(key: String): String {
        if (data.isString(key)) return data.getString(key, "") ?: ""
        if (fallback.isString(key)) return fallback.getString(key, "") ?: ""
        return ""
    }

    private fun strings(key: String): List<String> {
        val fromData = data.getStringList(key)
        if (fromData.isNotEmpty()) return fromData
        return fallback.getStringList(key)
    }

    private fun dir(): File {
        val dir = File(plugin.dataFolder, "lang")
        dir.mkdirs()
        return dir
    }

    private fun ensure(name: String) {
        val target = File(dir(), "$name.yml")
        if (!target.exists()) {
            runCatching { plugin.saveResource("lang/$name.yml", false) }
        }
    }

    private fun load(name: String): FileConfiguration {
        return YamlConfiguration.loadConfiguration(File(dir(), "$name.yml"))
    }

    private fun reportMissing() {
        if (code == "tr_tr") return
        val missing = fallback.getKeys(true)
            .filter { !data.contains(it) && data.getStringList(it).isEmpty() }
            .filter { fallback.isString(it) || fallback.isList(it) }
        missing.forEach { plugin.logger.warning("Lang key missing in $code.yml, using tr_tr: $it") }
    }
}
