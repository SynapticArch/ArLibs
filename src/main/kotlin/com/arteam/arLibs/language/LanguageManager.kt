/**
 * Language manager for handling internationalization and localization.
 * Manages language files, message loading, and language switching.
 *
 * 用于处理国际化和本地化的语言管理器。
 * 管理语言文件、消息加载和语言切换。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.language

import com.arteam.arLibs.utils.Logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Language manager for handling internationalization and localization.
 * 用于处理国际化和本地化的语言管理器。
 */
object LanguageManager {

    // Cache for loaded language files
    // 已加载语言文件的缓存
    private val languageCache = ConcurrentHashMap<String, YamlConfiguration>()
    
    // Cache for player language preferences
    // 玩家语言偏好的缓存
    private val playerLanguages = ConcurrentHashMap<String, String>()
    
    // Plugin instance for resource access
    // 用于资源访问的插件实例
    private lateinit var plugin: Plugin
    
    // Language configuration
    // 语言配置
    private lateinit var languageConfig: LanguageConfig
    
    // Flag to track if the manager is initialized
    // 跟踪管理器是否已初始化的标志
    private var isInitialized = false

    /**
     * Initializes the language manager.
     * 初始化语言管理器。
     *
     * @param pluginInstance The plugin instance.
     *                      插件实例。
     * @param config The language configuration.
     *               语言配置。
     */
    fun initialize(pluginInstance: Plugin, config: LanguageConfig) {
        plugin = pluginInstance
        languageConfig = config
        isInitialized = true
        
        // Load all supported languages
        // 加载所有支持的语言
        loadLanguages()
        
        Logger.info("Language manager initialized successfully")
    }

    /**
     * Loads all supported languages.
     * 加载所有支持的语言。
     */
    private fun loadLanguages() {
        val languagesFolder = File(plugin.dataFolder, "languages")
        
        // Create languages folder if it doesn't exist
        // 如果不存在则创建语言文件夹
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs()
            extractDefaultLanguages(languagesFolder)
        }
        
        // Load each supported language
        // 加载每个支持的语言
        languageConfig.supportedLanguages.forEach { language ->
            loadLanguage(language)
        }
        
        Logger.info("Loaded ${languageCache.size} language files")
    }

    /**
     * Extracts default language files from plugin resources.
     * 从插件资源中提取默认语言文件。
     *
     * @param languagesFolder The languages folder to extract to.
     *                        要提取到的语言文件夹。
     */
    private fun extractDefaultLanguages(languagesFolder: File) {
        languageConfig.supportedLanguages.forEach { language ->
            val resourcePath = "languages/$language.yml"
            val file = File(languagesFolder, "$language.yml")
            
            try {
                plugin.getResource(resourcePath)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Logger.debug("Extracted default language file: $language.yml")
            } catch (e: Exception) {
                Logger.warn("Failed to extract language file: $language.yml - ${e.message}")
            }
        }
    }

    /**
     * Loads a specific language file.
     * 加载特定的语言文件。
     *
     * @param language The language code to load.
     *                 要加载的语言代码。
     */
    private fun loadLanguage(language: String) {
        val file = File(plugin.dataFolder, "languages/$language.yml")
        
        if (!file.exists()) {
            Logger.warn("Language file not found: $language.yml")
            return
        }
        
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            
            // Validate the language file
            if (!validateLanguageFile(config, language)) {
                Logger.warn("Language file validation failed: $language.yml")
                return
            }
            
            languageCache[language] = config
            Logger.debug("Loaded language file: $language.yml")
        } catch (e: Exception) {
            Logger.severe("Failed to load language file: $language.yml - ${e.message}")
        }
    }

    /**
     * Validates a language file for required keys and format.
     * 验证语言文件的必需键和格式。
     *
     * @param config The configuration to validate.
     *               要验证的配置。
     * @param language The language code for error reporting.
     *                 用于错误报告的语言代码。
     * @return True if the file is valid, false otherwise.
     *         如果文件有效则返回true，否则返回false。
     */
    private fun validateLanguageFile(config: YamlConfiguration, language: String): Boolean {
        // Check if the file is empty or has no keys
        if (config.getKeys(false).isEmpty()) {
            Logger.warn("Language file is empty: $language.yml")
            return false
        }
        
        // Check for required keys (you can customize this list)
        val requiredKeys = listOf("general", "errors", "success")
        val missingKeys = mutableListOf<String>()
        
        requiredKeys.forEach { key ->
            if (!config.contains(key)) {
                missingKeys.add(key)
            }
        }
        
        if (missingKeys.isNotEmpty()) {
            Logger.warn("Missing required keys in language file $language.yml: ${missingKeys.joinToString(", ")}")
            // Don't fail completely, just warn
        }
        
        return true
    }

    /**
     * Reloads all language files.
     * 重新加载所有语言文件。
     */
    fun reloadLanguages() {
        languageCache.clear()
        loadLanguages()
        Logger.info("Language files reloaded successfully")
    }

    /**
     * Gets a message for a specific language and key with improved error handling.
     * 获取特定语言和键的消息，改进错误处理。
     *
     * @param language The language code.
     *                 语言代码。
     * @param key The message key.
     *            消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message, or the key if not found.
     *         格式化后的消息，如果未找到则返回键。
     */
    fun getMessage(language: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        if (!isInitialized) {
            Logger.warn("Language manager not initialized")
            return key
        }
        
        // Validate input parameters
        if (language.isBlank() || key.isBlank()) {
            Logger.warn("Invalid parameters: language='$language', key='$key'")
            return key
        }
        
        val message = getRawMessage(language, key)
        
        if (message == null) {
            if (languageConfig.debugMissingKeys) {
                Logger.debug("Missing language key: $key for language: $language")
            }
            return key
        }
        
        return try {
            MessageFormatter.format(message, placeholders)
        } catch (e: Exception) {
            Logger.warn("Failed to format message for key '$key' in language '$language': ${e.message}")
            message // Return unformatted message as fallback
        }
    }

    /**
     * Gets a raw message without formatting.
     * 获取未格式化的原始消息。
     *
     * @param language The language code.
     *                 语言代码。
     * @param key The message key.
     *            消息键。
     * @return The raw message, or null if not found.
     *         原始消息，如果未找到则返回null。
     */
    private fun getRawMessage(language: String, key: String): String? {
        val config = languageCache[language] ?: return null
        
        // Try to get the message from the specified language
        // 尝试从指定语言获取消息
        var message = config.getString(key)
        
        // If not found and not the fallback language, try fallback
        // 如果未找到且不是回退语言，尝试回退
        if (message == null && language != languageConfig.fallbackLanguage) {
            val fallbackConfig = languageCache[languageConfig.fallbackLanguage]
            message = fallbackConfig?.getString(key)
        }
        
        return message
    }

    /**
     * Gets a message for a player, using their preferred language.
     * 为玩家获取消息，使用其首选语言。
     *
     * @param playerName The player's name.
     *                  玩家名称。
     * @param key The message key.
     *            消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message.
     *         格式化后的消息。
     */
    fun getMessageForPlayer(playerName: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val playerLanguage = playerLanguages[playerName] ?: languageConfig.defaultLanguage
        return getMessage(playerLanguage, key, placeholders)
    }

    /**
     * Sets a player's preferred language.
     * 设置玩家的首选语言。
     *
     * @param playerName The player's name.
     *                  玩家名称。
     * @param language The language code.
     *                 语言代码。
     */
    fun setPlayerLanguage(playerName: String, language: String) {
        if (language in languageConfig.supportedLanguages) {
            playerLanguages[playerName] = language
            Logger.debug("Set language for player $playerName: $language")
        } else {
            Logger.warn("Unsupported language: $language for player: $playerName")
        }
    }

    /**
     * Gets a player's preferred language.
     * 获取玩家的首选语言。
     *
     * @param playerName The player's name.
     *                  玩家名称。
     * @return The player's language code.
     *         玩家的语言代码。
     */
    fun getPlayerLanguage(playerName: String): String {
        return playerLanguages[playerName] ?: languageConfig.defaultLanguage
    }

    /**
     * Removes a player's language preference.
     * 移除玩家的语言偏好。
     *
     * @param playerName The player's name.
     *                  玩家名称。
     */
    fun removePlayerLanguage(playerName: String) {
        playerLanguages.remove(playerName)
        Logger.debug("Removed language preference for player: $playerName")
    }

    /**
     * Gets a list of messages for a key.
     * 获取键的消息列表。
     *
     * @param language The language code.
     *                 语言代码。
     * @param key The message key.
     *            消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The list of formatted messages.
     *         格式化后的消息列表。
     */
    fun getMessageList(language: String, key: String, placeholders: Map<String, String> = emptyMap()): List<String> {
        if (!isInitialized) {
            Logger.warn("Language manager not initialized")
            return listOf(key)
        }
        
        val config = languageCache[language] ?: languageCache[languageConfig.fallbackLanguage]
        val messages = config?.getStringList(key) ?: emptyList()
        
        if (messages.isEmpty()) {
            if (languageConfig.debugMissingKeys) {
                Logger.debug("Missing language key list: $key for language: $language")
            }
            return listOf(key)
        }
        
        return MessageFormatter.formatList(messages, placeholders)
    }

    /**
     * Checks if a language is supported.
     * 检查语言是否受支持。
     *
     * @param language The language code to check.
     *                 要检查的语言代码。
     * @return True if the language is supported, false otherwise.
     *         如果语言受支持则返回true，否则返回false。
     */
    fun isLanguageSupported(language: String): Boolean {
        return language in languageConfig.supportedLanguages
    }

    /**
     * Gets all supported languages.
     * 获取所有支持的语言。
     *
     * @return The list of supported language codes.
     *         支持的语言代码列表。
     */
    fun getSupportedLanguages(): List<String> {
        return languageConfig.supportedLanguages.toList()
    }

    /**
     * Gets the default language.
     * 获取默认语言。
     *
     * @return The default language code.
     *         默认语言代码。
     */
    fun getDefaultLanguage(): String {
        return languageConfig.defaultLanguage
    }

    /**
     * Gets the fallback language.
     * 获取回退语言。
     *
     * @return The fallback language code.
     *         回退语言代码。
     */
    fun getFallbackLanguage(): String {
        return languageConfig.fallbackLanguage
    }
} 