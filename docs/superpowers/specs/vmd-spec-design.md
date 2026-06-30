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

### §4.3 F3 EOL 解析联动生命周期 + 发布零件绑定事件（CR-035 修订）

**补偿绑定语义（CR-035）：**

EOL 解析器在绑定零件时采用补偿绑定语义：
- **TOL 已绑则幂等跳过**：若车辆在 TOL 阶段已绑定某位置的零件（`vehicle_part` active 状态），EOL 阶段不再重复绑定，仅回填零件细节
- **漏绑则补绑**：若 TOL 阶段未绑定（绑定记录不存在或非 active），EOL 阶段正常执行绑定
- **回填零件细节**：无论跳过或补绑，均将零件细节（软件版本、配置字/变体编码等）回填至 `part_info.extra` 字段

**自动建车兜底（CR-035）：**

EOL 解析时若 VIN 不存在（未经过 PRODUCE 建档），自动创建残档车辆：
- 输出 WARN 日志提示
- 创建的残档缺七项生产配置与选项值快照
- 需后续 PRODUCE 重导补全完整数据
- EOL 补发的 `VehicleProduceEvent` 不触发 per-VIN 安全常量预置

**事件订阅副作用（CR-035 修订）：**
- `VehicleLifecycleSubscribe.onProduce(event)` → `recordProduceNode(vin)`
- `VehicleLifecycleSubscribe.onEol(event)` → `recordEolNode(vin, event.eolTime)`
- `VehicleLifecycleSubscribe.onTol(event)` → `recordTolNode(vin, event.tolTime)`（CR-035 新增）

**对应 US**：US-019 ~ US-020、US-026。

### §4.5 F5 内部事件订阅链路总览（CR-035 修订）

**事件发布（应用层）：**
- `VehiclePublish` → `produce` / `eol` / `eolPartBound` / `tol`（CR-035 新增 `tol`）

**事件类型：**
- `VehicleProduceEvent` - 车辆生产建档事件
- `VehicleEolEvent` - 车辆下线事件
- `VehicleEolPartBoundEvent` - EOL 零件绑定事件
- `VehicleTolEvent` - 总装上线事件（CR-035 新增）

**订阅者：**
- `VehicleLifecycleSubscribe`：
  - `onProduce` → PRODUCE 节点
  - `onEol` → EOL 节点
  - `onTol` → TOL 节点（CR-035 新增，写入总装上线生命周期节点）
- `VehicleEolTspOtaSubscribe`：
  - `@Async onEolPartBound` → 发布 `VehiclePartBindingChangedEvent`

**对应 US**：US-026、US-019（PRODUCE 事件）、US-020（EOL 事件 + 零件绑定事件）、US-042（TOL 事件）。

## §6 Coverage Mapping（CR-035 修订）

| US | 实体/功能 | 设计锚点 |
|----|----------|----------|
| US-018 | 零件实例后台导入（入口②） | §2 D14 / §3.1 导入域(veh_import_data) / §4.2 F2 |
| US-020 | EOL 解析器（CR-035 重构） | §4.3 F3 / §4.5 F5 / §2 D15（车辆数据导入域）/ §3.1 导入域(veh_import_data) |
| US-026 | VehicleLifecycle（CR-035 新增 TOL 节点） | §3.2 枚举 / §4.5 F5 / §4.3 F3（onTol 事件订阅） |
| US-040 | 整车主档后台导入（type=PRODUCE） | §4.2 F2 / §3.2 VehiclePublish.produce / §4.5 F5 / §3.1 导入域(veh_import_data) |
| US-042 | 总装上线 ECU 零件清单导入与 VIN 绑定（TOL 导入类型） | §2 D15/D17 / §3.1 导入域(veh_import_data type=TOL) / §4.2 F2 / §4.9 F9 / §4.5 F5（TOL 事件） |
