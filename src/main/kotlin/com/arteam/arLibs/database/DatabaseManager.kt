/**
 * Main manager for the ArLibs database system.
 * Handles database connections, operations, and entity management.
 *
 * ArLibs数据库系统的主要管理器。
 * 处理数据库连接、操作和实体管理。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database

import com.arteam.arLibs.database.annotations.Entity
import com.arteam.arLibs.database.annotations.PrimaryKey
import com.arteam.arLibs.database.migration.DatabaseMigration
import com.arteam.arLibs.database.query.QueryBuilder
import com.arteam.arLibs.database.query.QueryBuilderImpl
import com.arteam.arLibs.utils.Logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Main manager for database operations.
 * 数据库操作的主要管理器。
 */
object DatabaseManager {
    
    private var dataSource: HikariDataSource? = null
    private var config: DatabaseConfig? = null
    private var isInitialized = false
    
    // Cache for entity metadata
    private val entityMetadataCache = ConcurrentHashMap<KClass<*>, EntityMetadata>()
    
    // Statistics tracking
    private val statistics = DatabaseStatistics()
    
    // Coroutine scope for async operations
    private val asyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initializes the database system.
     * 初始化数据库系统。
     *
     * @param databaseConfig The database configuration.
     *                      数据库配置。
     * @return true if initialization was successful, false otherwise.
     *         如果初始化成功则返回true，否则返回false。
     */
    fun initialize(databaseConfig: DatabaseConfig): Boolean {
        if (isInitialized) {
            Logger.warn("Database system is already initialized")
            return true
        }
        
        try {
            config = databaseConfig
            dataSource = createDataSource(databaseConfig)
            
            // Pre-register known entities
            preRegisterEntities()
            
            // Test connection and run migrations
            dataSource?.connection?.use { connection ->
                Logger.info("Database connection test successful")
                
                // Run migrations if enabled
                if (databaseConfig.autoMigrationEnabled) {
                    val entities = getRegisteredEntities()
                    if (entities.isNotEmpty()) {
                        DatabaseMigration.migrate(connection, entities, databaseConfig)
                    }
                }
            }
            
            isInitialized = true
            Logger.info("Database system initialized successfully with type: ${databaseConfig.type}")
            return true
            
        } catch (e: Exception) {
            Logger.severe("Failed to initialize database system: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Creates a HikariCP data source based on configuration.
     * 根据配置创建HikariCP数据源。
     */
    private fun createDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdleConnections
            connectionTimeout = config.connectionTimeout
            idleTimeout = config.idleTimeout
            leakDetectionThreshold = 60000 // 1 minute
            
            when (config.type.lowercase()) {
                "sqlite" -> {
                    driverClassName = "org.sqlite.JDBC"
                    val plugin = com.arteam.arLibs.ArLibs.getInstance()
                    val dbFile = plugin.dataFolder.resolve(config.sqliteFile)
                    jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                }
                "mysql" -> {
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                    jdbcUrl = "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}?useSSL=false&serverTimezone=UTC"
                    username = config.mysqlUsername
                    password = config.mysqlPassword
                }
                else -> {
                    throw IllegalArgumentException("Unsupported database type: ${config.type}")
                }
            }
        }
        
        return HikariDataSource(hikariConfig)
    }
    
    /**
     * Checks if the database system is initialized.
     * 检查数据库系统是否已初始化。
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Saves an entity to the database.
     * 将实体保存到数据库。
     */
    fun <T : Any> save(entity: T): Boolean {
        if (!isInitialized) {
            Logger.warn("Database system is not initialized")
            return false
        }
        
        return try {
            val metadata = getEntityMetadata(entity::class)
            val connection = dataSource?.connection ?: return false
            
            connection.use { conn ->
                val sql = buildInsertOrUpdateSql(metadata, entity)
                val startTime = System.currentTimeMillis()
                
                conn.prepareStatement(sql).use { stmt ->
                    setParameters(stmt, metadata, entity)
                    val result = stmt.executeUpdate() > 0
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    statistics.recordQuery(executionTime, result)
                    
                    if (config?.enableSqlLog == true) {
                        Logger.debug("SQL: $sql")
                    }
                    
                    if (config?.logSlowQueries == true && executionTime > (config?.slowQueryThreshold ?: 1000)) {
                        Logger.warn("Slow query detected: $sql (${executionTime}ms)")
                    }
                    
                    result
                }
            }
        } catch (e: Exception) {
            Logger.severe("Failed to save entity: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Saves an entity to the database asynchronously.
     * 异步将实体保存到数据库。
     */
    fun <T : Any> saveAsync(entity: T): Deferred<Boolean> {
        return asyncScope.async {
            save(entity)
        }
    }
    
    /**
     * Finds an entity by its primary key.
     * 通过主键查找实体。
     */
    fun <T : Any> find(entityClass: Class<T>, primaryKey: Any): T? {
        if (!isInitialized) {
            Logger.warn("Database system is not initialized")
            return null
        }
        
        return try {
            val metadata = getEntityMetadata(entityClass.kotlin)
            val connection = dataSource?.connection ?: return null
            
            connection.use { conn ->
                val sql = "SELECT * FROM ${metadata.tableName} WHERE ${metadata.primaryKeyColumn} = ?"
                val startTime = System.currentTimeMillis()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, primaryKey)
                    val rs = stmt.executeQuery()
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    statistics.recordQuery(executionTime, true)
                    
                    if (rs.next()) {
                        mapResultSetToEntity(rs, entityClass, metadata)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Logger.severe("Failed to find entity: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Finds an entity by its primary key asynchronously.
     * 异步通过主键查找实体。
     */
    fun <T : Any> findAsync(entityClass: Class<T>, primaryKey: Any): Deferred<T?> {
        return asyncScope.async {
            find(entityClass, primaryKey)
        }
    }
    
    /**
     * Deletes an entity from the database.
     * 从数据库中删除实体。
     */
    fun <T : Any> delete(entity: T): Boolean {
        if (!isInitialized) {
            Logger.warn("Database system is not initialized")
            return false
        }
        
        return try {
            val metadata = getEntityMetadata(entity::class)
            val connection = dataSource?.connection ?: return false
            
            connection.use { conn ->
                val sql = "DELETE FROM ${metadata.tableName} WHERE ${metadata.primaryKeyColumn} = ?"
                val startTime = System.currentTimeMillis()
                
                conn.prepareStatement(sql).use { stmt ->
                    val primaryKeyValue = getPrimaryKeyValue(entity, metadata)
                    stmt.setObject(1, primaryKeyValue)
                    val result = stmt.executeUpdate() > 0
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    statistics.recordQuery(executionTime, result)
                    
                    result
                }
            }
        } catch (e: Exception) {
            Logger.severe("Failed to delete entity: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Deletes an entity from the database asynchronously.
     * 异步从数据库中删除实体。
     */
    fun <T : Any> deleteAsync(entity: T): Deferred<Boolean> {
        return asyncScope.async {
            delete(entity)
        }
    }
    
    /**
     * Creates a query builder for the specified entity class.
     * 为指定的实体类创建查询构建器。
     */
    fun <T : Any> query(entityClass: Class<T>): QueryBuilder<T> {
        val metadata = getEntityMetadata(entityClass.kotlin)
        return QueryBuilderImpl(entityClass, metadata, dataSource, statistics, config)
    }
    
    /**
     * Executes a transaction with the specified block.
     * 执行包含指定代码块的事务。
     */
    fun transaction(block: TransactionScope.() -> Unit): Boolean {
        if (!isInitialized) {
            Logger.warn("Database system is not initialized")
            return false
        }
        
        val connection = dataSource?.connection ?: return false
        return connection.use { conn ->
            try {
                conn.autoCommit = false
                val scope = TransactionScope(conn, statistics, config)
                scope.block()
                conn.commit()
                true
            } catch (e: Exception) {
                conn.rollback()
                Logger.severe("Transaction failed: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                conn.autoCommit = true
            }
        }
    }
    
    /**
     * Executes a transaction asynchronously with the specified block.
     * 异步执行包含指定代码块的事务。
     */
    fun transactionAsync(block: suspend TransactionScope.() -> Unit): Deferred<Boolean> {
        return asyncScope.async {
            if (!isInitialized) {
                Logger.warn("Database system is not initialized")
                return@async false
            }
            
            val connection = dataSource?.connection ?: return@async false
            connection.use { conn ->
                try {
                    conn.autoCommit = false
                    val scope = TransactionScope(conn, statistics, config)
                    scope.block()
                    conn.commit()
                    true
                } catch (e: Exception) {
                    conn.rollback()
                    Logger.severe("Async transaction failed: ${e.message}")
                    e.printStackTrace()
                    false
                } finally {
                    conn.autoCommit = true
                }
            }
        }
    }
    
    /**
     * Closes the database connection.
     * 关闭数据库连接。
     */
    fun close() {
        if (isInitialized) {
            dataSource?.close()
            asyncScope.cancel()
            isInitialized = false
            Logger.info("Database system closed")
        }
    }
    
    /**
     * Gets database statistics.
     * 获取数据库统计信息。
     */
    fun getStatistics(): DatabaseStatistics = statistics
    
    /**
     * Pre-registers known entities for the system.
     * 预注册系统中已知的实体。
     */
    private fun preRegisterEntities() {
        // Register PlayerLanguage entity
        getEntityMetadata(com.arteam.arLibs.database.entities.PlayerLanguage::class)
        Logger.info("Pre-registered entities for database migration")
    }
    
    /**
     * Gets all registered entities for migration.
     * 获取所有已注册的实体用于迁移。
     */
    private fun getRegisteredEntities(): List<EntityMetadata> {
        return entityMetadataCache.values.toList()
    }
    
    // Helper methods for entity metadata and SQL generation
    private fun getEntityMetadata(entityClass: KClass<*>): EntityMetadata {
        return entityMetadataCache.getOrPut(entityClass) {
            val entityAnnotation = entityClass.findAnnotation<Entity>()
                ?: throw IllegalArgumentException("Class ${entityClass.simpleName} is not annotated with @Entity")
            
            val tableName = entityAnnotation.tableName.ifEmpty {
                entityClass.simpleName?.lowercase() ?: "unknown"
            }
            
            val properties = entityClass.memberProperties
            val columns = mutableListOf<ColumnMetadata>()
            var primaryKeyColumn: String? = null
            
            properties.forEach { property ->
                val columnAnnotation = property.findAnnotation<com.arteam.arLibs.database.annotations.Column>()
                val primaryKeyAnnotation = property.findAnnotation<PrimaryKey>()
                
                val columnName = when {
                    columnAnnotation?.name?.isNotEmpty() == true -> columnAnnotation.name
                    else -> property.name
                }
                
                val columnType = when {
                    columnAnnotation?.type?.isNotEmpty() == true -> columnAnnotation.type
                    else -> inferSqlType(property.returnType.classifier)
                }
                
                columns.add(ColumnMetadata(
                    name = columnName,
                    propertyName = property.name,
                    type = columnType,
                    nullable = columnAnnotation?.nullable ?: true,
                    defaultValue = columnAnnotation?.defaultValue ?: "",
                    isPrimaryKey = primaryKeyAnnotation != null,
                    autoIncrement = primaryKeyAnnotation?.autoIncrement ?: false
                ))
                
                if (primaryKeyAnnotation != null) {
                    primaryKeyColumn = columnName
                }
            }
            
            EntityMetadata(tableName, columns, primaryKeyColumn)
        }
    }
    
    private fun inferSqlType(classifier: Any?): String {
        return when (classifier) {
            String::class -> "VARCHAR(255)"
            Int::class -> "INT"
            Long::class -> "BIGINT"
            Double::class -> "DOUBLE"
            Float::class -> "FLOAT"
            Boolean::class -> "BOOLEAN"
            else -> "TEXT"
        }
    }
    
    private fun buildInsertOrUpdateSql(metadata: EntityMetadata, entity: Any): String {
        val columns = metadata.columns.map { it.name }
        val placeholders = columns.joinToString(", ") { "?" }

        return when (config?.type?.lowercase()) {
            "sqlite" -> {
                "INSERT OR REPLACE INTO ${metadata.tableName} (${columns.joinToString(", ")}) VALUES ($placeholders)"
            }
            "mysql" -> {
                val updateClause = columns.joinToString(", ") { "$it = VALUES($it)" }
                "INSERT INTO ${metadata.tableName} (${columns.joinToString(", ")}) VALUES ($placeholders) ON DUPLICATE KEY UPDATE $updateClause"
            }
            else -> {
                "INSERT OR REPLACE INTO ${metadata.tableName} (${columns.joinToString(", ")}) VALUES ($placeholders)"
            }
        }
    }
    
    private fun setParameters(stmt: PreparedStatement, metadata: EntityMetadata, entity: Any) {
        var paramIndex = 1
        metadata.columns.forEach { column ->
            val value = getPropertyValue(entity, column.propertyName)
            stmt.setObject(paramIndex++, value)
        }
    }
    
    private fun getPropertyValue(entity: Any, propertyName: String): Any? {
        return try {
            val property = entity::class.memberProperties.find { it.name == propertyName }
            property?.getter?.call(entity)
        } catch (_: Exception) {
            null
        }
    }
    
    private fun getPrimaryKeyValue(entity: Any, metadata: EntityMetadata): Any? {
        val primaryKeyColumn = metadata.columns.find { it.isPrimaryKey }
        return primaryKeyColumn?.let { getPropertyValue(entity, it.propertyName) }
    }
    
    private fun <T : Any> mapResultSetToEntity(rs: ResultSet, entityClass: Class<T>, metadata: EntityMetadata): T? {
        return try {
            val entity = entityClass.getDeclaredConstructor().newInstance()
            metadata.columns.forEach { column ->
                val value = rs.getObject(column.name)
                setPropertyValue(entity, column.propertyName, value)
            }
            entity
        } catch (e: Exception) {
            Logger.severe("Failed to map result set to entity: ${e.message}")
            null
        }
    }
    
    private fun setPropertyValue(entity: Any, propertyName: String, value: Any?) {
        try {
            val property = entity::class.memberProperties.find { it.name == propertyName }
            if (property is kotlin.reflect.KMutableProperty1<*, *>) {
                (property as kotlin.reflect.KMutableProperty1<*, *>).setter.call(entity, value)
            }
        } catch (e: Exception) {
            Logger.debug("Failed to set property $propertyName: ${e.message}")
        }
    }
}

/**
 * Metadata for a database entity.
 * 数据库实体的元数据。
 */
data class EntityMetadata(
    val tableName: String,
    val columns: List<ColumnMetadata>,
    val primaryKeyColumn: String?
)

/**
 * Metadata for a database column.
 * 数据库列的元数据。
 */
data class ColumnMetadata(
    val name: String,
    val propertyName: String,
    val type: String,
    val nullable: Boolean,
    val defaultValue: String,
    val isPrimaryKey: Boolean,
    val autoIncrement: Boolean
)

/**
 * Database statistics for monitoring performance.
 * 用于监控性能的数据库统计信息。
 */
class DatabaseStatistics {
    private var totalQueries = 0L
    private var successfulQueries = 0L
    private var failedQueries = 0L
    private var totalExecutionTime = 0L
    private var slowQueries = 0L
    
    fun recordQuery(executionTime: Long, success: Boolean) {
        totalQueries++
        totalExecutionTime += executionTime
        
        if (success) {
            successfulQueries++
        } else {
            failedQueries++
        }
        
        if (executionTime > 1000) {
            slowQueries++
        }
    }
    
    fun getAverageExecutionTime(): Long = if (totalQueries > 0) totalExecutionTime / totalQueries else 0
    fun getSuccessRate(): Double = if (totalQueries > 0) successfulQueries.toDouble() / totalQueries else 0.0
    fun getTotalQueries(): Long = totalQueries
    fun getSlowQueries(): Long = slowQueries
}

/**
 * Transaction scope for database transactions.
 * 数据库事务的事务作用域。
 */
class TransactionScope(
    private val connection: Connection,
    private val statistics: DatabaseStatistics,
    private val config: DatabaseConfig?
) {
    fun <T : Any> save(entity: T): Boolean {
        // Implementation for transaction-scoped save
        return true // Placeholder
    }
    
    fun <T : Any> delete(entity: T): Boolean {
        // Implementation for transaction-scoped delete
        return true // Placeholder
    }
} 