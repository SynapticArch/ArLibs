/**
 * Marks a class as a database entity.
 * Classes annotated with @Entity will be processed by the database system
 * to create tables and handle database operations.
 *
 * 标记一个类作为数据库实体。
 * 被 @Entity 注解的类将由数据库系统处理，用于创建表和处理数据库操作。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Entity(
    /**
     * The name of the database table.
     * If not specified, the class name in lowercase will be used.
     *
     * 数据库表的名称。
     * 如果未指定，将使用小写的类名。
     */
    val tableName: String = ""
) 