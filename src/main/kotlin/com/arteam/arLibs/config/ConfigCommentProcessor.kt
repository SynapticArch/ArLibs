/**
 * Helper class for processing and saving configuration comments.
 * This class handles adding comments to configuration files during save operations.
 *
 * 处理和保存配置注释的辅助类。
 * 该类在保存操作期间处理向配置文件添加注释。
 *
 * @author ArteamTech
 * @since 2025-05-18
 * @version 1.0.0
 */
package com.arteam.arLibs.config

import com.arteam.arLibs.config.annotations.Config
import com.arteam.arLibs.config.annotations.ConfigField
import com.arteam.arLibs.config.annotations.ConfigValue
import com.arteam.arLibs.utils.Logger
import org.bukkit.configuration.file.YamlConfiguration
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Processes and writes comments to configuration files based on annotations.
 * 根据注解处理并将注释写入配置文件。
 */
@Suppress("unused")
class ConfigCommentProcessor {
    companion object {
        /**
         * Saves a configuration with comments.
         * 保存带有注释的配置。
         *
         * @param configClass The class of the configuration
         *                    配置的类
         * @param configFile The configuration file to save to
         *                   要保存到的配置文件
         * @param yamlConfig The YamlConfiguration containing the configuration data
         *                   包含配置数据的 YamlConfiguration
         */
        fun saveWithComments(configClass: KClass<*>, configFile: File, yamlConfig: YamlConfiguration) {
            // Store and clear the header from the YamlConfiguration object itself
            // to prevent saveToString() from outputting any header it might have loaded.
            // The actual header will be added by processComments based on @Config annotation.
            val loadedHeaderLines = yamlConfig.options().header // Non-deprecated way to get header
            yamlConfig.options().setHeader(null as List<String>?) // Non-deprecated way to clear header

            // Get the string representation of the YAML config (values only)
            val yamlString = yamlConfig.saveToString()

            // Restore the original header string to the YamlConfiguration object if needed for its state, though not strictly for this save.
            if (loadedHeaderLines.isNotEmpty()) { // Check if it's not null and not empty
                yamlConfig.options().setHeader(loadedHeaderLines)
            }
            // We don't need to restore copyHeader as it's a setting for save operations.

            val content = if (yamlString.isNotEmpty()) {
                yamlString.lines().toMutableList()
            } else {
                mutableListOf() // Ensure this is List<String> for processComments
            }
            
            // Process comments (add header, field, value comments to 'content')
            // processComments will add the header from @Config annotation.
            val commentedLines = processComments(configClass, content)
            
            // Write the content with comments back to the file, overwriting it.
            BufferedWriter(FileWriter(configFile)).use { writer ->
                commentedLines.forEachIndexed { index, line ->
                    writer.write(line)
                    if (index < commentedLines.size - 1) {
                        writer.newLine()
                    }
                }
            }
        }
        
        /**
         * Processes comments for a configuration class.
         * 处理配置类的注释。
         */
        private fun processComments(configClass: KClass<*>, content: MutableList<String>): List<String> {
            val result = mutableListOf<String>()
            
            // Add file header comments if any (only if not already present)
            configClass.findAnnotation<Config>()?.let { configAnnotation ->
                if (configAnnotation.comments.isNotEmpty()) {
                    // Check if header comments are already present
                    val hasHeaderComments = content.take(5).any { it.trim().startsWith("#") && 
                        configAnnotation.comments.any { comment -> it.contains(comment) } }
                    
                    if (!hasHeaderComments) {
                        configAnnotation.comments.forEach { result.add("# $it") }
                        result.add("")
                    }
                }
            }
            
            // Process field comments
            val pathComments = collectComments(configClass)
            var currentPath = ""
            val indentStack = mutableListOf<String>()
            
            content.forEachIndexed { lineIndex, line ->
                val trimmedLine = line.trim()
                var colonIndex = -1

                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    val indent = line.takeWhile { it.isWhitespace() }
                    
                    // Detect if we're moving to a parent level
                    while (indentStack.isNotEmpty() && indent.length < indentStack.last().length) {
                        indentStack.removeAt(indentStack.size - 1)
                        currentPath = currentPath.substringBeforeLast('.', "")
                    }
                    
                    colonIndex = trimmedLine.indexOf(':')
                    if (colonIndex > 0) {
                        val key = trimmedLine.substring(0, colonIndex).trim()
                        
                        currentPath = when {
                            currentPath.isEmpty() -> {
                                indentStack.add(indent)
                                key
                            }
                            indent.length > (indentStack.lastOrNull()?.length ?: -1) -> {
                                indentStack.add(indent)
                                "$currentPath.$key"
                            }
                            else -> {
                                if (indentStack.isEmpty() || indent.length != indentStack.last().length) {
                                    indentStack.clear()
                                    indentStack.add(indent)
                                }
                                if (currentPath.contains('.')) currentPath.substringBeforeLast('.') + ".$key" else key
                            }
                        }
                        
                        // Check if comments for this path are already present in the previous lines
                        val commentsForPath = pathComments[currentPath]
                        if (commentsForPath != null) {
                            val hasComments = lineIndex > 0 && content[lineIndex - 1].trim().startsWith("#") &&
                                commentsForPath.any { comment -> content[lineIndex - 1].contains(comment) }
                            
                            if (!hasComments) {
                                commentsForPath.forEach { comment ->
                                    result.add("${indent}# $comment")
                                }
                            }
                        }
                    }
                }
                result.add(line)
                
                // Add blank line after key-value pairs (only if not already present)
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && colonIndex > 0 && !trimmedLine.endsWith(":")) {
                    val nextLineIndex = lineIndex + 1
                    if (nextLineIndex < content.size) {
                        val nextTrimmedLine = content[nextLineIndex].trim()
                        if (nextTrimmedLine.isNotEmpty() && !nextTrimmedLine.startsWith("#")) {
                            result.add("")
                        }
                    } else {
                        result.add("")
                    }
                }
            }
            
            // Clean up trailing blank lines
            while (result.isNotEmpty() && result.last().isBlank()) {
                result.removeAt(result.size - 1)
            }
            if (result.isNotEmpty() && result.last().isNotEmpty()) {
                result.add("")
            }

            return result
        }
        
        /**
         * Collects comments from a configuration class.
         * 从配置类收集注释。
         */
        private fun collectComments(configClass: KClass<*>): Map<String, List<String>> {
            val comments = mutableMapOf<String, List<String>>()

            configClass.memberProperties.forEach { prop ->
                val field = try { prop.javaField } catch (_: Exception) { null } ?: return@forEach
                field.isAccessible = true
                if (!Modifier.isStatic(field.modifiers)) {
                    collectFieldComments(prop, field, "", comments, configClass)
                }
            }
            return comments
        }
        
        /**
         * Collects comments from a KProperty and its backing Field.
         * 从 KProperty 和其支持的 Field 收集注释。
         */
        private fun collectFieldComments(prop: kotlin.reflect.KProperty1<out Any, *>, field: Field, parentPath: String, comments: MutableMap<String, List<String>>, ownerClass: KClass<*>) {
            val configFieldAnnotation = prop.findAnnotation<ConfigField>()
            val configValueAnnotation = prop.findAnnotation<ConfigValue>()

            when {
                configFieldAnnotation != null -> {
                    val fieldPath = if (parentPath.isEmpty()) configFieldAnnotation.path else "$parentPath.${configFieldAnnotation.path}"
                    if (configFieldAnnotation.comments.isNotEmpty()) {
                        comments[fieldPath] = configFieldAnnotation.comments.toList()
                    }
                    try {
                        field.type.kotlin.memberProperties.forEach { nestedProp ->
                            val nestedField = try { nestedProp.javaField } catch (_: Exception) { null } ?: return@forEach
                            nestedField.isAccessible = true
                            if (!Modifier.isStatic(nestedField.modifiers)) {
                                collectFieldComments(nestedProp, nestedField, fieldPath, comments, field.type.kotlin)
                            }
                        }
                    } catch (_: Exception) {
                        Logger.warn("Could not reflect on nested fields of ${field.name} (type ${field.type.simpleName})")
                    }
                }
                configValueAnnotation != null -> {
                    val valuePath = if (parentPath.isEmpty()) configValueAnnotation.path else "$parentPath.${configValueAnnotation.path}"
                    if (configValueAnnotation.comments.isNotEmpty()) {
                        comments[valuePath] = configValueAnnotation.comments.toList()
                    }
                }
            }
        }
    }
} 