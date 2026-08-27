/**
 * Marks a field as the primary key for an entity.
 * Only one field per entity should be annotated with @PrimaryKey.
 *
 * 标记一个字段作为实体的主键。
 * 每个实体只能有一个字段使用 @PrimaryKey 注解。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryKey(
    /**
     * Whether the primary key should auto-increment.
     * Only applicable to numeric types.
     *
     * 主键是否应该自动递增。
     * 仅适用于数字类型。
     */
    val autoIncrement: Boolean = false
) 