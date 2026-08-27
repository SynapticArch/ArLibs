/**
 * Language configuration for the ArLibs plugin.
 * Contains settings for internationalization and localization.
 *
 * ArLibs 插件的语言配置。
 * 包含国际化和本地化的设置。
 *
 * @author ArteamTech
 * @since 2025-07-12
 * @version 1.0.0
 */
package com.arteam.arLibs.language

import com.arteam.arLibs.config.annotations.Config
import com.arteam.arLibs.config.annotations.ConfigValue

/**
 * Language configuration for the ArLibs plugin.
 * ArLibs 插件的语言配置。
 */
@Config(
    fileName = "language",
    comments = [
        "ArLibs Language Configuration",
        "Contains settings for internationalization and localization"
    ]
)
class LanguageConfig {

    @ConfigValue(
        path = "default_language",
        comments = [
            "Default language for the server",
            "This language will be used when a player's language is not set or not available"
        ]
    )
    var defaultLanguage: String = "en"

    @ConfigValue(
        path = "fallback_language",
        comments = [
            "Fallback language when a message is not found in the current language",
            "This ensures that messages are always displayed even if translation is incomplete"
        ]
    )
    var fallbackLanguage: String = "en"

    @ConfigValue(
        path = "auto_reload",
        comments = [
            "Automatically reload language files when they are modified",
            "Enable this for development, disable for production for better performance"
        ]
    )
    var autoReload: Boolean = false

    @ConfigValue(
        path = "debug_missing_keys",
        comments = [
            "Show debug messages when language keys are missing",
            "Helpful for identifying missing translations during development"
        ]
    )
    var debugMissingKeys: Boolean = false

    @ConfigValue(
        path = "supported_languages",
        comments = [
            "List of supported languages",
            "Only languages listed here will be loaded"
        ]
    )
    var supportedLanguages: List<String> = listOf("en", "zh_cn", "zh_tw")

    @ConfigValue(
        path = "cache.enabled",
        comments = [
            "Enable message formatting cache for better performance",
            "Disable this if you have memory constraints"
        ]
    )
    var cacheEnabled: Boolean = true

    @ConfigValue(
        path = "cache.max_size",
        comments = [
            "Maximum number of cached formatted messages",
            "Higher values use more memory but provide better performance"
        ]
    )
    var maxCacheSize: Int = 1000

    @ConfigValue(
        path = "performance.parallel_loading",
        comments = [
            "Load language files in parallel for faster startup",
            "May use more CPU during initialization"
        ]
    )
    var parallelLoading: Boolean = true

    @ConfigValue(
        path = "validation.strict_mode",
        comments = [
            "Enable strict validation of language files",
            "Will fail to load files with missing required keys"
        ]
    )
    var strictValidation: Boolean = false

    @ConfigValue(
        path = "validation.required_keys",
        comments = [
            "List of keys that must be present in all language files",
            "Only used when strict_mode is enabled"
        ]
    )
    var requiredKeys: List<String> = listOf("general", "errors", "success")

    @ConfigValue(
        path = "formatting.placeholder_style",
        comments = [
            "Style of placeholders to use",
            "Options: curly_braces (default), square_brackets, percent_signs"
        ]
    )
    var placeholderStyle: String = "curly_braces"

    @ConfigValue(
        path = "formatting.color_codes",
        comments = [
            "Enable color code processing in messages",
            "Set to false to disable color support for better performance"
        ]
    )
    var colorCodesEnabled: Boolean = true
} 