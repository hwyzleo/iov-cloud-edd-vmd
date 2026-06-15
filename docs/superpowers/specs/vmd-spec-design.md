# VMD 设计文档

> 本文档记录 VMD（Vehicle Master Data）系统的设计决策和规范。

## §2 领域设计

### §2 D8 导入解析器 SPI——分发口径补注

**CR-025 变更：**
- PRODUCE 类型由 US-040 独立处理，不再与其他零件类型混装分发
- 分发逻辑更新：
  - PRODUCE → US-040 VehicleProduceDataParserV1_0
  - EOL/SIM/TBOX/BTM/CCP/CPT_DCU → 现有解析器

### §2 D14 零件实例数据入站——类型来源=Part投影、人工类型（source=MANUAL）下线

**CR-025 变更：**
- 零件类型（part_type）一律取自 Part 只读投影（partCode -> part.pn）
- 人工录入类型（source=MANUAL）下线
- 此处「类型来源」下线区别于入站来源枚举 InboundSourceType 的 MANUAL
- 二者取值域独立、不混用

## §3 数据模型

### §3.1 导入域（veh_import_data）

- 主键：id
- 唯一键：batch_num
- 字段：type, version, data, handle, created_at, updated_at

### §3.2 VehiclePublish.produce

- 整车主档发布数据结构
- 包含 VIN、零件列表等信息

## §4 功能设计

### §4.2 F2 导入数据解析——「对应 US」行 + 注（CR-025）

**对应 US：** US-018 ~ US-025、US-040

**注（CR-025）：**
- US-018 收敛为零件实例数据后台导入（入口②）
- 按 partCode -> part.pn（MDM Part 投影）确定零件与类型
- source=MANUAL 人工录入类型下线
- 整车主档（type=PRODUCE）批量导入拆为独立 US-040
- 复用本 SPI 框架分发至 US-019 PRODUCE 解析器（VehicleProduceEvent 契约不变）
- veh_import_data（UK batch_num）结构、ImportResult 计数不变

### §4.5 F5 整车发布

- 整车主档发布功能
- 包含 PRODUCE 类型处理

## §6 Coverage Mapping

| US | 实体/功能 | 设计锚点 |
|----|----------|----------|
| US-018 | 零件实例后台导入（入口②） | §2 D14 / §3.1 导入域(veh_import_data) / §4.2 F2 |
| US-040 | 整车主档后台导入（type=PRODUCE） | §4.2 F2 / §3.2 VehiclePublish.produce / §4.5 F5 / §3.1 导入域(veh_import_data) |
