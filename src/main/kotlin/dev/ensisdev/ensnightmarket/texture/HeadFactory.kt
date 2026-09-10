package dev.ensisdev.ensnightmarket.texture

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URI
import java.net.URL
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Paper-only siniflara compile/runtime bagimliligi olmadan kafa uretir.
 * Sira: 1) com.destroystokyo.paper.profile (Paper 1.16-1.21+)
 *       2) org.bukkit.profile.PlayerProfile (Bukkit 1.18+, Spigot dahil)
 *       3) GameProfile reflection fallback
 */
object HeadFactory {

    private const val TEXTURES_URL = "https://textures.minecraft.net/texture/"

    private val cache = ConcurrentHashMap<String, ItemStack>()

    fun create(type: String, value: String): ItemStack {
        val key = "${type.trim().uppercase()}|$value"
        return cache.getOrPut(key) { build(type, value) }.clone()
    }

    fun clearCache() = cache.clear()

    private fun build(type: String, value: String): ItemStack {
        val stack = ItemStack(Material.PLAYER_HEAD)
        val meta = stack.itemMeta as? SkullMeta ?: return stack
        val raw = value.trim()
        if (raw.isEmpty()) return stack
        when (type.trim().uppercase()) {
            "HDB", "HEADSDB", "HEADSDATABASE" -> {
                return dev.ensisdev.ensnightmarket.items.CustomItems.hdbHead(raw) ?: stack
            }
            "RAW", "VALUE", "TEXTURE_ID", "URL", "TEXTURE_URL" -> {
                if (applyTexture(meta, normalizeUrl(raw))) stack.itemMeta = meta
                return stack
            }
            else -> {
                if (applyTexture(meta, raw)) { stack.itemMeta = meta; return stack }
                if (applyTexture(meta, normalizeUrl(raw))) stack.itemMeta = meta
                return stack
            }
        }
    }

    private fun applyTexture(meta: SkullMeta, raw: String): Boolean {
        if (raw.isEmpty()) return false
        val base64 = if (isBase64Texture(raw)) raw else urlToBase64(raw) ?: return false
        if (!isBase64Texture(base64)) return false
        if (applyViaPaperProfile(meta, base64)) return true
        if (applyViaBukkitProfile(meta, base64)) return true
        if (applyViaGameProfile(meta, base64)) return true
        return false
    }

    private fun isBase64Texture(s: String): Boolean {
        if (!s.startsWith("eyJ0ZXh0dXJlcyI6")) return false
        val decoded = runCatching { String(Base64.getDecoder().decode(s)) }.getOrNull() ?: return false
        return decoded.contains("\"textures\"")
    }

    private fun urlToBase64(raw: String): String? {
        val url = normalizeUrl(raw)
        if (!url.startsWith("http")) return null
        runCatching { URI(url).toURL() }.getOrNull() ?: return null
        val json = "{\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}"
        return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    private fun normalizeUrl(raw: String): String {
        val cleaned = raw.removePrefix("http://textures.minecraft.net/texture/")
            .removePrefix("https://textures.minecraft.net/texture/")
        return if (cleaned.startsWith("http")) cleaned else "$TEXTURES_URL$cleaned"
    }

    private fun skullUuid(base64: String) = UUID.nameUUIDFromBytes(base64.toByteArray())

    private fun applyViaPaperProfile(meta: SkullMeta, base64: String): Boolean = runCatching {
        val profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile")
        val propClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty")
        val bukkitClass = Class.forName("org.bukkit.Bukkit")
        val create = bukkitClass.getMethod("createProfile", UUID::class.java, String::class.java)
        val profile = create.invoke(null, skullUuid(base64), "ensnm_head")
        val property = propClass.getConstructor(String::class.java, String::class.java)
            .newInstance("textures", base64)
        profileClass.getMethod("setProperty", propClass).invoke(profile, property)
        meta.javaClass.getMethod("setPlayerProfile", profileClass).invoke(meta, profile)
        true
    }.getOrDefault(false)

    private fun applyViaBukkitProfile(meta: SkullMeta, base64: String): Boolean = runCatching {
        val hash = extractHash(base64) ?: return false
        val bukkitClass = Class.forName("org.bukkit.Bukkit")
        val create = bukkitClass.getMethod("createPlayerProfile", UUID::class.java, String::class.java)
        val profile = create.invoke(null, skullUuid(base64), "ensnm_head")
        val textures = profile.javaClass.getMethod("getTextures").invoke(profile)
        textures.javaClass.getMethod("setSkin", URL::class.java)
            .invoke(textures, URL("$TEXTURES_URL$hash"))
        val setter = meta.javaClass.methods.firstOrNull {
            it.name == "setPlayerProfile" && it.parameterTypes.size == 1
        } ?: return false
        setter.invoke(meta, profile)
        true
    }.getOrDefault(false)

    private fun applyViaGameProfile(meta: SkullMeta, base64: String): Boolean = runCatching {
        val gpClass = Class.forName("com.mojang.authlib.GameProfile")
        val propClass = Class.forName("com.mojang.authlib.properties.Property")
        val gp = gpClass.getConstructor(UUID::class.java, String::class.java)
            .newInstance(skullUuid(base64), "ensnm_head")
        val props = gpClass.getMethod("getProperties").invoke(gp)
        val put = props.javaClass.getMethod("put", Any::class.java, Any::class.java)
        val prop = propClass.getConstructor(String::class.java, String::class.java)
            .newInstance("textures", base64)
        put.invoke(props, "textures", prop)
        val setProfile = meta.javaClass.methods.firstOrNull {
            it.name == "setProfile" && it.parameterTypes.size == 1 &&
                it.parameterTypes[0].name == "com.mojang.authlib.GameProfile"
        } ?: return false
        setProfile.invoke(meta, gp)
        true
    }.getOrDefault(false)

    private fun extractHash(base64: String): String? = runCatching {
        val decoded = String(Base64.getDecoder().decode(base64))
        val marker = "textures.minecraft.net/texture/"
        val i = decoded.indexOf(marker)
        if (i < 0) return null
        val start = i + marker.length
        val end = decoded.indexOf('"', start).takeIf { it > start } ?: decoded.length
        decoded.substring(start, end).trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}
