package dev.ensisdev.ensnightmarket.items

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

object CustomItems {
    private val availability = ConcurrentHashMap<String, Boolean>()

    fun refresh() {
        availability["oraxen"] = present("Oraxen", "io.th0rgal.oraxen.api.OraxenItems")
        availability["itemsadder"] = present("ItemsAdder", "dev.lone.itemsadder.api.CustomStack")
        availability["nexo"] = present("Nexo", "com.nexomc.nexo.api.NexoItems")
        availability["mmoitems"] = present("MMOItems", "net.Indyuce.mmoitems.MMOItems")
        availability["headsdb"] = present("HeadDatabase", "me.arcaniax.hdb.api.HeadDatabaseAPI")
    }

    fun isAvailable(key: String): Boolean = availability.getOrDefault(key.lowercase(), false)

    private fun present(pluginName: String, apiClass: String): Boolean {
        if (Bukkit.getPluginManager().getPlugin(pluginName) == null) return false
        return runCatching { Class.forName(apiClass) }.isSuccess
    }

    fun resolve(spec: String, amount: Int = 1): ItemStack? {
        val clean = spec.trim()
        if (clean.isEmpty()) return null
        val count = amount.coerceIn(1, 64)
        val stack = if (':' in clean) resolveNamespaced(clean) else vanilla(clean)
        if (stack == null || stack.type.isAir) return null
        stack.amount = count.coerceAtMost(stack.maxStackSize.coerceAtLeast(1))
        return stack
    }

    fun knownPrefix(spec: String): Boolean {
        val prefix = spec.substringBefore(':', "").lowercase()
        return prefix in setOf("oraxen", "itemsadder", "ia", "nexo", "mmoitems", "hdb", "headsdb", "headdatabase")
    }

    private fun vanilla(spec: String): ItemStack? {
        val material = Material.matchMaterial(spec) ?: return null
        return ItemStack(material)
    }

    private fun resolveNamespaced(spec: String): ItemStack? {
        val prefix = spec.substringBefore(':').lowercase()
        val rest = spec.substringAfter(':')
        return when (prefix) {
            "oraxen" -> oraxenItem(rest)
            "itemsadder", "ia" -> customStackItem(rest)
            "nexo" -> nexoItem(rest)
            "mmoitems" -> {
                val type = rest.substringBefore(':', "")
                val id = rest.substringAfter(':', "")
                if (type.isEmpty() || id.isEmpty()) null else mmoItem(type, id)
            }
            "hdb", "headsdb", "headdatabase" -> hdbHead(rest)
            else -> vanilla(spec)
        }
    }

    private fun oraxenItem(id: String): ItemStack? {
        if (!isAvailable("oraxen")) return null
        return runCatching {
            val api = Class.forName("io.th0rgal.oraxen.api.OraxenItems")
            val builder = api.getMethod("getItemById", String::class.java).invoke(null, id) ?: return null
            builder.javaClass.getMethod("build").invoke(builder) as? ItemStack
        }.getOrNull()
    }

    private fun customStackItem(id: String): ItemStack? {
        if (!isAvailable("itemsadder")) return null
        return runCatching {
            val api = Class.forName("dev.lone.itemsadder.api.CustomStack")
            val stack = api.getMethod("getInstance", String::class.java).invoke(null, id) ?: return null
            stack.javaClass.getMethod("getItemStack").invoke(stack) as? ItemStack
        }.getOrNull()
    }

    private fun nexoItem(id: String): ItemStack? {
        if (!isAvailable("nexo")) return null
        return runCatching {
            val api = Class.forName("com.nexomc.nexo.api.NexoItems")
            val builder = api.getMethod("itemFromId", String::class.java).invoke(null, id) ?: return null
            builder.javaClass.getMethod("build").invoke(builder) as? ItemStack
        }.getOrNull()
    }

    private fun mmoItem(type: String, id: String): ItemStack? {
        if (!isAvailable("mmoitems")) return null
        return runCatching {
            val pluginClass = Class.forName("net.Indyuce.mmoitems.MMOItems")
            val plugin = pluginClass.getMethod("getPlugin").invoke(null) ?: return null
            val stringString = plugin.javaClass.methods.firstOrNull {
                it.name == "getItem" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == String::class.java && it.parameterTypes[1] == String::class.java
            }
            if (stringString != null) return stringString.invoke(plugin, type.uppercase(), id) as? ItemStack
            val typeClass = Class.forName("net.Indyuce.mmoitems.api.Type")
            val mmoType = typeClass.getMethod("get", String::class.java).invoke(null, type.uppercase()) ?: return null
            val typed = plugin.javaClass.methods.firstOrNull {
                it.name == "getItem" && it.parameterTypes.size == 2 && it.parameterTypes[1] == String::class.java
            } ?: return null
            typed.invoke(plugin, mmoType, id) as? ItemStack
        }.getOrNull()
    }

    fun hdbHead(id: String): ItemStack? {
        if (!isAvailable("headsdb")) return null
        return runCatching {
            val apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI")
            val api = apiClass.getConstructor().newInstance()
            apiClass.getMethod("getItemHead", String::class.java).invoke(api, id) as? ItemStack
        }.getOrNull()
    }
}
