package dev.ensisdev.ensnightmarket.admin

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.util.Msgs
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ChatInput : Listener {
    enum class Type { STRING, INT, DOUBLE, LONG }

    private data class Session(
        val player: UUID,
        val type: Type,
        val loreMode: Boolean,
        val loreLines: MutableList<String>,
        val loreInitial: List<String>,
        val callback: (String) -> Unit,
        val loreCallback: ((List<String>) -> Unit)?,
        val onCancel: () -> Unit,
        val expiresAt: Long
    )

    private lateinit var plugin: EnsNightMarket
    private val sessions = ConcurrentHashMap<UUID, Session>()

    fun init(plugin: EnsNightMarket) {
        this.plugin = plugin
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { sweep() }, 100L, 100L)
    }

    fun prompt(
        player: Player,
        type: Type,
        onValue: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        player.closeInventory()
        sessions[player.uniqueId] = Session(
            player.uniqueId, type, false, mutableListOf(), emptyList(),
            onValue, null, onCancel,
            System.currentTimeMillis() + 60_000L
        )
        Msgs.send(plugin, player, "admin-prompt")
    }

    fun promptLore(
        player: Player,
        initial: List<String>,
        onDone: (List<String>) -> Unit,
        onCancel: () -> Unit
    ) {
        player.closeInventory()
        sessions[player.uniqueId] = Session(
            player.uniqueId, Type.STRING, true, mutableListOf(), initial,
            {}, onDone, onCancel,
            System.currentTimeMillis() + 180_000L
        )
        Msgs.send(plugin, player, "admin-lore-hint")
    }

    fun cancel(player: UUID) {
        sessions.remove(player)
    }

    private fun sweep() {
        val now = System.currentTimeMillis()
        sessions.entries.toList().forEach { (uuid, session) ->
            if (now >= session.expiresAt) {
                sessions.remove(uuid)
                plugin.server.getPlayer(uuid)?.let {
                    Msgs.send(plugin, it, "admin-timeout")
                    session.onCancel()
                }
            }
        }
    }

    // Bukkit evrensel chat eventi: Spigot + Paper + tüm forklar.
    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(e: AsyncPlayerChatEvent) {
        val session = sessions[e.player.uniqueId] ?: return
        e.isCancelled = true
        try { e.recipients.clear() } catch (_: Throwable) {}
        val text = e.message.trim()
        plugin.server.scheduler.runTask(plugin, Runnable { handle(session, text) })
    }

    /** Paper AsyncChatEvent varsa reflection ile de yakala (chat(handle) iptal edilsin). */
    fun handlePaperChat(playerId: java.util.UUID, text: String): Boolean {
        val session = sessions[playerId] ?: return false
        plugin.server.scheduler.runTask(plugin, Runnable { handle(session, text.trim()) })
        return true
    }

    private fun handle(session: Session, text: String) {
        val player = plugin.server.getPlayer(session.player) ?: run {
            sessions.remove(session.player)
            return
        }
        if (matches("words-cancel", text)) {
            sessions.remove(session.player)
            Msgs.send(plugin, player, "admin-cancelled")
            session.onCancel()
            return
        }
        if (session.loreMode) {
            if (matches("words-done", text)) {
                sessions.remove(session.player)
                val result = if (session.loreLines.isEmpty()) session.loreInitial else session.loreLines.toList()
                session.loreCallback?.invoke(result)
                return
            }
            if (matches("words-clear", text)) {
                session.loreLines.clear()
                Msgs.send(plugin, player, "admin-lore-cleared")
                return
            }
            session.loreLines.add(text)
            return
        }
        val ok = when (session.type) {
            Type.STRING -> true
            Type.INT -> text.toIntOrNull() != null
            Type.DOUBLE -> text.replace(",", ".").toDoubleOrNull() != null
            Type.LONG -> text.toLongOrNull() != null
        }
        if (!ok) {
            Msgs.send(plugin, player, "admin-invalid")
            return
        }
        sessions.remove(session.player)
        session.callback(text)
    }

    private fun matches(key: String, text: String): Boolean {
        return dev.ensisdev.ensnightmarket.lang.Lang.words(key).any { it.equals(text, true) }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        sessions.remove(e.player.uniqueId)
    }
}
