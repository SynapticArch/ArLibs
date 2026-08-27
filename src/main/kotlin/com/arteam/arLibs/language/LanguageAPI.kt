/**
 * Public API for the language system.
 * Provides easy-to-use methods for internationalization and localization.
 *
 * 语言系统的公共API。
 * 提供易于使用的国际化和本地化方法。
 *
 * @author ArteamTech
 * @since 2025-07-12
 * @version 1.0.0
 */
package com.arteam.arLibs.language

import org.bukkit.entity.Player

/**
 * Public API for the language system.
 * 语言系统的公共API。
 */
@Suppress("unused")
object LanguageAPI {

    /**
     * Gets a message for the default language.
     * 获取默认语言的消息。
     */
    fun getMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        return LanguageManager.getMessage(LanguageManager.getDefaultLanguage(), key, placeholders)
    }

    /**
     * Gets a message for a specific language.
     * 获取特定语言的消息。
     */
    fun getMessage(language: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        return LanguageManager.getMessage(language, key, placeholders)
    }

    /**
     * Gets a message for a player using their preferred language.
     * 为玩家获取消息，使用其首选语言。
     */
    fun getMessage(player: Player, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val playerLanguage = PlayerLanguageManager.getPlayerLanguage(player)
        return LanguageManager.getMessage(playerLanguage, key, placeholders)
    }

    /**
     * Gets a message for a player by name using their preferred language.
     * 通过玩家名称获取消息，使用其首选语言。
     */
    fun getMessageForPlayer(playerName: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        return LanguageManager.getMessageForPlayer(playerName, key, placeholders)
    }

    /**
     * Gets a list of messages for the default language.
     * 获取默认语言的消息列表。
     */
    fun getMessageList(key: String, placeholders: Map<String, String> = emptyMap()): List<String> {
        return LanguageManager.getMessageList(LanguageManager.getDefaultLanguage(), key, placeholders)
    }

    /**
     * Gets a list of messages for a specific language.
     * 获取特定语言的消息列表。
     */
    fun getMessageList(language: String, key: String, placeholders: Map<String, String> = emptyMap()): List<String> {
        return LanguageManager.getMessageList(language, key, placeholders)
    }

    /**
     * Gets a list of messages for a player using their preferred language.
     * 为玩家获取消息列表，使用其首选语言。
     */
    fun getMessageList(player: Player, key: String, placeholders: Map<String, String> = emptyMap()): List<String> {
        val playerLanguage = PlayerLanguageManager.getPlayerLanguage(player)
        return LanguageManager.getMessageList(playerLanguage, key, placeholders)
    }

    /**
     * Sets a player's preferred language.
     * 设置玩家的首选语言。
     */
    fun setPlayerLanguage(player: Player, language: String) {
        PlayerLanguageManager.setPlayerLanguage(player, language)
    }

    /**
     * Sets a player's preferred language by name.
     * 通过玩家名称设置首选语言。
     */
    fun setPlayerLanguage(playerName: String, language: String) {
        LanguageManager.setPlayerLanguage(playerName, language)
    }

    /**
     * Gets a player's preferred language.
     * 获取玩家的首选语言。
     */
    fun getPlayerLanguage(player: Player): String {
        return PlayerLanguageManager.getPlayerLanguage(player)
    }

    /**
     * Gets a player's preferred language by name.
     * 通过玩家名称获取首选语言。
     */
    fun getPlayerLanguage(playerName: String): String {
        return LanguageManager.getPlayerLanguage(playerName)
    }

    /**
     * Removes a player's language preference.
     * 移除玩家的语言偏好。
     */
    fun removePlayerLanguage(player: Player) {
        LanguageManager.removePlayerLanguage(player.name)
    }

    /**
     * Removes a player's language preference by name.
     * 通过玩家名称移除语言偏好。
     */
    fun removePlayerLanguage(playerName: String) {
        LanguageManager.removePlayerLanguage(playerName)
    }

    /**
     * Reloads all language files.
     * 重新加载所有语言文件。
     */
    fun reloadLanguages() {
        LanguageManager.reloadLanguages()
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
        return LanguageManager.isLanguageSupported(language)
    }

    /**
     * Gets all supported languages.
     * 获取所有支持的语言。
     */
    fun getSupportedLanguages(): List<String> {
        return LanguageManager.getSupportedLanguages()
    }

    /**
     * Gets the default language.
     * 获取默认语言。
     */
    fun getDefaultLanguage(): String {
        return LanguageManager.getDefaultLanguage()
    }

    /**
     * Gets the fallback language.
     * 获取回退语言。
     */
    fun getFallbackLanguage(): String {
        return LanguageManager.getFallbackLanguage()
    }

    /**
     * Gets a message with plural form support for the default language.
     * 获取默认语言的复数形式消息。
     *
     * @param singularKey The key for the singular form.
     *                    单数形式的键。
     * @param pluralKey The key for the plural form.
     *                  复数形式的键。
     * @param count The count to determine which form to use.
     *              用于确定使用哪种形式的计数。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message with appropriate plural form.
     *         具有适当复数形式的格式化消息。
     */
    fun getMessagePlural(
        singularKey: String,
        pluralKey: String,
        count: Int,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val language = LanguageManager.getDefaultLanguage()
        val singular = LanguageManager.getMessage(language, singularKey, emptyMap())
        val plural = LanguageManager.getMessage(language, pluralKey, emptyMap())
        return MessageFormatter.formatPlural(singular, plural, count, placeholders)
    }

    /**
     * Gets a message with plural form support for a specific language.
     * 获取特定语言的复数形式消息。
     *
     * @param language The language code.
     *                 语言代码。
     * @param singularKey The key for the singular form.
     *                    单数形式的键。
     * @param pluralKey The key for the plural form.
     *                  复数形式的键。
     * @param count The count to determine which form to use.
     *              用于确定使用哪种形式的计数。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message with appropriate plural form.
     *         具有适当复数形式的格式化消息。
     */
    fun getMessagePlural(
        language: String,
        singularKey: String,
        pluralKey: String,
        count: Int,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val singular = LanguageManager.getMessage(language, singularKey, emptyMap())
        val plural = LanguageManager.getMessage(language, pluralKey, emptyMap())
        return MessageFormatter.formatPlural(singular, plural, count, placeholders)
    }

    /**
     * Gets a message with plural form support for a player.
     * 为玩家获取复数形式消息。
     *
     * @param player The player to get the message for.
     *               要获取消息的玩家。
     * @param singularKey The key for the singular form.
     *                    单数形式的键。
     * @param pluralKey The key for the plural form.
     *                  复数形式的键。
     * @param count The count to determine which form to use.
     *              用于确定使用哪种形式的计数。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message with appropriate plural form.
     *         具有适当复数形式的格式化消息。
     */
    fun getMessagePlural(
        player: Player,
        singularKey: String,
        pluralKey: String,
        count: Int,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val language = LanguageManager.getPlayerLanguage(player.name)
        val singular = LanguageManager.getMessage(language, singularKey, emptyMap())
        val plural = LanguageManager.getMessage(language, pluralKey, emptyMap())
        return MessageFormatter.formatPlural(singular, plural, count, placeholders)
    }

    /**
     * Gets a random message from a list for the default language.
     * 从默认语言的消息列表中随机获取一条消息。
     *
     * @param key The message key that contains a list of messages.
     *            包含消息列表的消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return A randomly selected and formatted message.
     *         随机选择并格式化的消息。
     */
    fun getMessageRandom(key: String, placeholders: Map<String, String> = emptyMap()): String {
        val messages = getMessageList(key, placeholders)
        return MessageFormatter.formatRandom(messages, emptyMap())
    }

    /**
     * Gets a random message from a list for a specific language.
     * 从特定语言的消息列表中随机获取一条消息。
     *
     * @param language The language code.
     *                 语言代码。
     * @param key The message key that contains a list of messages.
     *            包含消息列表的消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return A randomly selected and formatted message.
     *         随机选择并格式化的消息。
     */
    fun getMessageRandom(language: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val messages = getMessageList(language, key, placeholders)
        return MessageFormatter.formatRandom(messages, emptyMap())
    }

    /**
     * Gets a random message from a list for a player.
     * 为玩家从消息列表中随机获取一条消息。
     *
     * @param player The player to get the message for.
     *               要获取消息的玩家。
     * @param key The message key that contains a list of messages.
     *            包含消息列表的消息键。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return A randomly selected and formatted message.
     *         随机选择并格式化的消息。
     */
    fun getMessageRandom(player: Player, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val messages = getMessageList(player, key, placeholders)
        return MessageFormatter.formatRandom(messages, emptyMap())
    }

    /**
     * Clears the message formatter cache.
     * 清除消息格式化器缓存。
     */
    fun clearCache() {
        MessageFormatter.clearCache()
    }

    /**
     * Gets the current cache size.
     * 获取当前缓存大小。
     */
    fun getCacheSize(): Int {
        return MessageFormatter.getCacheSize()
    }
} 