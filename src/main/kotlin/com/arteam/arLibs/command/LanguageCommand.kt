package com.arteam.arLibs.command

import com.arteam.arLibs.command.annotations.Command
import com.arteam.arLibs.command.annotations.TabComplete
import com.arteam.arLibs.language.LanguageAPI
import com.arteam.arLibs.language.LanguageManager
import org.bukkit.entity.Player

/**
 * Language command for setting player language preference.
 * 用于设置玩家语言偏好的语言命令。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
@Command(
    name = "language",
    description = "Set your language preference",
    aliases = ["lang"]
)
class LanguageCommand : BaseCommand() {
    
    override fun execute(context: CommandContext): CommandResult {
        val player = context.sender as? Player ?: return CommandResult.PLAYER_ONLY
        
        // If no arguments provided, show available languages
        // 如果没有提供参数，显示可用语言
        if (context.args.isEmpty()) {
            showAvailableLanguages(player)
            return CommandResult.SUCCESS
        }
        
        // Set language preference
        // 设置语言偏好
        val language = context.args[0].lowercase()
        setPlayerLanguage(player, language)
        
        return CommandResult.SUCCESS
    }
    
    /**
     * Shows available languages to the player.
     * 向玩家显示可用语言。
     *
     * @param player The player to show languages to.
     *               要显示语言的玩家。
     */
    private fun showAvailableLanguages(player: Player) {
        val availableLanguages = LanguageManager.getSupportedLanguages()
        val currentLanguage = LanguageAPI.getPlayerLanguage(player)
        
        sendLocalized("language.command.title")
        sendLocalized("language.command.available_title")
        
        availableLanguages.forEach { lang: String ->
            val displayName = when (lang) {
                "en" -> "English"
                "zh_cn" -> "简体中文"
                "zh_tw" -> "繁體中文"
                else -> lang
            }
            
            val placeholders = mapOf(
                "code" to lang,
                "name" to displayName
            )
            
            if (lang == currentLanguage) {
                sendLocalized("language.command.current_format", placeholders)
            } else {
                sendLocalized("language.command.language_format", placeholders)
            }
        }
        
        send("")
        sendLocalized("language.command.usage")
        sendLocalized("language.command.example")
    }
    
    /**
     * Sets the player's language preference.
     * 设置玩家的语言偏好。
     *
     * @param player The player to set language for.
     *               要设置语言的玩家。
     * @param language The language code to set.
     *                 要设置的语言代码。
     */
    private fun setPlayerLanguage(player: Player, language: String) {
        if (!LanguageManager.isLanguageSupported(language)) {
            sendLocalizedError("language.not_supported", mapOf("language" to language))
            return
        }
        
        LanguageAPI.setPlayerLanguage(player, language)
        
        val displayName = when (language) {
            "en" -> "English"
            "zh_cn" -> "简体中文"
            "zh_tw" -> "繁體中文"
            else -> language
        }
        
        sendLocalizedSuccess("language.set_success", mapOf("language" to displayName))
    }
    
    /**
     * Tab completion for language command.
     * 语言命令的Tab补全。
     */
    @TabComplete(argument = 0)
    fun languageTabComplete(context: CommandContext): List<String> {
        return LanguageManager.getSupportedLanguages()
    }
} 