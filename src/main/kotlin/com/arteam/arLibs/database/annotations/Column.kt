/**
 * Defines a database column for an entity field.
 * Fields annotated with @Column will be mapped to database columns.
 *
 * 为实体字段定义数据库列。
 * 使用 @Column 注解的字段将被映射到数据库列。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    /**
     * The name of the database column.
     * If not specified, the field name will be used.
     *
     * 数据库列的名称。
     * 如果未指定，将使用字段名。
     */
    val name: String = "",
    
    /**
     * The SQL data type for this column.
     * If not specified, the type will be inferred from the field type.
     *
     * 此列的SQL数据类型。
     * 如果未指定，类型将从字段类型推断。
     */
    val type: String = "",
    
    /**
     * Whether this column can be null.
     * Default is true.
     *
     * 此列是否可以为null。
     * 默认为true。
     */
    val nullable: Boolean = true,
    
    /**
     * The default value for this column.
     * If not specified, no default value will be set.
     *
     * 此列的默认值。
     * 如果未指定，将不设置默认值。
     */
    val defaultValue: String = ""
) 