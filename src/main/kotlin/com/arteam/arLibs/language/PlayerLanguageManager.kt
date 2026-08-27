/**
 * Player language manager for handling player language preferences with database persistence.
 * Automatically saves and loads player language preferences from the database.
 *
 * 玩家语言管理器，用于处理玩家语言偏好并持久化到数据库。
 * 自动保存和加载玩家的语言偏好。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.language

import com.arteam.arLibs.database.DatabaseAPI
import com.arteam.arLibs.database.entities.PlayerLanguage
import com.arteam.arLibs.utils.Logger
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * Player language manager for handling player language preferences with database persistence.
 * 玩家语言管理器，用于处理玩家语言偏好并持久化到数据库。
 */
object PlayerLanguageManager {
    
    // Cache for player language preferences to avoid frequent database queries
    // 玩家语言偏好缓存，避免频繁的数据库查询
    private val playerLanguageCache = ConcurrentHashMap<String, String>()
    
    // Flag to track if the manager is initialized
    // 跟踪管理器是否已初始化的标志
    private var isInitialized = false
    
    /**
     * Initializes the player language manager.
     * 初始化玩家语言管理器。
     */
    fun initialize() {
        if (isInitialized) {
            Logger.warn("PlayerLanguageManager is already initialized")
            return
        }
        
        isInitialized = true
        Logger.info("PlayerLanguageManager initialized successfully")
    }
    
    /**
     * Gets the language preference for a player.
     * First checks cache, then database, then returns default.
     * 获取玩家的语言偏好。
     * 首先检查缓存，然后检查数据库，最后返回默认值。
     *
     * @param player The player to get the language for.
     *               要获取语言的玩家。
     * @return The language code for the player.
     *         玩家的语言代码。
     */
    fun getPlayerLanguage(player: Player): String {
        if (!isInitialized) {
            Logger.warn("PlayerLanguageManager is not initialized")
            return "en"
        }
        
        val playerUuid = player.uniqueId.toString()
        
        // Check cache first
        // 首先检查缓存
        playerLanguageCache[playerUuid]?.let { cachedLanguage ->
            return cachedLanguage
        }
        
        // Check database
        // 检查数据库
        try {
            val playerLanguage = DatabaseAPI.find(PlayerLanguage::class.java, playerUuid)
            if (playerLanguage != null) {
                val language = playerLanguage.languageCode
                // Cache the result
                // 缓存结果
                playerLanguageCache[playerUuid] = language
                return language
            }
        } catch (e: Exception) {
            Logger.warn("Failed to load language preference for player ${player.name}: ${e.message}")
        }
        
        // Return default language
        // 返回默认语言
        return "en"
    }
    
    /**
     * Sets the language preference for a player.
     * Updates both cache and database.
     * 设置玩家的语言偏好。
     * 同时更新缓存和数据库。
     *
     * @param player The player to set the language for.
     *               要设置语言的玩家。
     * @param languageCode The language code to set.
     *                     要设置的语言代码。
     * @return true if the language was set successfully, false otherwise.
     *         如果语言设置成功则返回true，否则返回false。
     */
    fun setPlayerLanguage(player: Player, languageCode: String): Boolean {
        if (!isInitialized) {
            Logger.warn("PlayerLanguageManager is not initialized")
            return false
        }
        
        val playerUuid = player.uniqueId.toString()
        
        try {
            // Create or update player language preference
            // 创建或更新玩家语言偏好
            val playerLanguage = PlayerLanguage.create(
                playerUuid = playerUuid,
                playerName = player.name,
                languageCode = languageCode
            )
            
            // Save to database
            // 保存到数据库
            val success = DatabaseAPI.save(playerLanguage)
            if (success) {
                // Update cache
                // 更新缓存
                playerLanguageCache[playerUuid] = languageCode
                Logger.debug("Language preference saved for player ${player.name}: $languageCode")
                return true
            } else {
                Logger.warn("Failed to save language preference for player ${player.name}")
                return false
            }
        } catch (e: Exception) {
            Logger.severe("Error setting language preference for player ${player.name}: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Removes a player's language preference from cache when they disconnect.
     * 当玩家断开连接时从缓存中移除其语言偏好。
     *
     * @param player The player who disconnected.
     *               断开连接的玩家。
     */
    fun onPlayerDisconnect(player: Player) {
        val playerUuid = player.uniqueId.toString()
        playerLanguageCache.remove(playerUuid)
        Logger.debug("Removed language preference from cache for player ${player.name}")
    }
    
    /**
     * Loads a player's language preference when they join.
     * 当玩家加入时加载其语言偏好。
     *
     * @param player The player who joined.
     *               加入的玩家。
     */
    fun onPlayerJoin(player: Player) {
        // Pre-load the player's language preference into cache
        // 预加载玩家的语言偏好到缓存
        getPlayerLanguage(player)
        Logger.debug("Loaded language preference for player ${player.name}: ${getPlayerLanguage(player)}")
    }
    
    /**
     * Gets language statistics from the database.
     * 从数据库获取语言统计信息。
     *
     * @return Map of language codes to player counts.
     *         语言代码到玩家数量的映射。
     */
    fun getLanguageStatistics(): Map<String, Int> {
        if (!isInitialized) {
            return emptyMap()
        }
        
        return try {
            val languages = DatabaseAPI.query(PlayerLanguage::class.java).findAll()
            languages.groupBy { it.languageCode }
                .mapValues { it.value.size }
        } catch (e: Exception) {
            Logger.warn("Failed to get language statistics: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Clears the language cache.
     * 清除语言缓存。
     */
    fun clearCache() {
        playerLanguageCache.clear()
        Logger.debug("Language cache cleared")
    }
    
    /**
     * Gets the cache size.
     * 获取缓存大小。
     *
     * @return The number of cached language preferences.
     *         缓存的语言偏好数量。
     */
    fun getCacheSize(): Int = playerLanguageCache.size
} 