# 车系品牌关联重构设计文档

## 概述

**变更目标:** 调整车系(Series)与平台(Platform)、品牌(Brand)的关联关系，使车系选择品牌而非平台，车型(Model)的平台代码成为独立输入参数。

**变更日期:** 2025-05-11

## 背景

当前系统中：
- 车系(Series)关联平台(Platform)，字段为 `platformCode`
- 车型(Model)已有 `platformCode` 和 `seriesCode`，但平台代码从车系继承

需求变更：
- 车系改为关联品牌(Brand)，字段改为 `brandCode`
- 车型的平台代码成为独立输入，不从车系继承
- 品牌与平台为独立实体，无关联关系

## 设计决策

### 方案选择

采用 **方案1: 最小改动** - 快速、干净、最小风险，不添加校验逻辑。

**关键决策:**
- Brand与Platform独立存在，无关联关系
- 现有车系数据迁移由用户手动处理
- 车系代码格式保持不变

## 数据库设计

### Series表变更 (tb_veh_series)

```sql
-- 移除
ALTER TABLE tb_veh_series DROP COLUMN platform_code;

-- 新增
ALTER TABLE tb_veh_series ADD COLUMN brand_code VARCHAR(32) COMMENT '品牌代码';

-- 添加索引
CREATE INDEX idx_series_brand_code ON tb_veh_series(brand_code);
```

### Model表变更 (tb_veh_model)

无需变更 - 已有 `platform_code` 和 `series_code` 字段。

### 迁移顺序

1. 先添加 `brand_code` 列 (允许用户手动填充数据)
2. 用户确认数据准备完成后移除 `platform_code` 列

## 实体与DTO变更

### Series相关文件

| 文件路径 | 变更内容 |
|----------|----------|
| `domain/model/entity/Series.java` | 移除 `platformCode`，新增 `brandCode` |
| `infrastructure/persistence/po/VehSeriesPo.java` | 移除 `platformCode`，新增 `brandCode` |
| `application/dto/cmd/SeriesCmd.java` | 移除 `platformCode`，新增 `brandCode` |
| `application/dto/result/SeriesDto.java` | 移除 `platformCode`，新增 `brandCode` |
| `application/dto/query/SeriesQuery.java` | 移除 `platformCode`，新增 `brandCode` |
| `adapter/web/vo/request/SeriesRequest.java` | 移除 `platformCode`，新增 `brandCode` |
| `adapter/web/vo/response/SeriesResponse.java` | 移除 `platformCode`，新增 `brandCode` |
| `resources/mappers/VehSeriesMapper.xml` | 所有 `platform_code` 改为 `brand_code` |

### Model相关文件

无需变更 - 实体已有 `platformCode` 和 `seriesCode`。

## 服务层变更

### SeriesAppService

- `search()` 方法：查询参数从 `platformCode` 改为 `brandCode`
- 其他方法无需变更

### ModelAppService

无需变更 - `platformCode` 已为独立输入。

## API变更

### Series API

| 原接口 | 新接口 |
|--------|--------|
| `/api/mpt/series/v1/list` 参数 `platformCode` | 参数改为 `brandCode` |
| `/api/mpt/series/v1/listByPlatformCode` | 改为 `/api/mpt/series/v1/listByBrandCode` |

### Model API

无需变更 - `/api/mpt/model/v1/listByPlatformCodeAndSeriesCode` 已接受两个独立参数。

## 受影响文件清单

```
# Series 层
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/domain/model/entity/Series.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/infrastructure/persistence/po/VehSeriesPo.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/cmd/SeriesCmd.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/result/SeriesDto.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/query/SeriesQuery.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/request/SeriesRequest.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/response/SeriesResponse.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/service/SeriesAppService.java
edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/controller/mpt/MptSeriesController.java
edd-vmd-service/src/main/resources/mappers/VehSeriesMapper.xml
```

## 数据兼容性

- 现有车系数据需用户手动关联品牌
- 车型数据无需迁移，平台代码已是独立字段
- MapStruct assembler 自动映射，无需手动修改

## 测试要点

1. 车系创建时必须选择品牌
2. 车系查询可按品牌过滤
3. 车型创建时平台和车系为独立输入
4. 车型查询可按平台和车系组合过滤