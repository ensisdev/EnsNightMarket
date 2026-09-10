package dev.ensisdev.ensnightmarket.command

import dev.ensisdev.ensnightmarket.EnsNightMarket
import dev.ensisdev.ensnightmarket.lang.Lang
import dev.ensisdev.ensnightmarket.util.Msgs
import dev.ensisdev.ensnightmarket.util.Text
import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.entity.Player

class EnmCommand(private val plugin: EnsNightMarket) : CommandExecutor, TabCompleter {
    private val animCommand = AnimationCommand(plugin)
    private val cooldowns = RefreshCooldowns(plugin)

    init { cooldowns.load() }

    private fun message(sender: CommandSender, key: String, replacements: Map<String, String> = emptyMap()): Boolean {
        Msgs.send(plugin, sender, key, replacements)
        return true
    }

    private fun needPlayer(sender: CommandSender): Player? {
        val p = sender as? Player
        if (p == null) help(sender)
        return p
    }

    private fun openShowcase(p: Player) {
        if (!plugin.schedule.isOpen(p.world.name)) {
            message(p, "schedule-closed"); return
        }
        if (!plugin.market.canGenerate()) {
            plugin.logger.warning("Market cannot open for ${p.name}: no enabled offers or no rarities configured.")
            message(p, "no-offers"); return
        }
        runCatching { plugin.display.showcase(p) }.onFailure {
            plugin.logger.warning("Showcase failed for ${p.name}: ${it.message}")
            message(p, "no-offers"); return
        }
        message(p, "showcase")
    }

    private fun paidRefresh(p: Player) {
        if (!plugin.config.getBoolean("refresh.enabled", true)) {
            help(p); return
        }
        if (!plugin.schedule.isOpen(p.world.name)) {
            message(p, "schedule-closed"); return
        }
        val cooldownMs = plugin.config.getLong("refresh.cooldown-minutes", 30).coerceAtLeast(0) * 60_000L
        cooldowns.sweep(cooldownMs)
        val last = cooldowns.last(p.uniqueId)
        val remaining = last + cooldownMs - System.currentTimeMillis()
        if (remaining > 0) {
            message(p, "refresh-cooldown", mapOf("%time%" to shortDuration(remaining))); return
        }
        if (!plugin.market.canGenerate()) {
            message(p, "no-offers"); return
        }
        val price = plugin.config.getDouble("refresh.price", 500.0).coerceAtLeast(0.0)
        if (price > 0 && !plugin.economy.withdraw(p, price)) {
            message(p, "insufficient"); return
        }
        cooldowns.mark(p.uniqueId)
        val ok = runCatching { plugin.market.refresh(p.uniqueId) }.onFailure {
            plugin.logger.warning("Paid refresh failed for ${p.name}, refunding: ${it.message}")
        }.isSuccess
        if (!ok) {
            cooldowns.clear(p.uniqueId)
            if (price > 0) plugin.economy.deposit(p.uniqueId, price)
            message(p, "no-offers"); return
        }
        plugin.display.removeFor(p.uniqueId)
        plugin.display.showcase(p)
        message(p, "refresh-paid", mapOf("%price%" to plugin.economy.format(price)))
    }

    private fun shortDuration(ms: Long): String {
        val total = ms.coerceAtLeast(0) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        val u = Lang
        val hs = u.get("unit-hour"); val ms_ = u.get("unit-min"); val ss = u.get("unit-sec")
        return if (h > 0) "$h$hs $m$ms_" else if (m > 0) "$m$ms_ $s$ss" else "$s$ss"
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val resolved = CommandRegistry.resolve(args.toList()) ?: run { help(sender); return true }
        val perm = resolved.permission()
        if (perm.isNotEmpty() && !sender.hasPermission(perm)) return message(sender, "no-permission")
        when (resolved.action()) {
            "showcase" -> {
                val p = needPlayer(sender) ?: return true
                openShowcase(p)
            }
            "close" -> {
                val p = needPlayer(sender) ?: return true
                plugin.display.removeFor(p.uniqueId, animated = true)
                message(p, "showcase-closed")
            }
            "refresh-paid" -> {
                val p = needPlayer(sender) ?: return true
                paidRefresh(p)
            }
            "info" -> {
                message(sender, "info-line", mapOf(
                    "%version%" to plugin.description.version,
                    "%economy%" to plugin.economy.providerName(),
                    "%offers%" to "${plugin.market.offerDefinitions.size}"
                ))
                return true
            }
            "help" -> help(sender)
            "admin-menu" -> adminMenu(sender, resolved.remaining)
            "admin-refresh" -> adminRefresh(sender, resolved.remaining)
            "admin-reset" -> adminReset(sender, resolved.remaining)
            "admin-reload" -> adminReload(sender)
            "admin-anim" -> animCommand.onCommand(sender, command, label, resolved.remaining.toTypedArray())
            "admin-lang" -> adminLang(sender, resolved.remaining)
            else -> help(sender)
        }
        return true
    }

    private fun adminMenu(sender: CommandSender, remaining: List<String>) {
        val token = remaining.firstOrNull()
        if (token == null) {
            val p = needPlayer(sender) ?: return
            dev.ensisdev.ensnightmarket.admin.MainMenu.open(plugin, p)
            return
        }
        val target = Bukkit.getPlayerExact(token) ?: run { message(sender, "not-found"); return }
        if (sender is Player) {
            dev.ensisdev.ensnightmarket.admin.PlayerMarketMenu.open(plugin, sender, target.uniqueId)
        } else {
            plugin.market.refresh(target.uniqueId)
            plugin.display.removeFor(target.uniqueId)
            message(sender, "reload")
        }
    }

    private fun adminRefresh(sender: CommandSender, remaining: List<String>) {
        val target = remaining.firstOrNull()?.let { Bukkit.getPlayerExact(it) } ?: (sender as? Player)
        if (target == null) { message(sender, "not-found"); return }
        val ok = runCatching { plugin.market.refresh(target.uniqueId) }.onFailure {
            plugin.logger.warning("Admin refresh failed for ${target.name}: ${it.message}")
        }.isSuccess
        if (!ok) { message(sender, "no-offers"); return }
        plugin.display.removeFor(target.uniqueId)
        message(sender, "reload")
    }

    private fun adminReset(sender: CommandSender, remaining: List<String>) {
        val target = remaining.firstOrNull()?.let { Bukkit.getOfflinePlayer(it) }
            ?: (sender as? Player)?.let { Bukkit.getOfflinePlayer(it.uniqueId) }
        if (target == null) { message(sender, "not-found"); return }
        plugin.market.reset(target.uniqueId); plugin.display.removeFor(target.uniqueId); message(sender, "reload")
    }

    private fun adminReload(sender: CommandSender) {
        plugin.mergeMissingConfigKeys()
        plugin.configs.reload()
        Lang.reload()
        CommandRegistry.reload()
        plugin.economy.refresh()
        dev.ensisdev.ensnightmarket.items.CustomItems.refresh()
        plugin.market.loadDefinitions()
        plugin.schedule.reload()
        plugin.protection.refresh()
        plugin.reloadStorage()
        // Stale-state cleanup: open showcases keep old prices/names/heads otherwise.
        plugin.display.removeAll()
        dev.ensisdev.ensnightmarket.texture.HeadFactory.clearCache()
        dev.ensisdev.ensnightmarket.performance.PerformanceMonitor.reset()
        message(sender, "reload")
    }

    private fun adminLang(sender: CommandSender, remaining: List<String>) {
        val code = remaining.firstOrNull()
        if (code == null) {
            message(sender, "lang-current", mapOf(
                "%lang%" to Lang.current(),
                "%langs%" to Lang.available().joinToString(", ")
            ))
            return
        }
        val applied = Lang.setLanguage(code)
        if (applied == null) {
            message(sender, "lang-invalid", mapOf(
                "%langs%" to Lang.available().joinToString(", ")
            ))
            return
        }
        message(sender, "lang-set", mapOf("%lang%" to applied))
    }

    private fun help(sender: CommandSender) {
        val label = "nightmarket"
        val playerCmds = mutableListOf<String>()
        val adminCmds = mutableListOf<String>()
        CommandRegistry.roots().forEach { node ->
            if (node.permission.isNotEmpty() && !sender.hasPermission(node.permission)) return@forEach
            if (node.description.isNotEmpty()) {
                playerCmds.add("<white>/$label ${node.name} <dark_gray>· <gray>${node.description}")
            }
            node.subcommands.forEach { sub ->
                if (sub.permission.isNotEmpty() && !sender.hasPermission(sub.permission)) return@forEach
                if (sub.description.isNotEmpty()) {
                    adminCmds.add("<white>/$label ${node.name} ${sub.name} <dark_gray>· <gray>${sub.description}")
                }
            }
        }
        val C = dev.ensisdev.ensnightmarket.util.Compat
        val T = dev.ensisdev.ensnightmarket.util.Text
        val L = dev.ensisdev.ensnightmarket.lang.Lang
        C.send(sender, T.component(L.get("help-player-title")), T.legacy(L.get("help-player-title")))
        playerCmds.forEach { C.send(sender, T.component(it), T.legacy(it)) }
        if (adminCmds.isNotEmpty()) {
            C.send(sender, T.component(L.get("help-admin-title")), T.legacy(L.get("help-admin-title")))
            adminCmds.forEach { C.send(sender, T.component(it), T.legacy(it)) }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return CommandRegistry.roots()
                .filter { it.permission.isEmpty() || sender.hasPermission(it.permission) }
                .flatMap { listOf(it.name) + it.aliases }
                .filter { it.startsWith(args[0], true) }
        }
        val root = CommandRegistry.roots().firstOrNull {
            (it.permission.isEmpty() || sender.hasPermission(it.permission)) && it.matches(args[0])
        } ?: return emptyList()
        if (args.size == 2) {
            val subs = root.subcommands
                .filter { it.permission.isEmpty() || sender.hasPermission(it.permission) }
                .flatMap { listOf(it.name) + it.aliases }
            val players = when (root.action) {
                "admin-menu", "admin-refresh", "admin-reset" ->
                    Bukkit.getOnlinePlayers().map(Player::getName)
                else -> emptyList()
            }
            return (subs + players).filter { it.startsWith(args[1], true) }
        }
        if ((args.size == 3 || args.size == 4) && root.action == "admin-menu") {
            val sub = root.matchSub(args[1]) ?: return emptyList()
            if (sub.permission.isNotEmpty() && !sender.hasPermission(sub.permission)) return emptyList()
            when (sub.action) {
                "admin-lang" -> {
                    if (args.size == 3) return Lang.available().filter { it.startsWith(args[2], true) }
                }
                "admin-anim" -> {
                    if (args.size == 3) return listOf("preview", "stats", "quality", "patterns", "shapes", "reload", "help").filter { it.startsWith(args[2], true) }
                    if (args.size == 4) return when (args[2].lowercase()) {
                        "preview" -> plugin.market.rarityDefinitions.keys.filter { it.startsWith(args[3], true) }
                        "quality" -> listOf("LOW", "MEDIUM", "HIGH", "ULTRA").filter { it.startsWith(args[3], true) }
                        else -> emptyList()
                    }
                }
            }
            return emptyList()
        }
        return emptyList()
    }
}
