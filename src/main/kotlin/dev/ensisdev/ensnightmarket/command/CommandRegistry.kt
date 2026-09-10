package dev.ensisdev.ensnightmarket.command

import dev.ensisdev.ensnightmarket.EnsNightMarket
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class CommandNode(
    val name: String,
    val aliases: List<String>,
    val permission: String,
    val action: String,
    val description: String,
    val subcommands: List<CommandNode>
) {
    fun matches(token: String): Boolean {
        return name.equals(token, true) || aliases.any { it.equals(token, true) }
    }

    fun matchSub(token: String): CommandNode? {
        return subcommands.firstOrNull { it.matches(token) }
    }
}

data class ResolvedCommand(
    val node: CommandNode,
    val sub: CommandNode?,
    val remaining: List<String>
) {
    fun action(): String = sub?.action ?: node.action
    fun permission(): String = if (sub != null && sub.permission.isNotEmpty()) sub.permission else node.permission
    fun description(): String = sub?.description ?: node.description
}

object CommandRegistry {
    private lateinit var plugin: EnsNightMarket
    private var nodes: List<CommandNode> = emptyList()
    private var defaultAction: String = "showcase"

    fun init(plugin: EnsNightMarket) {
        this.plugin = plugin
        reload()
    }

    fun reload() {
        val file = File(plugin.dataFolder, "commands.yml")
        if (!file.exists()) {
            runCatching { plugin.saveResource("commands.yml", false) }
        }
        val yml = YamlConfiguration.loadConfiguration(file)
        defaultAction = yml.getString("default-action", "showcase") ?: "showcase"
        nodes = yml.getConfigurationSection("commands")
            ?.getKeys(false)
            ?.mapNotNull { readNode(yml, "commands.$it", it) }
            ?: emptyList()
    }

    private fun readNode(yml: YamlConfiguration, path: String, name: String): CommandNode? {
        val action = yml.getString("$path.action", "") ?: ""
        if (action.isEmpty()) return null
        val subs = yml.getConfigurationSection("$path.subcommands")
            ?.getKeys(false)
            ?.mapNotNull { readNode(yml, "$path.subcommands.$it", it) }
            ?: emptyList()
        return CommandNode(
            name = name,
            aliases = yml.getStringList("$path.aliases"),
            permission = yml.getString("$path.permission", "") ?: "",
            action = action,
            description = yml.getString("$path.description", "") ?: "",
            subcommands = subs
        )
    }

    fun roots(): List<CommandNode> = nodes

    fun defaultNode(): CommandNode? {
        return nodes.firstOrNull { it.action == defaultAction } ?: nodes.firstOrNull()
    }

    fun resolve(args: List<String>): ResolvedCommand? {
        if (args.isEmpty()) {
            val node = defaultNode() ?: return null
            return ResolvedCommand(node, null, emptyList())
        }
        val node = nodes.firstOrNull { it.matches(args[0]) } ?: return null
        if (args.size >= 2) {
            val sub = node.matchSub(args[1])
            if (sub != null) return ResolvedCommand(node, sub, args.drop(2))
        }
        return ResolvedCommand(node, null, args.drop(1))
    }
}
