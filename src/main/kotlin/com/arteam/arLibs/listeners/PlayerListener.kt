/**
 * Player event listener for handling player join and quit events.
 * Automatically loads and cleans up player language preferences.
 *
 * 玩家事件监听器，用于处理玩家加入和退出事件。
 * 自动加载和清理玩家语言偏好。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.listeners

import com.arteam.arLibs.language.PlayerLanguageManager
import com.arteam.arLibs.utils.Logger
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Player event listener for handling player join and quit events.
 * 玩家事件监听器，用于处理玩家加入和退出事件。
 */
class PlayerListener : Listener {
    
    /**
     * Handles player join events.
     * Loads the player's language preference from database.
     * 处理玩家加入事件。
     * 从数据库加载玩家的语言偏好。
     *
     * @param event The player join event.
     *              玩家加入事件。
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        try {
            // Load player's language preference
            // 加载玩家的语言偏好
            PlayerLanguageManager.onPlayerJoin(player)
            
            Logger.debug("Loaded language preference for player ${player.name}")
        } catch (e: Exception) {
            Logger.warn("Failed to load language preference for player ${player.name}: ${e.message}")
        }
    }
    
    /**
     * Handles player quit events.
     * Cleans up the player's language preference from cache.
     * 处理玩家退出事件。
     * 从缓存中清理玩家的语言偏好。
     *
     * @param event The player quit event.
     *              玩家退出事件。
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        try {
            // Clean up player's language preference from cache
            // 从缓存中清理玩家的语言偏好
            PlayerLanguageManager.onPlayerDisconnect(player)
            
            Logger.debug("Cleaned up language preference for player ${player.name}")
        } catch (e: Exception) {
            Logger.warn("Failed to clean up language preference for player ${player.name}: ${e.message}")
        }
    }
} 