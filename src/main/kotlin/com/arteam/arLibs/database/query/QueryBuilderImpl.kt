/**
 * Implementation of the QueryBuilder interface.
 * Provides the actual implementation for building and executing database queries.
 *
 * QueryBuilder接口的实现。
 * 提供构建和执行数据库查询的实际实现。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.query

import com.arteam.arLibs.database.DatabaseConfig
import com.arteam.arLibs.database.DatabaseStatistics
import com.arteam.arLibs.database.EntityMetadata
import com.arteam.arLibs.utils.Logger
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.full.memberProperties

/**
 * Implementation of the QueryBuilder interface.
 * QueryBuilder接口的实现。
 */
class QueryBuilderImpl<T>(
    private val entityClass: Class<T>,
    private val metadata: EntityMetadata,
    private val dataSource: HikariDataSource?,
    private val statistics: DatabaseStatistics,
    private val config: DatabaseConfig?
) : QueryBuilder<T> {
    
    private val conditions = mutableListOf<QueryCondition>()
    private val orderByClauses = mutableListOf<String>()
    private var limitValue: Int? = null
    private var offsetValue: Int? = null
    
    // Coroutine scope for async operations
    private val asyncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun where(condition: String, vararg params: Any?): QueryBuilder<T> {
        conditions.clear() // Clear existing conditions when starting a new WHERE clause
        conditions.add(QueryCondition(condition, params.toList(), "AND"))
        return this
    }
    
    override fun and(condition: String, vararg params: Any?): QueryBuilder<T> {
        conditions.add(QueryCondition(condition, params.toList(), "AND"))
        return this
    }
    
    override fun or(condition: String, vararg params: Any?): QueryBuilder<T> {
        conditions.add(QueryCondition(condition, params.toList(), "OR"))
        return this
    }
    
    override fun orderBy(column: String, direction: String): QueryBuilder<T> {
        orderByClauses.add("$column $direction")
        return this
    }
    
    override fun limit(limit: Int): QueryBuilder<T> {
        limitValue = limit
        return this
    }
    
    override fun offset(offset: Int): QueryBuilder<T> {
        offsetValue = offset
        return this
    }
    
    override fun findAll(): List<T> {
        return executeQuery(false)
    }
    
    override fun findFirst(): T? {
        val results = executeQuery(true)
        return results.firstOrNull()
    }
    
    override fun count(): Long {
        return executeCountQuery()
    }
    
    override fun findAllAsync(): Deferred<List<T>> {
        return asyncScope.async {
            findAll()
        }
    }
    
    override fun findFirstAsync(): Deferred<T?> {
        return asyncScope.async {
            findFirst()
        }
    }
    
    override fun countAsync(): Deferred<Long> {
        return asyncScope.async {
            count()
        }
    }
    
    /**
     * Executes the query and returns the results.
     * 执行查询并返回结果。
     */
    private fun executeQuery(limitToOne: Boolean): List<T> {
        if (dataSource == null) {
            Logger.warn("Database is not initialized")
            return emptyList()
        }
        
        return try {
            val connection = dataSource.connection
            connection.use { conn ->
                val sql = buildSelectSql(limitToOne)
                val startTime = System.currentTimeMillis()
                
                conn.prepareStatement(sql).use { stmt ->
                    setParameters(stmt)
                    val rs = stmt.executeQuery()
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    statistics.recordQuery(executionTime, true)
                    
                    if (config?.enableSqlLog == true) {
                        Logger.debug("SQL: $sql")
                    }

                    val results = mutableListOf<T>()
                    while (rs.next()) {
                        val entity = mapResultSetToEntity(rs)
                        if (entity != null) {
                            results.add(entity)
                        }
                    }
                    results
                }
            }
        } catch (e: Exception) {
            Logger.severe("Failed to execute query: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Executes a count query and returns the result.
     * 执行计数查询并返回结果。
     */
    private fun executeCountQuery(): Long {
        if (dataSource == null) {
            Logger.warn("Database is not initialized")
            return 0L
        }
        
        return try {
            val connection = dataSource.connection
            connection.use { conn ->
                val sql = buildCountSql()
                val startTime = System.currentTimeMillis()
                
                conn.prepareStatement(sql).use { stmt ->
                    setParameters(stmt)
                    val rs = stmt.executeQuery()
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    statistics.recordQuery(executionTime, true)
                    
                    if (rs.next()) {
                        rs.getLong(1)
                    } else {
                        0L
                    }
                }
            }
        } catch (e: Exception) {
            Logger.severe("Failed to execute count query: ${e.message}")
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * Builds the SELECT SQL statement.
     * 构建SELECT SQL语句。
     */
    private fun buildSelectSql(limitToOne: Boolean): String {
        val sql = StringBuilder("SELECT * FROM ${metadata.tableName}")
        
        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ")
            conditions.forEachIndexed { index, condition ->
                if (index > 0) {
                    sql.append(" ${condition.operator} ")
                }
                sql.append(condition.condition)
            }
        }
        
        if (orderByClauses.isNotEmpty()) {
            sql.append(" ORDER BY ${orderByClauses.joinToString(", ")}")
        }
        
        if (limitToOne) {
            sql.append(" LIMIT 1")
        } else {
            limitValue?.let { sql.append(" LIMIT $it") }
            offsetValue?.let { sql.append(" OFFSET $it") }
        }
        
        return sql.toString()
    }
    
    /**
     * Builds the COUNT SQL statement.
     * 构建COUNT SQL语句。
     */
    private fun buildCountSql(): String {
        val sql = StringBuilder("SELECT COUNT(*) FROM ${metadata.tableName}")
        
        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ")
            conditions.forEachIndexed { index, condition ->
                if (index > 0) {
                    sql.append(" ${condition.operator} ")
                }
                sql.append(condition.condition)
            }
        }
        
        return sql.toString()
    }
    
    /**
     * Sets parameters in the prepared statement.
     * 在预处理语句中设置参数。
     */
    private fun setParameters(stmt: PreparedStatement) {
        var paramIndex = 1
        conditions.forEach { condition ->
            condition.params.forEach { param ->
                stmt.setObject(paramIndex++, param)
            }
        }
    }
    
    /**
     * Maps a result set row to an entity.
     * 将结果集行映射到实体。
     */
    private fun mapResultSetToEntity(rs: ResultSet): T? {
        return try {
            val entity = entityClass.getDeclaredConstructor().newInstance()
            metadata.columns.forEach { column ->
                val value = rs.getObject(column.name)
                setPropertyValue(entity as Any, column.propertyName, value)
            }
            entity
        } catch (e: Exception) {
            Logger.severe("Failed to map result set to entity: ${e.message}")
            null
        }
    }
    
    /**
     * Sets a property value on an entity.
     * 在实体上设置属性值。
     */
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
 * Represents a query condition with its parameters and operator.
 * 表示带有参数和操作符的查询条件。
 */
data class QueryCondition(
    val condition: String,
    val params: List<Any?>,
    val operator: String
) 