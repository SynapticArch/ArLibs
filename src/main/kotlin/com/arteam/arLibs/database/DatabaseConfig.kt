/**
 * Database configuration for the ArLibs database system.
 * Contains settings for database connection and operation.
 *
 * ArLibs数据库系统的数据库配置。
 * 包含数据库连接和操作的设置。
 *
 * @author ArteamTech
 * @since 2025-01-27
 * @version 1.0.0
 */
package com.arteam.arLibs.database

import com.arteam.arLibs.config.annotations.Config
import com.arteam.arLibs.config.annotations.ConfigValue

/**
 * Database configuration for the ArLibs database system.
 * ArLibs数据库系统的数据库配置。
 */
@Config(
    fileName = "database",
    comments = [
        "ArLibs Database Configuration",
        "Configure database connection and operation settings"
    ]
)
class DatabaseConfig {

    @ConfigValue(
        path = "type",
        defaultValue = "sqlite",
        comments = [
            "Database type to use",
            "Supported types: sqlite, mysql"
        ]
    )
    var type: String = "sqlite"

    @ConfigValue(
        path = "sqlite.file",
        defaultValue = "database.db",
        comments = [
            "SQLite database file name",
            "File will be created in the plugin's data folder"
        ]
    )
    var sqliteFile: String = "database.db"

    @ConfigValue(
        path = "mysql.host",
        defaultValue = "localhost",
        comments = [
            "MySQL database host address"
        ]
    )
    var mysqlHost: String = "localhost"

    @ConfigValue(
        path = "mysql.port",
        defaultValue = "3306",
        comments = [
            "MySQL database port"
        ]
    )
    var mysqlPort: Int = 3306

    @ConfigValue(
        path = "mysql.database",
        defaultValue = "arlibs",
        comments = [
            "MySQL database name"
        ]
    )
    var mysqlDatabase: String = "arlibs"

    @ConfigValue(
        path = "mysql.username",
        defaultValue = "root",
        comments = [
            "MySQL database username"
        ]
    )
    var mysqlUsername: String = "root"

    @ConfigValue(
        path = "mysql.password",
        defaultValue = "",
        comments = [
            "MySQL database password"
        ]
    )
    var mysqlPassword: String = ""

    @ConfigValue(
        path = "connection_pool.max_size",
        defaultValue = "10",
        comments = [
            "Maximum number of database connections in the pool"
        ]
    )
    var maxPoolSize: Int = 10

    @ConfigValue(
        path = "connection_pool.min_idle",
        defaultValue = "2",
        comments = [
            "Minimum number of idle connections in the pool"
        ]
    )
    var minIdleConnections: Int = 2

    @ConfigValue(
        path = "connection_pool.connection_timeout",
        defaultValue = "30000",
        comments = [
            "Connection timeout in milliseconds"
        ]
    )
    var connectionTimeout: Long = 30000

    @ConfigValue(
        path = "connection_pool.idle_timeout",
        defaultValue = "600000",
        comments = [
            "Idle connection timeout in milliseconds"
        ]
    )
    var idleTimeout: Long = 600000

    @ConfigValue(
        path = "performance.enable_cache",
        defaultValue = "true",
        comments = [
            "Enable query result caching for better performance"
        ]
    )
    var enableCache: Boolean = true

    @ConfigValue(
        path = "performance.cache_size",
        defaultValue = "1000",
        comments = [
            "Maximum number of cached query results"
        ]
    )
    var cacheSize: Int = 1000

    @ConfigValue(
        path = "logging.enable_sql_log",
        defaultValue = "false",
        comments = [
            "Enable SQL query logging for debugging"
        ]
    )
    var enableSqlLog: Boolean = false

    @ConfigValue(
        path = "logging.log_slow_queries",
        defaultValue = "true",
        comments = [
            "Log queries that take longer than the threshold"
        ]
    )
    var logSlowQueries: Boolean = true

    @ConfigValue(
        path = "logging.slow_query_threshold",
        defaultValue = "1000",
        comments = [
            "Threshold in milliseconds for logging slow queries"
        ]
    )
    var slowQueryThreshold: Long = 1000

    @ConfigValue(
        path = "auto_migration.enabled",
        defaultValue = "true",
        comments = [
            "Enable automatic database schema migration"
        ]
    )
    var autoMigrationEnabled: Boolean = true

    @ConfigValue(
        path = "auto_migration.backup_before_migration",
        defaultValue = "true",
        comments = [
            "Create backup before running migrations"
        ]
    )
    var backupBeforeMigration: Boolean = true
} 