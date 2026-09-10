package dev.ensisdev.ensnightmarket.schedule

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.util.Msgs
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

class ScheduleManager(private val plugin: EnsNightMarket) {
    private val openWorlds = ConcurrentHashMap.newKeySet<String>()
    private var taskId = -1

    fun start() {
        stop()
        openWorlds.clear()
        Bukkit.getWorlds().forEach { if (isOpenAt(it.time)) openWorlds.add(it.name) }
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { poll() }, 100L, 100L).taskId
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
    }

    fun reload() {
        stop()
        start()
    }

    fun enabled(): Boolean = plugin.config.getBoolean("schedule.enabled", false)

    fun openTick(): Long = plugin.config.getLong("schedule.open-tick", 13000L)

    fun closeTick(): Long = plugin.config.getLong("schedule.close-tick", 23000L)

    fun isOpen(worldName: String): Boolean {
        if (!enabled()) return true
        val world = Bukkit.getWorld(worldName) ?: return true
        return isOpenAt(world.time)
    }

    private fun isOpenAt(time: Long): Boolean {
        val t = ((time % 24000) + 24000) % 24000
        val open = ((openTick() % 24000) + 24000) % 24000
        val close = ((closeTick() % 24000) + 24000) % 24000
        if (open == close) return true
        return if (open < close) t in open until close else t >= open || t < close
    }

    private fun poll() {
        if (!enabled()) return
        Bukkit.getWorlds().forEach { world ->
            val nowOpen = isOpenAt(world.time)
            val wasOpen = openWorlds.contains(world.name)
            if (nowOpen && !wasOpen) {
                openWorlds.add(world.name)
                Msgs.broadcast(plugin, "schedule-open", mapOf("%world%" to world.name))
            } else if (!nowOpen && wasOpen) {
                openWorlds.remove(world.name)
                Msgs.broadcast(plugin, "schedule-closed", mapOf("%world%" to world.name))
                world.players.forEach { plugin.display.removeFor(it.uniqueId, animated = true) }
            }
        }
    }
}
