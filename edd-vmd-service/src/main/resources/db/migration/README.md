# Flyway 数据库版本管理说明

## 配置概述

本项目已集成 Flyway 进行数据库版本管理，采用 **baseline-on-migrate** 机制，确保首次启动时不会影响已有的数据库表结构和数据。

## 关键配置

在 `bootstrap.yml` 中已配置以下关键参数：

```yaml
spring:
  flyway:
    enabled: true                      # 启用 Flyway
    baseline-on-migrate: true          # 对已有数据库自动创建 baseline
    baseline-version: 0                # baseline 版本号设置为 0
    locations: db/migration            # 迁移脚本位置
    table: flyway_schema_history       # Flyway 历史记录表名
    validate-on-migrate: true          # 迁移前验证脚本
```

## Baseline 机制说明

### 对已有数据库的处理

当项目首次在有表结构的数据库上启动时：

1. **Flyway 检测到数据库已有表结构**
2. **自动创建 baseline**：将当前数据库状态标记为版本 `V0`
3. **不会执行 `V0__Baseline.sql`**：因为该版本已被 baseline 标记
4. **不会影响现有数据**：所有表结构和数据保持不变
5. **后续迁移从 V1 开始执行**：只会执行版本号大于 0 的脚本

### 对新数据库的处理

当项目在新环境（无表结构）启动时：

1. **执行 `V0__Baseline.sql`**：创建所有表结构
2. **记录到 `flyway_schema_history` 表**：标记版本 0 已执行
3. **后续迁移正常执行**：按照版本号顺序执行后续脚本

## 迁移脚本命名规范

迁移脚本必须遵循 Flyway 命名规范：

```
V{版本号}__{描述}.sql
```

### 示例

- `V0__Baseline.sql` - 基线版本（已创建）
- `V1__Add_user_table.sql` - 第 1 版变更
- `V2__Add_column_to_vehicle.sql` - 第 2 版变更
- `V3__Update_supplier_table.sql` - 第 3 版变更

### 重要规则

1. **版本号必须递增**：每个新脚本的版本号必须大于前一个
2. **版本号不能重复**：已执行的版本号不能再次使用
3. **文件名不能修改**：已执行的脚本文件名不能修改
4. **脚本内容不能修改**：已执行的脚本内容不能修改

## 如何添加新的迁移

### 步骤

1. **确定版本号**：查看现有脚本，确定下一个版本号
2. **创建脚本文件**：在 `db/migration` 目录下创建新脚本
3. **编写 SQL**：使用 `CREATE TABLE IF NOT EXISTS`、`ALTER TABLE` 等语句
4. **启动应用**：Flyway 自动执行新脚本

### 示例：添加新字段

```sql
-- V1__Add_vehicle_status_column.sql
ALTER TABLE tb_veh_basic_info 
ADD COLUMN IF NOT EXISTS `vehicle_status` VARCHAR(20) DEFAULT NULL COMMENT '车辆状态';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_vehicle_status ON tb_veh_basic_info(vehicle_status);
```

## Flyway 历史记录表

Flyway 会自动创建 `flyway_schema_history` 表，记录所有迁移执行情况：

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

字段说明：
- `installed_rank` - 执行顺序
- `version` - 版本号
- `description` - 描述
- `type` - 类型（SQL、JAVA 等）
- `script` - 脚本文件名
- `checksum` - 校验和
- `installed_on` - 执行时间
- `execution_time` - 执行耗时
- `success` - 是否成功

## 最佳实践

### 1. 安全性

- ✅ 使用 `IF NOT EXISTS` 创建表和索引
- ✅ 使用 `IF EXISTS` 删除表和索引
- ✅ 使用 `ADD COLUMN IF NOT EXISTS` 添加字段
- ❌ 避免 `DROP TABLE`（除非明确需要）
- ❌ 避免 `TRUNCATE TABLE`（会清空数据）

### 2. 可重复性

- ✅ 硓保脚本可以多次执行
- ✅ 使用条件判断避免重复操作
- ✅ 测试脚本在新旧环境的执行效果

### 3. 版本管理

- ✅ 脚本文件纳入 Git 版本控制
- ✅ 每次数据库变更都创建新脚本
- ✅ 脚本描述清晰明确

### 4. 团队协作

- ✅ 团队成员共享迁移脚本
- ✅ 避免本地创建未提交的脚本
- ✅ 代码审查脚本变更

## 当前状态

### 已创建脚本

- `V0__Baseline.sql` - 包含项目所有现有表结构（38 个表）

### 下一步

1. 后续表结构变更从 `V1` 开始编号
2. 在 `db/migration` 目录创建新的迁移脚本
3. 启动项目，Flyway 自动执行新迁移

## 故障排查

### 问题：Flyway 执行失败

**检查步骤：**

1. 查看 `flyway_schema_history` 表的 `success` 字段
2. 检查脚本语法是否正确
3. 确认脚本版本号是否递增
4. 检查数据库连接是否正常

### 问题：脚本已执行但需修改

**解决方案：**

1. **不修改已执行脚本**（违反 Flyway 规则）
2. 创建新的修复脚本（新版本号）
3. 或手动修复数据库后，清理 Flyway 历史

### 问题：需要回滚迁移

**说明：**

Flyway 不支持自动回滚，需要：

1. 手动编写回滚 SQL
2. 创建新迁移脚本执行回滚操作

## 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [Spring Boot Flyway 集成](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)