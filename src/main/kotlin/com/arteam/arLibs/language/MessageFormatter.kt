/**
 * Message formatter for processing and formatting language messages.
 * Handles color codes, placeholders, and message formatting.
 *
 * 用于处理和格式化语言消息的消息格式化器。
 * 处理颜色代码、占位符和消息格式化。
 *
 * @author ArteamTech
 * @since 2025-07-12
 * @version 1.0.0
 */
package com.arteam.arLibs.language

import com.arteam.arLibs.utils.ColorUtil
import com.arteam.arLibs.utils.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Message formatter for processing and formatting language messages.
 * 用于处理和格式化语言消息的消息格式化器。
 */
object MessageFormatter {

    // Cache for compiled regex patterns to improve performance
    private val placeholderRegex = Regex("\\{([^}]+)}")
    
    // Cache for different placeholder styles
    private val placeholderRegexCache = ConcurrentHashMap<String, Regex>()
    
    // Cache for formatted messages to avoid repeated processing
    private val messageCache = ConcurrentHashMap<String, String>()
    
    // Maximum cache size to prevent memory leaks
    private const val MAX_CACHE_SIZE = 1000

    /**
     * Gets the appropriate regex pattern for the given placeholder style.
     * 获取给定占位符样式的适当正则表达式模式。
     */
    private fun getPlaceholderRegex(style: String): Regex {
        return placeholderRegexCache.getOrPut(style) {
            when (style.lowercase()) {
                "square_brackets" -> Regex("\\[([^\\]]+)]")
                "percent_signs" -> Regex("%([^%]+)%")
                "curly_braces" -> Regex("\\{([^}]+)}")
                else -> Regex("\\{([^}]+)}")
            }
        }
    }

    /**
     * Formats a message with color codes and placeholders.
     * 使用颜色代码和占位符格式化消息。
     *
     * @param message The raw message to format.
     *                要格式化的原始消息。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @param placeholderStyle The style of placeholders to use.
     *                         要使用的占位符样式。
     * @return The formatted message.
     *         格式化后的消息。
     */
    fun format(
        message: String, 
        placeholders: Map<String, String> = emptyMap(),
        placeholderStyle: String = "curly_braces"
    ): String {
        if (message.isEmpty()) return message
        
        // If no placeholders, just process color codes
        if (placeholders.isEmpty()) {
            return ColorUtil.process(message)
        }
        
        // Create cache key for this message and placeholders combination
        val cacheKey = createCacheKey(message, placeholders, placeholderStyle)
        
        // Check cache first
        messageCache[cacheKey]?.let { return it }
        
        var formattedMessage = message

        // Use regex to replace all placeholders at once for better performance
        val regex = getPlaceholderRegex(placeholderStyle)
        formattedMessage = regex.replace(formattedMessage) { matchResult ->
            val placeholderKey = matchResult.groupValues[1]
            placeholders[placeholderKey] ?: matchResult.value
        }

        // Process color codes
        formattedMessage = ColorUtil.process(formattedMessage)
        
        // Cache the result
        cacheMessage(cacheKey, formattedMessage)

        return formattedMessage
    }

    /**
     * Creates a cache key for message and placeholders.
     * 为消息和占位符创建缓存键。
     */
    private fun createCacheKey(
        message: String, 
        placeholders: Map<String, String>,
        placeholderStyle: String = "curly_braces"
    ): String {
        val sortedPlaceholders = placeholders.entries.sortedBy { it.key }
        return "$message|$placeholderStyle|${sortedPlaceholders.joinToString(",") { "${it.key}=${it.value}" }}"
    }

    /**
     * Caches a formatted message with size limit.
     * 缓存格式化的消息并限制大小。
     */
    private fun cacheMessage(key: String, value: String) {
        if (messageCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries when cache is full
            val keysToRemove = messageCache.keys.take(messageCache.size - MAX_CACHE_SIZE + 1)
            keysToRemove.forEach { messageCache.remove(it) }
        }
        messageCache[key] = value
    }

    /**
     * Clears the message cache.
     * 清除消息缓存。
     */
    fun clearCache() {
        messageCache.clear()
    }

    /**
     * Gets the current cache size.
     * 获取当前缓存大小。
     */
    fun getCacheSize(): Int = messageCache.size

    /**
     * Formats a list of messages.
     * 格式化消息列表。
     *
     * @param messages The list of messages to format.
     *                 要格式化的消息列表。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @param placeholderStyle The style of placeholders to use.
     *                         要使用的占位符样式。
     * @return The list of formatted messages.
     *         格式化后的消息列表。
     */
    fun formatList(
        messages: List<String>, 
        placeholders: Map<String, String> = emptyMap(),
        placeholderStyle: String = "curly_braces"
    ): List<String> {
        return messages.map { format(it, placeholders, placeholderStyle) }
    }

    /**
     * Formats a message with support for plural forms.
     * 格式化支持复数形式的消息。
     *
     * @param singular The singular form of the message.
     *                 消息的单数形式。
     * @param plural The plural form of the message.
     *               消息的复数形式。
     * @param count The count to determine which form to use.
     *              用于确定使用哪种形式的计数。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return The formatted message.
     *         格式化后的消息。
     */
    fun formatPlural(
        singular: String,
        plural: String,
        count: Int,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val message = if (count == 1) singular else plural
        val allPlaceholders = placeholders.toMutableMap()
        allPlaceholders["count"] = count.toString()
        
        return format(message, allPlaceholders)
    }

    /**
     * Formats a message with random selection from a list.
     * 从列表中随机选择并格式化消息。
     *
     * @param messages The list of possible messages.
     *                 可能的消息列表。
     * @param placeholders Map of placeholder keys to replacement values.
     *                    占位符键到替换值的映射。
     * @return A randomly selected and formatted message.
     *         随机选择并格式化的消息。
     */
    fun formatRandom(messages: List<String>, placeholders: Map<String, String> = emptyMap()): String {
        if (messages.isEmpty()) return ""
        
        val randomMessage = messages.random()
        return format(randomMessage, placeholders)
    }

    /**
     * Strips color codes from a message.
     * 从消息中移除颜色代码。
     *
     * @param message The message to strip color codes from.
     *                要移除颜色代码的消息。
     * @return The message without color codes.
     *         没有颜色代码的消息。
     */
    fun stripColors(message: String): String {
        return ColorUtil.stripColorCodes(message)
    }

    /**
     * Validates if a message contains valid placeholders.
     * 验证消息是否包含有效的占位符。
     *
     * @param message The message to validate.
     *                要验证的消息。
     * @param availablePlaceholders The list of available placeholder keys.
     *                              可用的占位符键列表。
     * @return True if all placeholders are valid, false otherwise.
     *         如果所有占位符都有效则返回true，否则返回false。
     */
    fun validatePlaceholders(message: String, availablePlaceholders: Set<String>): Boolean {
        val placeholderRegex = Regex("\\{([^}]+)}")
        val foundPlaceholders = placeholderRegex.findAll(message).map { it.groupValues[1] }.toSet()
        
        val invalidPlaceholders = foundPlaceholders - availablePlaceholders
        if (invalidPlaceholders.isNotEmpty()) {
            Logger.debug("Invalid placeholders found in message: $invalidPlaceholders")
            return false
        }
        
        return true
    }
} 