package dev.ensisdev.ensnightmarket.command

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.animation.MotionPattern
import dev.ensisdev.ensnightmarket.animation.ParticleShape
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.performance.PerformanceMonitor
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AnimationCommand(private val plugin: EnsNightMarket) : CommandExecutor, TabCompleter {

    private fun msg(p: Player, key: String, replacements: Map<String, String> = emptyMap()) =
        Msgs.send(plugin, p, key, replacements)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            Msgs.send(plugin, sender, "anim-players-only")
            return true
        }

        if (!sender.hasPermission("ensnightmarket.animation") && !sender.hasPermission("ensnightmarket.admin")) {
            Msgs.send(plugin, sender, "no-permission")
            return true
        }

        val sub = args.firstOrNull()?.lowercase() ?: "help"
        when (sub) {
            "preview" -> previewAnimation(sender, args)
            "stats" -> showStats(sender)
            "quality" -> setQuality(sender, args)
            "patterns" -> listPatterns(sender)
            "shapes" -> listShapes(sender)
            "reload" -> reloadAnimations(sender)
            "help" -> showHelp(sender)
            else -> showHelp(sender)
        }
        return true
    }

    private fun previewAnimation(player: Player, args: Array<out String>) {
        val rarityName = args.getOrNull(1) ?: "legendary"
        val market = plugin.market.getOrCreate(player.uniqueId)
        val offer = market.offers.firstOrNull { it.rarity.id.equals(rarityName, true) }
            ?: market.offers.firstOrNull()

        if (offer == null) {
            Msgs.send(plugin, player, "not-found")
            return
        }

        val previewOffer = offer.copy()
        previewOffer.revealed = true

        plugin.display.previewReveal(player.location, previewOffer)

        msg(player, "anim-preview", mapOf("%rarity%" to rarityName))
    }

    private fun showStats(player: Player) {
        val stats = PerformanceMonitor.getStats()
        msg(player, "anim-stats-title")
        say(player, Lang.get("anim-avg", mapOf(
            "%ms%" to "%.2f".format(stats.averageAnimationTime),
            "%follow%" to "%.2f".format(stats.followMs)
        )))
        say(player, Lang.get("anim-particles", mapOf(
            "%total%" to "${stats.totalParticles}",
            "%frames%" to "${stats.frameCount}"
        )))

        if (stats.particleCounts.isNotEmpty()) {
            say(player, Lang.get("anim-dist"))
            stats.particleCounts.forEach { (key, count) ->
                say(player, Lang.get("anim-dist-row", mapOf("%key%" to key, "%count%" to "$count")))
            }
        }
    }

    private fun setQuality(player: Player, args: Array<out String>) {
        val quality = args.getOrNull(1)?.uppercase() ?: "HIGH"
        plugin.config.set("performance.animation-quality", quality)
        plugin.saveConfig()
        msg(player, "anim-quality-set", mapOf("%quality%" to quality))
    }

    private fun listPatterns(player: Player) {
        msg(player, "anim-patterns", mapOf("%count%" to "${MotionPattern.values().size}"))
        MotionPattern.values().forEach { pattern ->
            say(player, Lang.get("anim-pattern-row", mapOf("%name%" to pattern.name)))
        }
    }

    private fun listShapes(player: Player) {
        msg(player, "anim-shapes", mapOf("%count%" to "${ParticleShape.values().size}"))
        ParticleShape.values().forEach { shape ->
            say(player, Lang.get("anim-shape-row", mapOf("%name%" to shape.name)))
        }
    }

    private fun reloadAnimations(player: Player) {
        plugin.display.removeAll()
        PerformanceMonitor.reset()
        msg(player, "anim-reloaded")
    }

    private fun showHelp(player: Player) {
        msg(player, "anim-help-title")
        listOf("anim-h-preview", "anim-h-stats", "anim-h-quality", "anim-h-patterns", "anim-h-shapes", "anim-h-reload").forEach { key ->
            say(player, Lang.get(key))
        }
    }

    private fun say(player: Player, miniMessage: String) =
        dev.ensisdev.ensnightmarket.util.Compat.send(player, Text.component(miniMessage), Text.legacy(miniMessage))

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            2 -> when (args[0].lowercase()) {
                "preview" -> listOf("common", "uncommon", "rare", "legendary", "mysterious").filter { it.startsWith(args[1], true) }
                "quality" -> listOf("LOW", "MEDIUM", "HIGH", "ULTRA").filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
