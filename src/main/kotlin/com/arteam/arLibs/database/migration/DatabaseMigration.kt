/**
 * Database migration manager for handling schema changes.
 * Automatically creates and updates database tables based on entity definitions.
 *
 * 数据库迁移管理器，用于处理模式更改。
 * 根据实体定义自动创建和更新数据库表。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.migration

import com.arteam.arLibs.database.DatabaseConfig
import com.arteam.arLibs.database.EntityMetadata
import com.arteam.arLibs.utils.Logger
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

/**
 * Database migration manager for handling schema changes.
 * 数据库迁移管理器，用于处理模式更改。
 */
object DatabaseMigration {
    
    /**
     * Creates or updates database tables for the given entities.
     * 为给定实体创建或更新数据库表。
     *
     * @param connection The database connection.
     *                   数据库连接。
     * @param entities The list of entity metadata to process.
     *                 要处理的实体元数据列表。
     * @param config The database configuration.
     *               数据库配置。
     * @return true if migration was successful, false otherwise.
     *         如果迁移成功则返回true，否则返回false。
     */
    fun migrate(connection: Connection, entities: List<EntityMetadata>, config: DatabaseConfig): Boolean {
        return try {
            Logger.info("Starting database migration...")
            
            entities.forEach { entity ->
                if (!tableExists(connection, entity.tableName)) {
                    createTable(connection, entity, config)
                } else {
                    updateTable(connection, entity, config)
                }
            }
            
            Logger.info("Database migration completed successfully")
            true
        } catch (e: Exception) {
            Logger.severe("Database migration failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Checks if a table exists in the database.
     * 检查数据库中是否存在表。
     */
    private fun tableExists(connection: Connection, tableName: String): Boolean {
        val metaData: DatabaseMetaData = connection.metaData
        val tables: ResultSet = metaData.getTables(null, null, tableName, arrayOf("TABLE"))
        return tables.next()
    }
    
    /**
     * Creates a new table based on entity metadata.
     * 根据实体元数据创建新表。
     */
    private fun createTable(connection: Connection, entity: EntityMetadata, config: DatabaseConfig) {
        val sql = buildCreateTableSql(entity, config)
        
        connection.createStatement().use { stmt ->
            stmt.execute(sql)
        }
        
        Logger.info("Created table: ${entity.tableName}")
    }
    
    /**
     * Updates an existing table to match entity metadata.
     * 更新现有表以匹配实体元数据。
     */
    private fun updateTable(connection: Connection, entity: EntityMetadata, config: DatabaseConfig) {
        // For now, we'll just log that the table exists
        // In a full implementation, this would check for missing columns and add them
        Logger.debug("Table ${entity.tableName} already exists, skipping creation")
    }
    
    /**
     * Builds the CREATE TABLE SQL statement.
     * 构建CREATE TABLE SQL语句。
     */
    private fun buildCreateTableSql(entity: EntityMetadata, config: DatabaseConfig): String {
        val sql = StringBuilder("CREATE TABLE ${entity.tableName} (")
        
        val columnDefinitions = entity.columns.map { column ->
            buildColumnDefinition(column, config)
        }
        
        sql.append(columnDefinitions.joinToString(", "))
        sql.append(")")
        
        return sql.toString()
    }
    
    /**
     * Builds a column definition for the CREATE TABLE statement.
     * 为CREATE TABLE语句构建列定义。
     */
    private fun buildColumnDefinition(column: com.arteam.arLibs.database.ColumnMetadata, config: DatabaseConfig): String {
        val definition = StringBuilder("${column.name} ${column.type}")
        
        if (!column.nullable) {
            definition.append(" NOT NULL")
        }
        
        if (column.isPrimaryKey) {
            if (config.type.lowercase() == "sqlite" && column.autoIncrement) {
                definition.append(" PRIMARY KEY AUTOINCREMENT")
            } else if (config.type.lowercase() == "mysql" && column.autoIncrement) {
                definition.append(" AUTO_INCREMENT")
            }
        }
        
        if (column.defaultValue.isNotEmpty()) {
            definition.append(" DEFAULT '${column.defaultValue}'")
        }
        
        return definition.toString()
    }
} 