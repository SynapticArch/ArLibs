# ArLibs 数据库模块使用指南

ArLibs 数据库模块提供了抽象化的数据库操作，支持 SQLite 和 MySQL 数据库，具有连接池、自动迁移、查询构建器等高级功能。

## 功能特性

- **多数据库支持**: SQLite 和 MySQL
- **连接池管理**: 基于 HikariCP 的高性能连接池
- **注解驱动**: 使用注解定义数据模型
- **自动迁移**: 自动创建和更新数据库表结构
- **查询构建器**: 流畅的 API 构建复杂查询
- **异步操作**: 支持异步数据库操作
- **性能监控**: 内置查询统计和慢查询日志
- **事务支持**: 完整的事务管理

## 快速开始

### 1. 配置数据库

在 `database.yml` 配置文件中设置数据库连接：

```yaml
# 数据库类型 (sqlite 或 mysql)
type: sqlite

# SQLite 配置
sqlite:
  file: database.db

# MySQL 配置
mysql:
  host: localhost
  port: 3306
  database: arlibs
  username: root
  password: ""

# 连接池配置
connection_pool:
  max_size: 10
  min_idle: 2
  connection_timeout: 30000
  idle_timeout: 600000

# 性能配置
performance:
  enable_cache: true
  cache_size: 1000

# 日志配置
logging:
  enable_sql_log: false
  log_slow_queries: true
  slow_query_threshold: 1000

# 自动迁移配置
auto_migration:
  enabled: true
  backup_before_migration: true
```

### 2. 定义实体类

使用注解定义数据模型：

```kotlin
@Entity(tableName = "players")
class Player {
    @PrimaryKey
    @Column(name = "uuid", type = "VARCHAR(36)")
    var uuid: String = ""
    
    @Column(name = "name", type = "VARCHAR(16)")
    var name: String = ""
    
    @Column(name = "level", type = "INT", defaultValue = "1")
    var level: Int = 1
    
    @Column(name = "join_date", type = "BIGINT")
    var joinDate: Long = 0L
}
```

### 3. 基本操作

```kotlin
// 保存实体
val player = Player().apply {
    uuid = "123e4567-e89b-12d3-a456-426614174000"
    name = "Steve"
    level = 10
    joinDate = System.currentTimeMillis()
}

DatabaseAPI.save(player)

// 查找实体
val foundPlayer = DatabaseAPI.find(Player::class.java, "123e4567-e89b-12d3-a456-426614174000")

// 删除实体
DatabaseAPI.delete(player)

// 异步操作
val deferred = DatabaseAPI.saveAsync(player)
val result = deferred.await()
```

### 4. 查询操作

使用查询构建器进行复杂查询：

```kotlin
// 基本查询
val players = DatabaseAPI.query(Player::class.java)
    .where("level > ?", 5)
    .orderBy("level", "DESC")
    .limit(10)
    .findAll()

// 条件组合
val highLevelPlayers = DatabaseAPI.query(Player::class.java)
    .where("level >= ?", 10)
    .and("join_date > ?", oneWeekAgo)
    .or("name LIKE ?", "%admin%")
    .findAll()

// 计数查询
val count = DatabaseAPI.query(Player::class.java)
    .where("level > ?", 5)
    .count()

// 异步查询
val deferred = DatabaseAPI.query(Player::class.java)
    .where("level > ?", 5)
    .findAllAsync()
val results = deferred.await()
```

### 5. 事务操作

```kotlin
// 同步事务
DatabaseAPI.transaction {
    val player1 = findPlayer("player1")
    val player2 = findPlayer("player2")
    
    player1.coins -= 100
    player2.coins += 100
    
    save(player1)
    save(player2)
}

// 异步事务
DatabaseAPI.transactionAsync {
    val player1 = findPlayer("player1")
    val player2 = findPlayer("player2")
    
    player1.coins -= 100
    player2.coins += 100
    
    save(player1)
    save(player2)
}
```

## 注解说明

### @Entity
标记类为数据库实体：

```kotlin
@Entity(tableName = "custom_table_name")
class MyEntity {
    // ...
}
```

### @PrimaryKey
标记主键字段：

```kotlin
@PrimaryKey(autoIncrement = true)
@Column(name = "id", type = "INT")
var id: Int = 0
```

### @Column
定义数据库列：

```kotlin
@Column(
    name = "custom_column_name",
    type = "VARCHAR(255)",
    nullable = false,
    defaultValue = "default_value"
)
var myField: String = ""
```

## 性能优化

### 1. 连接池配置

```yaml
connection_pool:
  max_size: 20          # 最大连接数
  min_idle: 5           # 最小空闲连接
  connection_timeout: 30000  # 连接超时
  idle_timeout: 600000      # 空闲超时
```

### 2. 查询缓存

```yaml
performance:
  enable_cache: true
  cache_size: 1000      # 缓存大小
```

### 3. 慢查询监控

```yaml
logging:
  log_slow_queries: true
  slow_query_threshold: 1000  # 慢查询阈值(毫秒)
```

## 监控和调试

### 1. 查看统计信息

```kotlin
val stats = DatabaseAPI.getStatistics()
println("总查询数: ${stats.getTotalQueries()}")
println("成功率: ${stats.getSuccessRate() * 100}%")
println("平均执行时间: ${stats.getAverageExecutionTime()}ms")
println("慢查询数: ${stats.getSlowQueries()}")
```

### 2. 启用 SQL 日志

```yaml
logging:
  enable_sql_log: true
```

### 3. 使用演示命令

ArLibs 提供了数据库演示命令：

```
/database setlanguage en     # 设置语言偏好
/database getlanguage        # 获取语言偏好
/database listlanguages      # 列出所有语言偏好
/database stats              # 显示数据库统计
/database test               # 测试数据库操作
```

## 最佳实践

### 1. 实体设计

- 使用有意义的表名和列名
- 为重要字段添加适当的约束
- 使用合适的数据类型
- 考虑索引优化

### 2. 查询优化

- 避免 SELECT *，只查询需要的字段
- 使用 LIMIT 限制结果集大小
- 合理使用索引
- 避免在循环中执行查询

### 3. 错误处理

```kotlin
try {
    val result = DatabaseAPI.save(entity)
    if (!result) {
        Logger.warn("Failed to save entity")
    }
} catch (e: Exception) {
    Logger.severe("Database operation failed: ${e.message}")
}
```

### 4. 异步操作

对于耗时操作，使用异步 API：

```kotlin
// 在协程中使用
coroutineScope {
    val deferred = DatabaseAPI.saveAsync(entity)
    val result = deferred.await()
    
    if (result) {
        // 处理成功
    } else {
        // 处理失败
    }
}
```

## 故障排除

### 1. 连接问题

- 检查数据库配置是否正确
- 确认数据库服务是否运行
- 验证用户名和密码
- 检查网络连接

### 2. 表创建失败

- 确认实体类有正确的注解
- 检查表名是否合法
- 验证列定义是否正确

### 3. 查询性能问题

- 启用慢查询日志
- 检查查询计划
- 优化索引
- 调整连接池配置

## 示例项目

完整的示例项目展示了如何使用数据库模块存储玩家语言偏好：

- 实体定义: `PlayerLanguage.kt`
- 演示命令: `DatabaseCommand.kt`
- 配置文件: `database.yml`

这个示例展示了完整的 CRUD 操作、查询构建、事务处理等功能。 