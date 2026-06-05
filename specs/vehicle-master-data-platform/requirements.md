# Vehicle Master Data Platform - Requirements

> 本文档由现有代码（commit @ 2026-05-23）+ graphify 知识图谱逆向生成，作为 SSOT 起点。
> 任何后续变更必须遵循 SPEC_GUIDE §6 变更管理规则，禁止直接改代码。

## 1. Overview

`edd-vmd`（车辆主数据 / Vehicle Master Data）是开源车联网（OpenIOV）云端企业数字底座的核心微服务，沉淀从产品定义（品牌→车系→车型→基础车型→生产配置）到物理实例（车辆→零件→设备）再到生命周期事件（生产→密钥→证书→下线→合格证→订单→PDI→交付）的完整车辆主数据，并对外提供管理后台运营能力，以及供下游微服务消费的内部 RPC 契约。

## 2. Background & Goals

### 背景
- 车联网平台需要一份"车辆"的权威事实来源（VIN→车系车型→零件设备→各阶段时间戳），其他业务域（TSP、OTA、IDK、安全密钥、订单）以此为锚点开展自身业务。
- 整车生产、下线、密钥、零部件四类离线数据来自 MES/SIM 供应商/Tier1 供应商，需要异步导入并触发跨域事件。

### 目标（Goals）
- G1：成为车辆全要素主数据（产品树、物理车、零件设备、生命周期）的 SSOT。
- G2：通过统一 Feign 契约（`Vmd*Service`）对外暴露车辆/零件/设备/车型配置/生命周期五类能力。
- G3：支持 6 类（PRODUCE/EOL/BTM/CCP/IDCM/TBOX/SIM）批量数据导入并下钻到 TSP/OTA/IDK 等下游服务。
- G4：对管理后台提供完整 CRUD + 鉴权（`completeVehicle:*` / `iov:configCenter:*` 权限点）能力。
- G5：在产品树（品牌/车系/平台）主数据上，VMD 作为 edd-mdm 的下游消费方，持有本地投影副本；其中 Brand 本地投影为**只读**视图，VMD 消费 MDM Brand 主数据，通过 Brand 投影支撑车辆查询、产品树关联、导入校验和历史追溯，`brandCode` 作为车辆主档与产品树的品牌关联编码长期保留，VMD 不再承担 Brand 主数据维护职责（CR-012）。
- G6：在工厂（Plant）主数据上，VMD 作为 edd-mdm 的下游消费方，持有 Plant 本地投影副本，用于车辆生产工厂追溯；车辆主档使用 `plantCode` 表示生产工厂编码，VMD 不再承担 Plant 主数据治理职责（CR-011）。

> **Plant / 工厂主数据语义统一（CR-011 补充）**：
> - VMD 中 Plant 本地投影用于支撑车辆生产工厂追溯，不再承担 Plant 主数据治理职责。
> - VMD 与 MDM 在工厂主数据命名上统一使用 **Plant**（MDM 侧实体与 VMD 侧本地投影同名）。
> - VMD 车辆主档使用 `plantCode` 表示生产工厂编码。
> - 历史 `manufacturerCode` 作为兼容字段或迁移来源处理，不作为长期新字段继续扩展。
> - VMD Plant 是面向车辆主数据上下文（bounded context）的消费型只读投影，不是 MDM Plant 的完整副本/镜像表。
> - VMD Plant 投影字段以车辆生产工厂追溯、导入校验、查询展示和运行时解耦为边界。

> **Brand / 品牌主数据语义统一（CR-012 补充）**：
> - Brand 主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 Brand 本地投影副本。
> - VMD Brand 本地投影面向车辆主数据上下文（bounded context），用于车辆主数据查询、车辆详情展示、导入校验、产品树关联、历史追溯，以及 MDM 不可用时的降级查询，不是 MDM Brand 的完整副本/镜像表。
> - VMD 车辆主档与产品树继续使用 `brandCode` 作为品牌关联编码长期保留，不因维护权迁移而改名或删除。
> - VMD 不再承担 Brand 主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - VMD Brand 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

### 非目标（Non-Goals，本期不做）
- N1：不替代账号服务（`ExAccountService`）做用户身份/手机号实名核验。
- N2：不替代安全密钥服务（`ExSkService`）执行 IMMO_SK 的实际生成。
- N3：不实现 V2.0+ 解析器（当前仅 V1.0）。
- N4：不再充当品牌/车系/平台/**Plant（工厂）**的企业级 SSOT；不实施跨系统主数据治理（Golden Record / 审批工作流 / 数据质量打分 / 编码规则生成 / 生命周期管理）。
- N5：不再作为 **Brand（品牌）**主数据的企业级 SSOT；VMD 不负责 Brand 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与品牌生命周期管理；不要求完整复制 MDM Brand 的全部字段；不承担 MDM Brand 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD Brand 投影（CR-012）。

## 3. User Stories

> 角色定义（贯穿全文）：
> - **Mpt-User**：管理后台运营/工程师，持有 `completeVehicle:*` 或 `iov:configCenter:*` 权限点。
> - **Service-Caller**：内部微服务（OTA/TSP/账号/订单等）通过 Feign 调用方。
> - **System**：`edd-vmd` 自身后台异步流程。

### 3.1 产品主数据维护域

#### US-001: 消费 MDM Brand 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Brand 主数据并维护本地 Brand 投影表, **so that** 每台车辆可通过 `brandCode` 关联品牌信息，同时 VMD 不再承担 Brand 主数据维护职责。

> **语义重构（CR-012）**：本 US 由原「US-001 维护车辆品牌（Brand）」演进而来。Brand 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Brand 本地投影副本。VMD Brand 投影为 MDM Brand 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Brand 主数据字段完全一致（字段范围见 §4「Brand 投影字段范围原则」）。`brandCode` 作为车辆主档与产品树的品牌关联编码长期保留。VMD Brand 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-001c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 BrandCreated / BrandUpdated / BrandDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Brand 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Brand 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段，不要求 VMD Brand 投影表结构与 MDM Brand 主数据模型完全一致。
- WHEN MDM Brand 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Brand 投影表结构。
- WHEN MDM Brand 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Brand 投影模型。
- WHEN VMD 本地 Brand 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `brandCode` 字段，用于车辆品牌关联和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Brand 投影数据展示或关联品牌信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Brand 投影数据支撑车辆查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `brandCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Brand 信息缺失。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:brand:list/query/export` 权限点；`completeVehicle:product:brand:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝，并规划后续兼容性清理 CR 下线。

#### US-001b: Bootstrap 时从 MDM 全量同步 Brand 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Brand 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Brand 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Brand 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Brand 全量快照接口拉取 Brand 数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=brand` THE SYSTEM SHALL 调用 MDM Brand 全量快照接口拉取数据并 upsert 本地 Brand 投影副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Brand 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。
- THE SYSTEM SHALL 不因 MDM Brand 快照接口失败而删除或清空本地已有 Brand 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Brand 投影所需字段，不要求同步 MDM Brand 的完整字段集。

#### US-001c: Brand 本地维护能力兼容清理
**As a** System, **I want** 将 VMD 现有 Brand 本地维护能力逐步收敛为只读投影能力, **so that** Brand 主数据维护职责统一回归 MDM，同时历史 source=MANUAL 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 MDM Brand 投影语义，不再将 VMD Brand 视为权威主数据。
- WHEN 历史 Brand 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Brand 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述 Brand 维护能力 THE SYSTEM SHALL 明确 VMD Brand add/edit/remove 为兼容期遗留能力，不作为长期能力继续扩展。
- THE SYSTEM SHALL 规划后续兼容性清理 CR，逐步下线或隐藏 VMD Brand 本地维护入口、旧权限点和相关后台操作。
- THE SYSTEM SHALL 保留 Brand 查询能力，包括列表、详情、listAll 或车辆详情展示所需查询。
- THE SYSTEM SHALL 保留 `brandCode` 字段，不因维护权迁移而改名或删除。

#### US-002: 维护车系（CarLine）
**As a** Mpt-User, **I want** 维护车系并按品牌过滤, **so that** 形成"品牌→车系"产品树。

**Acceptance Criteria**:
- WHEN Mpt-User 调用 `GET /api/mpt/carLine/v1/listByBrandCode?brandCode=<x>` THE SYSTEM SHALL 返回该品牌下全部车系（不分页）。
- WHEN 删除某车系 IF 其下存在车型 OR 存在车辆 THEN THE SYSTEM SHALL 拒绝删除并提示"该车系下存在车型/车辆"。
- THE SYSTEM SHALL 在车系数据中保存 `brandCode` 冗余字段以支持跨域回查（参见迁移脚本 `V2__CarLine_brand_code_migration.sql`）。
- WHEN MDM 通过 Kafka 推送 CarLineCreated / CarLineUpdated / CarLineDeleted 事件 THE SYSTEM SHALL upsert 本地副本，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件（乱序处理）。
- IF 记录的 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 的 add / edit / delete 操作并返回明确错误。
- WHEN MPT 操作 source=MANUAL 的记录 THE SYSTEM SHALL 维持现有 CRUD 行为不变。

#### US-002b: Bootstrap 时从 MDM 全量同步车系数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步车系数据, **so that** 首次接入或数据丢失后可恢复一致性。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 车系记录数为 0 THE SYSTEM SHALL 自动调用 MDM 全量快照接口拉取车系数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=carLine` THE SYSTEM SHALL 调用 MDM 全量快照接口拉取车系数据并 upsert 本地副本（不删除本地记录）。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。

#### US-003: 维护车型（Model）
**As a** Mpt-User, **I want** 维护车型并按"平台+车系"过滤, **so that** 在产品树中精确定位车型层。

**Acceptance Criteria**:
- WHEN 调用 `GET /api/mpt/model/v1/listByPlatformCodeAndCarLineCode` THE SYSTEM SHALL 返回平台+车系交集下的全部车型。
- WHEN 删除某车型 IF 其下存在基础车型 OR 存在车辆 THEN THE SYSTEM SHALL 拒绝删除。
- WHEN 创建/修改车型 IF `code` 已存在（不含自身 ID）THEN THE SYSTEM SHALL 返回唯一性失败。

#### US-004: 维护基础车型（BaseModel）及其特征值（BaseModelFeatureCode）
**As a** Mpt-User, **I want** 维护基础车型、按"平台+车系+车型"过滤、为基础车型挂特征族特征值, **so that** 形成生产配置上游的完整"车型→基础车型→特征值"骨架。

**Acceptance Criteria**:
- WHEN 调用 `GET /api/mpt/baseModel/v1/listByPlatformCodeAndCarLineCodeAndModelCode` THE SYSTEM SHALL 支持 `platformCode/carLineCode/modelCode` 三参数任意组合查询。
- WHEN Mpt-User 在基础车型下新增/修改特征值 IF 同一基础车型下同一 `familyCode` 已存在 THEN THE SYSTEM SHALL 返回"基础车型特征值已存在"。
- WHEN 删除基础车型 IF 其下存在生产配置 OR 存在车辆 THEN THE SYSTEM SHALL 拒绝删除。

#### US-005: 维护生产配置（BuildConfig）及其特征值（BuildConfigFeatureCode）
**As a** Mpt-User, **I want** 维护生产配置（CRUD）、按基础车型查询、维护配置下的特征值, **so that** 每台物理车辆都能映射到一个唯一 `buildConfigCode`。

**Acceptance Criteria**:
- WHEN 调用 `GET /api/mpt/buildConfig/v1/listByBaseModelCode/{baseModelCode}` THE SYSTEM SHALL 返回该基础车型下全部生产配置。
- WHEN 删除某生产配置 IF 其下已存在车辆 THEN THE SYSTEM SHALL 拒绝删除并返回"该生产配置下存在车辆"。
- WHEN 在某生产配置下新增/修改特征值 IF 同 `familyCode` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。

#### US-006: 维护车辆平台（Platform）
**As a** Mpt-User, **I want** 维护车辆平台并 `listAll`, **so that** 平台作为车系/车型/车辆的横向维度。

**Acceptance Criteria**:
- WHEN 删除某平台 IF 其下存在车系 OR 存在车辆 THEN THE SYSTEM SHALL 拒绝删除。
- WHEN 创建/修改平台 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。
- WHEN MDM 通过 Kafka 推送 PlatformCreated / PlatformUpdated / PlatformDeleted 事件 THE SYSTEM SHALL upsert 本地副本，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件（乱序处理）。
- IF 记录的 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 的 add / edit / delete 操作并返回明确错误。
- WHEN MPT 操作 source=MANUAL 的记录 THE SYSTEM SHALL 维持现有 CRUD 行为不变。

#### US-006b: Bootstrap 时从 MDM 全量同步平台数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步平台数据, **so that** 首次接入或数据丢失后可恢复一致性。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 平台记录数为 0 THE SYSTEM SHALL 自动调用 MDM 全量快照接口拉取平台数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=platform` THE SYSTEM SHALL 调用 MDM 全量快照接口拉取平台数据并 upsert 本地副本（不删除本地记录）。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。

#### US-007: 消费 MDM Plant 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Plant 主数据并维护本地 Plant 投影表, **so that** 每台车辆可通过 `plantCode` 追溯到生产工厂，同时 VMD 不再承担 Plant 主数据维护职责。

> **命名迁移（CR-011）**：本 US 由原「US-007 维护生产厂商（Manufacturer）」演进而来。Plant 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Plant 本地投影副本。Manufacturer / manufacturerCode 自此为历史兼容命名，新能力统一使用 Plant / plantCode（迁移与兼容策略见 US-007c）。VMD Plant 投影为 MDM Plant 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Plant 主数据字段完全一致（字段范围见 §4「Plant 投影字段范围原则」）。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 PlantCreated / PlantUpdated / PlantDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Plant 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Plant 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段，不要求 VMD Plant 投影表结构与 MDM Plant 主数据模型完全一致。
- WHEN MDM Plant 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Plant 投影表结构。
- WHEN MDM Plant 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Plant 投影模型。
- WHEN VMD 本地 Plant 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 写入 `plantCode` 字段，用于车辆生产工厂追溯。
- WHEN 历史车辆数据仍存在 `manufacturerCode` THEN THE SYSTEM SHALL 通过迁移脚本、兼容字段或映射逻辑保证历史车辆可继续查询和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Plant 投影数据展示或关联生产工厂信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Plant 投影数据支撑车辆查询和历史追溯（不对 MDM 形成运行时强依赖）。
- IF 本地不存在对应 `plantCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Plant 信息缺失。
- IF 历史数据仅存在 `manufacturerCode` 且未完成字段迁移 THEN THE SYSTEM SHALL 支持 `manufacturerCode` 到 `plantCode` 的兼容读取或迁移处理。
- WHEN VMD 启动时检测本地 source=MDM 的 Plant 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Plant 全量快照接口拉取 Plant 数据并 upsert 本地副本。
- WHEN Mpt-User 调用手工 Bootstrap 接口并指定 `entity=plant` THE SYSTEM SHALL 调用 MDM Plant 全量快照接口拉取 Plant 数据并 upsert 本地 Plant 投影副本，不删除本地已有记录。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:plant:list/query/export` 权限点；`completeVehicle:product:plant:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝，并规划后续兼容性清理 CR 下线。

#### US-007b: Bootstrap 时从 MDM 全量同步 Plant 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Plant 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Plant 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Plant 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Plant 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=plant` THE SYSTEM SHALL 调用 MDM Plant 全量快照接口拉取数据并 upsert 本地 Plant 投影副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Plant 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。
- THE SYSTEM SHALL 不因 MDM Plant 快照接口失败而删除或清空本地已有 Plant 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Plant 投影所需字段，不要求同步 MDM Plant 的完整字段集。

#### US-007c: Manufacturer 到 Plant 的兼容迁移
**As a** System, **I want** 将 VMD 现有 Manufacturer 命名与 `manufacturerCode` 字段逐步迁移为 Plant / `plantCode`, **so that** VMD 与 MDM 在工厂主数据语义上保持一致，同时历史数据和既有调用方不受影响。

**Acceptance Criteria**:
- WHEN 执行数据库迁移 THE SYSTEM SHALL 将原 `veh_manufacturer` 表迁移或重命名为 `veh_plant`。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 为车辆主档（`veh_basic_info`）新增 `plant_code` 字段，承接原 `manufacturer_code` 的语义。
- WHEN 历史车辆记录存在 `manufacturer_code` THE SYSTEM SHALL 将其值迁移或回填到 `plant_code`。
- WHEN 迁移期间仍存在旧接口或旧字段调用 THE SYSTEM SHALL 提供兼容策略（兼容读取 / 字段映射 / 旧接口保留），避免既有调用方立即失败。
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 Plant / `plantCode` 命名。
- WHEN 文档描述历史兼容逻辑 THE SYSTEM SHALL 明确 Manufacturer / `manufacturerCode` 为遗留命名，不再作为新能力命名。
- THE SYSTEM SHALL 在迁移完成后逐步废弃 Manufacturer 命名的 Controller / AppService / Repository / DTO / VO / API path。
- THE SYSTEM SHALL 将原 `completeVehicle:product:manufacturer:*` 权限点调整为 `completeVehicle:product:plant:list/query/add/edit/remove/export`；原 manufacturer 权限点如仍需兼容应标记 `deprecated`，并规划后续下线。
- THE SYSTEM SHALL 将 Manufacturer 到 Plant 的重命名影响纳入本次 CR（CR-011）的兼容性说明，旧字段与旧接口的最终下线由后续兼容性清理 CR 完成。

### 3.2 特征族 & 配置项域

#### US-008: 维护特征族（FeatureFamily）及其特征值（FeatureCode）
**As a** Mpt-User, **I want** 维护特征族（含 `type`）和族下特征值, **so that** 基础车型/生产配置可引用。

**Acceptance Criteria**:
- WHEN 调用 `GET /api/mpt/featureFamily/v1/listAllFeatureCode?familyCode=<x>` THE SYSTEM SHALL 返回该族下全部特征值（不分页）。
- WHEN 创建/修改特征族 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。
- WHEN 创建/修改特征值 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。
- WHEN 删除特征族 THE SYSTEM SHALL 同步可删除其下特征值（按 ID 数组）。

#### US-009: 维护配置项（ConfigItem）+ 枚举值（Option）+ 映射（Mapping）
**As a** Mpt-User, **I want** 配置项 CRUD、枚举值 CRUD、上下游映射 CRUD, **so that** 不同来源系统的字段能在 VMD 内完成翻译。

**Acceptance Criteria**:
- THE SYSTEM SHALL 提供 `GET /api/mpt/configItem/v1/listAll` 返回全部配置项。
- WHEN 操作枚举值 / 映射 THE SYSTEM SHALL 通过 `configItemCode` 路径定位归属配置项。
- WHEN 创建配置项 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。

### 3.3 物理车辆登记域

#### US-010: 车辆基础信息查询/删除/导出
**As a** Mpt-User, **I want** 分页查询车辆（按 VIN/buildConfigCode/时间窗口）、按 VIN 查询、按 ID 批量删除, **so that** 运维诊断和数据治理可执行。

**Acceptance Criteria**:
- WHEN Mpt-User 调用 `GET /api/mpt/vehicle/v1/list` THE SYSTEM SHALL 支持 `vin/buildConfigCode/beginTime/endTime` 过滤并分页（`startPage`+`getPageResult`），且 `vin` 走模糊匹配（`ParamHelper.fuzzyQueryParam`）。
- WHEN 调用 `GET /api/mpt/vehicle/v1/vin/{vin}` THE SYSTEM SHALL 返回完整车辆 DTO（包括 `Vehicle` 聚合视图）。
- WHEN 删除车辆 THE SYSTEM SHALL 联动调用 `vehicleLifecycleAppService.deleteVehicleLifecycleByVin(vin)` 物理删除其生命周期记录。
- THE SYSTEM SHALL 拒绝在内存中分页（强制 SQL 下沉，遵守 PROJECT_GUIDE 反向模式）。

#### US-011: 内部服务按 VIN 查询车辆
**As a** Service-Caller, **I want** 通过 Feign `VmdVehicleService.getByVin(vin)`, **so that** 下游服务可拿到 `VehicleExResponse`。

**Acceptance Criteria**:
- THE SYSTEM SHALL 通过 `GET /api/service/vehicle/v1/{vin}` 路径暴露此能力。
- THE SYSTEM SHALL 通过 `VmdVehicleServiceFallbackFactory` 提供 fallback 兜底。
- IF VIN 不存在 THEN THE SYSTEM SHALL 抛出 `VehicleNotExistException`（错误码 `VmdErrorCode.VEHICLE_NOT_EXIST`，即 `202001`），由 `GlobalExceptionHandler` 统一捕获并返回 `ApiResponse.fail`（`code=202001, message=车辆不存在`）。此设计遵循 fail-fast 原则，下游调用方可通过统一异常处理感知"VIN 不存在"与"服务故障"的区别。

#### US-012: 车辆订单绑定
**As a** Service-Caller, **I want** 通过 `POST /api/service/vehicle/v1/{vin}/action/bindOrder`（Body=`VehicleOrderExRequest`）绑定订单, **so that** 推进车辆生命周期到 `ORDER_BIND` 节点。

**Acceptance Criteria**:
- WHEN 调用方提交订单绑定请求 IF 该 VIN 已绑定订单 THEN THE SYSTEM SHALL 抛出 `VehicleHasBindOrderException`。
- WHEN 绑定成功 THE SYSTEM SHALL 持久化 `Vehicle.orderNum` 并写入 `VehicleLifecycleNodeEnum.ORDER_BIND` 节点。
- THE SYSTEM SHALL 通过 `VehicleOrderExRequest` 校验订单号（`@Validated`）。

### 3.4 车辆配置域

#### US-013: 维护车辆配置版本与配置项（VehicleConfig / VehicleConfigItem）
**As a** Mpt-User, **I want** 分页查询车辆配置（按 vin/version/时间）、按 VIN+版本看配置项, **so that** 在配置中心定位某车某版本的配置全貌。

**Acceptance Criteria**:
- WHEN 调用 `GET /api/mpt/vehicleConfig/v1/list` THE SYSTEM SHALL 按 `vin/version/beginTime/endTime` 过滤并分页。
- WHEN 调用 `GET /api/mpt/vehicleConfig/v1/{vin}/configItem/list` THE SYSTEM SHALL 返回该 VIN 在指定版本下的全部配置项并分页。
- THE SYSTEM SHALL 校验 `iov:configCenter:vehicleConfig:list/query/export` 权限。

### 3.5 零件 / 设备 / 供应商域

#### US-014: 维护零件信息（Part）
**As a** Mpt-User, **I want** 零件 CRUD、按 `key/pn/name/type/deviceCode` 过滤, **so that** 物料档案完整可控。

**Acceptance Criteria**:
- WHEN 创建零件 IF `pn` 已存在 THEN THE SYSTEM SHALL 返回"零件号已存在"。
- THE SYSTEM SHALL 通过 `GET /api/service/part/v1/{pn}` 对外暴露按零件号查询。
- THE SYSTEM SHALL 通过 `GET /api/service/part/v1/listAllFota?software=true|false|null` 返回全部可 FOTA 升级零件（按软硬件维度过滤）。

#### US-015: 维护设备信息（Device）
**As a** Mpt-User, **I want** 设备 CRUD、`listAllDeviceItem` 返回 `DeviceItem` 枚举、`listAllDevice` 列出全部设备, **so that** 在零件/车辆零件场景关联到具体设备类型。

**Acceptance Criteria**:
- WHEN 创建/修改设备 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。
- THE SYSTEM SHALL 通过 `GET /api/service/device/v1/{code}` 对外暴露按设备代码查询。
- THE SYSTEM SHALL 通过 `GET /api/service/device/v1/listAllFota` 返回全部可 FOTA 升级设备。

#### US-016: 维护供应商（Supplier）
**As a** Mpt-User, **I want** 供应商 CRUD, **so that** 零件/批次可追溯到供应商。

**Acceptance Criteria**:
- WHEN 创建/修改供应商 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。

### 3.6 车辆零件绑定域

#### US-017: 维护车辆零件（VehiclePart）
**As a** Mpt-User, **I want** 车辆零件 CRUD（按 VIN/PN 查询）, **so that** 手工修复或追加零件绑定关系。

**Acceptance Criteria**:
- WHEN 创建/修改车辆零件 IF (`pn`,`sn`) 组合已存在 THEN THE SYSTEM SHALL 返回"车辆零件已存在"。
- WHEN 调用 `GET /api/mpt/vehiclePart/v1/list` THE SYSTEM SHALL 按 `vin/pn/beginTime/endTime` 过滤并分页。
- WHEN System 通过 `bindVehiclePart()` 绑定零件 THE SYSTEM SHALL 将 `partState` 置为 `1`（在用）并记录 `bindTime=Instant.now()`。

### 3.7 车辆数据导入域

#### US-018: 管理后台车辆数据批次导入
**As a** Mpt-User, **I want** 通过 `POST /api/mpt/vehicleImportData/v1` 上传一批数据（含 `batchNum/type/version/data` JSON）并由系统自动选择解析器解析, **so that** 物理车辆/密钥模块/SIM 卡数据能批量入库并触发下游事件。

**Acceptance Criteria**:
- WHEN 提交导入数据 IF `batchNum` 已存在（不含自身 ID）THEN THE SYSTEM SHALL 返回"批次号已存在"。
- WHEN 解析时 THE SYSTEM SHALL 通过 `ImportDataParserRegistry.getParser(type, version)` 类型安全获取解析器；解析器不存在时 SHALL 抛出 `ParserNotFoundException`（`VmdErrorCode.PARSER_NOT_FOUND`，错误码 `202013`），由框架统一异常处理链路捕获返回前端明确错误信息。
- WHEN 解析成功 THE SYSTEM SHALL 将 `VehicleImportData.handle` 置为 true 并 update。
- WHEN 解析完成 THE SYSTEM SHALL 在 Response 中返回结构化的处理摘要（`ImportResultResponse`），包含 `totalCount`（总记录数）、`successCount`（成功记录数）、`failureCount`（失败记录数）、`invalidCount`（无效记录数），使运营人员可对账。
- IF 解析过程中抛异常 THEN THE SYSTEM SHALL 在 Controller 层返回 `ApiResponse.fail("车辆导入数据'<batchNum>'解析异常")` 但 import 数据 record 仍保留供重试。

#### US-019: PRODUCE 解析器（V1.0）
**As a** System, **I want** 解析 PRODUCE 类型数据, **so that** 创建/更新车辆基础信息并发布生产事件。

**Acceptance Criteria**:
- WHEN 解析每条 ITEM IF `VIN` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过该条；批次结束后 SHALL 对无效计数 > 0 的情况输出 `WARN` 日志。
- WHEN VIN 已存在 THE SYSTEM SHALL 更新 `plantCode/brandCode/platformCode/carLineCode/modelCode/baseModelCode/buildConfigCode` 七项；不存在则新建。（`plantCode` 承接原 `manufacturerCode` 语义，迁移期对仅存在 `manufacturerCode` 的历史数据按 US-007c 兼容读取/映射处理，参见 CR-011）
- WHEN 一条记录处理完成 THE SYSTEM SHALL 通过 `VehiclePublish.produce(vin)` 发布 `VehicleProduceEvent`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。IF 单条处理异常 THEN THE SYSTEM SHALL 计入 `failureCount` 并继续处理下一条。

#### US-020: EOL 解析器（V1.0）
**As a** System, **I want** 解析车辆下线数据, **so that** 完成 30+ 详细字段入库、绑定零部件、调用 TSP/OTA 下游、记录合格证节点、触发 EOL/PRODUCE 事件。

**Acceptance Criteria**:
- WHEN 解析每条 ITEM IF `VIN` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- WHEN `EOL_DATE` 缺失 THE SYSTEM SHALL 使用 `Instant.now()` 作为下线时间。
- WHEN 该车首次有 EOL（原 `eolTime==null`）THE SYSTEM SHALL 通过 `VehiclePublish.eol(vin, eolDate)` 发布 `VehicleEolEvent`。
- IF VIN 此前不存在 THEN THE SYSTEM SHALL 先建车、再补发 `VehicleProduceEvent`、再走 EOL 流程。
- THE SYSTEM SHALL 入库以下详细字段：`PRODUCTION_ORDER/MATNR/PROJECT/SALES_AREA/BODY_TYPE/CONFIG_LEVEL/MODEL_YEAR/STEERING_POSITION/INTERIOR_STYLE/EXTERIOR_COLOR/DRIVE_TYPE/WHEEL/TIRE/SEAT_TYPE/ASSISTED_DRIVING/ETC_SYSTEM/REAR_TOW_BAR/ENGINE_NO/ENGINE_TYPE/FRONT_DRIVE_MOTOR_NO/FRONT_DRIVE_MOTOR_TYPE/REAR_DRIVE_MOTOR_NO/REAR_DRIVE_MOTOR_TYPE/GENERATOR_NO/GENERATOR_TYPE/POWER_BATTERY_PACK_NO/POWER_BATTERY_TYPE/POWER_BATTERY_FACTORY`。
- WHEN `CERT_DATE` 非空 THE SYSTEM SHALL 调用 `vehicleLifecycleAppService.recordCertificateNode(vin, certDate)`。
- WHEN 解析 `PARTS` 数组 IF 元素 `VIN` 与外层 `VIN` 不一致 THEN THE SYSTEM SHALL 输出 `WARN` 并跳过该零件。
- WHEN 零件绑定完成 THE SYSTEM SHALL 发布 `VehicleEolPartBoundEvent` 事件（携带 `vin` + 零件列表 + 各零件的设备项/SN/ICCID 等元数据）。
- THE SYSTEM SHALL 通过 `VehicleEolTspOtaSubscribe` 异步订阅 `VehicleEolPartBoundEvent`，在订阅者中完成 TSP/OTA 下游调用：TBOX → `tspVehicleNetworkService.create()`（仅 `ICCID1` 非空时）+ `tspVehicleTboxService.bind()`；CCP → `tspVehicleCcpService.bind()`；IDCM → `tspVehicleIdcmService.bind()`；全量零件 → `otaVehiclePartService.saveVehicleParts(vin, "车辆下线")`。
- IF 订阅者调用下游服务失败 THEN THE SYSTEM SHALL 记录 `WARN` 日志，不影响 EOL 解析主流程的成功返回。
- THE SYSTEM SHALL 通过 `vehiclePartAppService.bindVehiclePart()` 绑定零件，`bindOrg="MES"`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。IF 单条处理异常 THEN THE SYSTEM SHALL 计入 `failureCount` 并继续处理下一条。

#### US-021: BTM 解析器（V1.0）
**As a** System, **I want** 解析蓝牙模块数据, **so that** 入库车辆零件并通知 IDK 服务批量导入。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `SN` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM/MAC` 序列化进 `extra` JSON。
- THE SYSTEM SHALL 创建 `VehiclePart`（`deviceCode="BTM_M"`, `deviceItem=BTM`）。
- THE SYSTEM SHALL 调用 `idkBtmInfoService.batchImport()` 同步至 IDK。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-022: TBOX 解析器（V1.0）
**As a** System, **I want** 解析车联终端数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `pn` 为空 OR `sn` 为空 OR `iccid1`/`iccid2` 都为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `IMEI/ICCID1/ICCID2/HSM` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspTboxInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-023: CCP 解析器（V1.0）
**As a** System, **I want** 解析中央计算平台数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `pn` 为空 OR `sn` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspCcpInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-024: IDCM 解析器（V1.0）
**As a** System, **I want** 解析信息娱乐模块数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `sn` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM/MAC` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspIdcmInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-025: SIM 解析器（V1.0）
**As a** System, **I want** 解析 SIM 卡数据。

**Acceptance Criteria**:
- IF `MNO` 为空 THEN THE SYSTEM SHALL 抛 `VehicleImportDataException(batchNum, "SIM卡导入数据运营商为空")`。
- IF `MNO` 不能解析为 `MnoType` 枚举值 THEN THE SYSTEM SHALL 抛 `VehicleImportDataException(batchNum, "SIM卡导入数据运营商[<mno>]未识别")`。
- WHEN ITEM `iccid/imsi/msisdn` 三者全空 THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 调用 `tspSimService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

### 3.8 车辆生命周期记录域

#### US-026: 维护车辆生命周期节点（VehicleLifecycle / VehicleLifecycleNode）
**As a** System / Service-Caller, **I want** 按 VIN 写入和查询生命周期节点, **so that** 跨域服务可追溯每辆车在每个阶段的触达时间。

**Acceptance Criteria**:
- THE SYSTEM SHALL 支持 `VehicleLifecycleNodeEnum` 中所有 23 个节点：`PRODUCE / IMMO_SK / TBOX_CERT / TBOX_COMM_SK / CCP_CERT / CCP_COMM_SK / ADCM_CERT / ADCM_COMM_SK / IDCM_CERT / IDCM_COMM_SK / EOL / CERTIFICATE / PDC_INBOUND / ORDER_BIND / PDI / SIM_BIND / VEHICLE_SHIPPING / VEHICLE_INVoICING / VEHICLE_OWNER_BIND / VEHICLE_ACTIVE / VEHICLE_DELIVERY / HD_MAP_ACTIVE / RTK_ACTIVE`（**已知缺陷**：`VEHICLE_INVoICING` 为拼写错误，应为 `VEHICLE_INVOICING`，但当前代码与 DB 中均为该错误形态，本 spec 仅记录现状）。
- THE SYSTEM SHALL 保留 `IMMO_SK` 节点枚举定义；其触发器 `VehicleSkSubscribe` 当前因依赖未启用的 `ExSkService` 整体注释，节点写入逻辑当前不会被触发（参见 §5 O7）。
- WHEN 写入节点 THE SYSTEM SHALL 调用 `VehicleLifecycleNode.init()` 初始化基础属性，再 `vehicleLifecycleNodeRepository.save(node)`。
- THE SYSTEM SHALL 通过订阅 `VehicleProduceEvent` 自动写入 PRODUCE 节点。
- THE SYSTEM SHALL 通过订阅 `VehicleEolEvent` 自动写入 EOL 节点（`reachTime=event.eolTime`）。
- THE SYSTEM SHALL 通过订阅 `QrcodeConfirmEvent`（`type==VEHICLE_ACTIVE`）自动写入 VEHICLE_ACTIVE 节点。
- THE SYSTEM SHALL 通过订阅 `QrcodeValidateEvent`（`type==VEHICLE_ACTIVE`）触发 `VehicleAppService.checkVehiclePresetOwner(vin, accountId)` 校验预设车主。
- WHEN 删除车辆 THE SYSTEM SHALL 通过 `vehLifecycleRepository.physicalDeleteByVin(vin)` 物理删除生命周期记录。
- THE SYSTEM SHALL 在数据库层对 `tb_veh_lifecycle` 表施加 `UNIQUE KEY uk_vin_node(vin, node)` 约束，保证同一 VIN 同一节点最多一条记录。
- THE SYSTEM SHALL 保证节点写入具有幂等性：首次写入记录 `reachTime`，重复调用忽略后续写入（首次写入胜出语义）。

#### US-027: 内部服务记录"首次申请"类节点
**As a** Service-Caller (TSP), **I want** 通过 `POST /api/service/vehicleLifecycle/v1/{vin}/recordFirstApply{X}` 记录 8 个证书/通讯密钥节点, **so that** 各模块的密钥/证书申请触达时间可被收口。

**Acceptance Criteria**:
- THE SYSTEM SHALL 暴露 8 个端点：`recordFirstApplyTboxCertNode / recordFirstApplyTboxCommSkNode / recordFirstApplyCcpCertNode / recordFirstApplyCcpCommSkNode / recordFirstApplyIdcmCertNode / recordFirstApplyIdcmCommSkNode / recordFirstApplyAdcmCertNode / recordFirstApplyAdcmCommSkNode`。
- WHEN 调用 SHALL 写入对应 `VehicleLifecycleNodeEnum`，`reachTime=Instant.now()`。
- THE SYSTEM SHALL 通过 `VmdVehicleLifecycleServiceFallbackFactory` 提供 fallback。
- THE SYSTEM SHALL 保证 `recordFirstApplyNode` 具有幂等性：首次调用写入节点及 `reachTime`，重复调用忽略后续写入（首次写入胜出语义）。

### 3.9 对外 RPC 服务域

#### US-030: 车辆/零件/设备/生命周期/车型配置 Feign 契约稳定性
**As a** Service-Caller, **I want** 引入 `edd-vmd-api`（5 个 `Vmd*Service` 接口）后即可通过 OpenFeign 调用全部对外能力, **so that** 跨服务调用统一收口在 API 模块。

**Acceptance Criteria**:
- THE SYSTEM SHALL 在 `edd-vmd-api` 模块定义并维护：`VmdVehicleService` / `VmdVehicleLifecycleService` / `VmdVehicleModelConfigService` / `VmdDeviceService` / `VmdPartService` 五个 `@FeignClient`。
- THE SYSTEM SHALL 为每个 Feign 接口提供同包路径的 `*FallbackFactory`。
- THE SYSTEM SHALL 通过 `ServiceNameConstants.EDD_VMD` 引用服务名（与 `bootstrap.yml` 中 `spring.application.name=edd-vmd` 一致）。
- THE SYSTEM SHALL 保证 Service 模块的 `@RequestMapping` 与 API 模块 Feign 上的 `path` 一一对应，覆盖：
  - `/api/service/vehicle/v1/{vin}` GET / `/{vin}/action/bindOrder` POST
  - `/api/service/part/v1/{pn}` GET / `/listAllFota` GET
  - `/api/service/device/v1/{code}` GET / `/listAllFota` GET
  - `/api/service/vehicleLifecycle/v1/{vin}/recordFirstApply*Node` POST × 8
  - `/api/service/vehicleModelConfig/v1/buildConfigCode` GET / `/buildConfig/list/{baseModelCode}` GET / `/buildConfig/{buildConfigCode}` GET

#### US-031: 内部服务按"特征族-特征值"反查生产配置
**As a** Service-Caller, **I want** 通过 `GET /api/service/vehicleModelConfig/v1/buildConfigCode?<familyCode>=<featureCode>...` 用任意特征族特征值组合反查生产配置代码, **so that** 在订单/前置库等场景将销售配置翻译为生产配置。

**Acceptance Criteria**:
- THE SYSTEM SHALL 接受 `Map<String,String>` 形式的特征值组合并返回单一生产配置代码。
- WHEN 调用 `GET /buildConfig/{buildConfigCode}` THE SYSTEM SHALL 返回包含 `featureCodes` 列表 + `brandCode` 的完整 `VmdBuildConfigResponse`。
- IF `carLineCode` 缺失或对应车系不存在 THEN THE SYSTEM SHALL 在响应中省略 `brandCode`（不视为错误）。

## 4. Constraints & Assumptions

### 技术约束（非协商）
- **JDK 17**（路径 `/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home`）。
- **Spring Boot / Spring Cloud / Spring Cloud Alibaba** 版本由 `net.hwyz.iov.cloud.parent:service` Parent POM 统一管理。
- 必须基于 `framework-*` Starter（`framework-common / framework-exception-starter / framework-mysql-starter / framework-redis-starter / framework-audit-starter / framework-kafka-starter / framework-web-starter / framework-security`）。
- **DDD 四层强分层**：`adapter / application / domain / infrastructure`，禁止跨层引用 PO/VO/Domain 混用，详见 PROJECT_GUIDE 的 Hard Bans。
- **MyBatis-Plus**（`com.baomidou.*`）+ Flyway 迁移；DB 访问层位于 `infrastructure.persistence`。
- **MapStruct** 强制用于跨层对象转换。
- **物理分页下沉 SQL**：禁止 `findAll()` + 内存分页；所有 Mpt 列表必须 `startPage()` + `getPageResult()` 配合 `PageHelper`。
- **Nacos** 注册 + 配置（namespace 默认 `32c13f29-1aa6-468a-bacb-81be7f437dc9`，可由 `NACOS_NAMESPACE` 覆盖；服务名 `edd-vmd`）。
- **API 网关**：所有外部访问通过统一网关进入，鉴权/限流/审计在网关层。
- **审计**：MPT 写操作通过 `@Log(title=..., businessType=...)` 切面落审计日志。
- **MDM 同步优先级**：品牌 / 车系 / 平台主数据的 SSOT 优先级为 MDM > VMD 本地；MDM 不可达时降级为只读。
- **MDM 事件消费**：VMD 通过 Kafka 订阅 MDM 事件，事件 payload schema / topic 命名 / partition 策略 / 重试与死信策略由「edd-mdm 接入规范」定义。
- **MDM 快照接口**：VMD 通过 Feign 调用 MDM 全量快照接口，路径 / 入参 / 出参由「edd-mdm 接入规范」定义。
- **数据来源标记**：veh_brand / veh_carLine / veh_platform / **veh_plant** 四张表新增 source 字段（MDM / MANUAL），source=MDM 的记录禁止通过 MPT 后台修改。

### Plant 主数据投影约束（CR-011）
- Plant 主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Plant 投影副本，不作为权威维护入口。
- VMD 中 `plantCode` 是车辆主档的一部分，作为车辆生产工厂追溯字段长期保留。
- 原 `manufacturerCode` 为历史遗留命名，应通过迁移脚本、兼容字段或映射逻辑逐步迁移到 `plantCode`。
- VMD 不负责 Plant 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Plant 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义。
- 新增需求、接口、领域对象、数据表、DTO、VO、文档统一使用 **Plant** 命名；Manufacturer / `manufacturerCode` 仅出现在历史兼容、迁移说明或旧字段映射场景中。
- VMD Plant 投影采用按需最小化字段设计，不要求与 MDM Plant 主数据模型完全一致；投影字段以车辆生产工厂追溯、导入校验、查询展示和运行时解耦为边界。
- MDM Plant 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Plant 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Plant 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

### Plant 投影字段范围原则（VMD Plant ⊂ MDM Plant，CR-011）
> VMD 侧 Plant 投影不要求与 MDM Plant 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Plant 投影是 MDM Plant 在 VMD bounded context 下的只读视图，不是 MDM Plant 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 Plant 字段。
2. VMD 不复制 MDM Plant 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Plant 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD Plant 投影模型。
4. VMD Plant 投影是 MDM Plant 在 VMD bounded context 下的只读视图，不是 MDM Plant 的完整副本。
5. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_plant` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `plant_code` | Plant 编码，车辆主档 `plantCode` 的关联键 |
| `plant_name` | Plant 名称，用于车辆详情展示 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Plant 实体 ID |
| `external_version` | MDM Plant 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Plant 审批状态。
- Plant 生命周期全量状态流转。
- Plant 组织归属全路径。
- Plant 地址、经纬度、联系人等详细档案。
- Plant 编码生成规则。
- Plant 数据质量评分。
- Plant 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### Brand 主数据投影约束（CR-012）
- Brand 主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Brand 投影副本，不作为权威维护入口。
- VMD 中 `brandCode` 是车辆主档和产品树的一部分，作为车辆品牌关联字段长期保留。
- VMD 不负责 Brand 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Brand 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义。
- VMD Brand 投影采用按需最小化字段设计，不要求与 MDM Brand 主数据模型完全一致；投影字段以车辆查询、车辆详情展示、导入校验、产品树关联、历史追溯和运行时解耦为边界。
- MDM Brand 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Brand 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Brand 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

### Brand 投影字段范围原则（VMD Brand ⊂ MDM Brand，CR-012）
> VMD 侧 Brand 投影不要求与 MDM Brand 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Brand 投影是 MDM Brand 在 VMD bounded context 下的只读视图，不是 MDM Brand 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 Brand 字段。
2. VMD 不复制 MDM Brand 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Brand 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD Brand 投影模型。
4. VMD Brand 投影是 MDM Brand 在 VMD bounded context 下的只读视图，不是 MDM Brand 的完整副本。
5. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_brand` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `brand_code` | Brand 编码，车辆主档 `brandCode` 的关联键 |
| `brand_name` | Brand 名称，用于车辆详情、列表和产品树展示 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Brand 实体 ID |
| `external_version` | MDM Brand 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Brand 审批状态。
- Brand 生命周期全量状态流转。
- Brand 组织归属全路径。
- Brand Logo、营销介绍、市场属性等非 VMD 必需字段。
- Brand 编码生成规则。
- Brand 数据质量评分。
- Brand Golden Record 合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### 依赖（外部）
- **TSP 服务**：`TspVehicleCcpService / TspVehicleIdcmService / TspVehicleNetworkService / TspVehicleTboxService / TspCcpInfoService / TspIdcmInfoService / TspTboxInfoService / TspSimService`。
- **OTA 服务**：`OtaVehiclePartService`（车辆零件同步）。
- **IDK 服务**：`IdkBtmInfoService`（蓝牙模块批量导入）。
- **账号服务（已注释）**：`ExAccountService`（预设车主校验，待启用）。
- **安全密钥服务（已注释）**：`ExSkService`（IMMO_SK 生成，待启用）。
- **edd-mdm 服务**：Product MDM 子域，提供品牌 / 车系 / 平台 / **Plant（工厂）**主数据的 Kafka 事件推送 + Feign 全量快照接口。详见「edd-mdm 接入规范」。

### 前置条件
- Nacos 中已存在共享配置 `application.yaml / mysql.yaml / redis.yaml`。
- MySQL 数据库已存在并允许 Flyway 在启动时执行 `V0__Baseline.sql / V1__BuildConfig_feature_code_migration.sql / V2__CarLine_brand_code_migration.sql`。
- API 网关下游路由已将 `edd-vmd` 注册到正确路径前缀。
- MDM 已完成首版 Product MDM 子域上线，且 Kafka topic 已开通。
- MDM 已上线 Plant（工厂）主数据实体，且 Plant 事件 Kafka topic 与 Plant 全量快照接口已就绪（CR-011）。
- MDM Feign 全量快照接口已就绪，VMD 可通过 Feign 调用。
- VMD 启动时若本地无 source=MDM 数据，需通过 Bootstrap 流程从 MDM 拉全量。

## 5. Out of Scope

- O1：解析器 V2.0+。当前所有解析器均为 V1.0；新版本需走 §6 变更流程。
- O2：账号/手机号实名核验（`ExAccountService` 集成被注释）；预设车主校验暂跳过。
- O3：IMMO_SK 安全密钥生成（`ExSkService` 集成被注释）；`recordGenerateVehicleSkNode` 当前依赖未启用的事件链路。
- O4：MPT 导出（Export）端点目前仅有 `@Log` 注解和日志，未实现 Excel/CSV 文件流；不在本 spec 必要交付内（如需启用，走 CR）。
- O5：物联网终端密钥真正颁发流程（VMD 仅记录"首次申请"节点，不参与密钥颁发）；`VehicleSkSubscribe` 整体注释、IMMO_SK 节点写入逻辑当前不生效。
- O6：车辆配置（VehicleConfig）的写入流程（当前 MPT 仅暴露查询/导出，不暴露 add/edit）。
- O7：`VehicleLifecycleNodeEnum.VEHICLE_INVoICING` 为拼写错误（应为 `VEHICLE_INVOICING`，**已知缺陷**）；本 spec 仅记录现状。
- O8：`MptVehiclePartController.add/edit` 当前未对 `vin` 执行存在性校验，可能产生脏数据（**已知缺陷**）；本 spec 仅记录现状。
- O9：MDM 与 VMD 的具体协议（Kafka topic 命名 / payload 字段映射 / 重试策略 / 死信队列处理）由「edd-mdm 接入规范」单独定义，本 spec 不展开。
- O10：跨系统 Golden Record 合并能力由 edd-mdm 负责，VMD 不实现。
- O11：现有数据 source 字段回标为 MDM 的数据治理任务（通过独立 CR 执行对账回标 source='MDM'），本期 spec 不实现。
- O12：VMD 不再提供 Plant 主数据的本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-011）。
- O13：VMD 不实现 Plant 主数据的 Golden Record 合并能力；Plant 编码规则生成与主数据审批流程不在 VMD 范围内（CR-011）。
- O14：VMD 不要求完整复制 MDM Plant 的所有字段；不承担 MDM Plant 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-011）。
- O15：历史车辆中的 `plantCode` / `manufacturerCode` 清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-011）。
- O16：本次 CR 只定义 Manufacturer 到 Plant 的系统命名迁移与兼容策略，不要求一次性删除所有旧字段和旧接口；旧字段（`manufacturer_code` 等）、旧接口（`/api/mpt/manufacturer/**`）、旧权限点（`completeVehicle:product:manufacturer:*`）的最终下线由后续兼容性清理 CR 完成（CR-011）。
- O17：MDM Plant 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-011）。
- O18：MDM Plant 的完整档案展示、主数据维护、审批、合并、拆分、数据质量管理不在 VMD 范围内（CR-011）。
- O19：VMD 不再提供 Brand 主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-012）。
- O20：VMD 不实现 Brand 主数据的 Golden Record 合并能力（CR-012）。
- O21：VMD 不实现 Brand 编码规则生成、主数据审批流程、生命周期管理（CR-012）。
- O22：VMD 不要求完整复制 MDM Brand 的所有字段；不承担 MDM Brand 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-012）。
- O23：历史 Brand 数据 source 回标、清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-012）。
- O24：本次 CR 只定义 Brand 从本地维护到本地投影的需求语义调整，不要求一次性删除所有 VMD Brand add/edit/remove 接口和权限点；最终下线由后续兼容性清理 CR 完成（CR-012）。
- O25：MDM Brand 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-012）。

## 6. Changelog

| Date | Change ID | Type | Description |
|------|-----------|------|-------------|
| 2026-05-23 | CR-001 | Added | 基于现有代码 + graphify 知识图谱逆向生成首版 requirements，覆盖 31 个 US，10 个能力域 |
| 2026-05-23 | CR-002 | Modified | OQ-1/2/3/4/5/6 决议落地：US-011 锁定 VIN 不存在返回 null（不抛异常）（**已被 CR-006 回退**）；§5 移除 O6；§7 Open Questions 已全部决议，章节删除 |
| 2026-05-23 | CR-003 | Modified | **回退 CR-002 中夹带的"未来改造"意图，回归纯逆向基线**：移除 US-017 VIN 校验、US-026 V3 迁移与 IMMO_SK 改造；这些改造意图改为以"已知缺陷"形式记录于 §5 Out of Scope；US-011（VIN 不存在返回 null）保留为现状契约（**已被 CR-006 回退**）。本 spec 自此为"代码现状的正本"，未来任何改造一律走新 CR 单独立项 |
| 2026-05-23 | CR-004 | Removed | **移除 US-028/US-029 车机+移动端二维码激活闭环**：该功能与 VMD 核心职责（车辆主数据管理）相关性不大，整体移除 §3.9 章节、G4 目标、N4 非目标、O1/O6/O9 Out of Scope 条目；对应代码（Qrcode 聚合、IDCM/Mobile 控制器、相关事件/异常/DTO）同步清除 |
| 2026-05-23 | CR-005 | Modified | **US-020 EOL 解析器改事件驱动**：将 TSP/OTA 同步 Feign 调用改为发布 `VehicleEolPartBoundEvent` 事件 + 异步订阅者处理，解除 VMD↔TSP/OTA 同步耦合，提升 EOL 解析可用性 |
| 2026-05-23 | CR-006 | Modified | **US-011 VIN 不存在改为抛异常（fail-fast）**：回退 CR-002/CR-003 中"VIN 不存在返回 null"的既定契约，改为抛出 `VehicleNotExistException`（`VmdErrorCode.VEHICLE_NOT_EXIST`，错误码 `202001`）；同时 `VmdBaseException` 基类从 `BaseException`（int code）改为继承 `BusinessException`（ErrorCode 接口），统一纳入 `GlobalExceptionHandler` 的 `BusinessException` 捕获链路；新增 `VmdErrorCode` 枚举集中管理 VMD 模块错误码；`VmdVehicleServiceFallbackFactory.getByVin` 改为抛 `RuntimeException` 而非返回 null |
| 2026-05-23 | CR-009 | Modified | **US-018~025 批量导入返回结构化处理摘要**：`ImportDataParser.parse()` 返回类型从 `void` 改为 `ImportResult`（含 `totalCount/successCount/failureCount/invalidCount`）；所有 7 个解析器（PRODUCE/EOL/BTM/TBOX/CCP/IDCM/SIM）实现计数回传；`MptVehicleImportDataController.add/edit` 响应从 `ApiResponse<Void>` 改为 `ApiResponse<ImportResultResponse>`，运营人员可对账 |
| 2026-05-26 | CR-010 | Modified | **品牌/车系/平台主数据 SSOT 上移至 edd-mdm**：VMD 降级为本地投影副本；新增 source / external_ref_id / external_version / last_sync_time 字段；MPT 后台对 source=MDM 记录禁止写操作；新增 Bootstrap 全量同步流程；新增 MDM 事件订阅流程 |
| 2026-06-05 | CR-011 | Modified | **工厂 / 生产厂商主数据统一调整为 Plant**：Plant 主数据 SSOT 上移至 MDM，VMD 保留 Plant 本地投影表（US-007 由「维护生产厂商 Manufacturer」改写为「消费 MDM Plant 主数据本地投影」，新增 US-007b Bootstrap 全量同步 Plant、US-007c Manufacturer→Plant 兼容迁移）；原 Manufacturer / manufacturerCode 作为历史兼容命名逐步迁移为 Plant / plantCode（`veh_manufacturer`→`veh_plant`、车辆主档 `manufacturer_code`→`plant_code`）；VMD Plant 投影采用按需最小化字段设计，不要求完整复制 MDM Plant 主数据模型（新增 §4「Plant 投影字段范围原则」）；新增 source / external_ref_id / external_version / last_sync_time 字段；MPT 后台禁止维护 source=MDM 的 Plant 投影数据；新增 Plant MDM 事件订阅与 Bootstrap 全量同步流程；车辆主档使用 plantCode 用于生产工厂追溯；权限点 `completeVehicle:product:manufacturer:*`→`completeVehicle:product:plant:*`（旧权限点标记 deprecated 待后续 CR 下线）；US-019 PRODUCE 解析器字段引用同步为 plantCode（含历史兼容）；§2 新增 G6 与 Plant 语义统一说明、N4 扩展含 Plant；§5 新增 O12~O18。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-05 | CR-012 | Modified | **品牌主数据重构为 MDM Brand 本地投影**：Brand 主数据 SSOT 上移至 MDM，VMD 保留 Brand 本地投影表（US-001 由「维护车辆品牌 Brand」改写为「消费 MDM Brand 主数据本地投影」；强化 US-001b Brand Bootstrap 全量同步；新增 US-001c Brand 本地维护能力兼容清理）；新增 §4「Brand 主数据投影约束」与「Brand 投影字段范围原则」（VMD Brand ⊂ MDM Brand，按需最小化投影，不要求完整复制 MDM Brand 主数据模型）；VMD Brand add/edit/remove 仅作为 source=MANUAL 兼容期遗留能力，对 source=MDM 记录一律只读；保留 `brandCode` 作为车辆主档和产品树的品牌关联字段（不改名、不删除）；§2 G5 纳入 Brand「MDM 下游消费方 + 只读本地投影副本」语义并新增 Brand 语义统一说明、新增 N5 Brand 非目标；§5 新增 O19~O25。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |

