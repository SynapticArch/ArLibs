/**
 * Main class for the ArLibs plugin.
 * This class initializes the library components and provides core functionality.
 *
 * ArLibs插件的主类。
 * 该类初始化库组件并提供核心功能。
 *
 * @author ArteamTech
 * @since 2025-05-18
 * @version 1.0.0
 */
package com.arteam.arLibs

import com.arteam.arLibs.command.ArLibsCommand
import com.arteam.arLibs.command.CommandManager
import com.arteam.arLibs.command.LanguageCommand
import com.arteam.arLibs.config.ConfigManager
import com.arteam.arLibs.config.CoreConfig
import com.arteam.arLibs.database.DatabaseAPI
import com.arteam.arLibs.database.DatabaseConfig
import com.arteam.arLibs.language.LanguageConfig
import com.arteam.arLibs.language.LanguageManager
import com.arteam.arLibs.language.PlayerLanguageManager
import com.arteam.arLibs.listeners.PlayerListener
import com.arteam.arLibs.utils.Logger
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class ArLibs : JavaPlugin() {
    companion object {
        private lateinit var instance: ArLibs
        
        /**
         * Gets the plugin instance.
         * 获取插件实例。
         */
        fun getInstance(): ArLibs {
            return instance
        }
    }
    
    // Core configuration
    private lateinit var coreConfig: CoreConfig
    
    // Language configuration
    private lateinit var languageConfig: LanguageConfig
    
    // Database configuration
    private lateinit var databaseConfig: DatabaseConfig
    
    @Suppress("UnstableApiUsage")
    override fun onEnable() {
        instance = this

        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        
        // Initialize core configuration first
        try {
            coreConfig = ConfigManager.register(CoreConfig::class)
            logger.info("Core configuration loaded successfully")
        } catch (e: Exception) {
            // Use Bukkit's logger directly if our Config fails critically early
            logger.severe("Failed to load core configuration: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize language configuration
        try {
            languageConfig = ConfigManager.register(LanguageConfig::class)
            logger.info("Language configuration loaded successfully")
        } catch (e: Exception) {
            logger.severe("Failed to load language configuration: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize database configuration
        try {
            databaseConfig = ConfigManager.register(DatabaseConfig::class)
            logger.info("Database configuration loaded successfully")
        } catch (e: Exception) {
            logger.severe("Failed to load database configuration: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize the logger with debug setting from config
        Logger.init(coreConfig.debug)
        
        // Log plugin startup
        Logger.info("Plugin is starting up...")
        Logger.info("Version: &e${pluginMeta.version}")
        Logger.info("Author: &e${pluginMeta.authors.joinToString(", ")}")
        Logger.info("Debug mode: &e${if (coreConfig.debug) "Enabled" else "Disabled"}")
        
        // Initialize language system
        try {
            LanguageManager.initialize(this, languageConfig)
            Logger.info("Language system initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize language system: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize player language manager
        try {
            PlayerLanguageManager.initialize()
            Logger.info("Player language manager initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize player language manager: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize database system
        try {
            if (DatabaseAPI.initialize(databaseConfig)) {
                Logger.info("Database system initialized successfully")
            } else {
                Logger.severe("Failed to initialize database system")
                server.pluginManager.disablePlugin(this)
                return
            }
        } catch (e: Exception) {
            Logger.severe("Failed to initialize database system: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Initialize other configurations if needed
        // TODO: Add other configuration registrations here
        
        // Initialize the command system ready for other plugins to use
        Logger.info("Command system initialized and ready for registration")
        
        // Register ArLibs main command
        if (CommandManager.registerCommand(this, ArLibsCommand::class)) {
            Logger.info("ArLibs main command registered successfully")
        } else {
            Logger.warn("Failed to register ArLibs main command")
        }
        
        // Register language command
        if (CommandManager.registerCommand(this, LanguageCommand::class)) {
            Logger.info("Language command registered successfully")
        } else {
            Logger.warn("Failed to register language command")
        }
        
        // Register event listeners
        server.pluginManager.registerEvents(PlayerListener(), this)
        Logger.info("Event listeners registered successfully")
        
        Logger.info("Plugin has been enabled successfully!")
    }

    override fun onDisable() {
        // Log plugin shutdown
        Logger.info("Plugin is shutting down...")
        
        // Unregister all commands registered by this plugin
        CommandManager.unregisterCommands(this)
        
        // Save all configurations before shutdown
        try {
            ConfigManager.saveConfig(CoreConfig::class)
            ConfigManager.saveConfig(LanguageConfig::class)
            ConfigManager.saveConfig(DatabaseConfig::class)
            Logger.info("Configurations saved successfully")
        } catch (e: Exception) {
            Logger.warn("Failed to save configurations: ${e.message}")
        }
        
        // Cleanup database resources
        if (DatabaseAPI.isInitialized()) {
            DatabaseAPI.close()
            Logger.info("Database system closed")
        }
        
        Logger.info("Plugin has been disabled.")
    }
}
