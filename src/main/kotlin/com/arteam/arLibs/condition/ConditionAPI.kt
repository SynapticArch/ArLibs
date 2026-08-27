/**
 * Public API for the ArLibs condition system.
 * ArLibs条件系统的公共API。
 *
 * @author ArteamTech
 * @since 2025-06-06
 * @version 1.0.0
 */
package com.arteam.arLibs.condition

import org.bukkit.entity.Player

/**
 * Main API class for evaluating conditions with the ArLibs condition system.
 * 用于通过ArLibs条件系统评估条件的主要API类。
 */
@Suppress("unused")
object ConditionAPI {
    
    /** Evaluates a condition expression for a player. 为玩家评估条件表达式。 */
    fun evaluate(player: Player, expression: String): Boolean = 
        ConditionManager.evaluate(player, expression)
    
    /** Evaluates multiple condition expressions for a player (all must be true). 为玩家评估多个条件表达式（全部必须为真）。 */
    fun evaluateAll(player: Player, expressions: List<String>): Boolean = 
        ConditionManager.evaluateAll(player, expressions)
    
    /** Evaluates multiple condition expressions for a player (any can be true). 为玩家评估多个条件表达式（任意一个为真即可）。 */
    fun evaluateAny(player: Player, expressions: List<String>): Boolean = 
        ConditionManager.evaluateAny(player, expressions)
    
    /** Evaluates multiple condition expressions for a player (all must be true) with vararg. 为玩家评估多个条件表达式（全部必须为真）可变参数版本。 */
    fun evaluateAll(player: Player, vararg expressions: String): Boolean = 
        ConditionManager.evaluateAll(player, expressions.toList())
    
    /** Evaluates multiple condition expressions for a player (any can be true) with vararg. 为玩家评估多个条件表达式（任意一个为真即可）可变参数版本。 */
    fun evaluateAny(player: Player, vararg expressions: String): Boolean = 
        ConditionManager.evaluateAny(player, expressions.toList())
    
    /** Validates if a condition expression is syntactically correct. 验证条件表达式在语法上是否正确。 */
    fun isValidExpression(expression: String): Boolean = 
        ConditionManager.isValidExpression(expression)
    
    /** Gets a description of a condition expression. 获取条件表达式的描述。 */
    fun getConditionDescription(expression: String): String? = 
        ConditionManager.getConditionDescription(expression)
    
    /** Parses a condition from a string expression. 从字符串表达式解析条件。 */
    fun parseCondition(expression: String): Condition? = 
        ConditionParser.parse(expression)
    
    /** Parses multiple conditions from a list of expressions. 从表达式列表解析多个条件。 */
    fun parseConditions(expressions: List<String>): List<Condition> = 
        ConditionParser.parseMultiple(expressions)
    
    /** Parses multiple conditions from vararg expressions. 从可变参数表达式解析多个条件。 */
    fun parseConditions(vararg expressions: String): List<Condition> = 
        ConditionParser.parseMultiple(expressions.toList())
    
    /** Validates if multiple condition expressions are syntactically correct. 验证多个条件表达式在语法上是否正确。 */
    fun areValidExpressions(expressions: List<String>): Boolean = 
        expressions.all { ConditionParser.isValid(it) }
    
    /** Validates if multiple condition expressions are syntactically correct with vararg. 验证多个条件表达式在语法上是否正确（可变参数版本）。 */
    fun areValidExpressions(vararg expressions: String): Boolean = 
        areValidExpressions(expressions.toList())
    
    /** Clears the condition cache. 清除条件缓存。 */
    fun clearCache() = ConditionManager.clearCache()
    
    /** Gets the current cache size. 获取当前缓存大小。 */
    fun getCacheSize(): Int = ConditionManager.getCacheSize()
    
    /** Checks if the condition system is available. 检查条件系统是否可用。 */
    fun isAvailable(): Boolean = try {
        ConditionManager::class.java
        ConditionParser::class.java
        true
    } catch (_: Exception) {
        false
    }
    
    /** Checks if PlaceholderAPI is available for placeholder conditions. 检查PlaceholderAPI是否可用于占位符条件。 */
    fun isPlaceholderAPIAvailable(): Boolean = try {
        org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
    } catch (_: Exception) {
        false
    }
} 