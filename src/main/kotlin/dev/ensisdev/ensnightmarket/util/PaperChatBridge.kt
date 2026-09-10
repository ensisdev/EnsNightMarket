package dev.ensisdev.ensnightmarket.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * Paper AsyncChatEvent köprüsü: sýnýf yoksa kayýt yapýlmaz, hata vermez.
 * Event'i reflection ile dinler, ChatInput oturumu varsa mesajý yutar.
 */
object PaperChatBridge : Listener {
    private var active = false
    private var eventClass: Class<*>? = null

    fun init(plugin: org.bukkit.plugin.Plugin) {
        if (active) return
        val clazz = runCatching { Class.forName("io.papermc.paper.event.player.AsyncChatEvent") }.getOrNull()
            ?: return
        eventClass = clazz
        runCatching {
            Bukkit.getPluginManager().registerEvents(this, plugin)
            active = true
        }
    }

    @Suppress("unused")
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPaperChat(e: org.bukkit.event.Event) {
        val clazz = eventClass ?: return
        if (!clazz.isInstance(e)) return
        runCatching {
            val player = clazz.getMethod("getPlayer").invoke(e) as? Player ?: return
            val comp = clazz.getMethod("message").invoke(e) ?: return
            val text = comp.javaClass.let { c ->
                runCatching {
                    val plain = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer")
                    val inst = plain.getMethod("plainText").invoke(null)
                    inst.javaClass.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"))
                        .invoke(inst, comp) as? String
                }.getOrNull() ?: comp.toString()
            }
            val consumed = dev.ensisdev.ensnightmarket.admin.ChatInput.handlePaperChat(player.uniqueId, text)
            if (consumed) {
                clazz.getMethod("setCancelled", Boolean::class.javaPrimitiveType).invoke(e, true)
            }
        }
    }
}
