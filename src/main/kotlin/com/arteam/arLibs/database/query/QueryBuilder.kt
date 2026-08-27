/**
 * Query builder interface for building database queries.
 * Provides a fluent API for constructing complex database queries.
 *
 * 数据库查询构建器接口。
 * 提供用于构建复杂数据库查询的流畅API。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.query

/**
 * Query builder interface for building database queries.
 * 数据库查询构建器接口。
 */
interface QueryBuilder<T> {
    
    /**
     * Adds a WHERE clause to the query.
     * 向查询添加WHERE子句。
     *
     * @param condition The condition string (e.g., "column = ?").
     *                  条件字符串（例如，"column = ?"）。
     * @param params The parameters for the condition.
     *               条件的参数。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun where(condition: String, vararg params: Any?): QueryBuilder<T>
    
    /**
     * Adds an AND condition to the WHERE clause.
     * 向WHERE子句添加AND条件。
     *
     * @param condition The condition string.
     *                  条件字符串。
     * @param params The parameters for the condition.
     *               条件的参数。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun and(condition: String, vararg params: Any?): QueryBuilder<T>
    
    /**
     * Adds an OR condition to the WHERE clause.
     * 向WHERE子句添加OR条件。
     *
     * @param condition The condition string.
     *                  条件字符串。
     * @param params The parameters for the condition.
     *               条件的参数。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun or(condition: String, vararg params: Any?): QueryBuilder<T>
    
    /**
     * Adds an ORDER BY clause to the query.
     * 向查询添加ORDER BY子句。
     *
     * @param column The column to order by.
     *               要排序的列。
     * @param direction The sort direction (ASC or DESC).
     *                  排序方向（ASC或DESC）。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun orderBy(column: String, direction: String = "ASC"): QueryBuilder<T>
    
    /**
     * Adds a LIMIT clause to the query.
     * 向查询添加LIMIT子句。
     *
     * @param limit The maximum number of results.
     *              结果的最大数量。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun limit(limit: Int): QueryBuilder<T>
    
    /**
     * Adds an OFFSET clause to the query.
     * 向查询添加OFFSET子句。
     *
     * @param offset The number of results to skip.
     *               要跳过的结果数量。
     * @return This query builder for chaining.
     *         此查询构建器用于链式调用。
     */
    fun offset(offset: Int): QueryBuilder<T>
    
    /**
     * Executes the query and returns all results.
     * 执行查询并返回所有结果。
     *
     * @return List of entities matching the query.
     *         匹配查询的实体列表。
     */
    fun findAll(): List<T>
    
    /**
     * Executes the query and returns the first result.
     * 执行查询并返回第一个结果。
     *
     * @return The first entity matching the query, or null if none found.
     *         匹配查询的第一个实体，如果未找到则返回null。
     */
    fun findFirst(): T?
    
    /**
     * Executes the query and returns the count of results.
     * 执行查询并返回结果数量。
     *
     * @return The number of entities matching the query.
     *         匹配查询的实体数量。
     */
    fun count(): Long
    
    /**
     * Executes the query asynchronously and returns all results.
     * 异步执行查询并返回所有结果。
     *
     * @return Deferred containing the list of entities.
     *         包含实体列表的Deferred。
     */
    fun findAllAsync(): kotlinx.coroutines.Deferred<List<T>>
    
    /**
     * Executes the query asynchronously and returns the first result.
     * 异步执行查询并返回第一个结果。
     *
     * @return Deferred containing the first entity.
     *         包含第一个实体的Deferred。
     */
    fun findFirstAsync(): kotlinx.coroutines.Deferred<T?>
    
    /**
     * Executes the query asynchronously and returns the count of results.
     * 异步执行查询并返回结果数量。
     *
     * @return Deferred containing the count.
     *         包含数量的Deferred。
     */
    fun countAsync(): kotlinx.coroutines.Deferred<Long>
} 