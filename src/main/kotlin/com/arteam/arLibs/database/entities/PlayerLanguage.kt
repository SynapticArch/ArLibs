/**
 * Entity representing a player's language preference.
 * Used to store player language selections in the database.
 *
 * 表示玩家语言偏好的实体。
 * 用于在数据库中存储玩家语言选择。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.entities

import com.arteam.arLibs.database.annotations.Column
import com.arteam.arLibs.database.annotations.Entity
import com.arteam.arLibs.database.annotations.PrimaryKey

/**
 * Entity representing a player's language preference.
 * 表示玩家语言偏好的实体。
 */
@Entity(tableName = "player_languages")
class PlayerLanguage {
    
    /**
     * The player's unique identifier (UUID).
     * 玩家的唯一标识符（UUID）。
     */
    @PrimaryKey
    @Column(name = "player_uuid", type = "VARCHAR(36)", nullable = false)
    var playerUuid: String = ""
    
    /**
     * The player's name.
     * 玩家的名称。
     */
    @Column(name = "player_name", type = "VARCHAR(16)", nullable = false)
    var playerName: String = ""
    
    /**
     * The player's selected language code.
     * 玩家选择的语言代码。
     */
    @Column(name = "language_code", type = "VARCHAR(10)", nullable = false, defaultValue = "en")
    var languageCode: String = "en"
    
    /**
     * When the language preference was last updated.
     * 语言偏好最后更新的时间。
     */
    @Column(name = "last_updated", type = "BIGINT", nullable = false)
    var lastUpdated: Long = 0L
    
    /**
     * Whether this is the player's first time setting a language.
     * 这是否是玩家第一次设置语言。
     */
    @Column(name = "is_first_setup", type = "BOOLEAN", nullable = false, defaultValue = "true")
    var isFirstSetup: Boolean = true
    
    /**
     * Creates a new PlayerLanguage instance for a player.
     * 为玩家创建新的PlayerLanguage实例。
     *
     * @param playerUuid The player's UUID.
     *                   玩家的UUID。
     * @param playerName The player's name.
     *                   玩家的名称。
     * @param languageCode The language code.
     *                     语言代码。
     * @return A new PlayerLanguage instance.
     *         新的PlayerLanguage实例。
     */
    companion object {
        fun create(playerUuid: String, playerName: String, languageCode: String = "en"): PlayerLanguage {
            return PlayerLanguage().apply {
                this.playerUuid = playerUuid
                this.playerName = playerName
                this.languageCode = languageCode
                this.lastUpdated = System.currentTimeMillis()
                this.isFirstSetup = true
            }
        }
    }
} 