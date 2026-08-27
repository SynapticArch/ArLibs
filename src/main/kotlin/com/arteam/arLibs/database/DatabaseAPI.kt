/**
 * Public API for the ArLibs database system.
 * Provides abstracted database operations with support for various database types.
 *
 * ArLibs数据库系统的公共API。
 * 提供抽象化的数据库操作，支持多种数据库类型。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database

import com.arteam.arLibs.database.query.QueryBuilder
import kotlinx.coroutines.Deferred

/**
 * Main API class for database operations.
 * 数据库操作的主要API类。
 */
@Suppress("unused")
object DatabaseAPI {
    
    /**
     * Initializes the database system with the specified configuration.
     * 使用指定配置初始化数据库系统。
     *
     * @param config The database configuration.
     *               数据库配置。
     * @return true if initialization was successful, false otherwise.
     *         如果初始化成功则返回true，否则返回false。
     */
    fun initialize(config: DatabaseConfig): Boolean {
        return DatabaseManager.initialize(config)
    }
    
    /**
     * Checks if the database system is initialized.
     * 检查数据库系统是否已初始化。
     *
     * @return true if initialized, false otherwise.
     *         如果已初始化则返回true，否则返回false。
     */
    fun isInitialized(): Boolean {
        return DatabaseManager.isInitialized()
    }
    
    /**
     * Saves an entity to the database.
     * 将实体保存到数据库。
     *
     * @param entity The entity to save.
     *               要保存的实体。
     * @return true if save was successful, false otherwise.
     *         如果保存成功则返回true，否则返回false。
     */
    fun <T : Any> save(entity: T): Boolean {
        return DatabaseManager.save(entity)
    }
    
    /**
     * Saves an entity to the database asynchronously.
     * 异步将实体保存到数据库。
     *
     * @param entity The entity to save.
     *               要保存的实体。
     * @return A Deferred that will contain the result.
     *         包含结果的Deferred。
     */
    fun <T : Any> saveAsync(entity: T): Deferred<Boolean> {
        return DatabaseManager.saveAsync(entity)
    }
    
    /**
     * Finds an entity by its primary key.
     * 通过主键查找实体。
     *
     * @param entityClass The entity class.
     *                    实体类。
     * @param primaryKey The primary key value.
     *                   主键值。
     * @return The found entity, or null if not found.
     *         找到的实体，如果未找到则返回null。
     */
    fun <T : Any> find(entityClass: Class<T>, primaryKey: Any): T? {
        return DatabaseManager.find(entityClass, primaryKey)
    }
    
    /**
     * Finds an entity by its primary key asynchronously.
     * 异步通过主键查找实体。
     *
     * @param entityClass The entity class.
     *                    实体类。
     * @param primaryKey The primary key value.
     *                   主键值。
     * @return A Deferred that will contain the found entity.
     *         包含找到实体的Deferred。
     */
    fun <T : Any> findAsync(entityClass: Class<T>, primaryKey: Any): Deferred<T?> {
        return DatabaseManager.findAsync(entityClass, primaryKey)
    }
    
    /**
     * Deletes an entity from the database.
     * 从数据库中删除实体。
     *
     * @param entity The entity to delete.
     *               要删除的实体。
     * @return true if delete was successful, false otherwise.
     *         如果删除成功则返回true，否则返回false。
     */
    fun <T : Any> delete(entity: T): Boolean {
        return DatabaseManager.delete(entity)
    }
    
    /**
     * Deletes an entity from the database asynchronously.
     * 异步从数据库中删除实体。
     *
     * @param entity The entity to delete.
     *               要删除的实体。
     * @return A Deferred that will contain the result.
     *         包含结果的Deferred。
     */
    fun <T : Any> deleteAsync(entity: T): Deferred<Boolean> {
        return DatabaseManager.deleteAsync(entity)
    }
    
    /**
     * Creates a query builder for the specified entity class.
     * 为指定的实体类创建查询构建器。
     *
     * @param entityClass The entity class to query.
     *                    要查询的实体类。
     * @return A query builder instance.
     *         查询构建器实例。
     */
    fun <T : Any> query(entityClass: Class<T>): QueryBuilder<T> {
        return DatabaseManager.query(entityClass)
    }
    
    /**
     * Executes a transaction with the specified block.
     * 执行包含指定代码块的事务。
     *
     * @param block The transaction block to execute.
     *              要执行的事务代码块。
     * @return true if transaction was successful, false otherwise.
     *         如果事务成功则返回true，否则返回false。
     */
    fun transaction(block: TransactionScope.() -> Unit): Boolean {
        return DatabaseManager.transaction(block)
    }
    
    /**
     * Executes a transaction asynchronously with the specified block.
     * 异步执行包含指定代码块的事务。
     *
     * @param block The transaction block to execute.
     *              要执行的事务代码块。
     * @return A Deferred that will contain the result.
     *         包含结果的Deferred。
     */
    fun transactionAsync(block: suspend TransactionScope.() -> Unit): Deferred<Boolean> {
        return DatabaseManager.transactionAsync(block)
    }
    
    /**
     * Closes the database connection.
     * 关闭数据库连接。
     */
    fun close() {
        DatabaseManager.close()
    }
    
    /**
     * Gets database statistics.
     * 获取数据库统计信息。
     *
     * @return Database statistics.
     *         数据库统计信息。
     */
    fun getStatistics(): DatabaseStatistics {
        return DatabaseManager.getStatistics()
    }
} 