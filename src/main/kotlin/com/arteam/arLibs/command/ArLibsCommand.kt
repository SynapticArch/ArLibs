/**
 * Main command for the ArLibs plugin.
 * This command provides information about ArLibs and its features.
 * 
 * ArLibs插件的主命令。
 * 此命令提供有关ArLibs及其功能的信息。
 *
 * @author ArteamTech
 * @since 2025-05-25
 * @version 1.0.0
 */
package com.arteam.arLibs.command

import com.arteam.arLibs.ArLibs
import com.arteam.arLibs.command.annotations.*
import com.arteam.arLibs.config.ConfigManager
import com.arteam.arLibs.config.CoreConfig
import com.arteam.arLibs.utils.Logger
import org.bukkit.Bukkit

/**
 * ArLibs main command providing system information and management features.
 * ArLibs主命令，提供系统信息和管理功能。
 */
@Suppress("unstableApiUsage", "unused")
@Command(
    name = "arlibs",
    description = "ArLibs main command - A powerful library for Bukkit plugins",
    usage = "/arlibs <subcommand>"
)
class ArLibsCommand : BaseCommand() {

    /**
     * Main command execution - shows ArLibs information.
     * 主命令执行 - 显示ArLibs信息。
     */
    override fun execute(context: CommandContext): CommandResult {
        val plugin = ArLibs.getInstance()
        val version = plugin.pluginMeta.version
        val authors = plugin.pluginMeta.authors.joinToString(", ")
        
        send(
            "",
            "&6╔═════════════════════════════════╗",
            "&6║        &eArLibs Framework &bv$version        &6║",
            "&6╠═════════════════════════════════╣",
            "&6║ &7${getLocalizedMessage("command.version.author", mapOf("author" to authors), fallback = "Author(s)")}",
            "&6║ &7${getLocalizedMessage("command.version.description", fallback = "A powerful library for Bukkit plugins")}",
            "&6║",
            "&6║ &7${getLocalizedMessage("command.features.title", fallback = "Features")}:",
            "&6║ &a• &7${getLocalizedMessage("command.features.commands", fallback = "Annotation-based command system")}",
            "&6║ &a• &7${getLocalizedMessage("command.features.config", fallback = "Advanced configuration management")}",
            "&6║ &a• &7${getLocalizedMessage("command.features.colors", fallback = "Color processing utilities")}",
            "&6║ &a• &7${getLocalizedMessage("command.features.logging", fallback = "Enhanced logging system")}",
            "&6║",
            "&6║ &7${getLocalizedMessage("command.help.usage", fallback = "Use /arlibs help for available commands")}",
            "&6╚═════════════════════════════════╝",
            ""
        )
        return CommandResult.SUCCESS
    }

    /**
     * Help subcommand - shows available commands and usage.
     * 帮助子命令 - 显示可用命令和用法。
     */
    @SubCommand(
        name = "help",
        aliases = ["h", "?"],
        description = "Show help information and available commands"
    )
    fun helpCommand(context: CommandContext): CommandResult {
        val mainCommandInfo = CommandAPI.getCommandInfo("arlibs")
            ?: return sendError("Could not load help information.").let { CommandResult.ERROR }

        if (context.args.isEmpty()) {
            val messages = mutableListOf<String>()
            
            messages.addAll(listOf(
                getLocalizedMessage("help.title", fallback = "&6=== &eArLibs Help &6==="),
                getLocalizedMessage("help.available_commands", fallback = "&7Available commands:"),
                ""
            ))

            // Subcommands (sorted alphabetically)
            mainCommandInfo.subCommands.values
                .distinctBy { it.name }
                .sortedBy { it.name }
                .filter { context.hasPermission(it.permission) }
                .forEach { subCmd ->
                    val usage = subCmd.usage.ifEmpty { "/${mainCommandInfo.name} ${subCmd.name}" }
                    val description = getLocalizedMessage("help.subcommand.${subCmd.name}", 
                        fallback = subCmd.description.ifEmpty { "No description available." })
                    messages.add("&e$usage &7- $description")
                }

            messages.addAll(listOf(
                "",
                getLocalizedMessage("help.specific_help", 
                    fallback = "&7Use &e/arlibs help <subcommand_name> &7for specific help.")
            ))

            send(*messages.toTypedArray())
        } else {
            val subCommandInfo = mainCommandInfo.subCommands[context.args[0].lowercase()]
                ?: return sendError("Unknown command: ${context.args[0]}").let { CommandResult.ERROR }

            if (!context.hasPermission(subCommandInfo.permission)) {
                return sendError("You don't have permission to view help for this command.").let { CommandResult.ERROR }
            }

            val effectiveExecutor = when {
                mainCommandInfo.executor == CommandExecutor.PLAYER || subCommandInfo.executor == CommandExecutor.PLAYER -> CommandExecutor.PLAYER
                mainCommandInfo.executor == CommandExecutor.CONSOLE || subCommandInfo.executor == CommandExecutor.CONSOLE -> CommandExecutor.CONSOLE
                else -> CommandExecutor.ALL
            }

            send(
                getLocalizedMessage("help.command_title", 
                    mapOf("command" to (subCommandInfo.usage.ifEmpty { "/${mainCommandInfo.name} ${subCommandInfo.name}" })),
                    fallback = "&6Help: &e${subCommandInfo.usage.ifEmpty { "/${mainCommandInfo.name} ${subCommandInfo.name}" }}"),
                getLocalizedMessage("help.description", 
                    mapOf("description" to (subCommandInfo.description.ifEmpty { getLocalizedMessage("help.no_description", fallback = "No description available.") })),
                    fallback = "&7Description: &f${subCommandInfo.description.ifEmpty { "No description available." }}"),
                if (subCommandInfo.aliases.isNotEmpty()) getLocalizedMessage("help.aliases", 
                    mapOf("aliases" to subCommandInfo.aliases.joinToString(", ")),
                    fallback = "&7Aliases: &f${subCommandInfo.aliases.joinToString(", ")}") else null,
                getLocalizedMessage("help.permission", 
                    mapOf("permission" to subCommandInfo.permission),
                    fallback = "&7Permission: &f${subCommandInfo.permission}"),
                getLocalizedMessage("help.executor", 
                    mapOf("executor" to effectiveExecutor.toString()),
                    fallback = "&7Executor: &f$effectiveExecutor")
            )
        }
        return CommandResult.SUCCESS
    }

    /**
     * Info subcommand - shows detailed system information.
     * 信息子命令 - 显示详细的系统信息。
     */
    @SubCommand(
        name = "info",
        aliases = ["information", "about"],
        description = "Show detailed ArLibs system information"
    )
    fun infoCommand(context: CommandContext): CommandResult {
        val plugin = ArLibs.getInstance()
        val version = plugin.pluginMeta.version
        val server = Bukkit.getServer()
        val allCommands = CommandAPI.getAllCommands()
        val pluginCommandCounts = allCommands.values
            .groupingBy { it.instance.javaClass.`package`?.name?.split(".")?.getOrNull(2) ?: "Unknown" }
            .eachCount()
        
        val messages = mutableListOf(
            getLocalizedMessage("command.about.title", fallback = "&6=== &eArLibs System Information &6==="),
            "",
            getLocalizedMessage("command.about.plugin_info", fallback = "&6Plugin Information:"),
            getLocalizedMessage("command.about.version", mapOf("version" to version), fallback = "&7• Version: &e$version"),
            getLocalizedMessage("command.about.authors", mapOf("authors" to plugin.pluginMeta.authors.joinToString(", ")), fallback = "&7• Authors: &e${plugin.pluginMeta.authors.joinToString(", ")}"),
            getLocalizedMessage("command.about.description", mapOf("description" to (plugin.pluginMeta.description ?: "A powerful library for Bukkit plugins")), fallback = "&7• Description: &e${plugin.pluginMeta.description ?: "A powerful library for Bukkit plugins"}"),
            "",
            getLocalizedMessage("command.about.server_info", fallback = "&6Server Information:"),
            getLocalizedMessage("command.about.server", mapOf("server" to "${server.name} ${server.version}"), fallback = "&7• Server: &e${server.name} ${server.version}"),
            getLocalizedMessage("command.about.bukkit_version", mapOf("version" to server.bukkitVersion), fallback = "&7• Bukkit Version: &e${server.bukkitVersion}"),
            getLocalizedMessage("command.about.online_players", mapOf("online" to server.onlinePlayers.size.toString(), "max" to server.maxPlayers.toString()), fallback = "&7• Online Players: &e${server.onlinePlayers.size}/${server.maxPlayers}"),
            "",
            getLocalizedMessage("command.about.command_system", fallback = "&6Command System:"),
            getLocalizedMessage("command.about.total_commands", mapOf("count" to allCommands.size.toString()), fallback = "&7• Total Registered Commands: &e${allCommands.size}"),
            getLocalizedMessage("command.about.commands_by_plugin", fallback = "&7• Commands by Plugin:")
        )

        pluginCommandCounts.toList().sortedByDescending { it.second }.forEach { (pluginName, count) ->
            messages.add(getLocalizedMessage("command.about.plugin_command_count", 
                mapOf("plugin" to pluginName, "count" to count.toString()), 
                fallback = "&7  - &e$pluginName&7: &a$count command(s)"))
        }
        
        messages.add("")
        messages.add(getLocalizedMessage("command.about.config_status", fallback = "&6Configuration Status:"))
        
        try {
            val coreConfig = ConfigManager.getConfig(CoreConfig::class)
            messages.addAll(listOf(
                getLocalizedMessage("command.about.debug_mode", 
                    mapOf("status" to if (coreConfig?.debug == true) "Enabled" else "Disabled"), 
                    fallback = "&7• Debug Mode: &e${if (coreConfig?.debug == true) "Enabled" else "Disabled"}"),
                getLocalizedMessage("command.about.config_loaded", fallback = "&7• Config Loaded: &aSuccessfully")
            ))
        } catch (e: Exception) {
            messages.add(getLocalizedMessage("command.about.config_error", 
                mapOf("error" to (e.message ?: "Unknown error")), 
                fallback = "&7• Config Status: &cError - ${e.message}"))
        }
        
        send(*messages.toTypedArray())
        return CommandResult.SUCCESS
    }

    /**
     * Commands subcommand - lists registered commands.
     * 命令子命令 - 列出已注册的命令。
     */
    @SubCommand(
        name = "commands",
        aliases = ["cmd", "list"],
        description = "List all commands registered through ArLibs",
        maxArgs = 1
    )
    fun commandsCommand(context: CommandContext): CommandResult {
        val allCommands = CommandAPI.getAllCommands()
        
        if (allCommands.isEmpty()) {
            return sendLocalizedWarning("command.commands.no_commands", fallbackMessage = "No commands are currently registered through ArLibs").let { CommandResult.SUCCESS }
        }
        
        val filterPlugin = context.getArg(0)
        val filteredCommands = if (filterPlugin != null) {
            allCommands.filter { it.value.instance.javaClass.`package`?.name?.contains(filterPlugin, ignoreCase = true) == true }
        } else {
            allCommands
        }
        
        if (filteredCommands.isEmpty()) {
            return sendLocalizedError("command.commands.no_commands_for_plugin", 
                mapOf("plugin" to (filterPlugin ?: "")), 
                fallbackMessage = "No commands found for plugin: $filterPlugin").let { CommandResult.ERROR }
        }
        
        val title = if (filterPlugin != null) {
            getLocalizedMessage("command.commands.title_for_plugin", 
                mapOf("plugin" to filterPlugin), 
                fallback = "&6=== &eCommands for $filterPlugin &6===")
        } else {
            getLocalizedMessage("command.commands.title_all", 
                fallback = "&6=== &eAll Registered Commands &6===")
        }
        
        val messages = mutableListOf(
            title,
            getLocalizedMessage("command.commands.total", 
                mapOf("count" to filteredCommands.size.toString()), 
                fallback = "&7Total: &e${filteredCommands.size} command(s)"),
            ""
        )
        
        filteredCommands.toList().sortedBy { it.first }.forEach { (commandName, commandInfo) ->
            val aliases = if (commandInfo.aliases.isNotEmpty()) " &8(${commandInfo.aliases.joinToString(", ")})" else ""
            val permission = if (commandInfo.permission.isNotEmpty()) " &8[${commandInfo.permission}]" else ""
            
            messages.add("&e/$commandName$aliases$permission")
            if (commandInfo.description.isNotEmpty()) {
                messages.add("&7  └─ ${commandInfo.description}")
            }
            if (commandInfo.subCommands.isNotEmpty()) {
                messages.add("&7  └─ Subcommands: &a${commandInfo.subCommands.size}")
            }
        }
        
        send(*messages.toTypedArray())
        return CommandResult.SUCCESS
    }

    /**
     * Reload subcommand - reloads ArLibs configuration.
     * 重载子命令 - 重新加载ArLibs配置。
     */
    @SubCommand(
        name = "reload",
        aliases = ["rl"],
        description = "Reload ArLibs configuration"
    )
    @Permission("arlibs.command.reload", defaultValue = PermissionDefault.OP)
    fun reloadCommand(context: CommandContext): CommandResult {
        sendLocalized("general.reloading", fallbackMessage = "&7Reloading ArLibs configuration...")
        
        return try {
            ConfigManager.reloadConfig(CoreConfig::class)
            ConfigManager.reloadConfig(com.arteam.arLibs.language.LanguageConfig::class)
            val coreConfig = ConfigManager.getConfig(CoreConfig::class)

            sendLocalizedSuccess("command.reload.success", 
                fallbackMessage = "&aArLibs configuration reloaded successfully!")
            sendLocalized("command.reload.debug_mode",
                mapOf("debug_status" to if (coreConfig?.debug == true) "Enabled" else "Disabled"),
                fallbackMessage = "&7Debug Mode: &e${if (coreConfig?.debug == true) "Enabled" else "Disabled"}")
            
            Logger.info("Configuration reloaded by ${context.sender.name}")
            CommandResult.SUCCESS
            
        } catch (e: Exception) {
            sendLocalizedError("errors.config_load", 
                mapOf("error" to (e.message ?: "Unknown error")),
                fallbackMessage = "Failed to reload configuration: ${e.message}")
            Logger.severe("Failed to reload configuration: ${e.message}")
            CommandResult.ERROR
        }
    }

    /**
     * Debug subcommand - toggles debug mode.
     * 调试子命令 - 切换调试模式。
     */
    @SubCommand(
        name = "debug",
        aliases = ["d"],
        description = "Toggle debug mode on/off"
    )
    @Permission("arlibs.command.debug", defaultValue = PermissionDefault.OP)
    fun debugCommand(context: CommandContext): CommandResult {
        return try {
            val coreConfig = ConfigManager.getConfig(CoreConfig::class)
            val newDebugState = !(coreConfig?.debug ?: false)
            
            coreConfig?.debug = newDebugState
            ConfigManager.saveConfig(CoreConfig::class)
            
            val statusKey = if (newDebugState) "command.debug.enabled" else "command.debug.disabled"
            
            sendLocalizedSuccess(statusKey, 
                fallbackMessage = if (newDebugState) "&aDebug mode enabled" else "&cDebug mode disabled")
            
            Logger.info("Debug mode ${if (newDebugState) "enabled" else "disabled"} by ${context.sender.name}")
            CommandResult.SUCCESS
            
        } catch (e: Exception) {
            sendLocalizedError("errors.config_save", 
                mapOf("error" to (e.message ?: "Unknown error")),
                fallbackMessage = "Failed to toggle debug mode: ${e.message}")
            CommandResult.ERROR
        }
    }

    /**
     * Version subcommand - shows version information.
     * 版本子命令 - 显示版本信息。
     */
    @SubCommand(
        name = "version",
        aliases = ["ver", "v"],
        description = "Show ArLibs version information"
    )
    fun versionCommand(context: CommandContext): CommandResult {
        val plugin = ArLibs.getInstance()
        val version = plugin.pluginMeta.version
        val authors = plugin.pluginMeta.authors.joinToString(", ")
        
        val messages = mutableListOf(
            getLocalizedMessage("command.version.title", fallback = "&6ArLibs Framework"),
            getLocalizedMessage("command.version.version", mapOf("version" to version), fallback = "&7Version: &e$version"),
            getLocalizedMessage("command.version.author", mapOf("author" to authors), fallback = "&7Author(s): &e$authors"),
            getLocalizedMessage("command.version.built_for", fallback = "&7Built for: &eBukkit/Spigot/Paper"),
            getLocalizedMessage("command.version.api_version", mapOf("version" to (plugin.pluginMeta.apiVersion ?: "Legacy")), fallback = "&7API Version: &e${plugin.pluginMeta.apiVersion ?: "Legacy"}")
        )
        
        if (context.hasPermission("arlibs.command.debug")) {
            messages.addAll(listOf(
                "",
                getLocalizedMessage("command.version.debug_info", fallback = "&7Debug Information:"),
                getLocalizedMessage("command.version.plugin_file", mapOf("file" to "${plugin.dataFolder.parent}/${plugin.pluginMeta.name}.jar"), fallback = "&7• Plugin File: &e${plugin.dataFolder.parent}/${plugin.pluginMeta.name}.jar"),
                getLocalizedMessage("command.version.data_folder", mapOf("folder" to plugin.dataFolder.absolutePath), fallback = "&7• Data Folder: &e${plugin.dataFolder.absolutePath}"),
                getLocalizedMessage("command.version.loaded", mapOf("status" to if (plugin.isEnabled) "Yes" else "No"), fallback = "&7• Loaded: &e${if (plugin.isEnabled) "Yes" else "No"}")
            ))
        }
        
        send(*messages.toTypedArray())
        return CommandResult.SUCCESS
    }

    /**
     * Action subcommand - execute action expressions directly.
     * 动作子命令 - 直接执行动作表达式。
     */
    @SubCommand(
        name = "action",
        aliases = ["act"],
        description = "Execute action expressions directly",
        minArgs = 0,
        executor = CommandExecutor.PLAYER
    )
    @Permission("arlibs.command.action", defaultValue = PermissionDefault.OP)
    fun actionCommand(context: CommandContext): CommandResult {
        if (context.args.isEmpty()) {
            showActionHelp()
            return CommandResult.SUCCESS
        }
        
        val actionExpression = context.args.joinToString(" ").trim()
        val player = getPlayer()
        
        sendLocalized("command.action.executing", mapOf("expression" to actionExpression), 
            fallbackMessage = "&6Executing action: &e$actionExpression")
        
        return try {
            val job = com.arteam.arLibs.action.ActionAPI.executeAction(player, actionExpression)
            if (job != null) {
                sendLocalizedSuccess("command.action.success", fallbackMessage = "Action executed successfully")
                CommandResult.SUCCESS
            } else {
                sendLocalizedError("command.action.parse_error", fallbackMessage = "Failed to parse action expression")
                showActionHelp()
                CommandResult.ERROR
            }
        } catch (e: Exception) {
            sendLocalizedError("command.action.execution_error", 
                mapOf("error" to (e.message ?: "Unknown error")),
                fallbackMessage = "Failed to execute action: ${e.message}")
            showActionHelp()
            CommandResult.ERROR
        }
    }

    /**
     * Condition subcommand - evaluate condition expressions directly.
     * 条件子命令 - 直接评估条件表达式。
     */
    @SubCommand(
        name = "condition",
        aliases = ["cond", "conditions"],
        description = "Evaluate condition expressions directly",
        minArgs = 0,
        executor = CommandExecutor.PLAYER
    )
    @Permission("arlibs.command.condition", defaultValue = PermissionDefault.OP)
    fun conditionCommand(context: CommandContext): CommandResult {
        if (context.args.isEmpty()) {
            showConditionHelp()
            return CommandResult.SUCCESS
        }
        val conditionExpression = context.args.joinToString(" ").trim()
        val player = getPlayer()
        
        sendLocalized("command.condition.evaluating", mapOf("expression" to conditionExpression), 
            fallbackMessage = "&6Evaluating condition: &e$conditionExpression")
        
        return try {
            if (!com.arteam.arLibs.condition.ConditionManager.isValidExpression(conditionExpression)) {
                sendLocalizedError("command.condition.invalid_syntax", 
                    fallbackMessage = "Invalid condition syntax")
                showConditionHelp()
                return CommandResult.ERROR
            }
            
            val result = com.arteam.arLibs.condition.ConditionManager.evaluate(player, conditionExpression)
            
            if (result) {
                sendLocalizedSuccess("command.condition.result_true", 
                    fallbackMessage = "Condition evaluated to: &atrue")
            } else {
                sendLocalized("command.condition.result_false", 
                    fallbackMessage = "&6Condition evaluated to: &cfalse")
            }
            CommandResult.SUCCESS
            
        } catch (e: Exception) {
            sendLocalizedError("command.condition.evaluation_error", 
                mapOf("error" to (e.message ?: "Unknown error")),
                fallbackMessage = "Error evaluating condition: ${e.message}")
            showConditionHelp()
            CommandResult.ERROR
        }
    }

    private fun showActionHelp() {
        sendLocalized(
            "command.action.help.title" to "&7Supported formats:",
            "command.action.help.tell" to "&7• tell <message>",
            "command.action.help.sound" to "&7• sound <sound>-<volume>-<pitch>",
            "command.action.help.if" to "&7• if {condition} then {actions} [else {actions}]",
            "command.action.help.delay" to "&7• delay <ticks>",
            "command.action.help.command" to "&7• command <command>",
            "command.action.help.console" to "&7• console <command>",
            "command.action.help.actionbar" to "&7• actionbar <message>",
            "command.action.help.title_format" to "&7• title `<title>` `<subtitle>` <fadeIn> <stay> <fadeOut>"
        )
    }

    private fun showConditionHelp() {
        sendLocalized(
            "command.condition.help.title" to "&7Supported formats:",
            "command.condition.help.permission" to "&7• permission <node>",
            "command.condition.help.papi" to "&7• papi <placeholder> [operator] [value]",
            "command.condition.help.any" to "&7• any [condition1; condition2; ...]",
            "command.condition.help.all" to "&7• all [condition1; condition2; ...]",
            "command.condition.help.not" to "&7• not <condition>"
        )
    }

    /**
     * Tab completion for help subcommand.
     * 帮助子命令的Tab补全。
     */
    @TabComplete(subCommand = "help", argument = 0)
    fun helpTabComplete(context: CommandContext): List<String> = 
        listOf("info", "commands", "reload", "debug", "version", "action", "condition")

    /**
     * Tab completion for commands subcommand.
     * 命令子命令的Tab补全。
     */
    @TabComplete(subCommand = "commands", argument = 0)
    fun commandsTabComplete(context: CommandContext): List<String> {
        return CommandAPI.getAllCommands().values
            .mapNotNull { it.instance.javaClass.`package`?.name?.split(".")?.getOrNull(2) }
            .toSet()
            .sorted()
    }

    /**
     * Global tab completion for the main command.
     * 主命令的全局Tab补全。
     */
    @TabComplete(argument = 0, priority = 1)
    fun globalTabComplete(context: CommandContext): List<String> {
        val subCommands = listOf("help", "info", "commands", "reload", "debug", "version", "action", "condition")
        val input = context.args.lastOrNull() ?: ""
        return subCommands.filter { it.startsWith(input, ignoreCase = true) }
    }

    // Helper methods to reduce code duplication / 辅助方法以减少代码重复
    
    /**
     * Validates command arguments and shows usage if invalid.
     * 验证命令参数，如果无效则显示用法。
     *
     * @param context The command context.
     *                命令上下文。
     * @param minArgs The minimum number of arguments required.
     *                所需的最小参数数量。
     * @param maxArgs The maximum number of arguments allowed.
     *                允许的最大参数数量。
     * @return true if arguments are valid, false otherwise.
     *         如果参数有效则返回true，否则返回false。
     */
    private fun validateArgs(context: CommandContext, minArgs: Int, maxArgs: Int = Int.MAX_VALUE): Boolean {
        if (!validateArgCount(minArgs, maxArgs)) {
            sendLocalizedError("command.invalid_usage", fallbackMessage = "Invalid number of arguments")
            return false
        }
        return true
    }
    
    /**
     * Shows a formatted help section.
     * 显示格式化的帮助部分。
     *
     * @param title The section title.
     *              部分标题。
     * @param items The help items to display.
     *              要显示的帮助项。
     */
    private fun showHelpSection(title: String, vararg items: String) {
        send(
            "&6=== &e$title &6===",
            *items,
            ""
        )
    }
} 