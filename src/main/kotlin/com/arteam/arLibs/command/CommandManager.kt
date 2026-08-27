/**
 * Main command management system for the ArLibs framework.
 * This class handles registration, parsing, and execution of annotation-based commands.
 * 
 * ArLibs框架的主要命令管理系统。
 * 此类处理基于注解的命令的注册、解析和执行。
 *
 * @author ArteamTech
 * @since 2025-05-25
 * @version 1.0.0
 */
package com.arteam.arLibs.command

import com.arteam.arLibs.command.annotations.*
import com.arteam.arLibs.utils.Logger
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.Plugin
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Manager for handling annotation-based commands.
 * 处理基于注解的命令的管理器。
 */
object CommandManager {
    private val registeredCommands = mutableMapOf<String, CommandInfo>()
    // pluginCommands is now primarily for unregistration, CommandInfo holds the authoritative plugin instance
    internal val pluginCommandsByPlugin = mutableMapOf<Plugin, MutableList<String>>()
    
    // Cache for tab completions to improve performance
    private val tabCompletionCache = mutableMapOf<String, List<String>>()
    

    
    // Maximum cache size to prevent memory leaks
    // 最大缓存大小以防止内存泄漏
    private const val MAX_CACHE_SIZE = 500
    
    // Command execution statistics for monitoring
    // 命令执行统计用于监控
    private val commandStats = mutableMapOf<String, CommandStats>()
    
    /**
     * Statistics for command execution.
     * 命令执行统计。
     */
    data class CommandStats(
        var totalExecutions: Long = 0,
        var successfulExecutions: Long = 0,
        var failedExecutions: Long = 0,
        var totalExecutionTime: Long = 0,
        var lastExecutionTime: Long = 0
    ) {
        val averageExecutionTime: Long get() = if (totalExecutions > 0) totalExecutionTime / totalExecutions else 0
        val successRate: Double get() = if (totalExecutions > 0) successfulExecutions.toDouble() / totalExecutions else 0.0
    }
    
    /**
     * Registers a command class with the command system.
     * 向命令系统注册命令类。
     *
     * @param plugin The plugin registering the command.
     *               注册命令的插件。
     * @param commandClass The command class to register.
     *                     要注册的命令类。
     * @return true if registration was successful, false otherwise.
     *         如果注册成功则返回true，否则返回false。
     */
    fun registerCommand(plugin: Plugin, commandClass: KClass<out BaseCommand>): Boolean {
        return try {
            val commandAnnotation = commandClass.annotations.filterIsInstance<Command>().firstOrNull()
                ?: throw IllegalArgumentException("Command class must be annotated with @Command")
            
            val commandName = commandAnnotation.name.ifEmpty {
                commandClass.simpleName?.lowercase() ?: throw IllegalArgumentException("Cannot determine command name")
            }
            
            if (registeredCommands.containsKey(commandName)) {
                Logger.warn("Command '$commandName' is already registered")
                return false
            }
            
            val instance = createCommandInstance(commandClass)
            val commandInfo = parseCommandInfo(commandName, commandClass, instance, commandAnnotation, plugin)
            
            if (registerBukkitCommand(plugin, commandInfo)) {
                registeredCommands[commandName] = commandInfo
                pluginCommandsByPlugin.computeIfAbsent(plugin) { mutableListOf() }.add(commandName)
                Logger.info("Successfully registered command: &e$commandName")
                return true
            }
            false
        } catch (e: Exception) {
            Logger.severe("Failed to register command class ${commandClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Registers multiple command classes at once.
     * 一次注册多个命令类。
     *
     * @param plugin The plugin registering the commands.
     *               注册命令的插件。
     * @param commandClasses The command classes to register.
     *                       要注册的命令类。
     * @return The number of successfully registered commands.
     *         成功注册的命令数量。
     */
    fun registerCommands(plugin: Plugin, vararg commandClasses: KClass<out BaseCommand>): Int {
        return commandClasses.count { registerCommand(plugin, it) }
    }
    
    /**
     * Unregisters all commands for a specific plugin.
     * 注销特定插件的所有命令。
     *
     * @param plugin The plugin whose commands should be unregistered.
     *               要注销命令的插件。
     */
    fun unregisterCommands(plugin: Plugin) {
        val commandNames = pluginCommandsByPlugin.remove(plugin) ?: return
        commandNames.forEach { registeredCommands.remove(it) }
        Logger.info("Unregistered ${commandNames.size} commands for plugin ${plugin.name}")
    }
    
    /**
     * Gets information about a registered command.
     * 获取已注册命令的信息。
     *
     * @param commandName The name of the command.
     *                    命令的名称。
     * @return The command information, or null if not found.
     *         命令信息，如果未找到则返回null。
     */
    fun getCommandInfo(commandName: String): CommandInfo? = registeredCommands[commandName]
    
    /**
     * Gets all registered commands.
     * 获取所有已注册的命令。
     *
     * @return A map of command names to command information.
     *         命令名称到命令信息的映射。
     */
    fun getAllCommands(): Map<String, CommandInfo> = registeredCommands.toMap()
    
    /**
     * Gets all commands registered by a specific plugin.
     * 获取特定插件注册的所有命令。
     *
     * @param plugin The plugin to get commands for.
     *               要获取命令的插件。
     * @return A list of command names.
     *         命令名称列表。
     */
    fun getPluginCommands(plugin: Plugin): List<String> = pluginCommandsByPlugin[plugin]?.toList() ?: emptyList()

    /**
     * Clears all caches to free memory.
     * 清除所有缓存以释放内存。
     */
    fun clearCaches() {
        tabCompletionCache.clear()
        Logger.debug("Command manager caches cleared")
    }

    /**
     * Gets cache statistics for monitoring.
     * 获取缓存统计信息用于监控。
     *
     * @return A map containing cache statistics.
     *         包含缓存统计信息的映射。
     */
    fun getCacheStats(): Map<String, Int> = mapOf(
        "tab_completion_cache_size" to tabCompletionCache.size,
        "registered_commands" to registeredCommands.size
    )

    /**
     * Gets command execution statistics.
     * 获取命令执行统计信息。
     *
     * @param commandName The command name, or null for all commands.
     *                    命令名称，或null表示所有命令。
     * @return The command statistics.
     *         命令统计信息。
     */
    fun getCommandStats(commandName: String? = null): Map<String, CommandStats> {
        return if (commandName != null) {
            commandStats.filterKeys { it == commandName }
        } else {
            commandStats.toMap()
        }
    }

    /**
     * Resets command execution statistics.
     * 重置命令执行统计信息。
     *
     * @param commandName The command name, or null for all commands.
     *                    命令名称，或null表示所有命令。
     */
    fun resetCommandStats(commandName: String? = null) {
        if (commandName != null) {
            commandStats.remove(commandName)
        } else {
            commandStats.clear()
        }
        Logger.debug("Command statistics reset for ${commandName ?: "all commands"}")
    }

    /**
     * Manages cache size to prevent memory leaks.
     * 管理缓存大小以防止内存泄漏。
     */
    internal fun manageCacheSize() {
        if (tabCompletionCache.size > MAX_CACHE_SIZE) {
            val keysToRemove = tabCompletionCache.keys.take(tabCompletionCache.size - MAX_CACHE_SIZE + 1)
            keysToRemove.forEach { tabCompletionCache.remove(it) }
        }
    }
    
    private fun createCommandInstance(commandClass: KClass<out BaseCommand>): BaseCommand {
        return try {
            commandClass.createInstance()
        } catch (_: Exception) {
            val noArgConstructor = commandClass.java.constructors.find { it.parameterCount == 0 }
                ?: throw IllegalArgumentException("Command class must have a no-argument constructor")
            @Suppress("UNCHECKED_CAST")
            (noArgConstructor as Constructor<BaseCommand>).newInstance()
        }
    }
    
    /**
     * Parses command information from annotations.
     * 从注解解析命令信息。
     */
    private fun parseCommandInfo(
        commandName: String,
        commandClass: KClass<out BaseCommand>,
        instance: BaseCommand,
        commandAnnotation: Command,
        plugin: Plugin
    ): CommandInfo {
        val permission = "${plugin.name.lowercase()}.command.${commandName.lowercase()}"
        val usage = commandAnnotation.usage.ifEmpty { "/$commandName" }
        val permissions = commandClass.annotations.filterIsInstance<Permission>().map {
            PermissionInfo(it.value, it.op, it.defaultValue, it.message, it.silent)
        }
        
        return CommandInfo(
            name = commandName,
            aliases = commandAnnotation.aliases.toList(),
            description = commandAnnotation.description,
            usage = usage,
            permission = permission,
            executor = commandAnnotation.executor,
            minArgs = commandAnnotation.minArgs,
            maxArgs = commandAnnotation.maxArgs,
            commandClass = commandClass,
            instance = instance,
            permissions = permissions,
            async = commandAnnotation.async,
            plugin = plugin
        ).also { parseMethodAnnotations(commandClass, it) }
    }
    
    /**
     * Parses method annotations to extract subcommands and tab completers.
     * 解析方法注解以提取子命令和Tab补全器。
     */
    private fun parseMethodAnnotations(commandClass: KClass<out BaseCommand>, commandInfo: CommandInfo) {
        commandClass.java.declaredMethods.forEach { method ->
            method.isAccessible = true
            method.getAnnotation(SubCommand::class.java)?.let { parseSubCommand(method, it, commandInfo) }
            method.getAnnotation(TabComplete::class.java)?.let { parseTabCompleter(method, it, commandInfo) }
        }
    }
    
    /**
     * Parses subcommand information from annotations.
     * 从注解解析子命令信息。
     */
    private fun parseSubCommand(method: Method, annotation: SubCommand, commandInfo: CommandInfo) {
        val subCommandName = annotation.name.ifEmpty { method.name }
        val permission = "${commandInfo.permission}.${subCommandName.lowercase()}"
        val usage = annotation.usage.ifEmpty { "/${commandInfo.name} $subCommandName" }
        val permissions = method.annotations.filterIsInstance<Permission>().map {
            PermissionInfo(it.value, it.op, it.defaultValue, it.message, it.silent)
        }
        
        val subCommandInfo = SubCommandInfo(
            name = subCommandName,
            aliases = annotation.aliases.toList(),
            description = annotation.description,
            usage = usage,
            permission = permission,
            executor = annotation.executor,
            minArgs = annotation.minArgs,
            maxArgs = annotation.maxArgs,
            method = method,
            permissions = permissions,
            async = annotation.async
        )
        
        commandInfo.subCommands[subCommandName] = subCommandInfo
        annotation.aliases.forEach { commandInfo.subCommands[it] = subCommandInfo }
    }
    
    /**
     * Parses tab completer information from annotations.
     * 从注解解析Tab补全器信息。
     */
    private fun parseTabCompleter(method: Method, annotation: TabComplete, commandInfo: CommandInfo) {
        commandInfo.tabCompleters.add(TabCompleterInfo(
            command = annotation.command,
            subCommand = annotation.subCommand,
            argument = annotation.argument,
            priority = annotation.priority,
            permission = annotation.permission,
            staticValues = annotation.staticValues.toList(),
            method = if (annotation.staticValues.isEmpty()) method else null
        ))
    }
    
    /**
     * Registers a Bukkit command.
     * 注册Bukkit命令。
     */
    private fun registerBukkitCommand(plugin: Plugin, commandInfo: CommandInfo): Boolean {
        return try {
            val commandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap").apply { isAccessible = true }
                .get(Bukkit.getServer()) as CommandMap
            val pluginCommand = PluginCommand::class.java.getDeclaredConstructor(String::class.java, Plugin::class.java)
                .apply { isAccessible = true }.newInstance(commandInfo.name, plugin)
            
            pluginCommand.apply {
                description = commandInfo.description
                usage = commandInfo.usage
                aliases = commandInfo.aliases
                permission = commandInfo.permission
                val executor = ArLibsCommandExecutor(commandInfo)
                setExecutor(executor)
                tabCompleter = executor
            }
            
            commandMap.register(plugin.name, pluginCommand)
            true
        } catch (e: Exception) {
            Logger.severe("Failed to register Bukkit command ${commandInfo.name}: ${e.message}")
            false
        }
    }
}

/**
 * Bukkit command executor that bridges to our annotation-based system.
 * Bukkit命令执行器，桥接到我们基于注解的系统。
 */
private class ArLibsCommandExecutor(private val commandInfo: CommandInfo) : org.bukkit.command.CommandExecutor, TabCompleter {
    /**
     * Handles the execution of a command.
     * 处理命令的执行。
     */
    override fun onCommand(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<String>
    ): Boolean {
        return try {
            val context = CommandContext(sender, commandInfo.plugin, commandInfo.name, null, args, args, label)
            val executionLogic = { 
                if (args.isNotEmpty() && commandInfo.subCommands.isNotEmpty() && commandInfo.subCommands.containsKey(args[0].lowercase())) {
                    executeSubCommand(context, args)
                } else {
                    executeMainCommand(context)
                }
            }
            
            val isAsync = commandInfo.async || (args.isNotEmpty() && commandInfo.subCommands[args[0].lowercase()]?.async == true)
            
            if (isAsync) {
                Bukkit.getScheduler().runTaskAsynchronously(commandInfo.plugin, Runnable {
                    val result = try { 
                        executionLogic() 
                    } catch (e: Exception) {
                        Logger.severe("Async error executing command ${commandInfo.name}: ${e.message}")
                        e.printStackTrace()
                        CommandResult.ERROR
                    }
                    
                    // Schedule result handling on main thread
                    Bukkit.getScheduler().runTask(commandInfo.plugin, Runnable { 
                        try {
                            handleCommandResult(context, result) 
                        } catch (e: Exception) {
                            Logger.severe("Error handling async command result: ${e.message}")
                            context.sender.sendMessage("§cAn error occurred while processing the command result.")
                        }
                    })
                })
            } else {
                try {
                    handleCommandResult(context, executionLogic())
                } catch (e: Exception) {
                    Logger.severe("Error executing command ${commandInfo.name}: ${e.message}")
                    context.sender.sendMessage("§cAn error occurred while executing the command.")
                }
            }
            true
        } catch (e: Exception) {
            Logger.severe("Error executing command ${commandInfo.name}: ${e.message}")
            sender.sendMessage("§cAn error occurred while executing the command.")
            true
        }
    }

    /**
     * Handles tab completion for a command.
     * 处理命令的Tab补全。
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        return try {
            val context = CommandContext(sender, commandInfo.plugin, commandInfo.name, null, args, args, alias)
            generateTabCompletions(context)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Executes the main command.
     * 执行主命令。
     */
    private fun executeMainCommand(context: CommandContext): CommandResult {
        checkPermissions(context, commandInfo.permission, commandInfo.executor, commandInfo.permissions).let {
            if (it != CommandResult.SUCCESS) return it
        }
        
        commandInfo.instance.setContext(context)
        try {
            if (!commandInfo.instance.onPreExecute(context)) return CommandResult.CANCELLED
            return commandInfo.instance.execute(context).also { 
                commandInfo.instance.onPostExecute(context, it) 
            }
        } finally {
            commandInfo.instance.clearContext()
        }
    }
    
    /**
     * Executes a subcommand.
     * 执行子命令。
     */
    private fun executeSubCommand(context: CommandContext, args: Array<String>): CommandResult {
        val subCommandInfo = commandInfo.subCommands[args[0].lowercase()] 
            ?: throw IllegalArgumentException("Subcommand ${args[0]} not found")
        val effectiveExecutor = combineExecutors(commandInfo.executor, subCommandInfo.executor)
        val subContext = context.copy(subCommand = subCommandInfo.name, args = args.sliceArray(1 until args.size))
        
        checkPermissions(subContext, subCommandInfo.permission, effectiveExecutor, subCommandInfo.permissions).let {
            if (it != CommandResult.SUCCESS) return it
        }
        
        commandInfo.instance.setContext(subContext)
        try {
            if (!commandInfo.instance.onPreExecute(subContext)) return CommandResult.CANCELLED
            
            return (subCommandInfo.method.invoke(commandInfo.instance, subContext) as? CommandResult ?: CommandResult.SUCCESS)
                .also { commandInfo.instance.onPostExecute(subContext, it) }
        } catch (e: Exception) {
            Logger.severe("Error executing subcommand ${subCommandInfo.name}: ${e.message}")
            return CommandResult.ERROR
        } finally {
            commandInfo.instance.clearContext()
        }
    }
    
    /**
     * Combines executor restrictions from parent and child.
     * 组合父级和子级的执行者限制。
     */
    private fun combineExecutors(parent: CommandExecutor, child: CommandExecutor): CommandExecutor = when {
        parent == CommandExecutor.PLAYER || child == CommandExecutor.PLAYER -> CommandExecutor.PLAYER
        parent == CommandExecutor.CONSOLE || child == CommandExecutor.CONSOLE -> CommandExecutor.CONSOLE
        else -> CommandExecutor.ALL
    }
    
    /**
     * Checks permissions for a command.
     * 检查命令的权限。
     */
    private fun checkPermissions(context: CommandContext, permission: String, executor: CommandExecutor, permissions: List<PermissionInfo>): CommandResult {
        when (executor) {
            CommandExecutor.PLAYER -> if (!context.isPlayer()) return CommandResult.PLAYER_ONLY
            CommandExecutor.CONSOLE -> if (context.isPlayer()) return CommandResult.CONSOLE_ONLY
            CommandExecutor.ALL -> {}
        }
        
        if (permission.isNotEmpty() && !context.hasPermission(permission)) return CommandResult.NO_PERMISSION
        
        permissions.forEach { permInfo ->
            if (!checkPermission(context.sender, permInfo)) {
                if (!permInfo.silent && permInfo.message.isNotEmpty()) {
                    context.sender.sendMessage(permInfo.message)
                }
                return CommandResult.NO_PERMISSION
            }
        }
        
        return CommandResult.SUCCESS
    }

    /**
     * Checks a permission for a sender.
     * 检查发送者的权限。
     */
    private fun checkPermission(sender: CommandSender, permInfo: PermissionInfo): Boolean {
        if (permInfo.op && sender.isOp) return true
        
        return when (permInfo.defaultValue) {
            PermissionDefault.TRUE -> sender.hasPermission(permInfo.value) || !sender.isPermissionSet(permInfo.value)
            PermissionDefault.FALSE -> sender.hasPermission(permInfo.value)
            PermissionDefault.OP -> sender.isOp || sender.hasPermission(permInfo.value)
            PermissionDefault.NOT_OP -> !sender.isOp && (sender.hasPermission(permInfo.value) || !sender.isPermissionSet(permInfo.value))
        }
    }

    /**
     * Generates tab completions for a command.
     * 生成命令的Tab补全。
     */
    private fun generateTabCompletions(context: CommandContext): List<String> {
        // Priority 1: BaseCommand.onTabComplete()
        try {
            val baseCompletions = commandInfo.instance.onTabComplete(context)
            if (baseCompletions.isNotEmpty()) {
                val currentArg = context.args.lastOrNull() ?: ""
                return baseCompletions.filter { it.startsWith(currentArg, ignoreCase = true) }.distinct().sorted()
            }
        } catch (e: Exception) {
            Logger.debug("Error calling onTabComplete: ${e.message}")
        }

        val completions = mutableListOf<String>()
        
        when {
            context.args.size == 1 -> {
                // Add subcommands (only main names, not aliases for cleaner experience)
                commandInfo.subCommands.values.distinctBy { it.name }.sortedBy { it.name }.forEach { subCmd ->
                    if (hasPermission(context.sender, subCmd.permission, subCmd.permissions)) {
                        if (subCmd.name.startsWith(context.args[0], ignoreCase = true)) completions.add(subCmd.name)
                        // Only add aliases if explicitly configured via TabComplete annotation
                    }
                }
                completions.addAll(getTabCompleterResults(context))
            }
            context.args.size > 1 -> {
                val subCommandInfo = commandInfo.subCommands[context.args[0].lowercase()]
                if (subCommandInfo != null && hasPermission(context.sender, subCommandInfo.permission, subCommandInfo.permissions)) {
                    val subContext = context.copy(subCommand = subCommandInfo.name, args = context.args.sliceArray(1 until context.args.size))
                    completions.addAll(getSubCommandCompletions(subContext, subCommandInfo))
                }
            }
            else -> {
                // No args, show all subcommands (only main names, not aliases for cleaner experience)
                commandInfo.subCommands.values.distinctBy { it.name }.sortedBy { it.name }.forEach { subCmd ->
                    if (hasPermission(context.sender, subCmd.permission, subCmd.permissions)) {
                        completions.add(subCmd.name)
                    }
                }
                completions.addAll(getTabCompleterResults(context))
            }
        }
        
        return completions.distinct().sorted()
    }

    /**
     * Checks if a subcommand has permission.
     * 检查子命令是否有权限。
     */
    private fun hasPermission(sender: CommandSender, permission: String, permissions: List<PermissionInfo>): Boolean {
        if (permission.isNotEmpty() && !sender.hasPermission(permission)) return false
        return permissions.all { checkPermission(sender, it) }
    }

    /**
     * Gets completions for a subcommand.
     * 获取子命令的补全。
     */
    private fun getSubCommandCompletions(context: CommandContext, subCommandInfo: SubCommandInfo): List<String> {
        val completions = mutableListOf<String>()
        try {
            completions.addAll(commandInfo.instance.onTabComplete(context))
        } catch (e: Exception) {
            Logger.debug("Error calling onTabComplete for subcommand: ${e.message}")
        }
        
        completions.addAll(getTabCompleterResultsForSubCommand(context, subCommandInfo))
        
        val currentArg = context.args.lastOrNull() ?: ""
        return completions.filter { it.startsWith(currentArg, ignoreCase = true) }.distinct().sorted()
    }

    /**
     * Get tab completion results specifically for a subcommand from @TabComplete annotations.
     * 获取子命令特定的@TabComplete注解的补全结果。
     */
    private fun getTabCompleterResultsForSubCommand(context: CommandContext, subCommandInfo: SubCommandInfo): List<String> {
        val results = mutableListOf<String>()
        val currentArgIndex = context.args.size - 1

        commandInfo.tabCompleters.sortedByDescending { it.priority }.forEach { completer ->
            if (completer.subCommand == subCommandInfo.name && 
                (completer.command.isEmpty() || completer.command == commandInfo.name) &&
                (completer.argument < 0 || completer.argument == currentArgIndex) &&
                (completer.permission.isEmpty() || context.hasPermission(completer.permission))) {
                
                if (completer.staticValues.isNotEmpty()) {
                    results.addAll(completer.staticValues)
                } else {
                    completer.method?.let { method ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            (method.invoke(commandInfo.instance, context) as? List<String>)?.let { results.addAll(it) }
                        } catch (e: Exception) {
                            Logger.debug("Error executing tab completer: ${e.message}")
                        }
                    }
                }
            }
        }
        return results
    }

    /**
     * Gets tab completion results for the main command.
     * 获取主命令的Tab补全结果。
     */
    private fun getTabCompleterResults(context: CommandContext): List<String> {
        val results = mutableListOf<String>()
        val currentArgIndex = if (context.args.isEmpty()) 0 else context.args.size - 1
        
        commandInfo.tabCompleters.sortedByDescending { it.priority }.forEach { completer ->
            val matchesContext = if (context.subCommand != null) {
                completer.subCommand.isEmpty() || completer.subCommand == context.subCommand
            } else {
                completer.subCommand.isEmpty()
            }
            
            if (matchesContext &&
                (completer.argument < 0 || completer.argument == currentArgIndex) &&
                (completer.permission.isEmpty() || context.hasPermission(completer.permission))) {
                
                if (completer.staticValues.isNotEmpty()) {
                    val currentInput = context.args.lastOrNull() ?: ""
                    results.addAll(completer.staticValues.filter { it.startsWith(currentInput, ignoreCase = true) })
                } else {
                    completer.method?.let { method ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            (method.invoke(commandInfo.instance, context) as? List<String>)?.let { methodResults ->
                                val currentInput = context.args.lastOrNull() ?: ""
                                results.addAll(methodResults.filter { it.startsWith(currentInput, ignoreCase = true) })
                            }
                        } catch (e: Exception) {
                            Logger.debug("Error executing tab completer: ${e.message}")
                        }
                    }
                }
            }
        }
        return results
    }

    /**
     * Handles the result of a command execution.
     * 处理命令执行的结果。
     */
    private fun handleCommandResult(context: CommandContext, result: CommandResult) {
        val feedbackType = when (result) {
            CommandResult.NO_PERMISSION -> FeedbackType.NO_PERMISSION
            CommandResult.INVALID_USAGE -> FeedbackType.INVALID_USAGE
            CommandResult.PLAYER_ONLY -> FeedbackType.PLAYER_ONLY
            CommandResult.CONSOLE_ONLY -> FeedbackType.CONSOLE_ONLY
            CommandResult.NOT_FOUND -> FeedbackType.SUBCOMMAND_NOT_FOUND
            CommandResult.ERROR -> FeedbackType.ERROR
            else -> return // SUCCESS or CANCELLED - do nothing
        }
        
        val message = commandInfo.instance.getFeedbackMessage(context, feedbackType)
        context.sender.sendMessage(com.arteam.arLibs.utils.ColorUtil.process(message))
    }
} 