# Vehicle Master Data Platform - Requirements

> 本文档由现有代码（commit @ 2026-05-23）+ graphify 知识图谱逆向生成，作为 SSOT 起点。
> 任何后续变更必须遵循 SPEC_GUIDE §6 变更管理规则，禁止直接改代码。

## 1. Overview

`edd-vmd`（车辆主数据 / Vehicle Master Data）是开源车联网（OpenIOV）云端企业数字底座的核心微服务，沉淀从产品定义（品牌→车系→车型→版本（Variant，原基础车型）→生产配置）到物理实例（车辆→零件→设备）再到生命周期事件（生产→密钥→证书→下线→合格证→订单→PDI→交付）的完整车辆主数据，并对外提供管理后台运营能力，以及供下游微服务消费的内部 RPC 契约。

> **CR-020 兼容说明**：自 CR-020 起，「设备（Device）」字典 / 类型层主数据演进为消费 MDM EEAD 子域「车载节点（VehicleNode）」的本地只读投影，Device / `deviceCode` 改名为 VehicleNode / `vehicleNodeCode`（历史命名兼容保留）；上文「车辆→零件→设备」链路中的**物理设备实例 + 绑定关系仍为 VMD 自有事务 / 实例数据，不上移、不投影化、保持留在 VMD**，仅其节点引用键由 `device_code` 兼容改名为 `vehicle_node_code`。详见 US-015 / US-015b / US-015c 与 §4「VehicleNode 主数据投影约束」。

> **CR-021 兼容说明**：自 CR-021 起，「零件（Part）」字典 / 类型层主数据演进为消费 MDM Part（零件）子域的本地只读投影。本 CR 与 Brand/Platform/CarLine/Model（CR-012~CR-015）同构——**Part 实体命名不变、关联键 `partCode` 不变、不做表 / 列重命名**（区别于 Plant(CR-011)/Variant(CR-016)/Configuration(CR-017)/VehicleNode(CR-020) 的命名迁移），Part 复用现有 `veh_part`（物理表 `tb_part`），仅新增 source / external_ref_id / external_version / last_sync_time 投影管理字段与 `UK(external_ref_id)`。上文「车辆→零件→设备」链路中的**物理零件实例 + 绑定关系（VIN 绑定的物理零件实例、零件→设备挂载、SN / part_number / hardware 等实例属性，及装车 / 换件 / 下线 / 密钥 / 证书等生命周期事件）仍为 VMD 自有事务 / 实例数据，不上移、不投影化、保持留在 VMD**，仅 Part 字典 / 类型层主数据（零件定义、零件类型、规格、part_number 字典等「车上应有哪些零件」）投影化。本期投影范围**仅持久化 P0 必投字段集 + 投影管理字段**，不要求与 MDM `mdm_material_part` 字段完全一致。详见 US-014 / US-014b / US-014c / US-014d 与 §4「Part 主数据投影约束」「Part 投影字段范围原则」。

## 2. Background & Goals

### 背景
- 车联网平台需要一份"车辆"的权威事实来源（VIN→车系车型→零件设备→各阶段时间戳），其他业务域（TSP、OTA、IDK、安全密钥、订单）以此为锚点开展自身业务。
- 整车生产、下线、密钥、零部件四类离线数据来自 MES/SIM 供应商/Tier1 供应商，需要异步导入并触发跨域事件。

### 目标（Goals）
- G1：成为车辆全要素主数据（产品树、物理车、零件设备、生命周期）的 SSOT。
- G2：通过统一 Feign 契约（`Vmd*Service`）对外暴露车辆/零件/设备/车型配置/生命周期五类能力。
- G3：支持 6 类（PRODUCE/EOL/BTM/CCP/IDCM/TBOX/SIM）批量数据导入并下钻到 TSP/OTA/IDK 等下游服务。
- G4：对管理后台提供完整 CRUD + 鉴权（`completeVehicle:*` / `iov:configCenter:*` 权限点）能力。
- G5：在产品树（品牌/车系/平台/车型）主数据上，VMD 作为 edd-mdm 的下游消费方，持有本地投影副本；其中 Brand、CarLine、Platform 与 Model 本地投影均为**只读**视图，VMD 消费 MDM Brand / MDM CarLine / MDM Platform / MDM Model 主数据，通过 Brand 投影支撑车辆查询、产品树关联、导入校验和历史追溯（`brandCode` 作为车辆主档与产品树的品牌关联编码长期保留，VMD 不再承担 Brand 主数据维护职责，CR-012），通过 CarLine 投影支撑车辆查询、产品树关联、导入校验和历史追溯（`carLineCode` 作为车辆主档与产品树的车系关联编码长期保留，车系投影上的 `brandCode` 冗余字段一并保留用于跨域回查，VMD 不再承担 CarLine 主数据维护职责，CR-014），通过 Platform 投影支撑车辆查询、产品树关联、导入校验和历史追溯（`platformCode` 作为车辆主档与产品树的平台关联编码长期保留，VMD 不再承担 Platform 主数据维护职责，CR-013），通过 Model 投影支撑车辆查询、产品树关联（车系→车型→版本（原基础车型）链路）、导入校验和历史追溯（`modelCode` 作为车辆主档与产品树的车型关联编码长期保留，VMD 不再承担 Model 主数据维护职责，CR-015），通过 Variant（版本，原 BaseModel 基础车型）投影支撑车辆查询、产品树关联（车型→版本链路）、导入校验和历史追溯（`variantCode` 作为车辆主档与产品树的版本关联编码长期保留，承接原 `baseModelCode` 语义，VMD 不再承担 Variant 主数据维护职责，CR-016）。
- G6：在工厂（Plant）主数据上，VMD 作为 edd-mdm 的下游消费方，持有 Plant 本地投影副本，用于车辆生产工厂追溯；车辆主档使用 `plantCode` 表示生产工厂编码，VMD 不再承担 Plant 主数据治理职责（CR-011）。
- G7：在配置（Configuration，原 BuildConfig 生产配置）主数据上，VMD 作为 edd-mdm 的下游消费方，持有 Configuration 配置本地投影副本，用于配置关联、特征-配置反查、导入校验、查询展示与历史追溯；车辆主档使用 `configurationCode`（承接原 `buildConfigCode` 语义）作为配置关联编码（每台物理车唯一映射的核心锚点），VMD 不再承担 Configuration 配置主数据维护职责（CR-017）。
- G8：在选项族（OptionFamily，原 FeatureFamily 特征族）/选项值（OptionCode，原 FeatureCode 特征值）主数据上，VMD 作为 edd-mdm 的下游消费方，持有 OptionFamily / OptionCode 本地**只读**投影副本，用于版本（Variant）/配置（Configuration）的选项引用、特征-配置反查（US-031）、查询展示与历史追溯；关联键使用 `optionFamilyCode`（承接原 `familyCode` 语义）/ `optionCode`（承接原 `featureCode` 语义），VMD 不再承担 OptionFamily / OptionCode 主数据治理（编码生成 / 审批 / Golden Record / 质量打分 / 生命周期）职责（CR-018）。
- G9：在供应商（Supplier）主数据上，VMD **彻底下线本地维护能力**（`Supplier` 聚合 + `tb_supplier` 表 + 增删改查 API + 契约及附属物），供应商主数据 SSOT 上移至 **edd-mdm 的 Party 子域**（MDM CR-006）；**与产品树各实体（CR-012~CR-018）的按需最小化只读投影策略不同，VMD 明确不为供应商建立任何本地只读投影**，仅保留零部件 / 设备 / 导入记录上的 `supplier_code` 作为溯源属性透传（CR-019）。
- G10：在车载节点（VehicleNode，原 Device 设备）字典 / 类型主数据上，VMD 作为 edd-mdm **EEAD 子域**的下游消费方，持有 VehicleNode 本地**只读**投影副本，用于车辆导入校验、车辆 / 设备详情展示、下游 RPC 暴露、历史追溯与 MDM 不可用时的降级查询；车辆物理设备实例继续使用 `vehicleNodeCode`（承接原 `deviceCode` 语义）作为节点关联编码长期保留，VMD 不再承担车载节点字典主数据维护职责。本 CR 仅处理「车载节点字典 / 类型层」主数据（节点定义、类型、功能域等「车上应有什么」），**VMD 自有的物理设备实例 + 绑定关系（VIN 绑定的 TBOX/IDCU/CCU/ADCU/TCU 实例及其 SN/part_number/hardware_vsn、绑车激活 / 下线 / 密钥 / 证书等生命周期事件）属于 VMD 事务 / 实例数据，不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**（CR-020）。
- G11：在零件（Part）字典 / 类型层主数据上，VMD 作为 edd-mdm Part 子域的下游消费方，持有 Part 本地**只读**投影副本，用于零件 / 车辆导入校验、零件 / 车辆详情展示、下游 RPC 暴露、历史追溯与 MDM 不可用时的降级只读查询；车辆物理零件实例继续使用 `partCode` 作为零件关联编码长期保留，VMD 不再承担 Part 字典 / 类型层主数据维护职责。本 CR 仅处理「Part 字典 / 类型层」主数据（零件定义、零件类型、规格、part_number 字典等「车上应有哪些零件」），**本期投影范围仅 P0 必投字段集 + 投影管理字段**（P1 / P2 字段不投影，留待后续按需 CR 增量升投）；**VMD 自有的物理零件实例 + 绑定关系（VIN 绑定的物理零件实例及其 SN / part_number / hardware 等实例属性，零件→设备挂载关系，装车 / 换件 / 下线 / 密钥 / 证书等生命周期事件）属于 VMD 事务 / 实例数据，不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**（CR-021）。
- G12：在**物理零件实例层**上，VMD 将「零件实例本体」与「车辆—零件绑定关系」显式分离为 `PartInfo` / `VehiclePart` 两个概念（车辆主档 `VehicleInfo` 沿用并瘦身），以干净支撑**游离零件（零件先于 VIN 到达）、换件历史、零件实例独立状态、异步乱序绑定**四类现模型无法表达的场景；该层为 VMD 自有事务 / 实例数据，**不上移、不投影化**，区别于字典 / 类型层（CR-011~021）（CR-022）。
- G13：在物理零件实例层之上建立**统一零件实例数据入站能力**——入口①（上游系统对接，独立链路 / 异步事件为主 + 批量兜底 / 含入站回执）与入口②（管理后台导入）**共用同一套入站内核**（字段校验 / 标准化 / 幂等 / 去重 / 落库 / 触发跨域事件），按 `part_type` 适配源差异、统一落 `part_info`（含 SIM）并经事件驱动下游域（TSP/OTA/IDK），实现零件实例数据的可靠入站、乱序兜底与对账（CR-023）。

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

> **Platform / 平台主数据语义统一（CR-013 补充）**：
> - Platform 主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 Platform 本地投影副本。
> - 平台与 Brand 同构：平台实体命名不变、`platformCode` 关联键不变，不涉及表/列重命名（复用 CR-010 为 `veh_platform` 建好的 source / external_ref_id / external_version / last_sync_time 字段，`veh_platform.code` 即 `platform_code`、`name` 即 `platform_name`），区别于 Plant 的命名迁移。
> - VMD Platform 本地投影面向车辆主数据上下文（bounded context），用于车辆主数据查询、车辆详情展示、导入校验、产品树关联、历史追溯，以及 MDM 不可用时的降级查询，不是 MDM Platform 的完整副本/镜像表。
> - VMD 车辆主档与产品树继续使用 `platformCode` 作为平台关联编码长期保留，不因维护权迁移而改名或删除。
> - VMD 不再承担 Platform 主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - VMD Platform 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **CarLine / 车系主数据语义统一（CR-014 补充）**：
> - CarLine 主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 CarLine 本地投影副本。
> - 车系与 Brand / Platform 同构：车系实体命名不变、`carLineCode` 关联键不变，不涉及表/列重命名（复用 CR-010 为 `veh_carLine` 建好的 source / external_ref_id / external_version / last_sync_time 字段），区别于 Plant 的命名迁移。
> - VMD CarLine 本地投影面向车辆主数据上下文（bounded context），用于车辆主数据查询、车辆详情展示、导入校验、产品树关联、历史追溯，以及 MDM 不可用时的降级查询，不是 MDM CarLine 的完整副本/镜像表。
> - VMD 车辆主档与产品树继续使用 `carLineCode` 作为车系关联编码长期保留，不因维护权迁移而改名或删除。
> - 车系投影上的 `brandCode` 冗余字段（由 `V2__CarLine_brand_code_migration.sql` 引入）必须保留，用于支撑跨域回查，并支撑 US-031 `getBuildConfig` 在响应中按 `carLineCode → brandCode` 补出 `brandCode`；这是车系区别于 Brand / Platform 的特殊点，不得删除或弱化。
> - VMD 不再承担 CarLine 主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - VMD CarLine 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **Model / 车型主数据语义统一（CR-015 补充）**：
> - Model 主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 Model 本地投影副本。
> - 车型与 Brand / Platform / CarLine 同构：车型实体命名不变、`modelCode` 关联键不变，不涉及表/列重命名，区别于 Plant 的命名迁移。**关键差异**：V3（`V3__Add_mdm_source_to_product_tree.sql`）仅为 `veh_brand`/`veh_series`/`veh_platform` 建了 source / external_ref_id / external_version / last_sync_time 字段，**未覆盖 `veh_model`**，故 CR-015 **新增 Flyway 迁移 `V6__Add_mdm_source_to_model.sql`** 为 `veh_model` 补齐上述投影字段与 `UK(external_ref_id)`（区别于 CR-013/CR-014 复用 V3）。
> - VMD Model 本地投影面向车辆主数据上下文（bounded context），用于车辆主数据查询、车辆详情展示、导入校验、产品树关联、历史追溯，以及 MDM 不可用时的降级查询，不是 MDM Model 的完整副本/镜像表。
> - VMD 车辆主档与产品树继续使用 `modelCode` 作为车型关联编码长期保留，不因维护权迁移而改名或删除；**`veh_base_model.model_code → veh_model.code` 的「车系→车型→基础车型」引用链不得切断**（BaseModel 当前仍为 VMD 自有，后续 CR 处理；**自 CR-016 起 `veh_base_model`→`veh_variant`、链路表述为「车系→车型→版本（原基础车型）」，语义延续**）。
> - VMD 不再承担 Model 主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - VMD Model 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **Variant / 版本主数据语义统一（CR-016 补充）**：
> - Variant（版本）主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 Variant 本地投影副本。
> - **本 CR 与 Plant（CR-011）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：本次涉及**实体重命名 + 关联键重命名**——MDM 侧实体由 BaseModel 改名为 **Variant（版本）**，VMD 同步将 `veh_base_model` 迁移/重命名为 `veh_variant`、关联键 `baseModelCode` → `variantCode`。BaseModel / `baseModelCode` / 「基础车型」自此为历史兼容命名，新能力统一使用 Variant / `variantCode` / 「版本」（迁移与兼容策略见 US-004c）。
> - VMD Variant 本地投影面向车辆主数据上下文（bounded context），用于车辆主数据查询、车辆详情展示、导入校验、产品树关联、历史追溯，以及 MDM 不可用时的降级查询，不是 MDM Variant 的完整副本/镜像表。
> - VMD 车辆主档与产品树使用 `variantCode` 作为版本关联编码长期保留，承接原 `baseModelCode` 语义并回填历史值；**`veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」引用链、以及 `BuildConfig → variantCode` 的引用链不得切断**。
> - 本 CR 仅处理 BaseModel 本体（投影化 + 改名 Variant）；**BaseModelFeatureCode / 特征值的归属与维护语义本 CR 不变**（仅做随实体重命名所必需的引用键改名与兼容，最终归属留待后续 CR）；**BuildConfig / FeatureFamily 的归属与改造不在本 CR**（CR-017 / CR-018 处理），BuildConfig 本体仍为 VMD 自有。
> - VMD 不再承担 Variant 主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - **Flyway 关键差异**：BaseModel 投影字段（source / external_ref_id / external_version / last_sync_time）此前未建立，故 CR-016 **新增 Flyway 迁移 `V7__Migrate_base_model_to_variant.sql`**（表迁移/重命名 + 补齐投影字段 + `UK(external_ref_id)` + 回填 source='MANUAL'）与 **`V8__Migrate_base_model_code_to_variant_code.sql`**（关联键 `base_model_code` → `variant_code` 迁移/回填），接续 CR-015 的 V6。
> - VMD Variant 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **Configuration / 配置主数据语义统一（CR-017 补充）**：
> - Configuration（配置，原 BuildConfig 生产配置）主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 Configuration 本地投影副本。
> - **本 CR 与 Plant（CR-011）/ Variant（CR-016）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 BuildConfig 改名为 **Configuration（配置）**，VMD 同步将配置实体与关联键由 BuildConfig / `buildConfigCode` 改名为 Configuration / `configurationCode`。BuildConfig / `buildConfigCode` / 「生产配置」自此为历史兼容命名，新能力统一使用 Configuration / `configurationCode` / 「配置」（迁移与兼容策略见 US-005c）。
> - **命名消歧**：VMD 内已存在 VehicleConfig（车辆配置，US-013）、ConfigItem（配置项，US-009）、configCenter（配置中心）等概念，与本 CR 的 Configuration（配置）含义不同；同段落出现易混概念时一律用全称限定（如「配置（Configuration）」「车辆配置（VehicleConfig）」「配置项（ConfigItem）」），避免裸用「配置」。
> - VMD Configuration 本地投影面向车辆主数据上下文（bounded context），用于配置关联、特征-配置反查（US-031）、车辆导入校验、查询展示、历史追溯，以及 MDM 不可用时的降级只读查询，是按需最小化只读视图，不是 MDM Configuration 的完整副本/镜像表。
> - VMD 车辆主档使用 `configurationCode` 作为配置关联编码长期保留，承接原 `buildConfigCode` 语义并回填历史值；**「版本（Variant）→配置（Configuration）」引用链、以及每台物理车 `configurationCode` 唯一映射不得切断**。
> - 本 CR 仅处理 Configuration 配置本体（投影化 + 改名）；**BuildConfigFeatureCode / 特征值的业务语义本 CR 不变**（仅做随实体重命名所必需的引用键改名与兼容），其最终归属与 FeatureFamily 改造留待 **CR-018**。
> - VMD 不再承担 Configuration 配置主数据治理、编码生成、审批、Golden Record、生命周期管理等职责。
> - VMD Configuration 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **OptionFamily / OptionCode（选项族 / 选项值）主数据语义统一（CR-018 补充）**：
> - OptionFamily（选项族，原 FeatureFamily 特征族）/ OptionCode（选项值，原 FeatureCode 特征值）主数据的权威来源（SSOT）为 **edd-mdm**，VMD 仅保留 OptionFamily / OptionCode 本地只读投影副本。
> - **本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 FeatureFamily / FeatureCode 改名为 **OptionFamily / OptionCode**，VMD 同步将选项族 / 选项值实体与关联键 `familyCode` / `featureCode` 改名为 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode`。FeatureFamily / FeatureCode / `familyCode` / `featureCode` / 「特征族」「特征值」自此为历史兼容命名，新能力统一使用 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode` / 「选项族」「选项值」（迁移与兼容策略见 US-008c）。
> - **命名消歧（强约束）**：本 CR 的 OptionFamily / OptionCode（选项族 / 选项值）区别于 ConfigItem（配置项，US-009）下的「枚举值 Option」、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；同段落出现易混概念时一律用全称限定（如「选项族（OptionFamily）」「选项值（OptionCode）」「配置项枚举值（ConfigItem Option）」），避免裸用「选项」或「Option」。
> - VMD OptionFamily / OptionCode 本地投影面向车辆主数据上下文（bounded context），用于版本（Variant）/ 配置（Configuration）的选项引用、特征-配置反查（US-031）、查询展示、历史追溯，以及 MDM 不可用时的降级只读查询，是按需最小化只读视图，不是 MDM OptionFamily / OptionCode 的完整副本/镜像表。
> - VMD 使用 `optionFamilyCode`（承接原 `familyCode` 语义）/ `optionCode`（承接原 `featureCode` 语义）作为关联键长期保留并回填历史值；随实体重命名所必需的引用键——Variant 侧（原 BaseModelFeatureCode，CR-016）、Configuration 侧（原 BuildConfigFeatureCode，CR-017）的特征值引用键——一并由 `featureCode` 兼容改名为 `optionCode`，**仅做改名兼容，不改其业务语义、不重复接管已随 Variant / Configuration 投影下发的选项值映射数据**。
> - 本 CR 仅处理 OptionFamily / OptionCode 本体（投影化 + 改名）；不切断特征-配置反查（US-031）能力与每台物理车 `configurationCode` 唯一映射。
> - VMD 不再承担 OptionFamily / OptionCode 主数据治理、编码生成、审批、Golden Record、质量打分、生命周期管理等职责。
> - VMD OptionFamily / OptionCode 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **Supplier / 供应商主数据语义统一（CR-019 补充）**：
> - 供应商（Supplier）主数据的权威来源（SSOT）为 **edd-mdm 的 Party 子域**（MDM CR-006），VMD 不再是供应商主数据维护入口。
> - **与产品树各实体（CR-011~CR-018）不同，VMD 不为供应商建立本地只读投影**：VMD 彻底下线供应商本地维护能力（`Supplier` 聚合、`tb_supplier` 表、`/api/mpt/supplier/v1/**` CRUD API、应用 / 持久化栈及附属物全部移除）。
> - VMD 仅保留 `supplier_code` 作为零部件 / 设备 / 导入记录上的溯源属性透传，该编码不依赖本地供应商表、不外键约束到本地供应商表。
> - 需要供应商主数据本体者改调 edd-mdm Party 子域；仅需供应商编码者继续使用 `supplier_code` 透传。
> - 历史本地供应商表采用直接清退（方案 B），清退前完成与 MDM Party 子域的一致性核对，配套 Flyway 删表脚本（仅需求说明，不在本 CR 生成）。

> **VehicleNode / 车载节点主数据语义统一（CR-020 补充）**：
> - VehicleNode（车载节点，原 Device 设备）字典 / 类型主数据的权威来源（SSOT）为 **edd-mdm 的 EEAD 子域**（MDM CR-007 车载节点字典首版），VMD 仅保留 VehicleNode 本地只读投影副本。
> - **本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 Device 改名为 **VehicleNode（车载节点）**，VMD 同步将设备字典实体与关联键 `deviceCode` 改名为 VehicleNode / `vehicleNodeCode`。Device / `deviceCode` / 「设备」自此为历史兼容命名，新能力统一使用 VehicleNode / `vehicleNodeCode` / 「车载节点」（迁移与兼容策略见 US-015c）。
> - **边界（务必区分，避免误迁移）**：本 CR 仅处理「车载节点字典 / 类型层」主数据（节点定义、类型、功能域等「车上应有什么」）；VMD 自有的「物理设备实例 + 绑定关系」（VIN 绑定的 TBOX/IDCU/CCU/ADCU/TCU 实例，含 SN、part_number、hardware_vsn，以及绑车 / 激活 / 下线 / 密钥 / 证书等生命周期事件）属于 VMD 事务 / 实例数据，**不是主数据，不上移、不投影化、保持留在 VMD**，不得切断「车辆→零件→设备→生命周期」链路。
> - **命名消歧**：本节 VehicleNode（车载节点）区别于「物理设备实例」（VehiclePart 上绑定的具体设备，US-017）、区别于 ConfigItem（配置项，US-009）/ configCenter（配置中心）/ VehicleConfig（车辆配置，US-013）；同段落出现易混概念时一律用全称限定，避免裸用「设备」「节点」。
> - **与供应商（CR-019 彻底下线、不建投影）不同**：车载节点属于「车上有什么」（EEAD），是 VMD 车辆主数据语义核心，按产品树模式建只读投影。
> - 物理设备实例上的节点引用键随实体重命名由 `device_code` 兼容改名为 `vehicle_node_code`，仅改名、不改业务语义。
> - VMD 不再承担 VehicleNode 主数据治理、审批、合并、编码生成和生命周期管理。
> - VMD VehicleNode 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **Part / 零件主数据语义统一（CR-021 补充）**：
> - Part（零件）字典 / 类型主数据的权威来源（SSOT）为 **edd-mdm 的 Part 子域**，VMD 仅保留 Part 本地只读投影副本。
> - **本 CR 与 Brand（CR-012）/ Platform（CR-013）/ CarLine（CR-014）/ Model（CR-015）同构、区别于 Plant(CR-011)/Variant(CR-016)/Configuration(CR-017)/VehicleNode(CR-020) 的命名迁移**：Part 实体命名不变、关联键 `partCode` 不变，不涉及表 / 列重命名。**关键差异**：CR-010（Flyway V3）仅为 `veh_brand`/`veh_series`/`veh_platform` 建了 source / external_ref_id / external_version / last_sync_time 字段，**未覆盖 `veh_part`（`tb_part`）**，故 CR-021 参照 Model（CR-015）那样**需新增一条 Flyway 迁移为 `veh_part`（`tb_part`）补齐上述投影字段与 `UK(external_ref_id)` 并回填 source='MANUAL'**（接续 CR-020 的迁移序列；具体 Flyway 文件名与脚本细节留 design.md / tasks.md，本 CR 不展开）。
> - **双层边界（务必区分，避免误迁移）**：
>   - **投影层（上移 + 本地只读投影）**：Part 字典 / 类型层主数据——零件定义、零件类型、规格、part_number 字典等「车上应有哪些零件」。
>   - **留 VMD 层（不上移、不投影化、保持事务 / 实例数据）**：VIN 绑定的物理零件实例 + 绑定关系——零件→设备挂载、SN / part_number / hardware 等实例属性，装车 / 换件 / 下线 / 密钥 / 证书等生命周期事件，不得切断「车辆→零件→设备→生命周期」链路（见 US-014d、§3.6 US-017 VehiclePart、§3.8 US-026 生命周期）。
> - **与供应商（CR-019 彻底下线、不建投影）不同**：判定标准为——Part 属于「车上有什么」、是车辆主数据语义核心、处于「车辆→零件→设备」链路中，故按产品树 / VehicleNode 模式建只读投影；区别于 Supplier（CR-019 彻底下线、不建任何本地投影、仅留 `supplier_code` 透传）。
> - **命名消歧**：本节 Part（零件实体 / 字典）区别于 VehicleNode（车载节点，CR-020）、物理设备实例（VehiclePart 绑定的具体设备，US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；同段落出现易混概念时一律用全称限定。
> - **本期投影范围 = P0 必投字段集 + 投影管理字段**，不要求与 MDM `mdm_material_part` 字段完全一致；MDM 治理 / 血缘 / 审计 / 设计 PLM / 供应链 / 履历类字段一律不投影（详见 §4「Part 投影字段范围原则」）。
> - VMD 不再承担 Part 字典 / 类型层主数据治理、审批、合并、编码生成、数据质量打分和生命周期管理。
> - VMD Part 投影采用按需最小化字段设计，对 source=MDM 记录保持只读语义。

> **物理实例层语义统一（CR-022 补充）**：
> - 本 CR 的处理对象是 **VMD 自有物理实例层**（VIN 绑定的物理零件实例及其 SN / part_number / hardware 等本体属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书等事件所依附的绑定关系），即 CR-020（US-015c）/ CR-021（US-014d）反复声明「留 VMD、不上移、不投影化」的那一层。
> - **与字典 / 类型层（CR-011~021）正交**：Part（零件字典，CR-021）/ VehicleNode（车载节点字典，CR-020）的 MDM 只读投影**一律不动、不拉回、不改归属**；`part_info` / `vehicle_part` 仅以 `part_code` → `tb_mdm_part.pn`、`vehicle_node_code` → `tb_mdm_vehicle_node.code` 持有**引用键**，不复制任何字典字段、不建本地外键。
> - **命名消歧**：`PartInfo`（物理零件实例本体）区别于 Part（零件字典 / 类型，CR-021）；`VehiclePart`（车辆—零件绑定关系）区别于物理设备实例与 VehicleNode（车载节点字典，CR-020）；`VehicleInfo`（车辆主档）区别于 VehicleConfig（车辆配置，US-013）。易混处用全称限定。
> - **不切断链路**：「车辆（VehicleInfo）→零件实例（PartInfo）→设备（VehicleNode 引用）→生命周期（VehicleLifecycle）」链路语义保持不变。

> **CR-023 兼容说明**：自 CR-023 起，VMD 物理零件实例的写入收敛为统一「零件实例数据入站」，仅两个录入入口——**入口①上游系统对接**（独立入站链路，异步事件为主、批量接口兜底，源系统由对接适配层承接并打标 `source`，**不绑定 MES 具体形态**，并向上游提供入站结果回执 / 错误通知）与**入口②管理后台导入**（人工 / 文件批量，用于补录 / 纠错 / 历史迁移 / 上游未覆盖）。两入口**共用同一套入站内核**（字段校验→标准化→幂等→去重→落库→触发跨域事件），严禁后台导入旁路。**所有带 SN 的物理零件实例（含 SIM）均落 `part_info`**，VIN / 安装位置就绪时建 `vehicle_part` 绑定；下游域（SIM→TSP 连接 / 激活、TBOX/CCP/IDCM→TSP 证书、BTM→IDK）经跨域事件订阅消费，TSP/OTA/IDK 为事件消费者而非落库分支。车载节点（`vehicle_node_code`）对零件实例为**可选**属性（仅联网件 / 可升级件 / 关键件具备）。本 CR 不改字典 / 类型层（CR-020 VehicleNode / CR-021 Part 投影不动），不实现 TSP 激活 / 连接回写建模（另一条写路径，仅边界声明），不设计 MES / 产线内部采集（VMD 仅定义接收契约），不纳入 PRODUCE 整车主档入站与 US-035 死表瘦身（CR-022）。**本 CR 反转 CR-022 O86：SIM 纳入物理实例层（落 `part_info`），不再「仅走 TSP 不入实例层」。**

### 非目标（Non-Goals，本期不做）
- N1：不替代账号服务（`ExAccountService`）做用户身份/手机号实名核验。
- N2：不替代安全密钥服务（`ExSkService`）执行 IMMO_SK 的实际生成。
- N3：不实现 V2.0+ 解析器（当前仅 V1.0）。
- N4：不再充当品牌/车系/平台/**Plant（工厂）**的企业级 SSOT；不实施跨系统主数据治理（Golden Record / 审批工作流 / 数据质量打分 / 编码规则生成 / 生命周期管理）。
- N5：不再作为 **Brand（品牌）**主数据的企业级 SSOT；VMD 不负责 Brand 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与品牌生命周期管理；不要求完整复制 MDM Brand 的全部字段；不承担 MDM Brand 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD Brand 投影（CR-012）。
- N6：不再作为 **Platform（平台）**主数据的企业级 SSOT；VMD 不负责 Platform 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与平台生命周期管理；不要求完整复制 MDM Platform 的全部字段；不承担 MDM Platform 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD Platform 投影（CR-013）。
- N7：不再作为 **CarLine（车系）**主数据的企业级 SSOT；VMD 不负责 CarLine 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与车系生命周期管理；不要求完整复制 MDM CarLine 的全部字段；不承担 MDM CarLine 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD CarLine 投影；车系投影上的 `brandCode` 冗余字段为 VMD 跨域回查所需，长期保留（CR-014）。
- N8：不再作为 **Model（车型）**主数据的企业级 SSOT；VMD 不负责 Model 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与车型生命周期管理；不要求完整复制 MDM Model 的全部字段；不承担 MDM Model 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD Model 投影；不在本 CR 内改造 BaseModel / BuildConfig / FeatureFamily（后续 CR-016~018 单独处理），不得切断「车系→车型→基础车型」引用链（CR-015）。
- N9：不再作为 **Variant（版本，原 BaseModel 基础车型）**主数据的企业级 SSOT；VMD 不负责 Variant 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与版本生命周期管理；不要求完整复制 MDM Variant 的全部字段；不承担 MDM Variant 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD Variant 投影；本 CR 不改造 BaseModelFeatureCode / 特征值的归属与维护语义（仅做随实体重命名必需的引用键兼容改名），不改造 BuildConfig / FeatureFamily 的归属（CR-017 / CR-018 处理），不得切断「车系→车型→版本」及 `BuildConfig → variantCode` 引用链（CR-016）。
- N10：不再作为 **Configuration（配置，原 BuildConfig 生产配置）**主数据的企业级 SSOT；VMD 不负责 Configuration 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与配置生命周期管理；不要求完整复制 MDM Configuration 的全部字段；不承担 MDM Configuration 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑时，才通过独立 CR 纳入 VMD Configuration 投影；本 CR 不改造 BuildConfigFeatureCode / 特征值的归属与维护语义（仅做随实体重命名必需的引用键兼容改名），不改造 FeatureFamily 的归属（CR-018 处理）；不得切断「版本（Variant）→配置（Configuration）」引用链及每台物理车 `configurationCode` 唯一映射（CR-017）。
- N11：不再作为 **OptionFamily / OptionCode（选项族 / 选项值，原 FeatureFamily / FeatureCode 特征族 / 特征值）**主数据的企业级 SSOT；VMD 不负责 OptionFamily / OptionCode 主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与选项族 / 选项值生命周期管理；不要求完整复制 MDM OptionFamily / OptionCode 的全部字段；不承担 MDM OptionFamily / OptionCode 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查（US-031）、展示或校验逻辑时，才通过独立 CR 纳入 VMD OptionFamily / OptionCode 投影；本 CR 仅做随实体重命名必需的引用键兼容改名（`familyCode`→`optionFamilyCode`、`featureCode`→`optionCode`，含 Variant / Configuration 侧引用键），**不重复接管已随 Variant（CR-016）/ Configuration（CR-017）投影下发的选项值映射数据**；旧字段、旧接口、旧权限点的最终下线留待后续兼容性清理 CR（CR-018）。
- N12：VMD 不再作为 **Supplier（供应商）**主数据的维护入口或 SSOT；供应商主数据治理、审批、编码生成、生命周期管理由 edd-mdm Party 子域承担。**区别于产品树各实体（CR-011~CR-018），VMD 不为供应商建立任何本地只读投影**，彻底下线供应商本地维护能力（`Supplier` 聚合、`tb_supplier` 表、`/api/mpt/supplier/v1/**` CRUD API 及附属物）；仅保留 `supplier_code` 作为溯源属性透传，`supplier_code` 及其导入写入逻辑不在下线 / 清退范围（CR-019）。
- N13：VMD 不再作为 **VehicleNode（车载节点字典，原 Device 设备）**主数据的企业级 SSOT；VMD 不负责节点字典治理、审批、编码生成、数据质量打分、Golden Record 合并与节点生命周期管理；不要求完整复制 MDM VehicleNode 的全部字段；不承担 MDM VehicleNode 字段变化的自动适配责任，仅当字段变化影响 VMD 的车辆导入、查询、追溯、展示或校验逻辑时，才通过独立 CR 纳入 VMD VehicleNode 投影；**不纳入 EEAD 外延的通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构四块**；**物理设备实例与绑定关系（含 SN/part_number/hardware_vsn 及绑车 / 激活 / 下线 / 密钥 / 证书生命周期）不在投影范围、不上移**（CR-020）。
- N14：VMD 不再作为 **Part（零件）**字典 / 类型层主数据的企业级 SSOT；VMD 不负责 Part 字典 / 类型层主数据治理、审批、编码生成、数据质量打分、Golden Record 合并与零件生命周期管理；不要求完整复制 MDM Part（`mdm_material_part`）的全部字段；不承担 MDM Part 字段变化的自动适配责任，仅当字段变化影响 VMD 的零件 / 车辆导入、查询、追溯、展示或校验逻辑时，才通过独立 CR 调整 VMD Part 投影模型（含将某 P1 字段升入投影）；**本期仅投影 P0 必投字段集 + 投影管理字段，不投影 P1 按需字段（如 `name_local` / `description` / `category_code` / `is_safety_critical` / `is_key_part` / `is_regulatory_part` / `is_frame_part` / `lifecycle_stage` / `substitute_part_code` / `production_code` / `ffa_code`）与 P2 字段（MDM 内部主键 / 乐观锁、接入血缘、审计、设计 PLM / 物流 / 履历、时效区间等）**；**VMD 自有的物理零件实例 + 绑定关系（VIN 绑定物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书生命周期）不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**（CR-021）。
- N15：VMD 物理实例层重构**不触及字典 / 类型层主数据归属**——不把 Part / VehicleNode / 产品树各实体从 MDM 投影拉回 VMD，不改其只读投影性质；**不为 `part_info` / `vehicle_part` 建立指向字典投影表的物理外键**（仅引用键透传）；不引入零件实例的独立生命周期事件表（留后续 CR）；不改造 SIM 链路（仍走 TSP）（CR-022）。
- N16：本轮零件实例数据入站重构不重构字典 / 类型层（Part / VehicleNode / 产品树 MDM 投影不动）；不实现 TSP 激活 / 连接回写（密钥 / 证书 / 激活状态 / SIM 连接）的建模（另一条写路径，仅边界声明）；不设计 MES / 产线内部如何采集零件（VMD 仅定义接收契约）；不纳入 PRODUCE 整车主档入站（US-019，非零件实例）与 US-035 死表清退 / 主档瘦身（属 CR-022）（CR-023）。

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

#### US-002: 消费 MDM CarLine 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 CarLine 主数据并维护本地 CarLine 投影表, **so that** 每台车辆及产品树可通过 `carLineCode` 关联车系信息，同时 VMD 不再承担 CarLine 主数据维护职责。

> **语义重构（CR-014）**：本 US 由原「US-002 维护车系（CarLine）」演进而来。CarLine 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 CarLine 本地投影副本。与 Brand（CR-012）、Platform（CR-013）同构：车系实体命名不变、`carLineCode` 关联键不变，不涉及表/列重命名（直接复用 CR-010 为 `veh_carLine` 建好的 source / external_ref_id / external_version / last_sync_time 字段），区别于 Plant 的命名迁移。VMD CarLine 投影为 MDM CarLine 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM CarLine 主数据字段完全一致（字段范围见 §4「CarLine 投影字段范围原则」）。`carLineCode` 作为车辆主档与产品树的车系关联编码长期保留；车系投影上的 `brandCode` 冗余字段（由 `V2__CarLine_brand_code_migration.sql` 引入）一并保留，用于跨域回查并支撑 US-031 `getBuildConfig` 在响应中补出 `brandCode`（区别于 Brand / Platform，该冗余字段不得删除或弱化）。VMD CarLine 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-002c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 CarLineCreated / CarLineUpdated / CarLineDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 CarLine 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM CarLine 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段（至少 `code` / `name` / `brand_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD CarLine 投影表结构与 MDM CarLine 主数据模型完全一致。
- WHEN MDM CarLine 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD CarLine 投影表结构。
- WHEN MDM CarLine 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD CarLine 投影模型。
- WHEN VMD 本地 CarLine 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `carLineCode` 字段（及车系投影上的 `brandCode` 冗余字段），用于车辆车系关联、品牌跨域回查和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 CarLine 投影数据展示或关联车系信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 CarLine 投影数据支撑车辆查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `carLineCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 CarLine 信息缺失。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:carLine:list/query/export` 权限点（含 `listByBrandCode` / `listAll` 等查询能力）；`completeVehicle:product:carLine:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝，并规划后续兼容性清理 CR 下线。

#### US-002b: Bootstrap 时从 MDM 全量同步车系数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 CarLine 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 CarLine 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 CarLine 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM CarLine 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=carLine` THE SYSTEM SHALL 调用 MDM CarLine 全量快照接口拉取数据并 upsert 本地 CarLine 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 CarLine 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time，并写入 `brandCode` 冗余字段以支撑跨域回查。
- THE SYSTEM SHALL 不因 MDM CarLine 快照接口失败而删除或清空本地已有 CarLine 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD CarLine 投影所需字段（至少 `code` / `name` / `brand_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求同步 MDM CarLine 的完整字段集。

#### US-002c: CarLine 本地维护能力兼容清理
**As a** System, **I want** 将 VMD 现有 CarLine 本地维护能力逐步收敛为只读投影能力, **so that** CarLine 主数据维护职责统一回归 MDM，同时历史 source=MANUAL 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 MDM CarLine 投影语义，不再将 VMD CarLine 视为权威主数据。
- WHEN 历史 CarLine 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN CarLine 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述 CarLine 维护能力 THE SYSTEM SHALL 明确 VMD CarLine add/edit/remove 为兼容期遗留能力，不作为长期能力继续扩展。
- THE SYSTEM SHALL 规划后续兼容性清理 CR，逐步下线或隐藏 VMD CarLine 本地维护入口、旧权限点（`completeVehicle:product:carLine:add/edit/remove`）和相关后台操作。
- THE SYSTEM SHALL 保留 CarLine 查询能力，包括 `list` / `listByBrandCode` / `listAll` / `query` / `export` 及车辆详情展示所需查询。
- THE SYSTEM SHALL 保留 `carLineCode` 字段，不因维护权迁移而改名或删除。
- THE SYSTEM SHALL 保留车系投影上的 `brandCode` 冗余字段（由 `V2__CarLine_brand_code_migration.sql` 引入），不因维护权迁移而改名或删除，以持续支撑跨域回查与 US-031 `getBuildConfig` 响应中补出 `brandCode`。

#### US-003: 消费 MDM Model 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Model 主数据并维护本地 Model 投影表, **so that** 每台车辆及产品树可通过 `modelCode` 关联车型信息，同时 VMD 不再承担 Model 主数据维护职责。

> **语义重构（CR-015）**：本 US 由原「US-003 维护车型（Model）」演进而来。Model 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Model 本地投影副本。与 Brand（CR-012）、Platform（CR-013）、CarLine（CR-014）同构：车型实体命名不变、`modelCode` 关联键不变，不涉及表/列重命名，区别于 Plant 的命名迁移。**关键差异**：CR-010（Flyway V3）只为 `veh_brand`/`veh_series`/`veh_platform` 建了 source / external_ref_id / external_version / last_sync_time 字段，**未覆盖 `veh_model`**，故 CR-015 **新增 Flyway 迁移 `V6__Add_mdm_source_to_model.sql`** 为 `veh_model` 补齐上述投影字段与 `UK(external_ref_id)` 并回填 source='MANUAL'（区别于 CR-013/CR-014 复用 V3）。VMD Model 投影为 MDM Model 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Model 主数据字段完全一致（字段范围见 §4「Model 投影字段范围原则」）。`modelCode` 作为车辆主档与产品树的车型关联编码长期保留；**`veh_base_model.model_code → veh_model.code` 的「车系→车型→基础车型」引用链不得切断**（BaseModel 当前仍为 VMD 自有，见 US-004）。VMD Model 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-003c。
>
> **CR-016 后续演进**：自 CR-016 起 BaseModel 已投影化并改名为 Variant（版本），`veh_base_model` 迁移/重命名为 `veh_variant`、关联键 `baseModelCode` → `variantCode`，故本 US 中「车系→车型→基础车型」引用链现表述为 **`veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」**引用链，语义延续、不得切断（见 US-004）。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 ModelCreated / ModelUpdated / ModelDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Model 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Model 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段（至少 `code` / `name` / `platform_code` / `carLine_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD Model 投影表结构与 MDM Model 主数据模型完全一致。
- WHEN MDM Model 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Model 投影表结构。
- WHEN MDM Model 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Model 投影模型。
- WHEN VMD 本地 Model 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN 调用 `GET /api/mpt/model/v1/listByPlatformCodeAndCarLineCode` THE SYSTEM SHALL 基于本地 Model 投影返回平台+车系交集下的全部车型（查询语义不变，数据来源变为投影）。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `modelCode` 字段，用于车辆车型关联和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Model 投影数据展示或关联车型信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Model 投影数据支撑车辆查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `modelCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Model 信息缺失。
- THE SYSTEM SHALL 不切断 `veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」产品树引用链（自 CR-016 起 `veh_base_model`→`veh_variant`、`baseModelCode`→`variantCode`，语义延续）。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:model:list/query/export` 权限点；`completeVehicle:product:model:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝，并规划后续兼容性清理 CR 下线。

#### US-003b: Bootstrap 时从 MDM 全量同步车型数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Model 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Model 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Model 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Model 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=model` THE SYSTEM SHALL 调用 MDM Model 全量快照接口拉取数据并 upsert 本地 Model 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Model 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time，并写入 `platformCode` / `carLineCode` 关联字段。
- THE SYSTEM SHALL 不因 MDM Model 快照接口失败而删除或清空本地已有 Model 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Model 投影所需字段（至少 `code` / `name` / `platform_code` / `carLine_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求同步 MDM Model 的完整字段集。

#### US-003c: Model 本地维护能力兼容清理
**As a** System, **I want** 将 VMD 现有 Model 本地维护能力逐步收敛为只读投影能力, **so that** Model 主数据维护职责统一回归 MDM，同时历史 source=MANUAL 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 MDM Model 投影语义，不再将 VMD Model 视为权威主数据。
- WHEN 历史 Model 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Model 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述 Model 维护能力 THE SYSTEM SHALL 明确 VMD Model add/edit/remove 为兼容期遗留能力，不作为长期能力继续扩展。
- THE SYSTEM SHALL 规划后续兼容性清理 CR，逐步下线或隐藏 VMD Model 本地维护入口、旧权限点（`completeVehicle:product:model:add/edit/remove`）和相关后台操作。
- THE SYSTEM SHALL 保留 Model 查询能力，包括 `list` / `listByPlatformCodeAndCarLineCode` / `query` / `export` 及车辆详情展示所需查询。
- THE SYSTEM SHALL 保留 `modelCode` 字段，不因维护权迁移而改名或删除。
- THE SYSTEM SHALL 保留 `veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」引用链（自 CR-016 起 `veh_base_model`→`veh_variant`），Variant（原 BaseModel）的投影化与命名迁移由 CR-016 处理（见 US-004 / US-004c）。

#### US-004: 消费 MDM Variant（版本，原 BaseModel 基础车型）主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Variant 主数据并维护本地 Variant 投影表, **so that** 每台车辆及产品树可通过 `variantCode` 关联版本信息，同时 VMD 不再承担 Variant 主数据维护职责。

> **语义重构 + 命名迁移（CR-016）**：本 US 由原「US-004 维护基础车型（BaseModel）及其特征值（BaseModelFeatureCode）」演进而来。Variant 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Variant 本地投影副本。**本 CR 与 Plant（CR-011）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 BaseModel 改名为 **Variant（版本）**，VMD 同步将 `veh_base_model` 迁移/重命名为 `veh_variant`、关联键 `baseModelCode` → `variantCode`。BaseModel / `baseModelCode` / 「基础车型」自此为历史兼容命名，新能力统一使用 Variant / `variantCode` / 「版本」（迁移与兼容策略见 US-004c）。**关键差异**：BaseModel 投影字段（source / external_ref_id / external_version / last_sync_time）此前未建立，且涉及表/键重命名，故 CR-016 **新增 Flyway 迁移 `V7__Migrate_base_model_to_variant.sql`** 与 **`V8__Migrate_base_model_code_to_variant_code.sql`**（区别于 CR-013/CR-014 复用 V3、CR-015 新增 V6）。VMD Variant 投影为 MDM Variant 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Variant 主数据字段完全一致（字段范围见 §4「Variant 投影字段范围原则」）。保留 `listByPlatformCodeAndCarLineCodeAndModelCode` 三参组合查询语义（数据来源变为投影）；不切断 `veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」引用链。**BaseModelFeatureCode / 特征值的业务语义本 CR 不变**（仍可按既有方式查询/挂载），仅做随实体重命名所必需的引用键改名与兼容（`base_model_code` → `variant_code`），其最终归属留待后续 CR。VMD Variant 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-004c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 VariantCreated / VariantUpdated / VariantDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Variant 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Variant 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段（至少 `code`（即 `variantCode` 关联键）/ `name` / `platform_code` / `car_line_code` / `model_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD Variant 投影表结构与 MDM Variant 主数据模型完全一致。
- WHEN MDM Variant 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Variant 投影表结构。
- WHEN MDM Variant 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Variant 投影模型。
- WHEN VMD 本地 Variant 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN 调用 `GET /api/mpt/variant/v1/listByPlatformCodeAndCarLineCodeAndModelCode` THE SYSTEM SHALL 基于本地 Variant 投影支持 `platformCode/carLineCode/modelCode` 三参数任意组合查询（查询语义不变，数据来源变为投影）；迁移期保留旧路径 `GET /api/mpt/baseModel/v1/listByPlatformCodeAndCarLineCodeAndModelCode` 兼容。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `variantCode` 字段（承接原 `baseModelCode` 语义），用于车辆版本关联和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Variant 投影数据展示或关联版本信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Variant 投影数据支撑车辆查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `variantCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Variant 信息缺失。
- IF 历史数据仅存在 `baseModelCode` 且未完成字段迁移 THEN THE SYSTEM SHALL 支持 `baseModelCode` 到 `variantCode` 的兼容读取或迁移处理。
- THE SYSTEM SHALL 不切断 `veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」产品树引用链。
- THE SYSTEM SHALL 保持 BaseModelFeatureCode / 特征值的既有业务语义不变（仍可按既有方式查询/挂载），仅将其引用键 `base_model_code` 随实体重命名兼容改名为 `variant_code`；其特征值引用键 `feature_code` 自 CR-018 起随 FeatureCode→OptionCode 重命名兼容改名为 `option_code`（仅引用键改名，业务语义不变，不重复接管已随投影下发的选项值映射），OptionFamily / OptionCode 本体的投影化与归属由 CR-018 处理（见 US-008）。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:variant:list/query/export` 权限点；`completeVehicle:product:variant:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝；原 `completeVehicle:product:baseModel:*` 权限点标记 `deprecated` 并规划后续兼容性清理 CR 下线。

#### US-004b: Bootstrap 时从 MDM 全量同步 Variant 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Variant 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Variant 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Variant 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Variant 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=variant` THE SYSTEM SHALL 调用 MDM Variant 全量快照接口拉取数据并 upsert 本地 Variant 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Variant 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time，并写入 `platformCode` / `carLineCode` / `modelCode` 关联字段以支撑产品树链路。
- THE SYSTEM SHALL 不因 MDM Variant 快照接口失败而删除或清空本地已有 Variant 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Variant 投影所需字段（至少 `code` / `name` / `platform_code` / `car_line_code` / `model_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求同步 MDM Variant 的完整字段集。

#### US-004c: Variant 本地维护能力兼容清理与 BaseModel→Variant 命名迁移
**As a** System, **I want** 将 VMD 现有 BaseModel 本地维护能力收敛为只读 Variant 投影能力，并将 BaseModel / `baseModelCode` 命名迁移为 Variant / `variantCode`, **so that** Variant 主数据维护职责统一回归 MDM、VMD 与 MDM 在版本主数据语义与命名上保持一致，同时历史 source=MANUAL 数据、历史 `baseModelCode` 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 执行数据库迁移 THE SYSTEM SHALL 将原 `veh_base_model`（`tb_veh_base_model`）迁移或重命名为 `veh_variant`（`tb_veh_variant`），保留现有列 `code` / `name` / `platform_code` / `car_line_code` / `model_code` 不变。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 为 `veh_variant` 新增 `source` / `external_ref_id` / `external_version` / `last_sync_time` 字段，增加 `UK(external_ref_id)`，并回填历史数据 source='MANUAL'。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 为车辆主档（`veh_basic_info`）新增 `variant_code` 字段，承接原 `base_model_code` 语义并回填历史值。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 将 BuildConfig 相关表（`veh_build_config`）的 `base_model_code` 迁移/回填为 `variant_code`；将 `veh_base_model_feature_code` 的 `base_model_code` 随实体重命名迁移/回填为 `variant_code`（仅引用键改名，特征值业务语义不变）。
- WHEN 迁移期间仍存在旧接口、旧字段或旧权限点调用 THE SYSTEM SHALL 提供兼容策略（兼容读取 / 字段映射 / 旧列保留 / 旧接口保留），避免既有调用方立即失败，历史 `base_model_code` / `baseModelCode` 数据不得丢失、历史车辆可继续查询追溯。
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 Variant / `variantCode` 命名，不再将 VMD Variant 视为权威主数据。
- WHEN 历史 Variant 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Variant 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述历史兼容逻辑 THE SYSTEM SHALL 明确 BaseModel / `baseModelCode` / 「基础车型」为遗留命名，不再作为新能力命名，仅出现在历史兼容、迁移说明或旧字段映射场景。
- THE SYSTEM SHALL 在迁移完成后逐步废弃 BaseModel 命名的 Controller / AppService / Repository / DTO / VO / API path（如 `/api/mpt/baseModel/**`），迁移为 Variant 命名（如 `/api/mpt/variant/**`），旧接口在兼容期保留。
- THE SYSTEM SHALL 将原 `completeVehicle:product:baseModel:*` 权限点调整为 `completeVehicle:product:variant:list/query/export`；`add/edit/remove` 仅作兼容期遗留（仅作用于 source=MANUAL 过渡数据，对 source=MDM 一律拒绝），旧 baseModel 权限点标记 `deprecated` 并规划后续下线。
- THE SYSTEM SHALL 保留 Variant 查询能力，包括 `list` / `listByPlatformCodeAndCarLineCodeAndModelCode` / `query` / `export` 及车辆详情展示所需查询。
- THE SYSTEM SHALL 保留并回填 `variantCode` 关联键，不因维护权迁移或命名迁移而丢失历史数据；保留 `veh_variant.model_code → veh_model.code` 的「车系→车型→版本」引用链与 `BuildConfig → variantCode` 引用链。
- THE SYSTEM SHALL 将 BaseModel→Variant、`baseModelCode`→`variantCode` 的重命名影响纳入本次 CR（CR-016）的兼容性说明，旧字段、旧接口、旧权限点的最终下线由后续兼容性清理 CR 完成。

#### US-005: 消费 MDM Configuration（配置，原 BuildConfig）主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Configuration 配置主数据并维护本地 Configuration 投影表, **so that** 每台物理车辆可通过唯一 `configurationCode` 映射到配置并支撑特征-配置反查，同时 VMD 不再承担 Configuration 配置主数据维护职责。

> **语义重构 + 命名迁移（CR-017）**：本 US 由原「US-005 维护生产配置（BuildConfig）及其特征值（BuildConfigFeatureCode）」演进而来。Configuration 配置主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Configuration 本地投影副本。**本 CR 与 Plant（CR-011）/ Variant（CR-016）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 BuildConfig 改名为 **Configuration（配置）**，VMD 同步将配置实体与关联键 `buildConfigCode` 改名为 Configuration / `configurationCode`。BuildConfig / `buildConfigCode` / 「生产配置」自此为历史兼容命名，新能力统一使用 Configuration / `configurationCode` / 「配置」（迁移与兼容策略见 US-005c）。⚠️ 命名消歧：本 US 的 Configuration（配置）区别于 VehicleConfig（车辆配置，US-013）、ConfigItem（配置项，US-009）、configCenter（配置中心），易混处用全称限定。VMD Configuration 投影为 MDM Configuration 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Configuration 主数据字段完全一致（字段范围见 §4「Configuration 投影字段范围原则」）。`configurationCode` 作为车辆主档的核心锚点（每台物理车唯一映射）长期保留，承接原 `buildConfigCode` 语义并回填历史值。**BuildConfigFeatureCode / 特征值的业务语义本 CR 不变**（仍可按既有方式查询/挂载，并支撑 US-031 反查），仅做随实体重命名所必需的引用键改名与兼容，其最终归属与 FeatureFamily 改造留待 CR-018。VMD Configuration 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-005c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 ConfigurationCreated / ConfigurationUpdated / ConfigurationDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Configuration 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Configuration 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需最小字段集（至少 `code`（即 `configurationCode` 关联键）/ `name` / `variant_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`，以及支撑 US-031 反查所需的特征值映射），不要求 VMD Configuration 投影表结构与 MDM Configuration 主数据模型完全一致。
- WHEN MDM Configuration 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Configuration 投影表结构。
- WHEN MDM Configuration 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Configuration 投影模型。
- WHEN VMD 本地 Configuration 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN 调用 `GET /api/mpt/configuration/v1/listByVariantCode/{variantCode}` THE SYSTEM SHALL 基于本地 Configuration 投影返回该版本（Variant）下全部配置（查询语义不变，数据来源变为投影）；迁移期保留旧路径 `GET /api/mpt/buildConfig/v1/listByVariantCode/{variantCode}` 与 `GET /api/mpt/buildConfig/v1/listByBaseModelCode/{baseModelCode}` 兼容（按 `buildConfig` → `configuration`、`baseModelCode` → `variantCode` 映射读取）。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `configurationCode` 字段（承接原 `buildConfigCode` 语义），用于车辆配置关联和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Configuration 投影数据展示或关联配置信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Configuration 投影数据支撑车辆查询、特征-配置反查、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `configurationCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Configuration 信息缺失。
- IF 历史数据仅存在 `buildConfigCode` 且未完成字段迁移 THEN THE SYSTEM SHALL 支持 `buildConfigCode` 到 `configurationCode` 的兼容读取或映射处理。
- THE SYSTEM SHALL 不切断「版本（Variant）→配置（Configuration）」引用链与每台物理车 `configurationCode` 唯一映射。
- THE SYSTEM SHALL 保持 BuildConfigFeatureCode / 特征值的既有业务语义不变（仍可按既有方式查询/挂载并支撑 US-031 反查），仅将其引用键随实体重命名兼容改名；其特征值引用键 `feature_code` 自 CR-018 起随 FeatureCode→OptionCode 重命名兼容改名为 `option_code`（仅引用键改名，业务语义不变，不重复接管已随 Configuration 投影下发的选项值映射），OptionFamily / OptionCode 本体的投影化与归属由 CR-018 处理（见 US-008）。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:configuration:list/query/export` 权限点；`completeVehicle:product:configuration:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝；原 `completeVehicle:product:buildConfig:*` 权限点标记 `deprecated` 并规划后续兼容性清理 CR 下线。

#### US-005b: Bootstrap 时从 MDM 全量同步 Configuration 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Configuration 配置数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Configuration 配置主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Configuration 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Configuration 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=configuration` THE SYSTEM SHALL 调用 MDM Configuration 全量快照接口拉取数据并 upsert 本地 Configuration 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Configuration 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time，并写入 `variantCode` 关联字段及支撑 US-031 反查的特征值映射。
- THE SYSTEM SHALL 不因 MDM Configuration 快照接口失败而删除或清空本地已有 Configuration 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Configuration 投影所需最小字段集（至少 `code` / `name` / `variant_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time` 及特征值映射），不要求同步 MDM Configuration 的完整字段集。

#### US-005c: Configuration 本地维护能力兼容清理与 BuildConfig→Configuration 命名迁移
**As a** System, **I want** 将 VMD 现有 BuildConfig 本地维护能力收敛为只读 Configuration 投影能力，并将 BuildConfig / `buildConfigCode` 命名迁移为 Configuration / `configurationCode`, **so that** Configuration 配置主数据维护职责统一回归 MDM、VMD 与 MDM 在配置主数据语义与命名上保持一致，同时历史 source=MANUAL 数据、历史 `buildConfigCode` 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 迁移期间仍存在旧接口、旧字段或旧权限点调用 THE SYSTEM SHALL 提供兼容策略（兼容读取 / 字段映射 / 旧列保留 / 旧接口保留），避免既有调用方立即失败，历史 `buildConfigCode` / 旧配置数据不得丢失、历史车辆可继续查询追溯。
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 Configuration / `configurationCode` 命名，不再将 VMD Configuration 视为权威主数据。
- WHEN 历史 Configuration 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Configuration 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述历史兼容逻辑 THE SYSTEM SHALL 明确 BuildConfig / `buildConfigCode` / 「生产配置」为遗留命名，不再作为新能力命名，仅出现在历史兼容、迁移说明或旧字段映射场景。
- THE SYSTEM SHALL 在迁移完成后逐步废弃 BuildConfig 命名的 Controller / AppService / Repository / DTO / VO / API path（如 `/api/mpt/buildConfig/**`），迁移为 Configuration 命名（如 `/api/mpt/configuration/**`），旧接口在兼容期保留。
- THE SYSTEM SHALL 将原 `completeVehicle:product:buildConfig:*` 权限点调整为 `completeVehicle:product:configuration:list/query/export`；`add/edit/remove` 仅作兼容期遗留（仅作用于 source=MANUAL 过渡数据，对 source=MDM 一律拒绝），旧 buildConfig 权限点标记 `deprecated` 并规划后续下线。
- THE SYSTEM SHALL 保留 Configuration 查询能力，包括 `list` / `listByVariantCode` / `query` / `export` 及车辆详情展示、US-031 特征-配置反查所需查询。
- THE SYSTEM SHALL 保留并回填 `configurationCode` 关联键，不因维护权迁移或命名迁移而丢失历史数据；保留「版本（Variant）→配置（Configuration）」引用链与每台物理车 `configurationCode` 唯一映射。
- THE SYSTEM SHALL 保持 BuildConfigFeatureCode / 特征值的既有业务语义不变，仅随实体重命名做引用键兼容改名；其特征值引用键 `feature_code` 自 CR-018 起随 FeatureCode→OptionCode 重命名兼容改名为 `option_code`（仅引用键改名，业务语义不变），OptionFamily / OptionCode 本体的投影化与归属及 FeatureFamily 改造由 CR-018 处理（见 US-008），旧物的最终下线由后续兼容性清理 CR 完成。
- THE SYSTEM SHALL 将 BuildConfig→Configuration、`buildConfigCode`→`configurationCode` 的重命名影响纳入本次 CR（CR-017）的兼容性说明，旧字段、旧接口、旧权限点的最终下线由后续兼容性清理 CR 完成。（注：具体迁移脚本、字段物理改名、Flyway 文件等实现细节放 design.md / tasks.md。）

#### US-006: 消费 MDM Platform 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Platform 主数据并维护本地 Platform 投影表, **so that** 每台车辆及产品树可通过 `platformCode` 关联平台信息，同时 VMD 不再承担 Platform 主数据维护职责。

> **语义重构（CR-013）**：本 US 由原「US-006 维护车辆平台（Platform）」演进而来。Platform 主数据 SSOT 上移至 edd-mdm，VMD 仅保留 Platform 本地投影副本。与 Brand（CR-012）同构：平台实体命名不变、`platformCode` 关联键不变，不涉及表/列重命名（直接复用 CR-010 为 `veh_platform` 建好的 source / external_ref_id / external_version / last_sync_time 字段，`veh_platform.code` 即 `platform_code`、`name` 即 `platform_name`）。VMD Platform 投影为 MDM Platform 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM Platform 主数据字段完全一致（字段范围见 §4「Platform 投影字段范围原则」）。`platformCode` 作为车辆主档（`veh_basic_info.platform_code`）与产品树（`veh_model.platform_code` / `veh_variant.platform_code`，原 `veh_base_model.platform_code`，CR-016）的平台关联编码长期保留。VMD Platform 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-006c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 PlatformCreated / PlatformUpdated / PlatformDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Platform 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM Platform 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段，不要求 VMD Platform 投影表结构与 MDM Platform 主数据模型完全一致。
- WHEN MDM Platform 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Platform 投影表结构。
- WHEN MDM Platform 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Platform 投影模型。
- WHEN VMD 本地 Platform 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / delete 操作，并返回明确错误。
- WHEN VMD 处理车辆生产导入数据 THE SYSTEM SHALL 保留并写入 `platformCode` 字段，用于车辆平台关联和追溯。
- WHEN 查询车辆详情 THE SYSTEM SHALL 可基于本地 Platform 投影数据展示或关联平台信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Platform 投影数据支撑车辆查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `platformCode` THEN THE SYSTEM SHALL 不阻断历史车辆查询，但应在展示或校验结果中体现 Platform 信息缺失。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:platform:list/query/export` 权限点；`completeVehicle:product:platform:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝，并规划后续兼容性清理 CR 下线。

#### US-006b: Bootstrap 时从 MDM 全量同步平台数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Platform 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Platform 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Platform 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Platform 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=platform` THE SYSTEM SHALL 调用 MDM Platform 全量快照接口拉取数据并 upsert 本地 Platform 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Platform 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。
- THE SYSTEM SHALL 不因 MDM Platform 快照接口失败而删除或清空本地已有 Platform 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Platform 投影所需字段，不要求同步 MDM Platform 的完整字段集。

#### US-006c: Platform 本地维护能力兼容清理
**As a** System, **I want** 将 VMD 现有 Platform 本地维护能力逐步收敛为只读投影能力, **so that** Platform 主数据维护职责统一回归 MDM，同时历史 source=MANUAL 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 MDM Platform 投影语义，不再将 VMD Platform 视为权威主数据。
- WHEN 历史 Platform 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Platform 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述 Platform 维护能力 THE SYSTEM SHALL 明确 VMD Platform add/edit/remove 为兼容期遗留能力，不作为长期能力继续扩展。
- THE SYSTEM SHALL 规划后续兼容性清理 CR，逐步下线或隐藏 VMD Platform 本地维护入口、旧权限点和相关后台操作。
- THE SYSTEM SHALL 保留 Platform 查询能力，包括列表、详情、listAll 或车辆详情展示所需查询。
- THE SYSTEM SHALL 保留 `platformCode` 字段，不因维护权迁移而改名或删除。

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

### 3.2 选项族（OptionFamily） & 配置项域

> **章节命名消歧（CR-018）**：本节标题由原「特征族 & 配置项域」演进而来。其中「选项族（OptionFamily，原 FeatureFamily 特征族）/ 选项值（OptionCode，原 FeatureCode 特征值）」（US-008）已投影化并改名，区别于「配置项（ConfigItem）」（US-009）及其下的「枚举值 Option」；易混处一律用全称限定。

#### US-008: 消费 MDM OptionFamily / OptionCode 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 OptionFamily（选项族）/ OptionCode（选项值）主数据并维护本地只读投影表, **so that** 版本（Variant）/ 配置（Configuration）可通过 `optionFamilyCode` / `optionCode` 引用选项并支撑特征-配置反查，同时 VMD 不再承担 OptionFamily / OptionCode 主数据维护职责。

> **语义重构 + 命名迁移（CR-018）**：本 US 由原「US-008 维护特征族（FeatureFamily）及其特征值（FeatureCode）」演进而来。OptionFamily / OptionCode 主数据 SSOT 上移至 edd-mdm，VMD 仅保留本地只读投影副本。**本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 FeatureFamily / FeatureCode 改名为 **OptionFamily / OptionCode**，VMD 同步将实体与关联键 `familyCode` / `featureCode` 改名为 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode`。FeatureFamily / FeatureCode / `familyCode` / `featureCode` / 「特征族」「特征值」自此为历史兼容命名，新能力统一使用 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode` / 「选项族」「选项值」（迁移与兼容策略见 US-008c）。⚠️ 命名消歧：本 US 的 OptionFamily / OptionCode（选项族 / 选项值）区别于 ConfigItem（配置项，US-009）下的「枚举值 Option」、configCenter（配置中心）、VehicleConfig（车辆配置，US-013），易混处用全称限定。VMD OptionFamily / OptionCode 投影为 MDM 对应主数据在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM 字段完全一致（字段范围见 §4「OptionFamily / OptionCode 投影字段范围原则」）。`optionFamilyCode` / `optionCode` 作为关联键长期保留，承接原 `familyCode` / `featureCode` 语义并回填历史值。**随实体重命名所必需的引用键——Variant 侧（原 BaseModelFeatureCode，CR-016）、Configuration 侧（原 BuildConfigFeatureCode，CR-017）的特征值引用键——一并由 `featureCode` 兼容改名为 `optionCode`，仅做改名兼容，不改其业务语义、不重复接管已随 Variant / Configuration 投影下发的选项值映射数据**。VMD OptionFamily / OptionCode 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-008c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 OptionFamilyCreated / OptionFamilyUpdated / OptionFamilyDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 OptionFamily 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN MDM 通过 Kafka 推送 OptionCodeCreated / OptionCodeUpdated / OptionCodeDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 OptionCode 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM OptionFamily / OptionCode 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需字段（OptionFamily 至少 `code`（即 `optionFamilyCode` 关联键）/ `name` / `type` / `source` / `external_ref_id` / `external_version` / `last_sync_time`；OptionCode 至少 `code`（即 `optionCode` 关联键）/ `name` / `option_family_code` / `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD 投影表结构与 MDM 主数据模型完全一致。
- WHEN MDM OptionFamily / OptionCode 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD 投影表结构。
- WHEN MDM OptionFamily / OptionCode 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD OptionFamily / OptionCode 投影模型。
- WHEN VMD 本地 OptionFamily / OptionCode 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / remove 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）；add/edit/remove 仅作为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据。
- WHEN 调用 `GET /api/mpt/optionFamily/v1/listAllOptionCode?optionFamilyCode=<x>` THE SYSTEM SHALL 基于本地投影返回该选项族下全部选项值（不分页，查询语义不变，数据来源变为投影）；迁移期保留旧路径 `GET /api/mpt/featureFamily/v1/listAllFeatureCode?familyCode=<x>` 兼容（按 `featureFamily` → `optionFamily`、`familyCode` → `optionFamilyCode`、`featureCode` → `optionCode` 映射读取）。
- THE SYSTEM SHALL 保留 OptionFamily / OptionCode 只读查询能力（如 `listAll`、按 `optionFamilyCode` 列出 `optionCode` 等），查询语义不变、数据源变为投影；迁移期保留旧 `familyCode` / `featureCode` 入参与旧路径兼容映射。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 OptionFamily / OptionCode 投影数据支撑选项引用、特征-配置反查、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `optionFamilyCode` / `optionCode` THEN THE SYSTEM SHALL 不阻断历史车辆 / 配置查询，但应在展示或校验结果中体现选项信息缺失。
- IF 历史数据仅存在 `familyCode` / `featureCode` 且未完成字段迁移 THEN THE SYSTEM SHALL 支持 `familyCode` → `optionFamilyCode`、`featureCode` → `optionCode` 的兼容读取或映射处理。
- THE SYSTEM SHALL 不切断特征-配置反查（US-031）能力与每台物理车 `configurationCode` 唯一映射。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:optionFamily:list/query/export` 与 `completeVehicle:product:optionCode:list/query/export` 权限点；`completeVehicle:product:optionFamily:add/edit/remove` 与 `completeVehicle:product:optionCode:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝；原 `completeVehicle:product:featureFamily:*` / `completeVehicle:product:featureCode:*` 权限点标记 `deprecated` 并规划后续兼容性清理 CR 下线。

#### US-008b: Bootstrap 时从 MDM 全量同步 OptionFamily / OptionCode 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 OptionFamily / OptionCode 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 OptionFamily / OptionCode 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 OptionFamily / OptionCode 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM OptionFamily / OptionCode 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=optionFamily` THE SYSTEM SHALL 调用 MDM OptionFamily 全量快照接口拉取数据并 upsert 本地 OptionFamily 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=optionCode` THE SYSTEM SHALL 调用 MDM OptionCode 全量快照接口拉取数据并 upsert 本地 OptionCode 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 OptionFamily 与 OptionCode 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time，并为 OptionCode 写入 `optionFamilyCode` 归属关联字段。
- THE SYSTEM SHALL 不因 MDM OptionFamily / OptionCode 快照接口失败而删除或清空本地已有投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD OptionFamily / OptionCode 投影所需字段，不要求同步 MDM 的完整字段集。

#### US-008c: OptionFamily / OptionCode 本地维护能力兼容清理与 Feature→Option 命名迁移
**As a** System, **I want** 将 VMD 现有 FeatureFamily / FeatureCode 本地维护能力收敛为只读 OptionFamily / OptionCode 投影能力，并将 FeatureFamily / FeatureCode / `familyCode` / `featureCode` 命名迁移为 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode`, **so that** OptionFamily / OptionCode 主数据维护职责统一回归 MDM、VMD 与 MDM 在选项族 / 选项值主数据语义与命名上保持一致，同时历史 source=MANUAL 数据、历史 `familyCode` / `featureCode` 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 迁移期间仍存在旧接口、旧字段或旧权限点调用 THE SYSTEM SHALL 提供兼容策略（兼容读取 / 字段映射 / 旧列保留 / 旧接口保留），避免既有调用方立即失败，历史 `familyCode` / `featureCode` / 旧选项数据不得丢失、历史车辆与配置可继续查询追溯。
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode` 命名，不再将 VMD OptionFamily / OptionCode 视为权威主数据。
- WHEN 历史 OptionFamily / OptionCode 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN OptionFamily / OptionCode 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述历史兼容逻辑 THE SYSTEM SHALL 明确 FeatureFamily / FeatureCode / `familyCode` / `featureCode` / 「特征族」「特征值」为遗留命名，不再作为新能力命名，仅出现在历史兼容、迁移说明或旧字段映射场景。
- THE SYSTEM SHALL 在迁移完成后逐步废弃 FeatureFamily / FeatureCode 命名的 Controller / AppService / Repository / DTO / VO / API path（如 `/api/mpt/featureFamily/**`），迁移为 OptionFamily / OptionCode 命名（如 `/api/mpt/optionFamily/**`），旧接口在兼容期保留。
- THE SYSTEM SHALL 将原 `completeVehicle:product:featureFamily:*` / `completeVehicle:product:featureCode:*` 权限点调整为 `completeVehicle:product:optionFamily:list/query/export` / `completeVehicle:product:optionCode:list/query/export`；`add/edit/remove` 仅作兼容期遗留（仅作用于 source=MANUAL 过渡数据，对 source=MDM 一律拒绝），旧 featureFamily / featureCode 权限点标记 `deprecated` 并规划后续下线。
- THE SYSTEM SHALL 保留 OptionFamily / OptionCode 查询能力，包括 `listAll` / `listAllOptionCode`（按 `optionFamilyCode`）/ `query` / `export` 及特征-配置反查（US-031）所需查询。
- THE SYSTEM SHALL 保留并回填 `optionFamilyCode` / `optionCode` 关联键，不因维护权迁移或命名迁移而丢失历史数据；**对 Variant 侧（原 BaseModelFeatureCode）、Configuration 侧（原 BuildConfigFeatureCode）的特征值引用键 `featureCode` 随实体重命名兼容改名为 `optionCode`，仅做引用键改名，不改业务语义、不重复接管已随 Variant（CR-016）/ Configuration（CR-017）投影下发的选项值映射数据**。
- THE SYSTEM SHALL 将 FeatureFamily→OptionFamily、FeatureCode→OptionCode、`familyCode`→`optionFamilyCode`、`featureCode`→`optionCode` 的重命名影响纳入本次 CR（CR-018）的兼容性说明，旧字段、旧接口、旧权限点的最终下线由后续兼容性清理 CR 完成。（注：具体迁移脚本、字段物理改名、Flyway 文件等实现细节放 design.md / tasks.md。）

#### US-009: 维护配置项（ConfigItem）+ 枚举值（Option）+ 映射（Mapping）
**As a** Mpt-User, **I want** 配置项 CRUD、枚举值 CRUD、上下游映射 CRUD, **so that** 不同来源系统的字段能在 VMD 内完成翻译。

**Acceptance Criteria**:
- THE SYSTEM SHALL 提供 `GET /api/mpt/configItem/v1/listAll` 返回全部配置项。
- WHEN 操作枚举值 / 映射 THE SYSTEM SHALL 通过 `configItemCode` 路径定位归属配置项。
- WHEN 创建配置项 IF `code` 已存在 THEN THE SYSTEM SHALL 返回唯一性失败。

### 3.3 物理车辆登记域

#### US-010: 车辆基础信息查询/删除/导出
**As a** Mpt-User, **I want** 分页查询车辆（按 VIN/configurationCode/时间窗口）、按 VIN 查询、按 ID 批量删除, **so that** 运维诊断和数据治理可执行。

**Acceptance Criteria**:
- WHEN Mpt-User 调用 `GET /api/mpt/vehicle/v1/list` THE SYSTEM SHALL 支持 `vin/configurationCode/beginTime/endTime` 过滤并分页（`startPage`+`getPageResult`），且 `vin` 走模糊匹配（`ParamHelper.fuzzyQueryParam`）；迁移期保留旧过滤参数 `buildConfigCode` 兼容（按 `buildConfigCode` → `configurationCode` 映射，CR-017）。
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

> **章节命名消歧（CR-020）**：本节「设备」相关能力（US-015）自 CR-020 起演进——设备字典 / 类型层主数据投影化并改名为「车载节点（VehicleNode，原 Device）」，消费 MDM EEAD 子域主数据；其中**物理设备实例 + 绑定关系仍为 VMD 自有，不上移、不投影化**（见 §3.6 US-017 VehiclePart）。VehicleNode（车载节点字典）区别于物理设备实例、ConfigItem（配置项，US-009）；易混处用全称限定。供应商相关能力（US-016）已于 CR-019 彻底下线（不建投影）。

> **章节命名消歧（CR-021）**：本节「零件」相关能力（US-014）自 CR-021 起演进——零件字典 / 类型层主数据投影化为「Part（零件）本地只读投影」，消费 MDM Part 子域主数据；其中**物理零件实例 + 绑定关系仍为 VMD 自有，不上移、不投影化**（见 §3.6 US-017 VehiclePart、§3.8 US-026 生命周期、US-014d 边界声明）。Part（零件实体 / 字典）区别于 VehicleNode（车载节点，CR-020）、物理设备实例（US-017）、ConfigItem（配置项，US-009）；易混处用全称限定。

#### US-014: 消费 MDM Part 主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 Part（零件）字典 / 类型主数据并维护本地只读投影表, **so that** 零件 / 车辆场景可通过 `partCode` 关联零件信息、零件 / 车辆详情可展示零件信息，同时 VMD 不再承担 Part 字典 / 类型层主数据维护职责。

> **语义重构（CR-021）**：本 US 由原「US-014 维护零件信息（Part）」演进而来。Part 字典 / 类型主数据 SSOT 上移至 **edd-mdm 的 Part 子域**，VMD 仅保留 Part 本地只读投影副本。**本 CR 与 Brand（CR-012）/ Platform（CR-013）/ CarLine（CR-014）/ Model（CR-015）同构、区别于 Plant / Variant / Configuration / VehicleNode 的命名迁移**：Part 实体命名不变、`partCode` 关联键不变，不涉及表 / 列重命名，Part 复用现有 `veh_part`（`tb_part`）。**关键差异**：CR-010（Flyway V3）未覆盖 `veh_part`（`tb_part`），故 CR-021 参照 Model（CR-015）那样**需新增一条 Flyway 迁移为 `veh_part`（`tb_part`）补齐 source / external_ref_id / external_version / last_sync_time 字段与 `UK(external_ref_id)` 并回填 source='MANUAL'**（接续 CR-020 序列；脚本与文件名留 design.md / tasks.md）。⚠️ **双层边界**：本 CR 仅处理「Part 字典 / 类型层」主数据（零件定义、零件类型、规格、part_number 字典）；**VMD 自有的物理零件实例 + 绑定关系（VIN 绑定的物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书生命周期）不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**（见 US-014d）。⚠️ **命名消歧**：本 US 的 Part（零件实体 / 字典）区别于 VehicleNode（车载节点，CR-020）、物理设备实例（US-017）、ConfigItem（配置项，US-009）。VMD Part 投影为 MDM Part 在 VMD bounded context 下的按需最小化只读视图，**本期仅持久化 P0 必投字段集 + 投影管理字段**，不要求与 MDM `mdm_material_part` 字段完全一致（字段范围见 §4「Part 投影字段范围原则」）。`partCode` 作为车辆 / 物理零件实例的零件关联编码长期保留；零件上的 `vehicleNodeCode`（承接原 `deviceCode`，CR-020）/ `supplier_code`（透传溯源，延续 CR-019）继续保留。VMD Part 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-014c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 PartCreated / PartUpdated / PartDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 Part 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据（`event.version` 统一映射进 `external_version`，不原样照搬 MDM `version` 列）。
- WHEN 同步 MDM Part 数据 THE SYSTEM SHALL 仅持久化 P0 必投字段集（`code`（即 `partCode` 关联键）/ `name` / `part_type` / `vehicle_node_code` / `supplier_code` / `is_software` / `fota_upgradeable` / `is_accurately_traced` / `status`）+ 投影管理字段（`source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD Part 投影表结构与 MDM Part 主数据模型完全一致。
- WHEN MDM Part 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD Part 投影表结构。
- WHEN MDM Part 字段变化影响 VMD 的零件 / 车辆导入、查询、追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD Part 投影模型（含将某 P1 字段升入投影）。
- WHEN VMD 本地 Part 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / remove 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN VMD 处理车辆 / 零件导入数据 THE SYSTEM SHALL 保留并写入 `partCode` 字段，用于零件关联和追溯；零件上的 `vehicleNodeCode`（承接原 `deviceCode`，CR-020）与 `supplier_code`（透传溯源，CR-019）一并保留。
- WHEN 调用 `GET /api/service/part/v1/{partCode}` THE SYSTEM SHALL 基于本地 Part 投影按零件编码返回零件信息（查询语义不变，数据来源变为投影）。
- WHEN 调用 `GET /api/service/part/v1/listAllFota?software=true|false|null` THE SYSTEM SHALL 基于本地 Part 投影返回全部可 FOTA 升级零件（按 `is_software` / `fota_upgradeable` 维度过滤，查询语义不变，数据来源变为投影）。
- WHEN 调用 `GET /api/mpt/part/v1/list` THE SYSTEM SHALL 基于本地 Part 投影按 `key/pn/name/part_type/vehicleNodeCode` 过滤并分页（CR-020：`deviceCode` 过滤键已兼容改名为 `vehicleNodeCode`，迁移期保留旧入参兼容）。
- WHEN 查询零件 / 车辆详情 THE SYSTEM SHALL 可基于本地 Part 投影数据展示或关联零件信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 Part 投影数据支撑零件 / 车辆导入校验、查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `partCode` THEN THE SYSTEM SHALL 不阻断历史车辆 / 零件查询，但应在展示或校验结果中体现 Part 信息缺失。
- WHEN 物理零件实例 / 装车校验依赖零件可用性 THE SYSTEM SHALL 仅将 `status=ACTIVE` 的 Part 视为可装车可用，`DRAFT` / `INACTIVE` 按校验规则处理。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:part:list/query/export` 权限点；`completeVehicle:product:part:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝；原 `completeVehicle:vehicle:part:*`（现状权限点，处于 `vehicle` 命名空间）标记 `deprecated` 并迁移至 `product` 命名空间（与产品树各实体 CR-011~020 一致），规划后续兼容性清理 CR 下线。
- THE SYSTEM SHALL 明确不切断「车辆→零件→设备」链路；物理零件实例与绑定关系不在本 Part 投影范围内（见 US-014d）。

#### US-014b: Bootstrap 时从 MDM 全量同步 Part 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 Part 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 Part 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 Part 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM Part 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=part` THE SYSTEM SHALL 调用 MDM Part 全量快照接口拉取数据并 upsert 本地 Part 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 Part 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。
- THE SYSTEM SHALL 不因 MDM Part 快照接口失败而删除或清空本地已有 Part 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD Part 投影所需的 P0 必投字段集 + 投影管理字段，不要求同步 MDM Part 的完整字段集。

#### US-014c: Part 本地维护能力兼容清理
**As a** System, **I want** 将 VMD 现有 Part 本地维护能力逐步收敛为只读投影能力, **so that** Part 字典 / 类型层主数据维护职责统一回归 MDM，同时历史 source=MANUAL 数据和既有查询能力不受影响。

**Acceptance Criteria**:
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 MDM Part 投影语义，不再将 VMD Part 视为权威主数据。
- WHEN 历史 Part 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN Part 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述 Part 维护能力 THE SYSTEM SHALL 明确 VMD Part add/edit/remove 为兼容期遗留能力，不作为长期能力继续扩展。
- THE SYSTEM SHALL 将原 `completeVehicle:vehicle:part:*` 权限点调整为 `completeVehicle:product:part:list/query/export`（迁入 `product` 命名空间）；`add/edit/remove` 仅作兼容期遗留（仅作用于 source=MANUAL 过渡数据，对 source=MDM 一律拒绝），旧 `vehicle:part` 权限点标记 `deprecated` 并规划后续兼容性清理 CR 下线。
- THE SYSTEM SHALL 保留 Part 查询能力，包括 `list` / `listAllFota` / 按 `partCode` 查询 / `query` / `export` 及车辆 / 零件详情展示所需查询。
- THE SYSTEM SHALL 保留 `partCode` 关联键与字段，不因维护权迁移而改名或删除。
- THE SYSTEM SHALL 保留物理零件实例（`tb_vehicle_part` / `tb_vehicle_part_history`）→ Part（`partCode`）引用链，不得切断（物理实例边界见 US-014d）。

#### US-014d: 物理零件实例 + 绑定关系 + 生命周期边界声明（留 VMD）
**As a** System, **I want** 明确 VMD 自有的物理零件实例、绑定关系及其生命周期事件不随 Part 字典投影化而上移, **so that** Part 字典 / 类型层投影化后，「车辆→零件→设备→生命周期」事务 / 实例链路完整保留在 VMD、不被切断。

> **边界声明（CR-021，参照 CR-020 对物理设备实例的处理）**：与 VehicleNode（CR-020）一样做**双层切分**——Part 字典 / 类型层主数据（零件定义、零件类型、规格、part_number 字典等「车上应有哪些零件」）上移并投影化（US-014）；**VIN 绑定的物理零件实例 + 绑定关系 + 生命周期（物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载关系、装车 / 换件 / 下线 / 密钥 / 证书等生命周期事件）属于 VMD 事务 / 实例数据，不是主数据，不上移、不投影化、保持留在 VMD**。本 US 不引入新增能力，仅声明边界，确保 US-017（VehiclePart）/ US-020（EOL 零件绑定）/ US-026（车辆生命周期节点）所覆盖的实例与事件链路在 Part 投影化后语义不变。

**Acceptance Criteria**:
- THE SYSTEM SHALL 将物理零件实例 + 绑定关系 + 生命周期事件（VIN 绑定的物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书生命周期）视为 VMD 自有事务 / 实例数据，不上移、不投影化、保持留在 VMD。
- THE SYSTEM SHALL NOT 将物理零件实例与绑定关系纳入 Part（US-014）字典 / 类型层投影范围。
- THE SYSTEM SHALL 不切断「车辆→零件→设备→生命周期」链路；物理零件实例所涉及的节点 / 设备引用键随既有命名保持（`partCode`、`vehicleNodeCode`（承接原 `deviceCode`，CR-020）），仅引用键命名延续，不改业务语义。
- WHEN Part 投影记录 `is_accurately_traced=true` THEN THE SYSTEM SHALL 在物理零件实例层按既有精准追溯语义记录单件 SN / 精准追溯信息（实例层行为由该字典属性驱动，实例数据仍留 VMD）。
- THE SYSTEM SHALL 保持 US-017（VehiclePart）/ US-020（EOL 零件绑定）/ US-026（生命周期节点）所覆盖的实例与事件能力语义不变，不因 Part 字典投影化而改变。

#### US-014e: 实现 Kafka 消费者接收 MDM Part 事件（增量同步）
**As a** System, **I want** 通过 Kafka 消费者实时接收 MDM Part 事件并同步到本地投影, **so that** MDM 中新增或更新的零件可以实时同步到 VMD，避免增量同步通道缺失导致数据不同步。

> **问题背景（CR-024）**：当前设计文档（D13）明确 MDM 同步策略为「Kafka 事件订阅为主 + Feign 全量快照兜底」，但 VMD 项目中仅实现了 Spring `@EventListener` 监听本地事件，**未实现 Kafka 消费者**，导致增量同步通道完全缺失。本 US 补全 Kafka 消费者实现，确保 MDM Part 事件能够通过 Kafka 实时同步到 VMD。

**Acceptance Criteria** (EARS):
- WHEN MDM Part 子域通过 Kafka 推送 PartCreated / PartUpdated / PartDeleted 事件 THE SYSTEM SHALL 通过 Kafka 消费者接收事件并转换为本地 `MdmPartEvent`。
- WHEN Kafka 消费者接收到 MDM Part 事件 THE SYSTEM SHALL 调用 `MdmSyncAppService.handlePartEvent()` 进行幂等 upsert。
- WHEN Kafka 消费者接收到事件但 MDM 服务不可用 THEN THE SYSTEM SHALL 记录错误日志并继续处理后续事件，不阻塞消费流程。
- WHEN Kafka 消费者处理事件失败 THEN THE SYSTEM SHALL 支持重试机制，重试次数超过阈值后将消息发送到死信队列。
- THE SYSTEM SHALL 与 MDM 团队确认 Part 事件的 Kafka topic 名称、payload schema、partition 策略。
- THE SYSTEM SHALL 确保 Kafka 消费者的消费组名称（group.id）与其他实体（Brand/CarLine/Platform 等）的消费组保持一致的命名规范。

#### US-014f: 定时同步 MDM Part 数据
**As a** System, **I want** 定期从 MDM 同步 Part 数据, **so that** 即使 Kafka 事件丢失或消费失败，VMD 也能通过定时任务保持 Part 投影数据的最终一致性。

> **问题背景（CR-024）**：当前 Bootstrap 同步仅在启动时执行一次，且修改后的逻辑基于 `externalRefId` 和 `externalVersion` 进行增量同步。但如果没有 Kafka 事件通道，MDM 中新增的零件不会自动同步。本 US 添加定时同步机制，作为 Kafka 事件同步的补充和兜底。

**Acceptance Criteria** (EARS):
- THE SYSTEM SHALL 支持配置定时同步任务，定期调用 MDM Part 全量快照接口同步数据。
- WHEN 定时同步任务执行 THE SYSTEM SHALL 基于 `externalRefId` 和 `externalVersion` 进行幂等 upsert，不删除本地已有记录。
- WHEN 定时同步任务执行失败 THEN THE SYSTEM SHALL 记录错误日志并发送告警通知，不清空本地已有数据。
- THE SYSTEM SHALL 支持通过配置开关启用/禁用定时同步任务。
- THE SYSTEM SHALL 支持配置定时同步的执行频率（如每小时、每天）。
- WHEN Kafka 事件同步通道正常工作 THEN THE SYSTEM SHALL 仍按计划执行定时同步，作为数据一致性兜底。

#### US-014g: MDM 同步监控告警
**As a** System, **I want** 监控 MDM 同步状态并在同步失败时发送告警, **so that** 运维人员可以及时发现同步异常并采取措施，避免数据长期不一致。

> **问题背景（CR-024）**：当前 MDM 同步失败仅记录错误日志，没有监控和告警机制。如果同步长期失败，运维人员无法及时发现，导致 VMD 本地投影数据与 MDM 数据不一致。本 US 添加监控告警机制，确保同步异常能够及时被发现和处理。

**Acceptance Criteria** (EARS):
- THE SYSTEM SHALL 记录 MDM 同步的关键指标（同步成功/失败次数、最后同步时间、同步延迟等）。
- WHEN MDM 同步连续失败次数超过阈值 THEN THE SYSTEM SHALL 发送告警通知（如邮件、短信、钉钉等）。
- WHEN MDM 同步延迟超过阈值 THEN THE SYSTEM SHALL 发送告警通知。
- THE SYSTEM SHALL 提供 MDM 同步状态查询接口，供运维人员查看同步状态。
- WHEN 同步恢复正常 THEN THE SYSTEM SHALL 发送恢复通知。
- THE SYSTEM SHALL 支持配置告警阈值和通知方式。

#### US-015: 消费 MDM VehicleNode（车载节点，原 Device 设备）主数据本地投影
**As a** System, **I want** VMD 从 MDM 同步 VehicleNode（车载节点）字典 / 类型主数据并维护本地只读投影表, **so that** 零件 / 车辆零件场景可通过 `vehicleNodeCode` 关联节点类型、车辆 / 设备详情可展示节点信息，同时 VMD 不再承担车载节点字典主数据维护职责。

> **语义重构 + 命名迁移（CR-020）**：本 US 由原「US-015 维护设备信息（Device）」演进而来。VehicleNode 字典 / 类型主数据 SSOT 上移至 **edd-mdm 的 EEAD 子域**（MDM CR-007），VMD 仅保留 VehicleNode 本地只读投影副本。**本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 Device 改名为 **VehicleNode（车载节点）**，VMD 同步将设备字典实体与关联键 `deviceCode` 改名为 VehicleNode / `vehicleNodeCode`。Device / `deviceCode` / 「设备」自此为历史兼容命名，新能力统一使用 VehicleNode / `vehicleNodeCode` / 「车载节点」（迁移与兼容策略见 US-015c）。⚠️ **边界**：本 CR 仅处理「车载节点字典 / 类型层」主数据（节点定义、类型、功能域）；**VMD 自有的物理设备实例 + 绑定关系（VIN 绑定的 TBOX/IDCU/CCU/ADCU/TCU 实例，含 SN/part_number/hardware_vsn 及绑车 / 激活 / 下线 / 密钥 / 证书生命周期）不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**。⚠️ **命名消歧**：本 US 的 VehicleNode（车载节点）区别于物理设备实例（VehiclePart 绑定的具体设备，US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）。VMD VehicleNode 投影为 MDM VehicleNode 在 VMD bounded context 下的按需最小化只读视图，不要求与 MDM VehicleNode 主数据字段完全一致（字段范围见 §4「VehicleNode 投影字段范围原则」）。`vehicleNodeCode` 作为物理设备实例的节点关联编码长期保留，承接原 `deviceCode` 语义并回填历史值。VMD VehicleNode 的 add/edit/remove 自此为兼容期遗留能力，仅作用于 source=MANUAL 过渡数据，最终下线策略见 US-015c。

**Acceptance Criteria** (EARS):
- WHEN MDM 通过 Kafka 推送 VehicleNodeCreated / VehicleNodeUpdated / VehicleNodeDeleted 事件 THE SYSTEM SHALL upsert VMD 本地 VehicleNode 投影数据，并写入 source=MDM / external_ref_id / external_version / last_sync_time。
- WHEN event.version <= local.external_version THEN THE SYSTEM SHALL 忽略该事件，避免乱序事件覆盖较新数据。
- WHEN 同步 MDM VehicleNode 数据 THE SYSTEM SHALL 仅持久化 VMD 业务场景所需最小字段集（至少 `code`（即 `vehicleNodeCode` 关联键）/ `name` / `vehicle_node_type`（节点类型，如 TBOX/IDCU/CCU/ADCU/TCU/BTM）/ `domain`（功能域）/ `status`（或 `enabled`，有效标志）/ `source` / `external_ref_id` / `external_version` / `last_sync_time`），不要求 VMD VehicleNode 投影表结构与 MDM VehicleNode 主数据模型完全一致。
- WHEN MDM VehicleNode 新增字段但 VMD 未消费该字段 THEN THE SYSTEM SHALL NOT 要求变更 VMD VehicleNode 投影表结构。
- WHEN MDM VehicleNode 字段变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑 THEN THE SYSTEM SHALL 通过独立 CR 调整 VMD VehicleNode 投影模型。
- WHEN VMD 本地 VehicleNode 记录 source=MDM THEN THE SYSTEM SHALL 拒绝来自 MPT 后台的 add / edit / remove 操作，并返回明确错误（`ProductDataReadOnlyException`，错误码 `202014`）。
- WHEN 调用 `GET /api/service/vehicleNode/v1/{code}` THE SYSTEM SHALL 基于本地 VehicleNode 投影按节点编码返回节点信息（查询语义不变，数据来源变为投影）；迁移期保留旧路径 `GET /api/service/device/v1/{code}` 兼容（按 `device` → `vehicleNode`、`deviceCode` → `vehicleNodeCode` 映射读取）。
- WHEN 调用 `GET /api/service/vehicleNode/v1/listAllFota` THE SYSTEM SHALL 基于本地 VehicleNode 投影返回全部可 FOTA 升级节点；迁移期保留旧路径 `GET /api/service/device/v1/listAllFota` 兼容。
- WHEN VMD 处理车辆导入与车辆零件绑定数据 THE SYSTEM SHALL 保留并写入 `vehicleNodeCode` 字段（承接原 `deviceCode` 语义），用于物理设备实例的节点类型关联与追溯。
- WHEN 查询车辆 / 设备详情 THE SYSTEM SHALL 可基于本地 VehicleNode 投影数据展示或关联车载节点信息。
- WHEN MDM 不可用 THEN THE SYSTEM SHALL 使用已同步的本地 VehicleNode 投影数据支撑导入校验、查询、展示和历史追溯，不对 MDM 形成运行时强依赖。
- IF 本地不存在对应 `vehicleNodeCode` THEN THE SYSTEM SHALL 不阻断历史查询，但应在展示或校验结果中体现节点信息缺失。
- IF 历史数据仅存在 `deviceCode` 且未完成字段迁移 THEN THE SYSTEM SHALL 支持 `deviceCode` → `vehicleNodeCode` 的兼容读取或映射处理。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:product:vehicleNode:list/query/export` 权限点；`completeVehicle:product:vehicleNode:add/edit/remove` 权限点仅作为兼容期遗留保留（仅可作用于 source=MANUAL 过渡数据），对 source=MDM 记录一律拒绝；原 `completeVehicle:vehicle:device:*`（现状权限点，处于 `vehicle` 命名空间）标记 `deprecated` 并迁移至 `product` 命名空间（与产品树各实体 CR-011~018 一致），规划后续兼容性清理 CR 下线。

#### US-015b: Bootstrap 时从 MDM 全量同步 VehicleNode 数据
**As a** System, **I want** Bootstrap 时从 MDM 全量同步 VehicleNode 数据, **so that** 首次接入、数据丢失或重新初始化后，VMD 可以恢复 VehicleNode 主数据本地投影。

**Acceptance Criteria**:
- WHEN VMD 启动时检测本地 source=MDM 的 VehicleNode 投影记录数为 0 THE SYSTEM SHALL 自动调用 MDM VehicleNode 全量快照接口拉取数据并 upsert 本地副本。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=vehicleNode` THE SYSTEM SHALL 调用 MDM VehicleNode 全量快照接口拉取数据并 upsert 本地 VehicleNode 投影副本（不删除本地记录）。
- WHEN Mpt-User 调用 `POST /api/mpt/mdmSync/v1/bootstrap?entity=all` THE SYSTEM SHALL 在全量同步中包含 VehicleNode 数据。
- THE SYSTEM SHALL 在 upsert 时写入 source=MDM / external_ref_id / external_version / last_sync_time。
- THE SYSTEM SHALL 不因 MDM VehicleNode 快照接口失败而删除或清空本地已有 VehicleNode 投影数据。
- THE SYSTEM SHALL 支持重复执行 Bootstrap，重复同步时按 external_ref_id / external_version 幂等 upsert。
- THE SYSTEM SHALL 只同步 VMD VehicleNode 投影所需字段，不要求同步 MDM VehicleNode 的完整字段集。

#### US-015c: VehicleNode 本地维护能力兼容清理与 Device→VehicleNode 命名迁移
**As a** System, **I want** 将 VMD 现有 Device 本地维护能力收敛为只读 VehicleNode 投影能力，并将 Device / `deviceCode` 命名迁移为 VehicleNode / `vehicleNodeCode`, **so that** 车载节点字典主数据维护职责统一回归 MDM EEAD 子域、VMD 与 MDM 在节点主数据语义与命名上保持一致，同时历史 source=MANUAL 数据、历史 `deviceCode` 数据和既有查询能力不受影响，物理设备实例 → 节点引用链不被切断。

**Acceptance Criteria**:
- WHEN 执行数据库迁移 THE SYSTEM SHALL 将原设备字典表 `tb_device` 迁移或重命名为 `mdm_vehicle_node`，保留现有列（`code` / `name` / `node_type` / `func_domain`（domain）/ `device_item` / `type` 等）。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 为 `mdm_vehicle_node` 新增 `source` / `external_ref_id` / `external_version` / `last_sync_time` 字段，增加 `UK(external_ref_id)`，并回填历史数据 source='MANUAL'。
- WHEN 执行数据库迁移 THE SYSTEM SHALL 将物理设备实例及相关表（`tb_vehicle_part` / `tb_vehicle_part_history` / `tb_part`）的 `device_code` 关联键迁移 / 回填为 `vehicle_node_code`（仅引用键改名，实例业务语义不变），并提供 `deviceCode` → `vehicleNodeCode` 兼容读取。
- WHEN 迁移期间仍存在旧接口、旧字段或旧权限点调用 THE SYSTEM SHALL 提供兼容策略（兼容读取 / 字段映射 / 旧列保留 / 旧接口保留），避免既有调用方立即失败，历史 `device_code` / `deviceCode` / 旧设备字典数据不得丢失、历史车辆与零件可继续查询追溯。
- WHEN 新增或修改 VMD 内部逻辑 THE SYSTEM SHALL 优先使用 VehicleNode / `vehicleNodeCode` 命名，不再将 VMD VehicleNode 视为权威主数据。
- WHEN 历史 VehicleNode 记录 source=MANUAL THEN THE SYSTEM SHALL 在兼容期允许保留查询和必要的过渡维护能力。
- WHEN VehicleNode 记录 source=MDM THEN THE SYSTEM SHALL 禁止通过 VMD MPT 后台新增、修改或删除。
- WHEN 文档描述历史兼容逻辑 THE SYSTEM SHALL 明确 Device / `deviceCode` / 「设备」为遗留命名，不再作为新能力命名，仅出现在历史兼容、迁移说明或旧字段映射场景。
- THE SYSTEM SHALL 在迁移完成后逐步废弃 Device 命名的 Controller / AppService / Repository / DTO / VO / API path（如 `/api/mpt/device/**`、`/api/service/device/**`），迁移为 VehicleNode 命名（如 `/api/mpt/vehicleNode/**`、`/api/service/vehicleNode/**`），旧接口在兼容期保留。
- THE SYSTEM SHALL 将原 `completeVehicle:vehicle:device:*` 权限点调整为 `completeVehicle:product:vehicleNode:list/query/export`；`add/edit/remove` 仅作兼容期遗留（仅作用于 source=MANUAL 过渡数据，对 source=MDM 一律拒绝），旧 device 权限点标记 `deprecated` 并规划后续下线。
- THE SYSTEM SHALL 保留 VehicleNode 查询能力，包括 `list` / `listAll`（原 `listAllDevice`）/ `query` / `export` / `listAllFota` 及车辆 / 设备详情展示所需查询。
- THE SYSTEM SHALL 保留并回填 `vehicleNodeCode` 关联键，不因维护权迁移或命名迁移而丢失历史数据；**保留物理设备实例（`tb_vehicle_part` / `tb_vehicle_part_history` / `tb_part`）→ 节点引用链，不得切断**。
- THE SYSTEM SHALL 将 Device→VehicleNode、`deviceCode`→`vehicleNodeCode` 的重命名影响纳入本次 CR（CR-020）的兼容性说明，旧字段、旧接口、旧权限点的最终下线由后续兼容性清理 CR 完成。（注：具体迁移脚本、字段物理改名、Flyway 文件等实现细节放 design.md / tasks.md。）

#### US-016: 供应商本地维护下线（Supplier 下线，CR-019）
**As a** VMD 维护者, **I want** 彻底下线 VMD 供应商本地维护能力（`Supplier` 聚合 + `tb_supplier` 表 + CRUD API + 契约及附属物），改由 edd-mdm Party 子域承接供应商主数据, **so that** 消除 VMD 与 MDM 的双源 / 双写，零部件 / 设备仅以 `supplier_code` 溯源透传。

> 供应商 SSOT 上移 edd-mdm Party 子域（MDM CR-006）。与产品树各实体不同，**VMD 不为供应商建立本地只读投影**；删除 / 保留边界、调用方迁移、数据处置（方案 B 直接清退）与回滚详见 §4「供应商本地维护下线约束（CR-019）」。

**Acceptance Criteria**:
- WHEN 调用方请求已下线的供应商维护 API（`/api/mpt/supplier/v1/*` 的 list / query / add / edit / remove / export）THE SYSTEM SHALL 返回明确的下线 / 不支持响应（约定 HTTP 410 Gone 或业务错误码，提示供应商主数据已上移至 edd-mdm Party 子域，VMD 不再提供供应商维护能力）。
- WHEN 调用方在过渡窗口内调用已标注 `@Deprecated` 的供应商接口 THEN THE SYSTEM SHALL 正常返回兼容响应并记录下线告警日志，便于识别残留调用方。
- WHERE 零部件 / 设备数据导入 WHEN 写入记录 THE SYSTEM SHALL 正常保留并写入 `supplier_code`（`tb_part / tb_btm / tb_ccp / tb_idcm / tb_tbox` 及 `ods_vmd_*_df / ods_vmd_data_import_di`），不受供应商本地维护下线影响。
- THE SYSTEM SHALL 不再保留任何供应商主数据的本地增删改查能力与本地表（`Supplier` 聚合、`SupplierAppService`、`tb_supplier` 及其全栈附属物在终版下线后均不存在）。
- THE SYSTEM SHALL NOT 将 `supplier_code` 溯源透传字段及其写入逻辑纳入任何删除 / 清退动作。
- WHEN 调用方需要供应商主数据本体 THE SYSTEM SHALL 由 edd-mdm Party 子域承接，VMD 不提供供应商主数据本地投影。

### 3.6 车辆零件绑定域

#### US-017: 维护车辆—零件绑定关系（VehiclePart）

> **数据模型重构（CR-022）**：本 US 由原「US-017 维护车辆零件（VehiclePart）」演进而来。原单表 `tb_vehicle_part` 同时承载「零件实例本体」与「装车绑定关系」，自 CR-022 起拆分——**实例本体属性迁入 `PartInfo`（US-032），本 US 的 `VehiclePart` 收敛为纯绑定关系**，承载装车位置（`vehicleNodeCode` / `deviceItem` 安装位置快照）、时间（`bindTime` / `unbindTime`）、状态（`bindState`：active / inactive）、换件溯源（`replaceOfBindingId`）。从未启用的 `tb_vehicle_part_history` 废弃，换件历史改由同一 (vin, 节点位) 下多条绑定的时间线表达。⚠️ 命名消歧：VehiclePart（绑定关系）区别于 PartInfo（实例本体，US-032）、物理设备实例、VehicleNode（车载节点字典，CR-020）。
>
> **演进（CR-023）**：`vehicle_part` 绑定纳入两入口统一入站内核（US-038）；支持**无车载节点**零件（`vehicle_node_code` 可空，安装位置以 `device_item` 表达）；`bind_org` 取自实例 `source`，移除 `MES` 硬编码。

**As a** Mpt-User / System, **I want** 车辆与零件实例之间的绑定关系 CRUD（按 VIN / part_id / SN 查询）, **so that** 可登记、查询、修复车辆装车关系并支撑换件与追溯。

**Acceptance Criteria**:
- WHEN System 通过 `bindVehiclePart()` 绑定零件 THE SYSTEM SHALL 在 `vehicle_part` 新建一条绑定，置 `bindState=active`、`bindTime=Instant.now()`，并将所引用 `part_info.instanceState` 置为「在用」。
- IF 待绑定的实例（`part_id`）当前已存在一条 `active` 绑定 THEN THE SYSTEM SHALL 拒绝并返回 `PART_BINDING_CONFLICT`（`VmdErrorCode`，错误码 `202017`）。
- IF 目标车辆同一节点位（`vin` + `vehicleNodeCode` + `deviceItem`）当前已存在一条 `active` 绑定 THEN THE SYSTEM SHALL 拒绝并返回 `PART_BINDING_CONFLICT`（`202017`）。
- IF 绑定引用的实例（`part_id` / `(part_code, sn)`）不存在 THEN THE SYSTEM SHALL 返回 `PART_INSTANCE_NOT_EXIST`（`202018`）。
- THE SYSTEM SHALL 在数据库层保证「同一实例同时仅一条 active 绑定」「同一车同一节点位同时仅一条 active 绑定」两项约束（实现手段留 design.md）。
- WHEN 调用 `GET /api/mpt/vehiclePart/v1/list` THE SYSTEM SHALL 按 `vin / partCode / sn / bindState / beginTime / endTime` 过滤并分页（`startPage`+`getPageResult`）。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:vehicle:vehiclePart:{list/query/export}` 权限点（保持 `vehicle` 命名空间，不迁 product）。
- THE SYSTEM SHALL 保持 EOL 发布的 `VehicleEolPartBoundEvent`（`PartMeta` 结构）契约不变，TSP/OTA 异步订阅链路语义不变。
- **（CR-023）** THE SYSTEM SHALL 支持**无车载节点**零件的绑定：安装位置以 `device_item`（通用安装位，前 / 后电机等亦具备）表达，`vehicle_node_code` 可空（仅联网 / 可升级 / 关键件具备）；「同时仅一条 active 绑定」约束按安装位置（`device_item`，节点位可空）+ 实例级单一 active 表达（物理实现留 design.md）。
- **（CR-023）** THE SYSTEM SHALL 由共用入站内核（US-038）经两入口（US-037 / US-018）统一建立 / 变更绑定；`bind_org` 取值来自实例 `source`（适配层注入），不硬编码 `MES`。

#### US-032: 维护物理零件实例本体（PartInfo）与游离零件

> **新增（CR-022）**：物理零件实例本体为 VMD 自有事务 / 实例数据，以 `(partCode, sn)` 唯一标识一颗物理零件，**允许在尚未绑定 VIN 时独立存在（游离零件）**。仅以 `partCode` 持 Part 字典（CR-021，`tb_mdm_part`）引用键，不复制字典字段、不建物理外键。

> **演进（CR-023）**：`part_info` 纳入两入口统一入站（US-037 / US-018）与共用入站内核（US-038）；新增 `source`（入站来源枚举）/ `part_type` 快照 / 入站溯源键（`inbound_batch_no` / `source_event_id`）/ `last_inbound_time`；`vehicle_node_code` 明确为**可空**（车载节点对零件可选，仅联网 / 可升级 / 关键件具备）；纳入 `part_type=SIM` 实例（反转 CR-022 O86）。此处 `source` 语义为「入站来源系统」，区别于字典投影表的 `source ∈ {MDM, MANUAL}`（见 §4「来源标记语义区分」）。

**As a** System / Mpt-User, **I want** 以 `(partCode, sn)` 唯一标识一颗物理零件实例并独立登记其本体属性, **so that** 零件可在尚未绑定 VIN 时独立存在（游离零件），且同一颗零件在多次导入中稳定收敛为同一实例。

**Acceptance Criteria**:
- THE SYSTEM SHALL 在数据库层对 `tb_part_info` 施加 `UNIQUE KEY (part_code, sn)`，保证同一零件号下序列号唯一。
- THE SYSTEM SHALL 在 `part_info` 持久化实例本体属性：`part_code`（→ `tb_mdm_part.pn` 引用键，NOT NULL）、`sn`、`vehicle_node_code`（节点归属，→ `tb_mdm_vehicle_node.code` 引用键）、`hardware_ver` / `software_ver` / `hardware_pn` / `software_pn` / `config_word` / `batch_num` / `supplier_code`（透传溯源）/ `extra`（IMEI/ICCID/HSM/MAC 等 JSON）/ `instance_state`（在库 / 在用 / 待更换 / 已报废）/ `first_seen_time`。
- WHEN 零件实例数据到达且对应 VIN / 装车位置尚未就绪 THE SYSTEM SHALL 仅落 `part_info`（不建 `vehicle_part`），形成游离实例，`instanceState` 置为「在库」。
- WHEN 同一 `(part_code, sn)` 实例重复到达 THE SYSTEM SHALL 以 upsert 幂等处理，更新本体非空字段，不产生重复实例行。
- IF 通过 MPT 后台新增实例时 `(part_code, sn)` 已存在 THEN THE SYSTEM SHALL 返回 `PART_INSTANCE_ALREADY_EXISTS`（`202016`）。
- THE SYSTEM SHALL NOT 为 `part_info` 复制 Part 字典字段（定义 / 类型 / 规格 / FOTA 能力等），仅持 `part_code` 引用键；不建指向 `tb_mdm_part` 的物理外键。
- WHEN 调用 `GET /api/mpt/partInfo/v1/list` THE SYSTEM SHALL 按 `partCode / sn / vehicleNodeCode / instanceState` 过滤并分页。
- THE SYSTEM SHALL 校验调用方持有 `completeVehicle:vehicle:partInfo:{list/query/export}` 权限点；`add/edit/remove` 仅供手工修复使用。
- **（CR-023）** THE SYSTEM SHALL 在 `part_info` 持久化入站治理字段：`source`（入站来源系统，可扩展枚举 `MES / MANUAL / WMS / IQC / OTHER`，游离实例亦必填）、`part_type`（类型快照，驱动 type-schema 校验与下游路由）、`inbound_batch_no` / `source_event_id`（入站溯源与事件 / 批次级幂等去重键）、`last_inbound_time`（最近一次入站 upsert 时间）。
- **（CR-023）** THE SYSTEM SHALL 将 `vehicle_node_code` 作为**可空**属性，仅联网件 / 可升级件 / 关键件具备车载节点；无车载节点的 SN 实例（如发动机 / 电机 / 电池包 / SIM）亦为合法实例。
- **（CR-023）** THE SYSTEM SHALL 支持 `part_type=SIM` 的实例（`sn`=ICCID，`extra` 承载 IMSI / MSISDN / MNO 等 SIM 特殊字段），与其它物理零件同等落 `part_info`；SIM 连接 / 激活状态由 TSP 经另一条写路径承接（见 US-038 / 边界声明）。
- **（CR-023）** THE SYSTEM SHALL 将 `extra` 写入约束为按 `part_type` 的字段契约（type-schema）标准化后落库，不接受未经校验的任意字段（见 US-038）。

#### US-033: 零件换件（解绑旧件 + 绑定新件 + 换件溯源）

> **新增（CR-022）**：换件 = 解绑旧绑定 + 为新实例建 active 绑定 + 记录换件溯源链，保证任一时刻同一 (vin, 节点位) 仅一颗在用零件；换件历史由绑定时间线表达（不依赖独立历史表）。

**As a** System / Mpt-User, **I want** 在同一车同一节点位用新零件实例替换旧零件实例, **so that** 换件全过程可被记录与追溯，且任一时刻该位置只有一颗在用零件。

**Acceptance Criteria**:
- WHEN 对某 (vin, 节点位) 执行换件 THE SYSTEM SHALL 将该位置当前 `active` 绑定置为 `bindState=inactive` 并写入 `unbindTime` / `unbindReason` / `unbindBy` / `unbindOrg`，同时将旧实例 `instanceState` 置为「待更换」或「已报废」。
- WHEN 旧绑定解绑后 THE SYSTEM SHALL 为新零件实例新建一条 `active` 绑定，并将其 `replaceOfBindingId` 指向被替换的旧绑定。
- THE SYSTEM SHALL 保证换件操作后该 (vin, 节点位) 仍满足「同时仅一条 active 绑定」约束。
- WHEN 查询某 (vin, 节点位) 的换件历史 THE SYSTEM SHALL 基于 `vehicle_part` 按 `bindTime` 排序返回该位置历经的全部绑定（含 active 与 inactive）及其所引用实例，无需依赖独立历史表。
- IF 换件时新实例不存在 THEN THE SYSTEM SHALL 先按 US-032 upsert `part_info` 再建绑定，或返回 `PART_INSTANCE_NOT_EXIST`（`202018`，视调用入参是否携带实例本体而定）。

#### US-034: 零件导入异步乱序绑定兜底

> **演进（CR-023）**：本 US 的「先 upsert 实例、再建绑定」与乱序兜底语义已并入共用入站内核 US-038；本 US 作为历史条目保留，规则以 US-038 为准。
>
> **新增（CR-022）**：各导入解析器改为「先 upsert 实例本体、再建车辆绑定」两步，并对「零件先到 / 车后到」具备乱序兜底（与现有 MDM 投影乱序收敛思路一致）。

**As a** System, **I want** 各导入解析器先 upsert 零件实例本体、再建立车辆绑定，且对「零件先到 / 车后到」具备乱序兜底, **so that** 导入到达次序不影响最终一致的车辆—零件关系。

**Acceptance Criteria**:
- THE SYSTEM SHALL 改造 EOL（US-020）/ BTM（US-021）/ TBOX（US-022）/ CCP（US-023）/ IDCM（US-024）解析器与 `VehiclePartBinder.bindParts()`：先按 `(partCode, sn)` upsert `part_info`，再在 VIN + 安装位置就绪时 upsert `vehicle_part` 绑定。
- WHEN 零件实例先于其 VIN 到达 THE SYSTEM SHALL 落游离 `part_info`；WHEN 该 VIN 的车辆登记 / 绑定信息随后到达 THE SYSTEM SHALL 按 `sn` 回扫匹配的游离实例并补建 `vehicle_part`。
- WHEN 车辆先于零件到达 THE SYSTEM SHALL 沿用现有流程，零件到达后 upsert 实例并建绑定。
- THE SYSTEM SHALL 保证导入 upsert 幂等：重复 / 乱序到达不产生重复实例与重复 active 绑定。
- THE SYSTEM SHALL 保持 SIM 解析器（US-025）不写 `part_info` / `vehicle_part`，仍走 TSP。
- THE SYSTEM SHALL 保持 US-018~025 的 `ImportResult`（`totalCount/successCount/failureCount/invalidCount`）计数语义不变。

#### US-035: 死表清退与车辆主档瘦身

> **新增（CR-022）**：清退已无任何代码 / 写入路径的死表与死 VO，使车辆主档（VehicleInfo）模型收敛清晰。外饰 / 内饰 / 轮毂 / 选装语义由版本（Variant）+ 配置（Configuration）→ 选项值（OptionCode）表达。

**As a** System Maintainer, **I want** 清退已无任何代码 / 写入路径的死表, **so that** 车辆主档模型收敛清晰、消除遗留债。

**Acceptance Criteria**:
- THE SYSTEM SHALL 下线并删除以下死表：`tb_veh_exterior` / `tb_veh_interior` / `tb_veh_wheel` / `tb_veh_optional` / `tb_veh_ecu` / `tb_veh_activation`，及遗留 `tb_mes_vehicle_data` / `tb_bom_part` / `tb_bom_part_nove` / `tr_veh_model_config_*` / `tr_veh_user_relation` / `tb_veh_user`。
- THE SYSTEM SHALL 同步下线对应死代码资产（如 `InteriorRequest/Response`、`WheelRequest/Response` 等无持久化支撑的遗留 VO）。
- THE SYSTEM SHALL 保留活表 `tb_veh_basic_info`（车辆主档）与 `tb_veh_detail_info`（EOL 详细字段 KV）、`tb_veh_preset_owner`（预设车主，US-026）不变。
- THE SYSTEM SHALL 明确外饰 / 内饰 / 轮毂 / 选装语义由版本（Variant）+ 配置（Configuration）→ 选项值（OptionCode）表达，不再依赖上述死表。
- THE SYSTEM SHALL NOT 触及任何字典 / 类型层投影表（`tb_mdm_*`）。

#### US-036: 物理实例层重构的查询 / RPC / 权限 / 错误码影响

> **新增（CR-022）**：声明重构后查询、RPC 契约、权限点与错误码的兼容与新增，确保上下游平滑过渡。

**As a** Service-Caller / Mpt-User, **I want** 重构后查询、RPC 契约、权限点与错误码保持可预期, **so that** 上下游无感、调用方平滑过渡。

**Acceptance Criteria**:
- THE SYSTEM SHALL 保持 `/api/mpt/vehiclePart/v1/**` 路径与响应形态兼容：旧响应字段（如 `partState`）由 `part_info.instanceState` + `vehicle_part.bindState` 推导回填。
- THE SYSTEM SHALL 保持 `VmdPartService`（`/api/service/part`，查 Part 字典投影 `tb_mdm_part`）契约不受本重构影响。
- THE SYSTEM SHALL 在 `vehicle` 命名空间维护权限点：`completeVehicle:vehicle:vehiclePart:{list/query/export/add/edit/remove}`（沿用）与新增 `completeVehicle:vehicle:partInfo:{list/query/export/add/edit/remove}`；**不迁 product 命名空间**。
- THE SYSTEM SHALL 在 `VmdErrorCode` 接续新增 `202016 PART_INSTANCE_ALREADY_EXISTS` / `202017 PART_BINDING_CONFLICT` / `202018 PART_INSTANCE_NOT_EXIST`，复用 `202011 PART_NOT_EXIST` / `202012 PART_NOT_ALLOW_BIND`。
- **（CR-023）** THE SYSTEM SHALL 在 `vehicle` 命名空间新增入站对账权限点 `completeVehicle:vehicle:partInbound:{list/query/export/retry}`（US-039）。
- **（CR-023）** THE SYSTEM SHALL 在 `VmdErrorCode` 接续新增 `202019 PART_INBOUND_VALIDATE_FAILED` / `202020 PART_TYPE_SCHEMA_NOT_FOUND`。

### 3.7 零件实例数据入站域（CR-023 重构）

> **章节定位（CR-023）**：本节由原「车辆数据导入域」演进而来，重定位为**零件实例数据统一入站**。零件实例（硬件件：TBOX/BTM/CCP/IDCM 等；连接件：SIM）仅经两个录入入口——**入口①上游系统对接**（US-037）与**入口②管理后台导入**（US-018）——并**共用同一套入站内核**（US-038），异常与对账见 US-039。
>
> **功能范围**：零件实例的接收、字段校验、标准化、幂等去重、按 `part_type` 适配源差异、统一落 `part_info`（VIN / 安装位置就绪时建 `vehicle_part`）、触发跨域事件（下游 TSP/OTA/IDK 订阅）、入站异常隔离与对账。
>
> **非目标**：PRODUCE 整车主档入站（US-019，非零件实例，沿用现状）；TSP 激活 / 连接回写建模（另一条写路径）；字典层投影（CR-020/021）；US-035 主档瘦身（CR-022）。
>
> **边界声明（CR-023）**：
> - 与 **MDM（字典投影）**：`part_info` / `vehicle_part` 仅以 `part_code` / `vehicle_node_code` / `supplier_code` 持引用键，**不复制字典字段、不建物理外键**（沿用 CR-021/022）；入站校验消费 Part 字典投影（`status=ACTIVE` 方可装车），MDM 不可用时按降级规则处理。
> - 与 **TSP（激活 / 连接回写）**：联网件密钥 / 证书 / 激活状态、SIM 连接 / 激活状态属对**已存在实例的状态变更**，是**另一条写路径**，不在本入站范围；SIM 等连接件落 `part_info` 后经跨域事件交由 TSP 承接连接 / 激活。
> - 与 **产线 / MES（数据源）**：VMD 仅定义**入站接收契约**（「上游推送零件实例」），源系统由对接适配层承接并打标 `source`，**不设计 MES 内部采集**。

#### US-037: 入口①上游系统对接（零件实例异步入站 + 批量兜底）

> **新增（CR-023）**：入口① = 上游系统（经对接适配层，如 MES / 收货 IQC / 前置 WMS）通过**独立入站链路**推送零件实例，以异步事件为主、批量接口兜底；接口语义为「上游推送零件实例」，**不绑定 MES 具体形态**，源系统由适配层承接并打标 `source`。

**As a** 上游系统（经对接适配层）, **I want** 通过独立入站链路以异步事件为主、批量接口兜底向 VMD 推送零件实例并获得入站结果回执, **so that** 零件实例可及时、可靠、可纠错地进入 VMD，且推送方能感知校验 / 落库结果。

**Acceptance Criteria** (EARS):
- THE SYSTEM SHALL 提供**独立于 MDM 字典投影链路**的零件实例入站通道（独立 topic / 契约），不复用「edd-mdm 接入规范」的投影事件链路。
- WHEN 上游通过异步事件推送一批零件实例 THE SYSTEM SHALL 将其交由共用入站内核（US-038）处理。
- WHEN 上游通过批量兜底接口推送零件实例 THE SYSTEM SHALL 经与异步事件**完全一致的入站内核**处理，不另设旁路规则。
- THE SYSTEM SHALL 要求每条入站记录携带 `part_code` 与来源标识；`source` 由对接适配层注入，取值属可扩展枚举 `MES / MANUAL / WMS / IQC / OTHER`。
- THE SYSTEM SHALL NOT 在入站契约或内核中硬编码具体源系统形态（如将 `bind_org` 固定为 `MES`），源系统差异由对接适配层承接。
- WHEN 一批入站处理完成 THE SYSTEM SHALL 向上游返回入站结果回执，包含 `totalCount / successCount / failureCount / invalidCount` 及失败明细（记录标识 + 错误码 + 原因）。
- IF 入站记录校验或落库失败 THEN THE SYSTEM SHALL 隔离该失败记录并在回执 / 错误通知中标明，不阻断同批其它记录。
- THE SYSTEM SHALL 校验入口①调用方身份（服务间 / 消息鉴权），拒绝未授权来源的入站。

#### US-038: 零件实例数据入站内核（两入口共用，合并 US-034）

> **新增（CR-023，合并 US-034）**：入口①（US-037）与入口②（US-018）共用一套入站内核，统一校验 / 标准化 / 幂等 / 去重 / 落库 / 触发事件；US-034 的「先 upsert 实例、再建绑定」与乱序兜底语义并入本 US。

**As a** System, **I want** 两入口共用一套零件实例入站内核, **so that** 无论系统推送还是人工上传，均经相同规则处理，避免双套口径漂移。

**Acceptance Criteria** (EARS):
- THE SYSTEM SHALL 将入站处理收敛为统一内核六步：① 字段校验 ② 标准化 ③ 幂等 ④ 去重 ⑤ 落库 ⑥ 触发跨域事件。
- WHEN 处理每条记录 IF `part_code` 为空 OR（该 `part_type` 要求 SN 时）`sn` 为空 THEN THE SYSTEM SHALL 计入 `invalidCount` 并跳过该条。
- THE SYSTEM SHALL 按 `part_type` 的字段契约（type-schema）校验该类型必需的特殊字段（如 TBOX 需 `iccid1` 与 `sn`；BTM 需 `sn`；SIM 需 `iccid / imsi / msisdn` 至少其一），缺失必需特殊字段的记录计入 `invalidCount`。
- THE SYSTEM SHALL 将上游异构特殊字段（IMEI / ICCID / HSM / MAC / IMSI / MSISDN 等）按 type-schema 标准化后写入 `part_info.extra`，不再由各源适配器各自随意序列化。
- WHEN 同一 `(part_code, sn)` 重复到达 THE SYSTEM SHALL 以 upsert 幂等更新本体非空字段，不产生重复实例行（沿用 US-032）。
- WHEN 同一入站事件（`source_event_id`）或同一入站批次记录重复到达 THE SYSTEM SHALL 按入站溯源键去重，不重复落库、不重复触发事件。
- THE SYSTEM SHALL 保证「同一实例 / 同一安装位置（节点位可空）仅一条 active 绑定」（沿用 US-017）。
- THE SYSTEM SHALL 将**所有带 SN 的物理零件实例（含 SIM）统一落 `part_info`**；VIN 与安装位置就绪时 upsert `vehicle_part` 绑定。
- WHEN `part_type=SIM` THE SYSTEM SHALL 落 `part_info`（`sn`=ICCID，特殊字段入 `extra`）后触发跨域事件交由 TSP 承接连接 / 激活（沿用 US-025 的 TSP 下游，反转 CR-022 O86 的「不入实例层」）。
- THE SYSTEM SHALL 经跨域事件驱动下游域消费（SIM→TSP 连接 / 激活、TBOX/CCP/IDCM→TSP 证书、BTM→IDK），下游域为事件消费者而非落库分支。
- WHEN 零件实例先于其 VIN 到达 THE SYSTEM SHALL 落游离 `part_info`（`instance_state`=在库）；WHEN 该 VIN 的绑定信息随后到达 THE SYSTEM SHALL 按 `sn` 回扫匹配游离实例并补建 `vehicle_part`（沿用 US-034）。
- WHEN 车辆先于零件到达 THE SYSTEM SHALL 沿用现有流程，零件到达后 upsert 实例并建绑定。
- THE SYSTEM SHALL 在落库时写入 `source`（来源系统枚举）、`part_type` 快照、入站溯源键（`inbound_batch_no` / `source_event_id`）与 `last_inbound_time`；`vehicle_part.bind_org` 取自 `source`，不硬编码。
- THE SYSTEM SHALL 保持既有 `VehicleEolPartBoundEvent`（`PartMeta` 结构）契约不变，TSP/OTA 异步订阅链路语义不变（沿用 US-020 / US-036）。
- THE SYSTEM SHALL 保持 `ImportResult`（`totalCount / successCount / failureCount / invalidCount`）计数语义在两入口一致（沿用 US-018 / US-034）。

#### US-039: 零件实例入站异常处理与对账

> **新增（CR-023）**：两入口的入站失败可隔离、可查询、可重放、可对账。

**As a** Mpt-User / System, **I want** 两入口的入站失败可隔离、可查询、可重放、可对账, **so that** 入站数据质量与完整性可治理。

**Acceptance Criteria** (EARS):
- WHEN 记录在内核任一步失败 THE SYSTEM SHALL 隔离该失败记录（保留原始载荷 + 错误码 + 原因 + 入站溯源键），不阻断同批其它记录。
- THE SYSTEM SHALL 支持按入站溯源键（`inbound_batch_no` / `source_event_id`）对失败记录重放；重放 SHALL 经同一内核且对已成功记录幂等。
- WHEN 入口①一批处理完成 THE SYSTEM SHALL 通过回执 / 错误通知将失败明细返回上游（配合 US-037）。
- THE SYSTEM SHALL 提供入站对账查询（按来源 / 批次 / 时间窗统计 `total / success / failure / invalid` 与失败明细）。
- THE SYSTEM SHALL 校验后台对账 / 重放操作调用方持有 `completeVehicle:vehicle:partInbound:{list/query/export/retry}` 权限点（留 `vehicle` 命名空间）。

#### US-018: 管理后台车辆数据批次导入（入口②）

> **演进（CR-023）**：本 US 重定位为**入口②管理后台导入**——后台批次导入框架（`batchNum` 唯一 + 解析器选择 + `ImportResultResponse` 计数）**复用并挂接共用入站内核（US-038）**，零件实例子集走内核，不另设旁路；PRODUCE 整车主档（US-019）非零件实例、SIM 经内核落 `part_info` 并触发 TSP 事件（US-025 / US-038）。

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
- WHEN VIN 已存在 THE SYSTEM SHALL 更新 `plantCode/brandCode/platformCode/carLineCode/modelCode/variantCode/configurationCode` 七项；不存在则新建。（`plantCode` 承接原 `manufacturerCode` 语义，迁移期对仅存在 `manufacturerCode` 的历史数据按 US-007c 兼容读取/映射处理，参见 CR-011；`variantCode` 承接原 `baseModelCode` 语义，迁移期对仅存在 `baseModelCode` 的历史数据按 US-004c 兼容读取/映射处理，参见 CR-016；`configurationCode` 承接原 `buildConfigCode` 语义，迁移期对仅存在 `buildConfigCode` 的历史数据按 US-005c 兼容读取/映射处理，参见 CR-017）
- WHEN 一条记录处理完成 THE SYSTEM SHALL 通过 `VehiclePublish.produce(vin)` 发布 `VehicleProduceEvent`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。IF 单条处理异常 THEN THE SYSTEM SHALL 计入 `failureCount` 并继续处理下一条。

#### US-020: EOL 解析器（V1.0）

> **演进（CR-023）**：EOL 零件段绑定收敛为共用入站内核（US-038）的来源适配器——仅保留 EOL 特有字段映射与下游调用差异，校验 / 标准化 / 幂等 / 去重 / 落库 / 事件触发上提至 US-038；`bindOrg` 取自实例 `source`，移除 `MES` 硬编码。

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

> **演进（CR-023）**：BTM 解析器收敛为入站内核（US-038）的来源适配器；`vehicleNodeCode=BTM_M` 等类型固定值改由 type-schema / 适配器配置表达，不在内核硬编码；BTM→IDK 经跨域事件订阅。

**As a** System, **I want** 解析蓝牙模块数据, **so that** 入库车辆零件并通知 IDK 服务批量导入。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `SN` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM/MAC` 序列化进 `extra` JSON。
- THE SYSTEM SHALL 创建 `VehiclePart`（`deviceCode="BTM_M"`, `deviceItem=BTM`）。（CR-020：物理设备实例上的节点引用键 `deviceCode`（`tb_vehicle_part.device_code`）兼容改名为 `vehicleNodeCode`（`vehicle_node_code`），仅引用键改名、实例业务语义不变，迁移期保留旧列兼容写入，参见 US-015c）
- THE SYSTEM SHALL 调用 `idkBtmInfoService.batchImport()` 同步至 IDK。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-022: TBOX 解析器（V1.0）

> **演进（CR-023）**：TBOX 解析器收敛为入站内核（US-038）的来源适配器；TBOX→TSP 经跨域事件订阅。

**As a** System, **I want** 解析车联终端数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `pn` 为空 OR `sn` 为空 OR `iccid1`/`iccid2` 都为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `IMEI/ICCID1/ICCID2/HSM` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspTboxInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-023: CCP 解析器（V1.0）

> **演进（CR-023）**：CCP 解析器收敛为入站内核（US-038）的来源适配器；CCP→TSP 经跨域事件订阅。

**As a** System, **I want** 解析中央计算平台数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `pn` 为空 OR `sn` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspCcpInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-024: IDCM 解析器（V1.0）

> **演进（CR-023）**：IDCM 解析器收敛为入站内核（US-038）的来源适配器；IDCM→TSP 经跨域事件订阅。

**As a** System, **I want** 解析信息娱乐模块数据。

**Acceptance Criteria**:
- WHEN 解析 ITEM IF `sn` 为空 THEN THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 将 `HSM/MAC` 序列化进 `extra`。
- THE SYSTEM SHALL 调用 `tspIdcmInfoService.batchImport()`。
- WHEN 解析完成 THE SYSTEM SHALL 返回 `ImportResult`，包含 `totalCount/successCount/failureCount/invalidCount` 四项计数。

#### US-025: SIM 解析器（V1.0）

> **演进（CR-023，反转 CR-022 O86）**：SIM 作为 `part_type=SIM` 的来源适配器，经入站内核（US-038）**落 `part_info`（`sn`=ICCID，IMSI/MSISDN/MNO 入 `extra`）**，再触发跨域事件交由 TSP 承接连接 / 激活；不再「仅走 TSP 不入实例层」。SIM 连接 / 激活状态归 TSP（另一条写路径）。

**As a** System, **I want** 解析 SIM 卡数据。

**Acceptance Criteria**:
- IF `MNO` 为空 THEN THE SYSTEM SHALL 抛 `VehicleImportDataException(batchNum, "SIM卡导入数据运营商为空")`。
- IF `MNO` 不能解析为 `MnoType` 枚举值 THEN THE SYSTEM SHALL 抛 `VehicleImportDataException(batchNum, "SIM卡导入数据运营商[<mno>]未识别")`。
- WHEN ITEM `iccid/imsi/msisdn` 三者全空 THE SYSTEM SHALL 计入无效计数并跳过。
- THE SYSTEM SHALL 调用 `tspSimService.batchImport()`。
- **（CR-023）** THE SYSTEM SHALL 经入站内核（US-038）将 SIM 落 `part_info`（`part_type=SIM`，`sn`=ICCID，IMSI/MSISDN/MNO 入 `extra`），并触发跨域事件交由 TSP（连接 / 激活属另一条写路径）。
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
- THE SYSTEM SHALL 在 `edd-vmd-api` 模块定义并维护：`VmdVehicleService` / `VmdVehicleLifecycleService` / `VmdVehicleModelConfigService` / `VmdDeviceService` / `VmdPartService` 五个 `@FeignClient`。（CR-020：`VmdDeviceService` 暴露的「设备」能力演进为「车载节点（VehicleNode）」，兼容改名为 `VmdVehicleNodeService`，路径 `/api/service/device/v1` → `/api/service/vehicleNode/v1`，旧契约 / 旧路径迁移期保留兼容，参见 US-015 / US-015c）
- THE SYSTEM SHALL 为每个 Feign 接口提供同包路径的 `*FallbackFactory`。
- THE SYSTEM SHALL 通过 `ServiceNameConstants.EDD_VMD` 引用服务名（与 `bootstrap.yml` 中 `spring.application.name=edd-vmd` 一致）。
- THE SYSTEM SHALL 保证 Service 模块的 `@RequestMapping` 与 API 模块 Feign 上的 `path` 一一对应，覆盖：
  - `/api/service/vehicle/v1/{vin}` GET / `/{vin}/action/bindOrder` POST
  - `/api/service/part/v1/{pn}` GET / `/listAllFota` GET
  - `/api/service/device/v1/{code}` GET / `/listAllFota` GET（迁移期保留旧路径兼容；CR-020 起新增 `/api/service/vehicleNode/v1/{code}` GET / `/listAllFota` GET，`VmdDeviceService` 兼容改名为 `VmdVehicleNodeService`、响应 `DeviceExResponse` → `VehicleNodeExResponse`，按 `device` → `vehicleNode`、`deviceCode` → `vehicleNodeCode` 映射，旧 Feign 契约 / 旧路径 / 旧响应类型迁移期保留兼容）
  - `/api/service/vehicleLifecycle/v1/{vin}/recordFirstApply*Node` POST × 8
  - `/api/service/vehicleModelConfig/v1/configurationCode` GET（迁移期保留旧路径 `/buildConfigCode` 兼容，CR-017）/ `/configuration/list/{variantCode}` GET（迁移期保留旧路径 `/buildConfig/list/{variantCode}` 及 `/buildConfig/list/{baseModelCode}` 兼容，按 `buildConfig` → `configuration`、`baseModelCode` → `variantCode` 映射，CR-016 与 CR-017 兼容并存）/ `/configuration/{configurationCode}` GET（迁移期保留旧路径 `/buildConfig/{buildConfigCode}` 兼容，CR-017）

#### US-031: 内部服务按"选项族-选项值（OptionFamily-OptionCode）"反查 Configuration（配置）
**As a** Service-Caller, **I want** 通过 `GET /api/service/vehicleModelConfig/v1/configurationCode?<optionFamilyCode>=<optionCode>...` 用任意选项族选项值组合反查 Configuration 配置代码, **so that** 在订单/前置库等场景将销售配置翻译为生产侧配置。

> **命名迁移（CR-017）**：本 US 由原「US-031 内部服务按"特征族-特征值"反查生产配置」演进而来，路径 `buildConfigCode` / `buildConfig` 改名为 `configurationCode` / `configuration`，响应类型 `VmdBuildConfigResponse` 改名为 `VmdConfigurationResponse`（迁移期保留旧路径与旧响应类型兼容）。⚠️ 命名消歧：此处 Configuration（配置）区别于 VehicleConfig（车辆配置，US-013）、ConfigItem（配置项，US-009）、configCenter（配置中心）。**反查逻辑仍属 VMD**：基于本地 Configuration 投影 + 特征值映射在 VMD 内完成，不对 MDM 形成运行时强依赖（MDM 不可用时仍可基于本地投影反查）。
>
> **命名迁移（CR-018）**：本 US 的「特征族-特征值」反查现表述为「选项族-选项值（OptionFamily-OptionCode）」反查，入参键 `familyCode` 改名为 `optionFamilyCode`、值 `featureCode` 改名为 `optionCode`，响应中 `featureCodes` 列表改名为 `optionCodes`（迁移期保留旧入参 `familyCode` / `featureCode` 与旧响应字段 `featureCodes` 兼容映射）。⚠️ 命名消歧：此处 OptionFamily / OptionCode（选项族 / 选项值）区别于 ConfigItem（配置项，US-009）下的「枚举值 Option」。**反查能力与每台物理车 `configurationCode` 唯一映射不得切断**；反查所依赖的选项值映射沿用已随 Configuration 投影下发的数据（CR-017），本 CR 仅做引用键兼容改名、不重复接管该映射。

**Acceptance Criteria**:
- THE SYSTEM SHALL 接受 `Map<String,String>` 形式的选项族-选项值（`optionFamilyCode` → `optionCode`）组合并返回单一 Configuration 配置代码（`configurationCode`）；迁移期兼容旧入参 `familyCode` / `featureCode`（按 `familyCode` → `optionFamilyCode`、`featureCode` → `optionCode` 映射）。
- THE SYSTEM SHALL 基于本地 Configuration 投影与选项值映射在 VMD 内完成反查，不强依赖 MDM 运行时可用性。
- WHEN 调用 `GET /configuration/{configurationCode}` THE SYSTEM SHALL 返回包含 `optionCodes` 列表 + `brandCode` 的完整 `VmdConfigurationResponse`；迁移期保留旧响应字段 `featureCodes` 与旧路径 `GET /buildConfig/{buildConfigCode}`、旧响应类型 `VmdBuildConfigResponse` 兼容（CR-017 / CR-018）。
- IF `carLineCode` 缺失或对应车系不存在 THEN THE SYSTEM SHALL 在响应中省略 `brandCode`（不视为错误）。
- THE SYSTEM SHALL 不切断特征-配置反查能力与每台物理车 `configurationCode` 唯一映射。

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
- **数据来源标记**：veh_brand / veh_carLine / veh_platform / **veh_plant** / **veh_model**（CR-015）/ **veh_variant**（原 veh_base_model，CR-016）/ **Configuration 配置投影**（原 BuildConfig，CR-017）/ **OptionFamily 选项族投影 / OptionCode 选项值投影**（原 FeatureFamily / FeatureCode，CR-018）/ **mdm_vehicle_node 车载节点投影**（原 tb_device，CR-020）/ **veh_part（tb_part）Part 零件投影**（CR-021）十一类实体投影表新增 source 字段（MDM / MANUAL），source=MDM 的记录禁止通过 MPT 后台修改。

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

### Platform 主数据投影约束（CR-013）
- Platform 主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Platform 投影副本，不作为权威维护入口。
- 平台与 Brand 同构、区别于 Plant 的命名迁移：平台实体命名不变、`platformCode` 关联键不变，不引入表/列重命名、不新增 Flyway 迁移，直接复用 CR-010（Flyway V3）为 `veh_platform` 建好的 source / external_ref_id / external_version / last_sync_time 字段（`veh_platform.code` 即 `platform_code`、`name` 即 `platform_name`）。
- VMD 中 `platformCode` 是车辆主档（`veh_basic_info.platform_code`）与产品树（`veh_model.platform_code` / `veh_variant.platform_code`，原 `veh_base_model.platform_code`，CR-016）的一部分，作为车辆平台关联字段长期保留，不改名、不删除。
- VMD 不负责 Platform 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Platform 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用 CR-010 已覆盖的事件订阅（F6）与 Bootstrap 全量同步（F7，entity=platform）链路，不新增链路。
- VMD Platform 投影采用按需最小化字段设计，不要求与 MDM Platform 主数据模型完全一致；投影字段以车辆查询、车辆详情展示、导入校验、产品树关联、历史追溯和运行时解耦为边界。
- MDM Platform 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Platform 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Platform 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

### Platform 投影字段范围原则（VMD Platform ⊂ MDM Platform，CR-013）
> VMD 侧 Platform 投影不要求与 MDM Platform 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Platform 投影是 MDM Platform 在 VMD bounded context 下的只读视图，不是 MDM Platform 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 Platform 字段。
2. VMD 不复制 MDM Platform 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Platform 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD Platform 投影模型。
4. VMD Platform 投影是 MDM Platform 在 VMD bounded context 下的只读视图，不是 MDM Platform 的完整副本。
5. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_platform` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `platform_code` | Platform 编码（即 `veh_platform.code`），车辆主档 `platformCode` 的关联键 |
| `platform_name` | Platform 名称（即 `veh_platform.name`），用于车辆详情、列表和产品树展示 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Platform 实体 ID |
| `external_version` | MDM Platform 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Platform 审批状态。
- Platform 生命周期全量状态流转。
- Platform 组织归属全路径。
- Platform 技术架构描述、研发归属、市场属性等非 VMD 必需字段。
- Platform 编码生成规则。
- Platform 数据质量评分。
- Platform 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### CarLine 主数据投影约束（CR-014）
- CarLine 主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 CarLine 投影副本，不作为权威维护入口。
- 车系与 Brand / Platform 同构、区别于 Plant 的命名迁移：车系实体命名不变、`carLineCode` 关联键不变，不引入表/列重命名、不新增 Flyway 迁移，直接复用 CR-010（Flyway V3）为 `veh_carLine` 建好的 source / external_ref_id / external_version / last_sync_time 字段。
- VMD 中 `carLineCode` 是车辆主档与产品树的一部分，作为车辆车系关联字段长期保留，不改名、不删除。
- 车系投影上的 `brandCode` 冗余字段（由 `V2__CarLine_brand_code_migration.sql` 引入）必须保留，不得删除或弱化：用于支撑跨域回查，并支撑 US-031 `getBuildConfig` 在响应中按 `carLineCode → brandCode` 补出 `brandCode`。这是车系区别于 Brand / Platform 投影的特殊点。
- VMD 不负责 CarLine 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 CarLine 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用 CR-010 已覆盖的事件订阅（F6）与 Bootstrap 全量同步（F7，entity=carLine）链路，不新增链路。
- VMD CarLine 投影采用按需最小化字段设计，不要求与 MDM CarLine 主数据模型完全一致；投影字段以车辆查询、车辆详情展示、导入校验、产品树关联、历史追溯和运行时解耦为边界。
- MDM CarLine 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM CarLine 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD CarLine 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

### CarLine 投影字段范围原则（VMD CarLine ⊂ MDM CarLine，CR-014）
> VMD 侧 CarLine 投影不要求与 MDM CarLine 主数据字段完全一致，应采用**按需最小化投影**原则。VMD CarLine 投影是 MDM CarLine 在 VMD bounded context 下的只读视图，不是 MDM CarLine 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 CarLine 字段。
2. VMD 不复制 MDM CarLine 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM CarLine 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD CarLine 投影模型。
4. VMD CarLine 投影是 MDM CarLine 在 VMD bounded context 下的只读视图，不是 MDM CarLine 的完整副本。
5. 车系投影上的 `brandCode` 冗余字段为 VMD 跨域回查与 US-031 `getBuildConfig` 响应所必需，属于 VMD 业务闭环必备字段，必须保留，不得删除或弱化（车系区别于 Brand / Platform 投影的特殊点）。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_carLine` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `carLine_code` | CarLine 编码，车辆主档与产品树 `carLineCode` 的关联键 |
| `carLine_name` | CarLine 名称，用于车辆详情、列表和产品树展示 |
| `brand_code` | 品牌冗余字段（`V2__CarLine_brand_code_migration.sql` 引入），用于跨域回查及 US-031 `getBuildConfig` 补出 `brandCode`，必须保留 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM CarLine 实体 ID |
| `external_version` | MDM CarLine 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- CarLine 审批状态。
- CarLine 生命周期全量状态流转。
- CarLine 组织归属全路径。
- CarLine 市场介绍、产品定位、营销属性等非 VMD 必需字段。
- CarLine 编码生成规则。
- CarLine 数据质量评分。
- CarLine 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### Model 主数据投影约束（CR-015）
- Model 主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Model 投影副本，不作为权威维护入口。
- 车型与 Brand / Platform / CarLine 同构、区别于 Plant 的命名迁移：车型实体命名不变、`modelCode` 关联键不变，不引入表/列重命名。
- **关键差异（区别于 CR-013/CR-014）**：CR-010（Flyway V3，`V3__Add_mdm_source_to_product_tree.sql`）仅覆盖 `veh_brand`/`veh_series`/`veh_platform`，**未覆盖 `veh_model`**，故 CR-015 **新增 Flyway 迁移 `V6__Add_mdm_source_to_model.sql`**：为 `veh_model` 增加 source / external_ref_id / external_version / last_sync_time 字段，增加 `UK(external_ref_id)`，并回填历史数据 source='MANUAL'；保持现有列 `code` / `name` / `platform_code` / `car_line_code`（即 `carLineCode`）不变。
- VMD 中 `modelCode` 是车辆主档与产品树的一部分，作为车辆车型关联字段长期保留，不改名、不删除。
- **`veh_base_model.model_code → veh_model.code` 的「车系→车型→基础车型」引用链不得切断**：BaseModel 当前仍为 VMD 自有主数据，本 CR 不改造 BaseModel / BuildConfig / FeatureFamily（后续 CR-016~018 单独处理）。
- VMD 不负责 Model 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Model 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6）与 Bootstrap 全量同步（F7，entity=model）机制，不新造链路。
- VMD Model 投影采用按需最小化字段设计，不要求与 MDM Model 主数据模型完全一致；投影字段以车辆查询、车辆详情展示、导入校验、产品树关联、历史追溯和运行时解耦为边界。
- MDM Model 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Model 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Model 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

### Model 投影字段范围原则（VMD Model ⊂ MDM Model，CR-015）
> VMD 侧 Model 投影不要求与 MDM Model 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Model 投影是 MDM Model 在 VMD bounded context 下的只读视图，不是 MDM Model 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 Model 字段。
2. VMD 不复制 MDM Model 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Model 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD Model 投影模型。
4. VMD Model 投影是 MDM Model 在 VMD bounded context 下的只读视图，不是 MDM Model 的完整副本。
5. `platformCode` / `carLineCode` 关联字段为产品树「平台→车系→车型」「车系→车型→基础车型」链路所必需，属于 VMD 业务闭环必备字段，必须保留。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_model` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `code` | Model 编码，车辆主档与产品树 `modelCode` 的关联键 |
| `name` | Model 名称，用于车辆详情、列表和产品树展示 |
| `platform_code` | 平台关联字段，产品树「平台→车系→车型」链路所需 |
| `car_line_code`（即 `carLineCode`）| 车系关联字段，产品树「车系→车型→基础车型」链路所需 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Model 实体 ID |
| `external_version` | MDM Model 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Model 审批状态。
- Model 生命周期全量状态流转。
- Model 组织归属全路径。
- Model 市场介绍、产品定位、营销属性等非 VMD 必需字段。
- Model 编码生成规则。
- Model 数据质量评分。
- Model 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### Variant 主数据投影约束（CR-016）
- Variant（版本，原 BaseModel 基础车型）主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Variant 投影副本，不作为权威维护入口。
- **本 CR 与 Plant（CR-011）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：本次涉及**实体重命名 + 关联键重命名**——MDM 侧实体由 BaseModel 改名为 Variant，VMD 将 `veh_base_model`（`tb_veh_base_model`）迁移/重命名为 `veh_variant`（`tb_veh_variant`）、关联键 `baseModelCode` → `variantCode`。
- **关键差异（区别于 CR-013/CR-014 复用 V3、CR-015 新增 V6）**：BaseModel 投影字段此前未建立，且涉及表/键重命名，故 CR-016 **新增 Flyway 迁移 `V7__Migrate_base_model_to_variant.sql`**（表迁移/重命名 + 补齐 source / external_ref_id / external_version / last_sync_time 字段 + `UK(external_ref_id)` + 回填 source='MANUAL'，保持现有列 `code` / `name` / `platform_code` / `car_line_code` / `model_code` 不变）与 **`V8__Migrate_base_model_code_to_variant_code.sql`**（`veh_basic_info` 新增 `variant_code` 回填、`veh_build_config` 与 `veh_base_model_feature_code` 的 `base_model_code` → `variant_code` 迁移/回填，迁移期保留旧列兼容）。
- VMD 中 `variantCode` 是车辆主档与产品树的一部分，作为车辆版本关联字段长期保留，承接原 `baseModelCode` 语义并回填历史值，不丢失历史数据。
- **`veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」引用链、以及 `BuildConfig → variantCode` 引用链不得切断**。
- 本 CR 仅处理 BaseModel 本体（投影化 + 改名 Variant）；**BaseModelFeatureCode / 特征值的归属与维护语义本 CR 不变**（仅做随实体重命名所必需的引用键改名 `base_model_code` → `variant_code` 与兼容，最终归属留待后续 CR）；**BuildConfig / FeatureFamily 的归属与改造不在本 CR**（CR-017 / CR-018 处理），BuildConfig 本体仍为 VMD 自有。
- VMD 不负责 Variant 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Variant 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6，新增 entity=variant）与 Bootstrap 全量同步（F7，entity=variant\|all）机制。
- VMD Variant 投影采用按需最小化字段设计，不要求与 MDM Variant 主数据模型完全一致；投影字段以车辆查询、车辆详情展示、导入校验、产品树关联、历史追溯和运行时解耦为边界。
- MDM Variant 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Variant 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Variant 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。
- 命名约束：新增需求、接口、领域对象、数据表、DTO、VO、文档统一使用 **Variant** / `variantCode` / 「版本」命名；BaseModel / `baseModelCode` / 「基础车型」仅出现在历史兼容、迁移说明或旧字段映射场景中（与 Manufacturer→Plant 的历史兼容命名约定并列）。

### Variant 投影字段范围原则（VMD Variant ⊂ MDM Variant，CR-016）
> VMD 侧 Variant 投影不要求与 MDM Variant 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Variant 投影是 MDM Variant 在 VMD bounded context 下的只读视图，不是 MDM Variant 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环所需的 Variant 字段。
2. VMD 不复制 MDM Variant 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Variant 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD Variant 投影模型。
4. VMD Variant 投影是 MDM Variant 在 VMD bounded context 下的只读视图，不是 MDM Variant 的完整副本。
5. `platform_code` / `car_line_code` / `model_code` 关联字段为产品树「车型→版本」「车系→车型→版本」链路所必需，属于 VMD 业务闭环必备字段，必须保留。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_variant` 至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `code` | Variant 编码（即 `variantCode` 关联键），车辆主档与产品树版本关联键，承接原 `baseModelCode` |
| `name` | Variant 名称，用于车辆详情、列表和产品树展示 |
| `platform_code` | 平台关联字段，产品树「车型→版本」链路所需 |
| `car_line_code` | 车系关联字段，产品树「车系→车型→版本」链路所需 |
| `model_code` | 车型关联字段，`veh_variant.model_code → veh_model.code` 引用链所需 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Variant 实体 ID |
| `external_version` | MDM Variant 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Variant 审批状态。
- Variant 生命周期全量状态流转。
- Variant 组织归属全路径。
- Variant 营销 / 产品定位属性。
- Variant 编码生成规则。
- Variant 数据质量评分。
- Variant 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### Configuration 配置主数据投影约束（CR-017）
- Configuration（配置，原 BuildConfig 生产配置）主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地 Configuration 投影副本，不作为权威维护入口。
- **本 CR 与 Plant（CR-011）/ Variant（CR-016）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 BuildConfig 改名为 Configuration，VMD 将配置实体与关联键 `buildConfigCode` 改名为 Configuration / `configurationCode`。
- **命名消歧**：本节 Configuration（配置）区别于 VehicleConfig（车辆配置，US-013）、ConfigItem（配置项，US-009）、configCenter（配置中心）；同段落出现易混概念时用全称限定，避免裸用「配置」。
- VMD 中 `configurationCode` 是车辆主档的核心锚点（每台物理车唯一映射），作为配置关联字段长期保留，承接原 `buildConfigCode` 语义并回填历史值，不丢失历史数据。
- **「版本（Variant）→配置（Configuration）」引用链与每台物理车 `configurationCode` 唯一映射不得切断**。
- 本 CR 仅处理 Configuration 配置本体（投影化 + 改名）；**BuildConfigFeatureCode / 特征值的归属与维护语义本 CR 不变**（仅做随实体重命名所必需的引用键兼容改名），其最终归属与 FeatureFamily 改造留待 CR-018。
- VMD 不负责 Configuration 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 Configuration 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6，新增 entity=configuration）与 Bootstrap 全量同步（F7，entity=configuration\|all）机制。
- VMD Configuration 投影采用按需最小化字段设计，不要求与 MDM Configuration 主数据模型完全一致；投影字段以配置关联、特征-配置反查、车辆导入校验、查询展示、历史追溯和运行时解耦为边界。
- MDM Configuration 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM Configuration 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD Configuration 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。
- 命名约束：新增需求、接口、领域对象、数据表、DTO、VO、文档统一使用 **Configuration** / `configurationCode` / 「配置」命名；BuildConfig / `buildConfigCode` / 「生产配置」仅出现在历史兼容、迁移说明或旧字段映射场景中（与 Manufacturer→Plant、BaseModel→Variant 的历史兼容命名约定并列）。

### Configuration 投影字段范围原则（VMD Configuration ⊂ MDM Configuration，CR-017）
> VMD 侧 Configuration 投影不要求与 MDM Configuration 主数据字段完全一致，应采用**按需最小化投影**原则。VMD Configuration 投影是 MDM Configuration 在 VMD bounded context 下的只读视图，不是 MDM Configuration 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环（配置关联、特征-配置反查、导入校验、查询展示、历史追溯）所需的 Configuration 字段。
2. VMD 不复制 MDM Configuration 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Configuration 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑时，才需要同步调整 VMD Configuration 投影模型。
4. VMD Configuration 投影是 MDM Configuration 在 VMD bounded context 下的只读视图，不是 MDM Configuration 的完整副本。
5. `variant_code` 关联字段为「版本（Variant）→配置（Configuration）」引用链所必需；支撑 US-031 反查的特征值映射为 VMD 业务闭环必备字段，必须保留。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 Configuration 投影至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `code` | Configuration 编码（即 `configurationCode` 关联键），车辆主档配置关联锚点（每台物理车唯一映射），承接原 `buildConfigCode` |
| `name` | Configuration 名称，用于车辆详情、列表展示 |
| `variant_code` | 版本关联字段，「版本（Variant）→配置（Configuration）」引用链所需 |
| 特征值映射 | 支撑 US-031「特征-配置反查」的特征族-特征值映射（业务语义不变，仅引用键随实体重命名兼容改名） |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM Configuration 实体 ID |
| `external_version` | MDM Configuration 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- Configuration 审批状态。
- Configuration 生命周期全量状态流转。
- Configuration 组织归属全路径。
- Configuration 营销 / 销售配置定位属性。
- Configuration 编码生成规则。
- Configuration 数据质量评分。
- Configuration 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### OptionFamily / OptionCode 主数据投影约束（CR-018）
- OptionFamily（选项族，原 FeatureFamily 特征族）/ OptionCode（选项值，原 FeatureCode 特征值）主数据的权威来源（SSOT）为 **MDM**，VMD 仅保留本地只读投影副本，不作为权威维护入口。
- **本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 FeatureFamily / FeatureCode 改名为 OptionFamily / OptionCode，VMD 将实体与关联键 `familyCode` / `featureCode` 改名为 OptionFamily / OptionCode / `optionFamilyCode` / `optionCode`。
- **命名消歧**：本节 OptionFamily / OptionCode（选项族 / 选项值）区别于 ConfigItem（配置项，US-009）下的「枚举值 Option」、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；同段落出现易混概念时用全称限定，避免裸用「选项」或「Option」。
- VMD 中 `optionFamilyCode` / `optionCode` 是版本（Variant）/ 配置（Configuration）的选项引用键，作为关联键长期保留，承接原 `familyCode` / `featureCode` 语义并回填历史值，不丢失历史数据。
- **特征-配置反查（US-031）能力与每台物理车 `configurationCode` 唯一映射不得切断**。
- 随实体重命名所必需的引用键——Variant 侧（原 BaseModelFeatureCode，CR-016）、Configuration 侧（原 BuildConfigFeatureCode，CR-017）的特征值引用键——一并由 `featureCode` 兼容改名为 `optionCode`，**仅做引用键改名，不改其业务语义、不重复接管已随 Variant / Configuration 投影下发的选项值映射数据**。
- VMD 不负责 OptionFamily / OptionCode 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 OptionFamily / OptionCode 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6，新增 entity=optionFamily / optionCode）与 Bootstrap 全量同步（F7，entity=optionFamily \| optionCode \| all）机制。
- VMD OptionFamily / OptionCode 投影采用按需最小化字段设计，不要求与 MDM 主数据模型完全一致；投影字段以选项引用、特征-配置反查、查询展示、历史追溯和运行时解耦为边界。
- MDM OptionFamily / OptionCode 的完整主数据属性、治理属性、审批属性、生命周期属性不在 VMD 投影模型中强制落库。
- 如 MDM OptionFamily / OptionCode 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD OptionFamily / OptionCode 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。
- 命名约束：新增需求、接口、领域对象、数据表、DTO、VO、文档统一使用 **OptionFamily / OptionCode** / `optionFamilyCode` / `optionCode` / 「选项族」「选项值」命名；FeatureFamily / FeatureCode / `familyCode` / `featureCode` / 「特征族」「特征值」仅出现在历史兼容、迁移说明或旧字段映射场景中（与 Manufacturer→Plant、BaseModel→Variant、BuildConfig→Configuration 的历史兼容命名约定并列）。

### OptionFamily / OptionCode 投影字段范围原则（VMD OptionFamily / OptionCode ⊂ MDM，CR-018）
> VMD 侧 OptionFamily / OptionCode 投影不要求与 MDM 主数据字段完全一致，应采用**按需最小化投影**原则。VMD OptionFamily / OptionCode 投影是 MDM 对应主数据在 VMD bounded context 下的只读视图，不是 MDM 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环（选项引用、特征-配置反查、查询展示、历史追溯）所需的 OptionFamily / OptionCode 字段。
2. VMD 不复制 MDM OptionFamily / OptionCode 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM OptionFamily / OptionCode 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、特征-配置反查、展示或校验逻辑时，才需要同步调整 VMD 投影模型。
4. VMD OptionFamily / OptionCode 投影是 MDM 在 VMD bounded context 下的只读视图，不是 MDM 的完整副本。
5. OptionCode 投影上的 `option_family_code` 归属关联字段为「选项族→选项值」从属关系及特征-配置反查所必需，属于 VMD 业务闭环必备字段，必须保留。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 OptionFamily 投影至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `code` | OptionFamily 编码（即 `optionFamilyCode` 关联键），承接原 `familyCode` |
| `name` | OptionFamily 名称，用于查询、展示 |
| `type` | 选项族类型（承接原特征族 `type` 语义），用于选项分类与反查 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM OptionFamily 实体 ID |
| `external_version` | MDM OptionFamily 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**建议 OptionCode 投影至少保留以下字段（最小投影集）**：

| 字段 | 说明 |
|------|------|
| `code` | OptionCode 编码（即 `optionCode` 关联键），承接原 `featureCode` |
| `name` | OptionCode 名称，用于查询、展示 |
| `option_family_code` | 归属选项族关联字段（承接原 `familyCode`），「选项族→选项值」从属关系与特征-配置反查所需 |
| `source` | 数据来源，MDM / MANUAL |
| `external_ref_id` | MDM OptionCode 实体 ID |
| `external_version` | MDM OptionCode 版本号 |
| `last_sync_time` | 最近同步时间 |
| `deleted` / `enabled` / `status` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**不建议默认同步以下字段（除非 VMD 明确消费，需走独立 CR）**：
- OptionFamily / OptionCode 审批状态。
- OptionFamily / OptionCode 生命周期全量状态流转。
- OptionFamily / OptionCode 组织归属全路径。
- OptionFamily / OptionCode 营销 / 销售选项定位属性。
- OptionFamily / OptionCode 编码生成规则。
- OptionFamily / OptionCode 数据质量评分。
- OptionFamily / OptionCode 主数据合并 / 拆分关系。
- MDM 内部治理字段、审批字段、流程字段。

### 供应商本地维护下线约束（CR-019）
> 供应商（Supplier）主数据的权威来源（SSOT）已上移至 **edd-mdm 的 Party 子域**（见 MDM CR-006）。**与产品树各实体（CR-011~CR-018）的按需最小化只读投影策略不同，供应商明确不在 VMD 建立任何本地只读投影**：VMD 彻底下线供应商本地维护能力（实体 + 表 + CRUD API + 契约及其附属物），仅保留 `supplier_code` 作为零部件 / 设备 / 导入记录上的溯源属性透传。本节为需求语言，落地实现（含 Flyway 删表脚本）另起任务。

**动机**：
- Supplier SSOT 上移 edd-mdm Party 子域，VMD 不再是供应商主数据维护入口。
- VMD 历史残留一整套供应商本地维护实现（领域实体 + 本地表 + 增删改查 REST API + 应用 / 持久化栈），与 MDM Party 子域形成双源 / 潜在双写风险。
- 需求上要求彻底移除，避免与 MDM 双写 / 双源、口径漂移。

**需求要求的删除范围（以需求语言逐项描述）**：
- 供应商领域对象 / 实体：`Supplier` 聚合（`domain/model/entity/Supplier`）及领域仓储接口 `SupplierRepository`。
- 供应商应用 / 持久化栈：应用服务 `SupplierAppService`（`search / getSupplierById / createSupplier / modifySupplier / deleteSupplierByIds / checkCodeUnique`）、仓储实现 `SupplierRepositoryImpl`、`SupplierMapper`（接口）与 `SupplierMapper.xml`、持久化对象 `SupplierPo`、转换器 `SupplierConverter`、装配器 `SupplierAssembler` / `MptSupplierAssembler`、专用 DTO / 命令 / 查询 / VO（`SupplierCmd` / `SupplierDto` / `SupplierQuery` / `SupplierRequest` / `SupplierResponse`）。
- 供应商本地库表：`tb_supplier`（供应商表，现有实际命名）。
- 对外供应商 CRUD REST API（`MptSupplierController`，base path `/api/mpt/supplier/v1`）：`GET /list`、`POST /export`、`GET /{supplierId}`、`POST`（新增）、`PUT`（修改）、`DELETE /{supplierIds}`，及关联权限点 `completeVehicle:vehicle:supplier:{list|export|query|add|edit|remove}`。
- 供应商相关 Feign 契约及 DTO：经核查 `edd-vmd-api` 模块当前**未发现** `VmdSupplier*Service` 等供应商对外 Feign 契约；需求上要求——若后续排查确认存在则一并移除，若不存在则在实现中明确记录「无对外 Feign 契约需移除」。
- 供应商专用附属物（如有）：仅供供应商维护使用的错误码 / 提示文案、校验（如供应商代码唯一性 `checkCodeUnique`）、枚举（如供应商类型 `type`）、Kafka 事件 / 订阅（经核查当前未发现供应商专用领域事件，如有则一并移除）。

**需求要求的保留范围（关键，明确不在下线之列）**：
- 零部件 / 设备表上的 `supplier_code` 字段一律保留，作为溯源属性透传（非主数据本体、不外键约束到本地供应商表）：`tb_part`、`tb_btm`、`tb_ccp`、`tb_idcm`、`tb_tbox`。
- 导入链路与导入批次表上的 `supplier_code` 保留：`ods_vmd_*_df` 各贴源导入表，以及导入批次表 `ods_vmd_data_import_di` 的 `supplier_code`。
- 6 类离线导入写入 `supplier_code` 的逻辑保留：PRODUCE / EOL / BTM / CCP / IDCM / TBOX / SIM 各解析 / 绑定链路（`BtmDataParserV1_0 / CcpDataParserV1_0 / IdcmDataParserV1_0 / TboxDataParserV1_0 / VehiclePartBinder` 等）写入 `supplier_code` 的行为不变。
- 数仓侧 `ods_vmd_supplier_mf` 属 DMP，不在 VMD 本 CR 范围内。
> 边界声明：`supplier_code` 是贴在零部件 / 设备 / 导入记录上的编码字符串属性，用于追溯供应商来源，**不依赖** VMD 本地供应商主数据表；下线供应商本地维护后 `supplier_code` 以纯透传方式存续。需要供应商主数据明细者改向 MDM Party 子域查询。

**调用方迁移（需求层面的要求）**：
- 要求实现前排查并列出所有调用 VMD 供应商接口 / 依赖 VMD 本地供应商数据的下游（如 SRM 供应商关系管理、导入 / 查询链路、MPT 后台供应商管理页面），形成调用方清单与影响面评估。
- 替代方案：需要供应商主数据本体者改调 edd-mdm Party 子域；仅需供应商编码者继续用 `supplier_code` 透传，无需改造。
- 要求约定下线公告、过渡窗口与灰度策略：先公告并冻结新接入，给过渡期供调用方切换至 MDM Party 子域，再按灰度逐步下线接口与本地表。

**数据处置要求（采用方案 B：直接清退）**：
- 历史本地供应商表 `tb_supplier` 采用**直接清退**：在确认无任何调用方依赖本地供应商本体、且 MDM Party 子域已完整覆盖历史数据并核对通过后，直接删表，**不做阶段性只读归档过渡**。
- 直接清退的前置条件：清退前必须完成 VMD 历史供应商数据与 MDM Party 子域的一致性核对，确保数据已在 MDM 侧可查、不丢失。
- 表删除需配套 **Flyway 删表脚本**（仅作需求说明，本 CR 不生成脚本本身）；脚本与代码删除同批次评审，并保留建表 DDL / 数据备份作为应急回滚物（备份仅作回滚兜底，不作为运行期只读归档）。
- `supplier_code` 列不在任何删表 / 清退脚本范围内（见保留范围）。

**兼容 / 废弃策略**：
- 对外供应商 CRUD REST API 建议「先 `@Deprecated` 过渡一版、再删除」：过渡版标注 `@Deprecated` 并写入下线告警日志以收敛残留调用方，过渡窗口结束后物理删除控制器、权限点及对应应用 / 持久化栈。
- 确认无调用方的内部接口可直接删除，无需过渡版。
- 注：本 CR 数据处置采用直接清退（方案 B），与 API 的 `@Deprecated` 过渡相互独立——API 过渡用于收敛调用方，数据表在调用方切换确认后直接删除。

**风险与回滚**：
- 误删 `supplier_code`：删表 / 清退脚本若波及零部件 / 设备 / 导入表的 `supplier_code` 将造成溯源断链；缓解——删除范围显式排除 `supplier_code`，脚本评审逐表核对，并以导入写入 `supplier_code` 回归用例验收。
- 调用方未切换：过渡窗口结束前仍有调用方依赖 VMD 供应商接口；缓解——先 `@Deprecated` + 下线告警日志收敛残留调用方，确认归零后再物理删除接口与表。
- 数据不可恢复（方案 B 直接清退的固有风险）：直接删表后本地无只读归档；缓解——删表前完成与 MDM Party 子域一致性核对，Flyway 删表脚本提供配套回滚 DDL 并保留建表 DDL + 数据备份，必要时可临时恢复。
- MDM 覆盖度：若 MDM Party 子域未完整覆盖历史，直接清退会丢失数据；缓解——清退前完成一致性核对，未覆盖部分先补齐 MDM 再清退。

### VehicleNode 主数据投影约束（CR-020）
- VehicleNode（车载节点，原 Device 设备）字典 / 类型主数据的权威来源（SSOT）为 **MDM 的 EEAD 子域**，VMD 仅保留本地 VehicleNode 只读投影副本，不作为权威维护入口。
- **本 CR 与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）**：MDM 侧实体由 Device 改名为 VehicleNode，VMD 将设备字典实体与关联键 `deviceCode` 改名为 VehicleNode / `vehicleNodeCode`。
- **命名消歧**：本节 VehicleNode（车载节点）区别于物理设备实例（VehiclePart 绑定的具体设备，US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；同段落出现易混概念时用全称限定，避免裸用「设备」「节点」。
- **范围边界（强约束）**：本 CR 仅处理「车载节点字典 / 类型层」主数据（节点定义、类型、功能域）；**VMD 自有的物理设备实例 + 绑定关系（VIN 绑定的 TBOX/IDCU/CCU/ADCU/TCU 实例，含 SN、part_number、hardware_vsn 及绑车 / 激活 / 下线 / 密钥 / 证书生命周期）属于 VMD 事务 / 实例数据，不上移、不投影化、保持留在 VMD**。
- VMD 中 `vehicleNodeCode` 是物理设备实例（`tb_vehicle_part` / `tb_vehicle_part_history` / `tb_part`）的节点关联键，作为节点关联字段长期保留，承接原 `deviceCode` 语义并回填历史值，不丢失历史数据。
- **物理设备实例 → 节点引用链（`tb_vehicle_part.vehicle_node_code` 等 → `mdm_vehicle_node.code`）及「车辆→零件→设备→生命周期」链路不得切断**；引用键由 `device_code` 兼容改名为 `vehicle_node_code`，仅改名、不改业务语义。
- VMD 不负责 VehicleNode 主数据治理、审批、合并、编码生成和生命周期管理。
- MDM 与 VMD 的 VehicleNode 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6，新增 entity=vehicleNode）与 Bootstrap 全量同步（F7，entity=vehicleNode \| all）机制。
- VMD VehicleNode 投影采用按需最小化字段设计，不要求与 MDM VehicleNode 主数据模型完全一致；投影字段以车辆导入校验、车辆 / 设备详情展示、下游 RPC 暴露、历史追溯和运行时解耦为边界。
- **不纳入 EEAD 外延的通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构四块**，以及物理实例字段（`sn` / `hardware_vsn` / `part_number` / IMEI / ICCID 等）。
- 如 MDM VehicleNode 后续新增字段，只有当该字段被 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑消费时，才通过独立 CR 纳入 VMD VehicleNode 投影。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。
- 命名约束：新增需求、接口、领域对象、数据表、DTO、VO、文档统一使用 **VehicleNode** / `vehicleNodeCode` / 「车载节点」命名；Device / `deviceCode` / 「设备」仅出现在历史兼容、迁移说明或旧字段映射场景中（与 Manufacturer→Plant、BaseModel→Variant、BuildConfig→Configuration、FeatureFamily→OptionFamily 的历史兼容命名约定并列）。

### VehicleNode 投影字段范围原则（VMD VehicleNode ⊂ MDM VehicleNode，CR-020）
> VMD 侧 VehicleNode 投影不要求与 MDM VehicleNode 主数据字段完全一致，应采用**按需最小化投影**原则。VMD VehicleNode 投影是 MDM VehicleNode 在 VMD bounded context 下的只读视图，不是 MDM VehicleNode 的完整副本/镜像表。

**字段设计原则**：
1. VMD 只保留支撑车辆主数据业务闭环（导入校验、车辆 / 设备详情展示、下游 RPC 暴露、历史追溯）所需的 VehicleNode 字段。
2. VMD 不复制 MDM VehicleNode 的完整治理模型、审批字段、生命周期状态、组织层级、扩展属性、EEAD 外延（通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构）等非 VMD 必需字段。
3. MDM VehicleNode 字段发生变化时，只有当变化影响 VMD 的车辆导入、车辆查询、车辆追溯、展示或校验逻辑时，才需要同步调整 VMD VehicleNode 投影模型（后加优于先冗余）。
4. VMD VehicleNode 投影是 MDM VehicleNode 在 VMD bounded context 下的只读视图，不是 MDM VehicleNode 的完整副本。
5. `vehicle_node_type` / `domain` / `status`（或 `enabled`）为节点类型关联与导入校验所必需，属于 VMD 业务闭环必备字段，必须保留。
6. **物理实例字段（`sn` / `hardware_vsn` / `part_number` / IMEI / ICCID 等）属实例数据，不进字典投影**。
7. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `mdm_vehicle_node` 至少保留以下字段（最小投影集 = A 治理元字段 + B 关联键 / 标识 + C 业务属性）**：

| 字段 | 说明 |
|------|------|
| `source` | （A）数据来源，MDM / MANUAL |
| `external_ref_id` | （A）MDM VehicleNode 实体 ID（+ `UK`） |
| `external_version` | （A）MDM VehicleNode 版本号 |
| `last_sync_time` | （A）最近同步时间 |
| `code` | （B）VehicleNode 编码（即 `vehicleNodeCode` 关联键），承接原 `deviceCode`、回填历史、长期保留 |
| `name` | （B）VehicleNode 名称，用于车辆 / 设备详情展示 |
| `vehicle_node_type` | （C）节点类型（如 TBOX/IDCU/CCU/ADCU/TCU/BTM），承接原设备项 / 类型语义 |
| `domain` | （C）功能域（承接原 `func_domain`），用于节点分类与展示 |
| `status` / `enabled` | （C）有效标志，导入校验用 |
| `deleted` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**明确排除（留 MDM、不镜像；除非 VMD 明确消费，需走独立 CR）**：
- VehicleNode 审批 / 审批流。
- Golden Record 元数据 / 来源系统映射 / 数据质量打分。
- 节点生命周期阶段。
- 负责人 / 维护组织。
- EEAD 外延：通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构。
- VMD 不展示的富描述 / 多语言 / 扩展属性。
- 物理实例字段（`sn` / `hardware_vsn` / `part_number` / IMEI / ICCID 等，属实例数据不进字典投影）。
- MDM 内部治理字段、审批字段、流程字段。

### Part 主数据投影约束（CR-021）
- Part（零件）字典 / 类型主数据的权威来源（SSOT）为 **MDM 的 Part 子域**，VMD 仅保留本地 Part 只读投影副本，不作为权威维护入口。
- **本 CR 与 Brand（CR-012）/ Platform（CR-013）/ CarLine（CR-014）/ Model（CR-015）同构、区别于 Plant(CR-011)/Variant(CR-016)/Configuration(CR-017)/VehicleNode(CR-020) 的命名迁移**：Part 实体命名不变、`partCode` 关联键不变，不涉及表 / 列重命名，Part 复用现有 `veh_part`（`tb_part`）。
- **关键差异（区别于 CR-013/CR-014 复用 V3）**：CR-010（Flyway V3）仅覆盖 `veh_brand`/`veh_series`/`veh_platform`，**未覆盖 `veh_part`（`tb_part`）**，故 CR-021 参照 Model（CR-015）那样**需新增一条 Flyway 迁移为 `veh_part`（`tb_part`）补齐 source / external_ref_id / external_version / last_sync_time 字段 + `UK(external_ref_id)` + 回填历史数据 source='MANUAL'**，保持现有业务列不变（接续 CR-020 序列；具体 Flyway 文件名与脚本细节留 design.md / tasks.md）。
- **命名消歧**：本节 Part（零件实体 / 字典）区别于 VehicleNode（车载节点，CR-020）、物理设备实例（VehiclePart 绑定的具体设备，US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；同段落出现易混概念时用全称限定。
- **范围边界（强约束，双层切分）**：本 CR 仅处理「Part 字典 / 类型层」主数据（零件定义、零件类型、规格、part_number 字典）；**VMD 自有的物理零件实例 + 绑定关系（VIN 绑定的物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书生命周期）属于 VMD 事务 / 实例数据，不上移、不投影化、保持留在 VMD**（见 US-014d）。
- VMD 中 `partCode` 是车辆 / 物理零件实例的零件关联键，作为零件关联字段长期保留，不改名、不删除；零件上的 `vehicleNodeCode`（承接原 `deviceCode`，CR-020）与 `supplier_code`（透传溯源，CR-019）一并保留。
- **物理零件实例 → Part 引用链（`tb_vehicle_part` / `tb_vehicle_part_history` → `partCode`）及「车辆→零件→设备→生命周期」链路不得切断**。
- VMD 不负责 Part 主数据治理、审批、合并、编码生成、数据质量打分和生命周期管理。
- MDM 与 VMD 的 Part 同步协议（Kafka topic、payload schema、快照接口路径、重试与死信策略）由「edd-mdm 接入规范」定义，复用现有事件订阅（F6，新增 entity=part）与 Bootstrap 全量同步（F7，entity=part \| all）机制。
- VMD Part 投影采用按需最小化字段设计，不要求与 MDM Part（`mdm_material_part`）主数据模型完全一致；**本期仅投影 P0 必投字段集 + 投影管理字段**，投影字段以零件 / 车辆导入校验、零件 / 车辆详情展示、下游 RPC 暴露、历史追溯和 MDM 不可用降级只读为边界。
- MDM Part 的完整主数据属性、治理 / 血缘 / 审计 / 设计 PLM / 供应链 / 履历类属性不在 VMD 投影模型中强制落库。
- **MDM Part 新增字段但 VMD 未消费时不强制改表**；仅当字段变化影响 VMD 的零件 / 车辆导入、查询、追溯、展示或校验逻辑时，才通过独立 CR 调整投影模型（含将某 P1 字段升入投影）。
- VMD 可根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。
- 命名约束：Part / `partCode` / 「零件」命名不变，长期沿用。

### Part 投影字段范围原则（VMD Part ⊂ MDM Part，CR-021）
> VMD 侧 Part 投影不要求与 MDM Part（`mdm_material_part`）主数据字段完全一致，应采用**按需最小化投影**原则。VMD Part 投影是 MDM Part 在 VMD bounded context 下的只读视图，不是 MDM Part 的完整副本 / 镜像表。**本期投影范围 = P0 必投字段集 + 投影管理字段**。

**字段设计原则**：
1. 按需最小化只读投影，仅持久化 VMD 业务场景（零件 / 车辆导入校验、零件 / 车辆详情展示、下游 RPC 暴露、历史追溯、MDM 不可用降级只读）所需字段。
2. VMD 不复制 MDM Part 的完整治理 / 血缘 / 审计 / 设计 PLM / 供应链 / 履历模型、生命周期状态、组织层级、扩展属性等非 VMD 必需字段。
3. MDM Part 字段变化时，只有当变化影响 VMD 的零件 / 车辆导入、查询、追溯、展示或校验逻辑时，才需要调整 VMD Part 投影模型（含将某 P1 字段升入投影）；MDM 新增未消费字段不强制改表（后加优于先冗余）。
4. VMD Part 投影是 MDM Part 在 VMD bounded context 下的只读视图，不是 MDM Part 的完整副本。
5. **物理零件实例字段（SN / part_number / hardware 等实例属性）属实例数据，不进字典投影**（留 VMD 物理零件实例层，见 US-014d）。
6. VMD 可以根据排障或审计需要保留 `raw_payload` / `extension_json` 等原始快照字段，但该字段不应作为 VMD 领域逻辑的主要依赖。

**建议 `veh_part`（`tb_part`）至少保留以下字段（最小投影集 = A 投影管理字段 + B 关联键 / 标识 + C 业务属性，P0 必投）**：

| 字段 | 说明 |
|------|------|
| `source` | （A）数据来源，MDM / MANUAL |
| `external_ref_id` | （A）← MDM Part `code`，UK，幂等 upsert 锚点 |
| `external_version` | （A）← MDM Part `version`（统一映射进 `external_version`，不原样照搬 MDM `version` 列），用于 `event.version <= local.external_version` 乱序保护 |
| `last_sync_time` | （A）最近同步时间 |
| `code` | （B）Part 编码（即 `partCode` 关联键），建 UK，同时作为 `external_ref_id` 来源；沿用现有零件关联键，长期保留、不改名 |
| `name` | （B）零件名称，用于零件 / 车辆详情展示 |
| `part_type` | （C）零件类型枚举（承接现有零件类型语义），类型语义 / 校验 / 展示 |
| `vehicle_node_code` | （C）车载节点编码（承接原 `deviceCode`，CR-020），→ VehicleNode 投影链路关联；透传，不建本地外键 |
| `supplier_code` | （C）供应商编码，→ Party.Supplier 透传溯源（延续 CR-019 透传语义）；不建本地外键 |
| `is_software` | （C）是否软件件，下游 OTA / 能力语义 |
| `fota_upgradeable` | （C）是否可 FOTA 升级，下游 OTA 强相关 |
| `is_accurately_traced` | （C）是否精准追溯（承接现有精准追溯语义），驱动 VMD 物理零件实例层是否记单件 SN / 精准追溯的行为 |
| `status` | （C）状态 DRAFT / ACTIVE / INACTIVE，装车可用性校验（仅 ACTIVE 可用）及展示 |
| `deleted` / `enabled` | 可选，用于处理 MDM 删除、停用或不可用状态 |
| `raw_payload` / `extension_json` | 可选，用于排障、审计或临时兼容 |

**本期明确不投影（P1 按需字段，留待后续按需 CR 增量升投）**：
- `name_local` / `description` / `category_code` / `is_safety_critical` / `is_key_part` / `is_regulatory_part` / `is_frame_part` / `lifecycle_stage` / `substitute_part_code` / `production_code` / `ffa_code` 等暂不纳入；仅当确有车辆 / 零件导入、查询、追溯、展示、校验或下游消费场景时，通过后续独立 CR 增量纳入投影（升入 P0）。

**本期明确不投影（P2 字段，属 MDM SSOT 职责，不镜像）**：
- MDM 内部主键 / 乐观锁：`id` / `row_version` / `row_valid`。
- 接入血缘：`source_system` / `source_id` / `source_version` / `ingestion_*` / `source_payload_hash`。
- 审计：`create_*` / `modify_*`。
- 设计 PLM / 物流 / 履历：`is_digitate` / `drawing_*` / `weight*` / `uom` / `first_production_date` / `designer*` / `initial_model` / `ffa_desc`。
- 时效区间：`effective_from` / `effective_to`。
- Part 审批 / 审批流、Golden Record 元数据 / 数据质量打分、零件生命周期阶段、负责人 / 维护组织、MDM 内部治理 / 流程字段。
- 物理零件实例字段（SN / part_number / hardware 等实例属性，属实例数据不进字典投影）。

> **后续增量升投机制**：MDM Part 新增字段但 VMD 未消费时不强制改表；仅当某字段变化影响 VMD 零件 / 车辆导入、查询、追溯、展示或校验时，才通过独立 CR 调整投影模型（含将某 P1 字段升入 P0 投影）。

### 依赖（外部）
- **TSP 服务**：`TspVehicleCcpService / TspVehicleIdcmService / TspVehicleNetworkService / TspVehicleTboxService / TspCcpInfoService / TspIdcmInfoService / TspTboxInfoService / TspSimService`。
- **OTA 服务**：`OtaVehiclePartService`（车辆零件同步）。
- **IDK 服务**：`IdkBtmInfoService`（蓝牙模块批量导入）。
- **账号服务（已注释）**：`ExAccountService`（预设车主校验，待启用）。
- **安全密钥服务（已注释）**：`ExSkService`（IMMO_SK 生成，待启用）。
- **edd-mdm 服务**：Product MDM 子域，提供品牌 / 车系 / 平台 / **Plant（工厂）**/ **车型（Model）**/ **版本（Variant，原 BaseModel 基础车型）**/ **配置（Configuration，原 BuildConfig 生产配置）**/ **选项族（OptionFamily，原 FeatureFamily 特征族）/ 选项值（OptionCode，原 FeatureCode 特征值）**主数据的 Kafka 事件推送 + Feign 全量快照接口；**EEAD MDM 子域，提供车载节点（VehicleNode，原 Device 设备）字典 / 类型主数据的 Kafka 事件推送 + Feign 全量快照接口（CR-020）**；**Part MDM 子域，提供零件（Part）字典 / 类型主数据的 Kafka 事件推送 + Feign 全量快照接口（CR-021）**。详见「edd-mdm 接入规范」。

### 前置条件
- Nacos 中已存在共享配置 `application.yaml / mysql.yaml / redis.yaml`。
- MySQL 数据库已存在并允许 Flyway 在启动时执行 `V0__Baseline.sql / V1__BuildConfig_feature_code_migration.sql / V2__CarLine_brand_code_migration.sql`。
- API 网关下游路由已将 `edd-vmd` 注册到正确路径前缀。
- MDM 已完成首版 Product MDM 子域上线，且 Kafka topic 已开通。
- MDM 已上线 Plant（工厂）主数据实体，且 Plant 事件 Kafka topic 与 Plant 全量快照接口已就绪（CR-011）。
- MDM 已上线 Variant（版本，原 BaseModel 基础车型）主数据实体，且 Variant 事件 Kafka topic 与 Variant 全量快照接口已就绪（CR-016）。
- MDM 已上线 OptionFamily（选项族，原 FeatureFamily）/ OptionCode（选项值，原 FeatureCode）主数据实体，且 OptionFamily / OptionCode 事件 Kafka topic 与全量快照接口已就绪（CR-018）。
- MDM 已上线 VehicleNode（车载节点，原 Device 设备）字典 / 类型主数据实体（EEAD 子域，MDM CR-007），且 VehicleNode 事件 Kafka topic 与 VehicleNode 全量快照接口已就绪（CR-020）。
- MDM 已上线 Part（零件）字典 / 类型主数据实体（Part 子域），且 Part 事件 Kafka topic 与 Part 全量快照接口已就绪（CR-021）。
- MDM Feign 全量快照接口已就绪，VMD 可通过 Feign 调用。
- VMD 启动时若本地无 source=MDM 数据，需通过 Bootstrap 流程从 MDM 拉全量。

### PartInfo / VehiclePart 物理实例层模型约束（CR-022）
- 物理实例层（`part_info` / `vehicle_part`）为 **VMD 自有事务 / 实例数据**，权威来源在 VMD，不上移、不投影化、不消费 MDM 同步。
- `part_info` 唯一键为 `(part_code, sn)`；`part_code` NOT NULL。
- `vehicle_part` 为纯绑定关系，必含 `vin`（→ `vehicle_info`）与 `part_id`（→ `part_info`），施加「同一实例 / 同一车同一节点位 仅一条 active 绑定」约束。
- 实例属性归位准则：本体属性（不随装车改变）→ `part_info`；绑定属性（随装车 / 位置 / 时间 / 状态 / 换件变化）→ `vehicle_part`；字典属性（「应有什么」）→ `tb_mdm_part` / `tb_mdm_vehicle_node`（不动）。
- `part_info` / `vehicle_part` 仅以 `part_code` / `vehicle_node_code` / `supplier_code` 持引用键，**不复制字典字段、不建物理外键**。
- 换件历史由 `vehicle_part` 绑定时间线表达，`tb_vehicle_part_history` 废弃。
- 无历史数据，结构干净重建，不做数据回迁。

### 零件实例数据入站约束（CR-023）
- 零件实例写入仅两个录入入口：入口①上游系统对接（US-037，独立链路、异步事件为主 + 批量兜底、含入站回执 / 错误通知）与入口②管理后台导入（US-018）；两入口**共用同一套入站内核**（US-038），**严禁后台导入旁路另写规则**。
- 入站内核统一六步：字段校验 → 标准化 → 幂等 → 去重 → 落库 → 触发跨域事件。
- **所有带 SN 的物理零件实例（含 SIM）统一落 `part_info`**；VIN / 安装位置就绪时建 `vehicle_part`；下游域（TSP/OTA/IDK）一律经跨域事件订阅消费，不作为落库分支。
- 车载节点（`vehicle_node_code`）对零件实例为**可选**属性（仅联网件 / 可升级件 / 关键件具备）；无车载节点的 SN 实例（发动机 / 电机 / 电池包 / SIM 等）为合法实例，安装位置以 `device_item` 表达。
- 各零件类型的特殊信息经**按 `part_type` 的字段契约（type-schema）校验 + 标准化**后写入 `part_info.extra`；落库去向按 `part_type` 适配（硬件件 → `part_info` + `vehicle_part`；SIM → `part_info` + 触发 TSP 连接 / 激活事件）。
- `vehicle_part.bind_org` 取值来自实例 `source`，**禁止硬编码具体源系统**（如 `MES`）。
- 入站来源以 `part_info.source` 打标，取值可扩展枚举 `MES / MANUAL / WMS / IQC / OTHER`。
- 入站失败记录隔离、可按入站溯源键（`inbound_batch_no` / `source_event_id`）重放与对账（US-039）。
- **本 CR 反转 CR-022 O86**：SIM 纳入物理实例层（落 `part_info`），不再「仅走 TSP 不入实例层」；SIM 连接 / 激活状态仍归 TSP（另一条写路径）。

### 来源标记语义区分（CR-023）
- **字典投影来源** `source ∈ {MDM, MANUAL}`：表达主数据权威归属（MDM 投影 vs 本地兼容数据），作用于 §4 各字典投影表（CR-010~021）。
- **物理实例入站来源** `source ∈ {MES, MANUAL, WMS, IQC, OTHER}`（可扩展）：表达零件实例的入站来源系统，作用于 `part_info`。
- 两套 `source` 为**独立语义**，**禁止混用同一取值域**。

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
- O26：VMD 不再提供 Platform 主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-013）。
- O27：VMD 不实现 Platform 主数据的 Golden Record 合并能力（CR-013）。
- O28：VMD 不实现 Platform 编码规则生成、主数据审批流程、生命周期管理（CR-013）。
- O29：VMD 不要求完整复制 MDM Platform 的所有字段；不承担 MDM Platform 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-013）。
- O30：历史 Platform 数据 source 回标、清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-013）。
- O31：本次 CR 只定义 Platform 从本地维护到本地投影的需求语义调整，不要求一次性删除所有 VMD Platform add/edit/remove 接口和权限点；最终下线由后续兼容性清理 CR 完成（CR-013）。
- O32：MDM Platform 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-013）。
- O33：VMD 不再提供 CarLine 主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-014）。
- O34：VMD 不实现 CarLine 主数据的 Golden Record 合并能力（CR-014）。
- O35：VMD 不实现 CarLine 编码规则生成、主数据审批流程、生命周期管理（CR-014）。
- O36：VMD 不要求完整复制 MDM CarLine 的所有字段；不承担 MDM CarLine 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-014）。
- O37：历史 CarLine 数据 source 回标、清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-014）。
- O38：本次 CR 只定义 CarLine 从本地维护到本地投影的需求语义调整，不要求一次性删除所有 VMD CarLine add/edit/remove 接口和权限点（`completeVehicle:product:carLine:add/edit/remove`）；最终下线由后续兼容性清理 CR 完成。其中车系投影上的 `brandCode` 冗余字段为 VMD 跨域回查与 US-031 `getBuildConfig` 所需，不在任何清理 / 下线范围内，长期保留（CR-014）。
- O39：MDM CarLine 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-014）。
- O40：VMD 不再提供 Model 主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-015）。
- O41：VMD 不实现 Model 主数据的 Golden Record 合并能力（CR-015）。
- O42：VMD 不实现 Model 编码规则生成、主数据审批流程、生命周期管理（CR-015）。
- O43：VMD 不要求完整复制 MDM Model 的所有字段；不承担 MDM Model 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-015）。
- O44：历史 Model 数据 source 回标、清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-015）。
- O45：本次 CR 只定义 Model 从本地维护到本地投影的需求语义调整，不要求一次性删除所有 VMD Model add/edit/remove 接口和权限点（`completeVehicle:product:model:add/edit/remove`）；最终下线由后续兼容性清理 CR 完成。**BaseModel / BuildConfig / FeatureFamily 的投影化改造不在本 CR 范围内**（后续 CR-016~018 单独处理），且「车系→车型→基础车型」引用链不得切断（CR-015）。
- O46：MDM Model 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-015）。
- O47：VMD 不再提供 Variant（版本，原 BaseModel）主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-016）。
- O48：VMD 不实现 Variant 主数据的 Golden Record 合并、编码规则生成、主数据审批流程与版本生命周期管理（CR-016）。
- O49：VMD 不要求完整复制 MDM Variant 的所有字段；不承担 MDM Variant 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-016）。
- O50：历史 Variant 数据 source 回标、历史 `baseModelCode` / `base_model_code` 清洗纠错归并由独立数据治理 CR 处理，本期 spec 不实现（CR-016）。
- O51：本次 CR 只定义 BaseModel→Variant 的投影化、实体重命名与关联键重命名（`baseModelCode`→`variantCode`）及兼容策略，不要求一次性删除所有旧字段（`base_model_code` 等）、旧接口（`/api/mpt/baseModel/**`、`listByBaseModelCode`、`/buildConfig/list/{baseModelCode}`）和旧权限点（`completeVehicle:product:baseModel:*`）；旧资产标记 `deprecated`，最终下线由后续兼容性清理 CR 完成（CR-016）。
- O52：**不改造 BaseModelFeatureCode / 特征值的归属与维护语义**（留待后续 CR），仅做随实体重命名必需的引用键兼容改名（`base_model_code`→`variant_code`）（CR-016）。
- O53：**不改造 BuildConfig / FeatureFamily 的归属**（CR-017 / CR-018 处理）；不得切断「车系→车型→版本」及 `BuildConfig → variantCode` 引用链（CR-016）。
- O54：MDM Variant 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-016）。
- O55：VMD 不再提供 OptionFamily / OptionCode（选项族 / 选项值，原 FeatureFamily / FeatureCode）主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-018）。
- O56：VMD 不实现 OptionFamily / OptionCode 主数据的 Golden Record 合并、编码规则生成、主数据审批流程与选项族 / 选项值生命周期管理（CR-018）。
- O57：VMD 不要求完整复制 MDM OptionFamily / OptionCode 的所有字段；不承担 MDM OptionFamily / OptionCode 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-018）。
- O58：历史 OptionFamily / OptionCode 数据 source 回标、历史 `familyCode` / `featureCode` 清洗纠错归并由独立数据治理 CR 处理，本期 spec 不实现（CR-018）。
- O59：本次 CR 只定义 FeatureFamily→OptionFamily / FeatureCode→OptionCode 的投影化、实体重命名与关联键重命名（`familyCode`→`optionFamilyCode`、`featureCode`→`optionCode`）及兼容策略，不要求一次性删除所有旧字段（`family_code` / `feature_code` 等）、旧接口（`/api/mpt/featureFamily/**`、`listAllFeatureCode`）和旧权限点（`completeVehicle:product:featureFamily:*` / `completeVehicle:product:featureCode:*`）；旧资产标记 `deprecated`，最终下线由后续兼容性清理 CR 完成（CR-018）。
- O60：**不重复接管已随 Variant（CR-016）/ Configuration（CR-017）投影下发的选项值映射数据**，本 CR 仅对 Variant 侧（原 BaseModelFeatureCode）/ Configuration 侧（原 BuildConfigFeatureCode）的特征值引用键 `feature_code` 做随实体重命名必需的兼容改名（`feature_code`→`option_code`），不改其业务语义；不得切断特征-配置反查（US-031）能力与每台物理车 `configurationCode` 唯一映射（CR-018）。
- O61：MDM OptionFamily / OptionCode 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-018）。
- O62：VMD 不再为供应商建立本地只读投影（区别于产品树各实体 CR-011~CR-018 的投影化策略，供应商不投影）（CR-019）。
- O63：数仓侧 `ods_vmd_supplier_mf` 属 DMP，不在 VMD 本 CR 范围内（CR-019）。
- O64：edd-mdm Party 子域供应商主数据的内部模型设计、治理、审批、编码规则、生命周期管理不在 VMD 范围内（CR-019）。
- O65：调用方（如 SRM）切换至 edd-mdm Party 子域的具体改造由各调用方自行立项，不在 VMD 本 CR 范围内（CR-019）。
- O66：历史供应商数据向 edd-mdm Party 子域的迁移 / 核对 / 回标由独立数据治理任务执行，本期 spec 不实现（CR-019）。
- O67：供应商相关 Flyway 删表脚本、字段物理删除、代码删除、API 下线响应等实现细节留 design.md / tasks.md（CR-019）。
- O68：VMD 不再提供 VehicleNode（车载节点，原 Device 设备）字典 / 类型主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-020）。
- O69：VMD 不实现 VehicleNode 主数据的 Golden Record 合并、编码规则生成、主数据审批流程、数据质量打分与节点生命周期管理（CR-020）。
- O70：VMD 不要求完整复制 MDM VehicleNode 的所有字段；不承担 MDM VehicleNode 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）（CR-020）。
- O71：**EEAD 外延的通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构四块不在 VMD VehicleNode 投影范围内**；**物理设备实例 + 绑定关系（含 SN/part_number/hardware_vsn 及绑车 / 激活 / 下线 / 密钥 / 证书生命周期）为 VMD 自有事务 / 实例数据，不上移、不投影化，不在本 CR 改造范围**（仅引用键 `device_code` → `vehicle_node_code` 兼容改名）（CR-020）。
- O72：历史 VehicleNode 数据 source 回标、历史 `deviceCode` / `device_code` 清洗纠错归并由独立数据治理 CR 处理，本期 spec 不实现（CR-020）。
- O73：本次 CR 只定义 Device→VehicleNode 的投影化、实体重命名与关联键重命名（`deviceCode`→`vehicleNodeCode`）及兼容策略，不要求一次性删除所有旧字段（`device_code` 等）、旧接口（`/api/mpt/device/**`、`/api/service/device/**`）和旧权限点（`completeVehicle:vehicle:device:*`）；旧资产标记 `deprecated`，最终下线由后续兼容性清理 CR 完成；不得切断物理设备实例 → 节点引用链与「车辆→零件→设备→生命周期」链路（CR-020）。
- O74：MDM VehicleNode 的内部模型设计、生命周期状态、审批流、编码规则、EEAD 外延（通讯矩阵 / 诊断架构 / 刷写 OTA 拓扑 / 安全架构）不在 VMD 范围内（CR-020）。
- O75：VehicleNode 相关 Flyway 迁移脚本（`tb_device`→`mdm_vehicle_node`、`device_code`→`vehicle_node_code`）、字段物理改名、代码改名等实现细节留 design.md / tasks.md（CR-020）。
- O76：VMD 不再提供 Part（零件）字典 / 类型层主数据的长期本地新增、修改、删除能力（source=MANUAL 过渡数据除外，且仅作为兼容期遗留）（CR-021）。
- O77：VMD 不实现 Part 主数据的 Golden Record 合并、编码规则生成、主数据审批流程、数据质量打分与零件生命周期管理（CR-021）。
- O78：VMD 不要求完整复制 MDM Part（`mdm_material_part`）的所有字段；不承担 MDM Part 字段变化的自动同步适配责任（字段变化影响 VMD 业务时走独立 CR）；**本期仅投影 P0 必投字段集 + 投影管理字段，P1 按需字段（`name_local` / `description` / `category_code` / `is_safety_critical` / `is_key_part` / `is_regulatory_part` / `is_frame_part` / `lifecycle_stage` / `substitute_part_code` / `production_code` / `ffa_code` 等）与 P2 字段（MDM 内部主键 / 乐观锁、接入血缘、审计、设计 PLM / 物流 / 履历、时效区间等）不投影**，P1 字段仅在确有车辆 / 零件导入 / 查询 / 追溯 / 展示 / 校验 / 下游消费场景时通过后续独立 CR 增量升投（CR-021）。
- O79：**物理零件实例 + 绑定关系 + 生命周期（VIN 绑定物理零件实例及其 SN / part_number / hardware 等实例属性、零件→设备挂载、装车 / 换件 / 下线 / 密钥 / 证书生命周期）为 VMD 自有事务 / 实例数据，不上移、不投影化，不在本 CR 改造范围**（US-017 / US-020 / US-026 语义不变）；不得切断「车辆→零件→设备→生命周期」链路（CR-021）。
- O80：历史 Part 数据 source 回标、清洗、纠错、归并由独立数据治理 CR 处理，本期 spec 不实现（CR-021）。
- O81：本次 CR 只定义 Part 从本地维护到本地只读投影的需求语义调整（实体 / 关联键命名不变、不做表 / 列重命名），不要求一次性删除所有 VMD Part add/edit/remove 接口和权限点；权限点 `completeVehicle:vehicle:part:*` 迁入 `product` 命名空间为 `completeVehicle:product:part:list/query/export`（`add/edit/remove` 仅兼容期遗留限 source=MANUAL），旧 `vehicle:part` 权限点标记 `deprecated`，最终下线由后续兼容性清理 CR 完成（CR-021）。
- O82：Part 相关 Flyway 迁移脚本（为 `tb_part` 补齐 source / external_ref_id / external_version / last_sync_time + `UK(external_ref_id)` + 回填 source='MANUAL'）、字段物理补齐、代码改造等实现细节留 design.md / tasks.md；MDM Part 的内部模型设计、生命周期状态、审批流、编码规则不在 VMD 范围内（CR-021）。
- O83：本 CR 不把 Part / VehicleNode / 产品树各实体从 MDM 投影拉回 VMD，不改其只读投影归属（CR-022）。
- O84：本 CR 不为 `part_info` / `vehicle_part` 建立指向 `tb_mdm_*` 的物理外键（仅引用键透传）（CR-022）。
- O85：本 CR 不引入零件实例独立生命周期事件表，零件实例状态由 `part_info.instanceState` + `vehicle_part` 时间线覆盖，独立事件表留后续 CR（CR-022）。
- O86：本 CR 不改造 SIM 链路（仍走 TSP，不入物理实例层）（CR-022）。
- O87：因无历史数据，本 CR 不实现旧表数据拆分迁移、`tb_vehicle_part_history` 回灌与旧本体列兼容读（CR-022）。
- O88：MySQL「仅一条 active 绑定」约束的物理实现手段（生成列 / NULL 技巧等）属 design.md / tasks.md，本 CR 仅声明约束目标（CR-022）。
- O89：死表 / 死 VO 的具体删除脚本、Flyway 文件（建议 `V18__Drop_dead_tables` / `V19__Create_part_info` / `V20__Rebuild_vehicle_part_as_binding`，接续 V17）等实现细节留 design.md / tasks.md（CR-022）。
- O90：历史车辆数据治理、SN 脏数据清洗、source 回标等不在本 CR（本 CR 假设无历史数据）（CR-022）。
- O91：PRODUCE 整车主档入站（US-019）不纳入零件实例数据入站，沿用现状（CR-023）。
- O92：TSP 激活 / 连接回写（密钥 / 证书 / 激活状态 / SIM 连接）不建模，仅边界声明（CR-023）。
- O93：MES / 产线内部如何采集零件不设计，VMD 仅定义入站接收契约（CR-023）。
- O94：`part_type` 字段契约（type-schema）的具体落地形态（配置表 / 注册中心 / 校验引擎）、入口①独立链路的传输实现（topic 命名 / 批量接口协议 / 鉴权机制）、入站失败隔离表与「仅一条 active 绑定」物理实现留 design.md / tasks.md（CR-023）。
- O95：US-035 死表清退 / 主档瘦身属 CR-022，不在本 CR 范围；本 CR 仅在边界处引用 VehicleInfo 瘦身上下文（CR-023）。

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
| 2026-06-08 | CR-013 | Modified | **平台主数据重构为 MDM Platform 本地投影**：Platform 主数据 SSOT 上移至 MDM，VMD 保留 Platform 本地投影表（US-006 由「维护车辆平台 Platform」改写为「消费 MDM Platform 主数据本地投影」；强化 US-006b Platform Bootstrap 全量同步，补齐最小字段集/失败不清空/幂等/entity=all 措辞；新增 US-006c Platform 本地维护能力兼容清理）；与 Brand（CR-012）同构、区别于 Plant 命名迁移——平台实体命名不变、`platformCode` 关联键不变，不引入表/列重命名、不新增 Flyway 迁移，直接复用 CR-010 为 `veh_platform` 建好的 source / external_ref_id / external_version / last_sync_time 字段（`veh_platform.code` 即 `platform_code`、`name` 即 `platform_name`）；新增 §4「Platform 主数据投影约束」与「Platform 投影字段范围原则」（VMD Platform ⊂ MDM Platform，按需最小化投影，不要求完整复制 MDM Platform 主数据模型）；VMD Platform add/edit/remove 仅作为 source=MANUAL 兼容期遗留能力，对 source=MDM 记录一律只读；保留 `platformCode` 作为车辆主档（`veh_basic_info.platform_code`）与产品树（`veh_model.platform_code` / `veh_base_model.platform_code`）的平台关联字段（不改名、不删除）；MDM 事件订阅（F6）与 Bootstrap 全量同步（F7，entity=platform）复用 CR-010 已覆盖链路，不新增链路；§2 G5 纳入 Platform「MDM 下游消费方 + 只读本地投影副本」语义并新增 Platform 语义统一说明、新增 N6 Platform 非目标；§5 新增 O26~O32；权限点 `completeVehicle:product:platform:list/query/export` 长期保留，`add/edit/remove` 仅作兼容期遗留（限 source=MANUAL）待后续 CR 下线。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-08 | CR-014 | Modified | **车系主数据重构为 MDM CarLine 本地投影**：CarLine 主数据 SSOT 上移至 MDM，VMD 保留 CarLine 本地投影表（US-002 由「维护车系 CarLine」改写为「消费 MDM CarLine 主数据本地投影」；强化 US-002b CarLine Bootstrap 全量同步，补齐最小字段集/失败不清空/幂等 upsert/entity=all/启动时 source=MDM 记录为 0 自动拉全量措辞；新增 US-002c CarLine 本地维护能力兼容清理）；与 Brand（CR-012）、Platform（CR-013）同构、区别于 Plant 命名迁移——车系实体命名不变、`carLineCode` 关联键不变，不引入表/列重命名、不新增 Flyway 迁移，直接复用 CR-010 为 `veh_carLine` 建好的 source / external_ref_id / external_version / last_sync_time 字段；新增 §4「CarLine 主数据投影约束」与「CarLine 投影字段范围原则」（VMD CarLine ⊂ MDM CarLine，按需最小化投影，不要求完整复制 MDM CarLine 主数据模型）；VMD CarLine add/edit/remove 仅作为 source=MANUAL 兼容期遗留能力，对 source=MDM 记录一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留 `carLineCode` 作为车辆主档与产品树的车系关联字段（不改名、不删除）；**特别保留车系投影上的 `brandCode` 冗余字段（`V2__CarLine_brand_code_migration.sql` 引入），用于跨域回查并支撑 US-031 `getBuildConfig` 在响应中补出 `brandCode`，不得删除或弱化（车系区别于 Brand / Platform 投影的特殊点）**；MDM 事件订阅（F6）与 Bootstrap 全量同步（F7，entity=carLine）复用 CR-010 已覆盖链路，不新增链路；§2 G5 纳入 CarLine「MDM 下游消费方 + 只读本地投影副本」语义并新增 CarLine 语义统一说明、新增 N7 CarLine 非目标；§5 新增 O33~O39；权限点 `completeVehicle:product:carLine:list/query/export`（含 listByBrandCode/listAll）长期保留，`add/edit/remove` 仅作兼容期遗留（限 source=MANUAL）待后续 CR 下线。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-08 | CR-015 | Modified | **车型主数据重构为 MDM Model 本地投影**：Model 主数据 SSOT 上移至 MDM，VMD 保留 Model 本地投影表（US-003 由「维护车型 Model」改写为「消费 MDM Model 主数据本地投影」；新增 US-003b Model Bootstrap 全量同步（entity=model\|all），补齐最小字段集/失败不清空/幂等 upsert/启动时 source=MDM 记录为 0 自动拉全量措辞；新增 US-003c Model 本地维护能力兼容清理；US-004 BaseModel 查询语义保持不变，仅其引用的 `modelCode` 数据来源变为投影）；与 Brand（CR-012）、Platform（CR-013）、CarLine（CR-014）同构、区别于 Plant 命名迁移——车型实体命名不变、`modelCode` 关联键不变，不引入表/列重命名；**关键差异：CR-010/V3 仅覆盖 veh_brand/veh_series/veh_platform，未覆盖 `veh_model`，故 CR-015 新增 Flyway 迁移 `V6__Add_mdm_source_to_model.sql` 为 `veh_model` 补齐 source / external_ref_id / external_version / last_sync_time 字段 + UK(external_ref_id) + 回填 source='MANUAL'（区别于 CR-013/CR-014 复用 V3）**，保持现有列 `code`/`name`/`platform_code`/`series_code`(=carLineCode) 不变；新增 §4「Model 主数据投影约束」与「Model 投影字段范围原则」（VMD Model ⊂ MDM Model，按需最小化投影，不要求完整复制 MDM Model 主数据模型）；VMD Model add/edit/remove 仅作为 source=MANUAL 兼容期遗留能力，对 source=MDM 记录一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留 `modelCode` 作为车辆主档与产品树的车型关联字段（不改名、不删除）；**保留 `veh_base_model.model_code → veh_model.code` 的「车系→车型→基础车型」引用链，不得切断（BaseModel 当前仍为 VMD 自有，BaseModel/BuildConfig/FeatureFamily 改造留待后续 CR-016~018）**；MDM 事件订阅（F6，新增 entity=model）与 Bootstrap 全量同步（F7，entity=model\|all）复用现有机制，新增 `MdmModelQueryClient` Feign 客户端用于运行时按需查询与降级兜底；§2 G5 纳入 Model「MDM 下游消费方 + 只读本地投影副本」语义并新增 Model 语义统一说明、新增 N8 Model 非目标；§5 新增 O40~O46；权限点 `completeVehicle:product:model:list/query/export` 长期保留，`add/edit/remove` 仅作兼容期遗留（限 source=MANUAL）待后续 CR 下线。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-09 | CR-016 | Modified | **基础车型（BaseModel）重构为 MDM Variant（版本）本地投影 + 命名迁移**：Variant 主数据 SSOT 上移至 MDM，VMD 保留 Variant 本地投影表（US-004 由「维护基础车型 BaseModel 及其特征值」改写为「消费 MDM Variant 主数据本地投影」；新增 US-004b Variant Bootstrap 全量同步（entity=variant\|all）、US-004c Variant 本地维护能力兼容清理 + BaseModel→Variant 命名迁移）；**与 Plant（CR-011）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）——本次涉及实体重命名 + 关联键重命名**：MDM 侧 BaseModel 改名为 Variant，VMD 将 `veh_base_model`→`veh_variant`、`baseModelCode`→`variantCode`，BaseModel/`baseModelCode`/「基础车型」转为历史兼容命名（参照 Manufacturer→Plant）；新增 §4「Variant 主数据投影约束」与「Variant 投影字段范围原则」（VMD Variant ⊂ MDM Variant，按需最小化投影）；VMD Variant add/edit/remove 仅作 source=MANUAL 兼容期遗留，对 source=MDM 一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留并回填 `variantCode`，**`veh_variant.model_code → veh_model.code` 的「车系→车型→版本（原基础车型）」与 `BuildConfig → variantCode` 引用链不得切断**；US-005 BuildConfig 引用键 `base_model_code`→`variant_code`、路径 `listByBaseModelCode/{baseModelCode}`→`listByVariantCode/{variantCode}`（保留旧路径兼容，BuildConfig 本体仍为 VMD 自有）；US-019 PRODUCE 解析器七项编码 `baseModelCode`→`variantCode`（含历史兼容读取/映射）；US-030 service 路径 `/buildConfig/list/{baseModelCode}`→`/buildConfig/list/{variantCode}`（保留旧路径兼容）；MDM 事件订阅（F6，新增 entity=variant）与 Bootstrap 全量同步（F7，entity=variant\|all）复用现有机制；§1 产品树链路、§2 G5 纳入 Variant 语义并新增 Variant 语义统一说明、新增 N9 Variant 非目标；§5 新增 O47~O54。**本 CR 不改造 BaseModelFeatureCode/特征值归属（仅引用键兼容改名）、不改造 BuildConfig/FeatureFamily 归属（CR-017/CR-018）**。<br>**受影响清单（供实现对账）**：<br>· 表：`tb_veh_base_model`→`tb_veh_variant`（重命名 + 补 source/external_ref_id/external_version/last_sync_time + UK(external_ref_id)）；`tb_veh_basic_info`（新增 `variant_code` 回填）；`tb_veh_build_config`（`base_model_code`→`variant_code`）；`tb_veh_base_model_feature_code`（`base_model_code`→`variant_code`，仅引用键改名）。<br>· 字段：`base_model_code`/`baseModelCode`→`variant_code`/`variantCode`（保留旧列/旧字段兼容并回填）；`veh_variant` 保留列 `code`/`name`/`platform_code`/`car_line_code`/`model_code` 不变。<br>· 权限点：`completeVehicle:product:baseModel:*`→`completeVehicle:product:variant:list/query/export`；`variant:add/edit/remove` 仅兼容期遗留（限 source=MANUAL）；旧 `baseModel:*` 标记 `deprecated` 待后续 CR 下线。<br>· API path：`/api/mpt/baseModel/**`→`/api/mpt/variant/**`（含 `listByPlatformCodeAndCarLineCodeAndModelCode`）；`/api/mpt/buildConfig/v1/listByBaseModelCode/{baseModelCode}`→`listByVariantCode/{variantCode}`；`/api/service/vehicleModelConfig/v1/buildConfig/list/{baseModelCode}`→`/buildConfig/list/{variantCode}`（旧路径迁移期保留兼容）。<br>· Flyway：新增 `V7__Migrate_base_model_to_variant.sql`（表迁移/重命名 + 投影字段 + UK + 回填 source='MANUAL'）、`V8__Migrate_base_model_code_to_variant_code.sql`（basic_info/build_config/base_model_feature_code 关联键迁移回填），接续 CR-015 的 V6。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-09 | CR-017 | Modified | **生产配置（BuildConfig）重构为 MDM Configuration（配置）本地投影 + 命名迁移**：Configuration 配置主数据 SSOT 上移至 MDM，VMD 保留 Configuration 本地投影表（US-005 由「维护生产配置 BuildConfig 及其特征值」改写为「消费 MDM Configuration 主数据本地投影」；新增 US-005b Configuration Bootstrap 全量同步（entity=configuration\|all）、US-005c Configuration 本地维护能力兼容清理 + BuildConfig→Configuration 命名迁移）；**与 Plant（CR-011）/ Variant（CR-016）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）——本次涉及实体重命名 + 关联键重命名**：MDM 侧 BuildConfig 改名为 Configuration，VMD 将 `buildConfigCode`→`configurationCode`，BuildConfig/`buildConfigCode`/「生产配置」转为历史兼容命名（参照 Manufacturer→Plant、BaseModel→Variant）；**命名消歧**——Configuration（配置）区别于 VehicleConfig（车辆配置，US-013）、ConfigItem（配置项，US-009）、configCenter（配置中心），易混处用全称限定；新增 §4「Configuration 配置主数据投影约束」与「Configuration 投影字段范围原则」（VMD Configuration ⊂ MDM Configuration，按需最小化投影）；VMD Configuration add/edit/remove 仅作 source=MANUAL 兼容期遗留，对 source=MDM 一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留并回填 `configurationCode`（每台物理车唯一映射的核心锚点），**「版本（Variant）→配置（Configuration）」引用链与每台物理车 `configurationCode` 唯一映射不得切断**；US-019 PRODUCE 解析器七项编码 `buildConfigCode`→`configurationCode`（含历史兼容读取/映射）；US-030 service 路径 `/buildConfigCode`→`/configurationCode`、`/buildConfig/list/{variantCode}`→`/configuration/list/{variantCode}`、`/buildConfig/{buildConfigCode}`→`/configuration/{configurationCode}`（旧路径迁移期保留兼容，与 CR-016 baseModel 兼容并存）；US-031 特征-配置反查标题/路径改 Configuration、响应类型 `VmdBuildConfigResponse`→`VmdConfigurationResponse`（旧路径/旧类型兼容），强调反查逻辑仍属 VMD（基于本地投影 + 特征值映射，不强依赖 MDM 运行时）；MDM 事件订阅（F6，新增 entity=configuration）与 Bootstrap 全量同步（F7，entity=configuration\|all）复用现有机制；§2 新增 G7、Configuration 语义统一说明、N10 Configuration 非目标；§4 数据来源标记由六类实体扩展为七类（纳入 Configuration 配置投影）、新增 Configuration 投影约束与字段范围原则两节。**本 CR 不改造 BuildConfigFeatureCode/特征值归属（仅引用键兼容改名）、不改造 FeatureFamily 归属（CR-018）；具体迁移脚本、字段物理改名、Flyway 文件等实现细节留 design.md / tasks.md**。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-09 | CR-018 | Modified | **特征族（FeatureFamily）/ 特征值（FeatureCode）重构为 MDM OptionFamily（选项族）/ OptionCode（选项值）本地投影 + 命名迁移**：OptionFamily / OptionCode 主数据 SSOT 上移至 MDM，VMD 保留本地只读投影表（US-008 由「维护特征族 FeatureFamily 及其特征值 FeatureCode」改写为「消费 MDM OptionFamily / OptionCode 主数据本地投影」；新增 US-008b OptionFamily / OptionCode Bootstrap 全量同步（entity=optionFamily \| optionCode \| all）、US-008c 本地维护能力兼容清理 + Feature→Option 命名迁移）；**与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）——本次涉及实体重命名 + 关联键重命名**：MDM 侧 FeatureFamily / FeatureCode 改名为 OptionFamily / OptionCode，VMD 将 `familyCode`→`optionFamilyCode`、`featureCode`→`optionCode`，FeatureFamily / FeatureCode / `familyCode` / `featureCode` / 「特征族」「特征值」转为历史兼容命名（参照 Manufacturer→Plant、BaseModel→Variant、BuildConfig→Configuration）；**命名消歧**——OptionFamily / OptionCode（选项族 / 选项值）区别于 ConfigItem（配置项，US-009）下的「枚举值 Option」、configCenter（配置中心）、VehicleConfig（车辆配置，US-013），易混处用全称限定；新增 §4「OptionFamily / OptionCode 主数据投影约束」与「OptionFamily / OptionCode 投影字段范围原则」（VMD ⊂ MDM，按需最小化投影，含 OptionFamily 与 OptionCode 两张最小投影集，OptionCode 含 `option_family_code` 归属字段）；VMD OptionFamily / OptionCode add/edit/remove 仅作 source=MANUAL 兼容期遗留，对 source=MDM 一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留并回填 `optionFamilyCode` / `optionCode`，**特征-配置反查（US-031）能力与每台物理车 `configurationCode` 唯一映射不得切断**；**对 Variant 侧（原 BaseModelFeatureCode，CR-016）/ Configuration 侧（原 BuildConfigFeatureCode，CR-017）的特征值引用键 `feature_code` 随实体重命名兼容改名为 `option_code`，仅引用键改名、不改业务语义、不重复接管已随 Variant / Configuration 投影下发的选项值映射数据**（US-004 / US-005 / US-005c 引用键表述同步更新）；US-031 特征-配置反查标题/入参/响应改 OptionFamily-OptionCode（`familyCode`→`optionFamilyCode`、`featureCode`→`optionCode`、响应 `featureCodes`→`optionCodes`，旧入参/旧字段/旧路径兼容），强调反查逻辑仍属 VMD（基于本地投影 + 选项值映射，不强依赖 MDM 运行时）；新接口 `/api/mpt/optionFamily/v1/listAllOptionCode?optionFamilyCode`（旧路径 `/api/mpt/featureFamily/v1/listAllFeatureCode?familyCode` 迁移期保留兼容）；权限点 `completeVehicle:product:featureFamily:*` / `featureCode:*`→`completeVehicle:product:optionFamily:list/query/export` / `optionCode:list/query/export`（`add/edit/remove` 仅兼容期遗留限 source=MANUAL，旧权限点标记 `deprecated` 待后续 CR 下线）；MDM 事件订阅（F6，新增 entity=optionFamily / optionCode）与 Bootstrap 全量同步（F7，entity=optionFamily \| optionCode \| all）复用现有机制；§2 新增 G8、OptionFamily / OptionCode 语义统一说明、N11 非目标；§3.2 章节标题由「特征族 & 配置项域」改为「选项族（OptionFamily） & 配置项域」并加消歧说明；§4 数据来源标记由七类实体扩展为九类（纳入 OptionFamily / OptionCode 投影）；§5 新增 O55~O61。**本 CR 仅做引用键兼容改名，不重复接管 CR-016/CR-017 已下发的选项值映射；具体迁移脚本、字段物理改名、Flyway 文件等实现细节留 design.md / tasks.md**。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-10 | CR-019 | Modified | **供应商本地维护彻底下线（不建本地投影）**：供应商（Supplier）主数据 SSOT 已上移至 edd-mdm Party 子域（MDM CR-006），VMD 彻底下线供应商本地维护能力。**区别于产品树各实体（CR-011~CR-018）的按需最小化只读投影策略——供应商不投影**：要求移除 `Supplier` 聚合 / `SupplierRepository` / `SupplierAppService` / `SupplierRepositoryImpl` / `SupplierMapper`(+xml) / `SupplierPo` / `SupplierConverter` / `SupplierAssembler` / `MptSupplierAssembler` 及专用 DTO/VO（`SupplierCmd`/`SupplierDto`/`SupplierQuery`/`SupplierRequest`/`SupplierResponse`）、本地表 `tb_supplier`、对外 CRUD API `MptSupplierController`（`/api/mpt/supplier/v1` 的 list/export/{id}/add/edit/remove）及权限点 `completeVehicle:vehicle:supplier:{list\|export\|query\|add\|edit\|remove}`；经核查 `edd-vmd-api` 无供应商 Feign 契约（如存在则一并移除）。**保留范围（不得删除）**：零部件/设备表 `tb_part`/`tb_btm`/`tb_ccp`/`tb_idcm`/`tb_tbox` 与导入链路 `ods_vmd_*_df`/`ods_vmd_data_import_di` 的 `supplier_code` 溯源透传字段、6 类离线导入（PRODUCE/EOL/BTM/CCP/IDCM/TBOX/SIM）写入 `supplier_code` 的逻辑；数仓 `ods_vmd_supplier_mf` 属 DMP 不在本 CR。调用方迁移：需供应商主数据者改调 edd-mdm Party 子域，仅需编码者用 `supplier_code` 透传，约定下线公告/过渡窗口/灰度；**数据处置采用方案 B 直接清退**（清退前与 MDM Party 子域一致性核对，配套 Flyway 删表脚本，保留建表 DDL+备份作回滚兜底，不做阶段性只读归档）；对外 API 建议先 `@Deprecated` 过渡一版再删（与数据直接清退相互独立）。US-016 由「维护供应商（Supplier）」改写为「供应商本地维护下线」并给出 EARS 验收（已下线 API 返回明确下线响应、导入仍保留 `supplier_code`、不再保留任何供应商本地增删改查与本地表）；§2 新增 G9、Supplier 语义统一说明、N12 非目标；§4 新增「供应商本地维护下线约束（CR-019）」；§5 新增 O62~O67。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-10 | CR-020 | Modified | **设备（Device）字典 / 类型主数据重构为 MDM VehicleNode（车载节点）本地只读投影 + 命名迁移**：VehicleNode 字典 / 类型主数据 SSOT 上移至 **edd-mdm EEAD 子域**（MDM CR-007），VMD 保留 VehicleNode 本地只读投影表（US-015 由「维护设备信息 Device」改写为「消费 MDM VehicleNode 主数据本地投影」；新增 US-015b VehicleNode Bootstrap 全量同步（entity=vehicleNode \| all）、US-015c 本地维护能力兼容清理 + Device→VehicleNode 命名迁移）；**与 Plant（CR-011）/ Variant（CR-016）/ Configuration（CR-017）同构、区别于 Brand/Platform/CarLine/Model（CR-012~015 命名不变、仅投影化）——本次涉及实体重命名 + 关联键重命名**：MDM 侧 Device 改名为 VehicleNode，VMD 将设备字典实体与关联键 `deviceCode`→`vehicleNodeCode`，Device / `deviceCode` / 「设备」转为历史兼容命名（参照 Manufacturer→Plant、BaseModel→Variant、BuildConfig→Configuration、FeatureFamily→OptionFamily）；**关键边界**——本 CR 仅处理「车载节点字典 / 类型层」主数据（节点定义、类型、功能域），**VMD 自有的物理设备实例 + 绑定关系（VIN 绑定的 TBOX/IDCU/CCU/ADCU/TCU 实例，含 SN/part_number/hardware_vsn 及绑车 / 激活 / 下线 / 密钥 / 证书生命周期）不上移、不投影化、保持留在 VMD，不切断「车辆→零件→设备→生命周期」链路**，物理实例上的节点引用键由 `device_code` 兼容改名为 `vehicle_node_code`（仅改名、不改业务语义）；**命名消歧**——VehicleNode（车载节点）区别于物理设备实例（VehiclePart 绑定的具体设备，US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）；**与供应商（CR-019 彻底下线、不建投影）不同**，车载节点属「车上有什么」（EEAD）、是 VMD 车辆主数据语义核心，按产品树模式建只读投影；新增 §4「VehicleNode 主数据投影约束」与「VehicleNode 投影字段范围原则」（VMD VehicleNode ⊂ MDM VehicleNode，按需最小化投影，最小字段集 = A 治理元字段（source/external_ref_id/external_version/last_sync_time）+ B 关联键 / 标识（code（即 vehicleNodeCode）/name）+ C 业务属性（vehicle_node_type/domain/status），明确排除审批 / Golden Record / 质量打分 / 生命周期 / 负责人 / EEAD 外延 / 物理实例字段）；VMD VehicleNode add/edit/remove 仅作 source=MANUAL 兼容期遗留，对 source=MDM 一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留并回填 `vehicleNodeCode`，**物理设备实例 → 节点引用链不得切断**；US-015 service 路径 `/api/service/device/v1/{code}`+`/listAllFota`→`/api/service/vehicleNode/v1/**`（旧路径迁移期保留兼容）；US-014 零件 `deviceCode` 过滤键→`vehicleNodeCode`（含历史兼容读取）；US-021 BTM 解析器 `VehiclePart.deviceCode`→`vehicleNodeCode`（仅引用键改名）；US-030 `VmdDeviceService`→`VmdVehicleNodeService`、`DeviceExResponse`→`VehicleNodeExResponse`、路径 `/api/service/device/v1`→`/api/service/vehicleNode/v1`（旧契约 / 旧路径 / 旧响应类型迁移期保留兼容）；权限点 `completeVehicle:vehicle:device:*`（现状处于 `vehicle` 命名空间）→`completeVehicle:product:vehicleNode:list/query/export`（迁入 `product` 命名空间，与产品树各实体 CR-011~018 一致；`add/edit/remove` 仅兼容期遗留限 source=MANUAL，旧 device 权限点标记 `deprecated` 待后续 CR 下线）；MDM 事件订阅（F6，新增 entity=vehicleNode）与 Bootstrap 全量同步（F7，entity=vehicleNode \| all）复用现有机制；§1 Overview 新增 CR-020 兼容说明、§2 新增 G10、VehicleNode 语义统一说明、N13 非目标；§3.5 章节加 CR-020 消歧说明；§4 数据来源标记由九类实体扩展为十类（纳入 mdm_vehicle_node 车载节点投影）、edd-mdm 依赖纳入 EEAD 子域 VehicleNode、前置条件新增 MDM VehicleNode 就绪；§5 新增 O68~O75。<br>**受影响清单（供实现对账）**：<br>· 表：`tb_device`→`mdm_vehicle_node`（重命名 + 补 source/external_ref_id/external_version/last_sync_time + UK(external_ref_id) + 回填 source='MANUAL'，保留 code/name/node_type/func_domain/device_item/type 等列）；`tb_vehicle_part` / `tb_vehicle_part_history` / `tb_part`（`device_code`→`vehicle_node_code`，仅引用键改名 + 回填，保留旧列兼容）。<br>· 字段：`device_code`/`deviceCode`→`vehicle_node_code`/`vehicleNodeCode`（保留旧列 / 旧字段兼容并回填）。<br>· 权限点：`completeVehicle:vehicle:device:*`→`completeVehicle:product:vehicleNode:list/query/export`；`vehicleNode:add/edit/remove` 仅兼容期遗留（限 source=MANUAL）；旧 `device:*` 标记 `deprecated` 待后续 CR 下线。<br>· API path：`/api/mpt/device/**`→`/api/mpt/vehicleNode/**`（含 list/listAllDevice→listAll/listAllDeviceItem/export/{id}/add/edit/remove）；`/api/service/device/v1/{code}`+`/listAllFota`→`/api/service/vehicleNode/v1/**`（旧路径迁移期保留兼容）。<br>· Feign：`VmdDeviceService`→`VmdVehicleNodeService`、`DeviceExResponse`→`VehicleNodeExResponse`（旧契约 / 旧类型迁移期保留兼容）。<br>· Flyway：新增 `V15__Migrate_device_to_vehicle_node.sql`（表迁移 / 重命名 + 投影字段 + UK + 回填 source='MANUAL'）、`V16__Migrate_device_code_to_vehicle_node_code.sql`（vehicle_part/vehicle_part_history/part 引用键迁移回填），接续 CR-019 的 V14。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-10 | CR-021 | Modified | **零件（Part）字典 / 类型主数据重构为 MDM Part 本地只读投影（不改名、按需最小化、本期仅 P0 投影）**：Part 字典 / 类型主数据 SSOT 上移至 **edd-mdm Part 子域**，VMD 保留 Part 本地只读投影表（US-014 由「维护零件信息 Part」改写为「消费 MDM Part 主数据本地投影」；新增 US-014b Part Bootstrap 全量同步（entity=part \| all）、US-014c 本地维护能力兼容清理、US-014d 物理零件实例 + 绑定关系 + 生命周期边界声明（留 VMD））；**与 Brand/Platform/CarLine/Model（CR-012~CR-015）同构、区别于 Plant(CR-011)/Variant(CR-016)/Configuration(CR-017)/VehicleNode(CR-020) 的命名迁移——Part 实体命名不变、关联键 `partCode` 不变、不做表 / 列重命名**，Part 复用现有 `veh_part`（`tb_part`）；**关键差异：CR-010/V3 未覆盖 `tb_part`，故 CR-021 参照 Model（CR-015）需新增一条 Flyway 迁移为 `tb_part` 补齐 source/external_ref_id/external_version/last_sync_time + UK(external_ref_id) + 回填 source='MANUAL'（接续 CR-020 序列；脚本与文件名留 design.md / tasks.md）**；**双层边界**——投影层为 Part 字典 / 类型主数据（零件定义、零件类型、规格、part_number 字典等「车上应有哪些零件」），留 VMD 层为 VIN 绑定的物理零件实例 + 绑定关系 + 生命周期（SN/part_number/hardware 等实例属性、零件→设备挂载、装车/换件/下线/密钥/证书生命周期），不上移、不投影化、不切断「车辆→零件→设备→生命周期」链路；**与供应商（CR-019 彻底下线、不建投影）不同**——Part 属「车上有什么」、是车辆主数据语义核心、处于「车辆→零件→设备」链路，故按产品树 / VehicleNode 模式建只读投影；**命名消歧**——Part（零件实体 / 字典）区别于 VehicleNode（车载节点，CR-020）、物理设备实例（US-017）、ConfigItem（配置项，US-009）、configCenter（配置中心）、VehicleConfig（车辆配置，US-013）；新增 §4「Part 主数据投影约束」与「Part 投影字段范围原则」（VMD Part ⊂ MDM Part，按需最小化投影，**本期投影范围 = P0 必投字段集（`code`(partCode)/`name`/`part_type`/`vehicle_node_code`/`supplier_code`/`is_software`/`fota_upgradeable`/`is_accurately_traced`/`status`）+ 投影管理字段（source/external_ref_id/external_version/last_sync_time）**，明确不投影 P1 按需字段与 P2（MDM 内部主键 / 乐观锁、接入血缘、审计、设计 PLM / 物流 / 履历、时效区间）及物理实例字段，后续按需独立 CR 增量升投）；VMD Part add/edit/remove 仅作 source=MANUAL 兼容期遗留，对 source=MDM 一律只读（拒绝时抛 `ProductDataReadOnlyException`，错误码 `202014`）；保留并长期沿用 `partCode`，零件上的 `vehicleNodeCode`（承接原 `deviceCode`，CR-020）/ `supplier_code`（透传溯源，CR-019）一并保留，**物理零件实例 → Part 引用链不得切断**；US-014 零件查询（`GET /api/service/part/v1/{partCode}`、`listAllFota`、`/api/mpt/part/v1/list` 按 `key/pn/name/part_type/vehicleNodeCode` 过滤）数据来源变为本地投影；权限点 `completeVehicle:vehicle:part:*`（现状处于 `vehicle` 命名空间）→`completeVehicle:product:part:list/query/export`（迁入 `product` 命名空间，与产品树各实体 CR-011~020 一致；`add/edit/remove` 仅兼容期遗留限 source=MANUAL，旧 `vehicle:part` 权限点标记 `deprecated` 待后续 CR 下线）；MDM 事件订阅（F6，新增 entity=part）与 Bootstrap 全量同步（F7，entity=part \| all）复用现有机制；§1 Overview 新增 CR-021 兼容说明、§2 新增 G11、Part 语义统一说明、N14 非目标；§3.5 章节加 CR-021 消歧说明；§4 数据来源标记由十类实体扩展为十一类（纳入 veh_part（tb_part）Part 零件投影）、edd-mdm 依赖纳入 Part 子域、前置条件新增 MDM Part 就绪；§5 新增 O76~O82。<br>**受影响清单（供实现对账）**：<br>· 表：`veh_part`（`tb_part`）补齐 source/external_ref_id/external_version/last_sync_time + UK(external_ref_id) + 回填 source='MANUAL'，保留现有业务列不变、不改名。<br>· 字段：新增投影管理字段 4 列 + UK；P0 业务字段沿用既有列（`vehicle_node_code` 承接 CR-020 改名后的引用键、`supplier_code` 延续 CR-019 透传）。<br>· 权限点：`completeVehicle:vehicle:part:*`→`completeVehicle:product:part:list/query/export`；`product:part:add/edit/remove` 仅兼容期遗留（限 source=MANUAL）；旧 `vehicle:part:*` 标记 `deprecated` 待后续 CR 下线。<br>· API path：`/api/mpt/part/v1/**`、`/api/service/part/v1/**` 命名 / 路径不变，查询数据来源变为本地投影；新增 Bootstrap entity=part。<br>· Flyway：需新增一条迁移为 `tb_part` 补齐投影字段 + UK + 回填 source='MANUAL'（接续 CR-020 的 V16；文件名与脚本细节留 design.md / tasks.md）。**本 CR 仅处理 Part 字典 / 类型层投影化，不改造物理零件实例 + 绑定关系 + 生命周期（US-014d 边界声明，留 VMD）；具体迁移脚本、字段物理补齐、Flyway 文件等实现细节留 design.md / tasks.md**。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |

| 2026-06-10 | CR-022 | Modified | **车辆—零件物理实例层数据模型重构为三表**：将现「实例+绑定」混血单表 `tb_vehicle_part` 拆分为 `tb_part_info`（物理零件实例本体，唯一键 `(part_code, sn)`，允许未绑定 VIN 时独立存在=游离零件）+ `tb_vehicle_part`（纯车辆—零件绑定关系，承载装车位置 `vehicleNodeCode`/`deviceItem` 快照、时间 `bindTime`/`unbindTime`、状态 `bindState`、换件溯源 `replaceOfBindingId`）；废弃从未启用的 `tb_vehicle_part_history`（换件历史改由同一 (vin,节点位) 绑定时间线表达）。US-017 由「维护车辆零件」演进为「维护车辆—零件绑定关系」；新增 US-032（物理零件实例本体 PartInfo 与游离零件）、US-033（换件：解绑旧+绑新+溯源）、US-034（导入异步乱序绑定兜底，零件先到游离落库+按 sn 回扫补绑）、US-035（死表清退与车辆主档瘦身：删 `tb_veh_exterior/interior/wheel/optional/ecu/activation` 及遗留 `tb_mes_vehicle_data`/`tb_bom_part`/`tb_bom_part_nove`/`tr_veh_model_config_*`/`tr_veh_user_relation`/`tb_veh_user`，外饰/内饰/轮毂/选装语义由 Variant+Configuration→OptionCode 表达）、US-036（查询/RPC/权限/错误码影响）。实例属性归位准则：本体属性→`part_info`、绑定属性→`vehicle_part`、字典属性→`tb_mdm_part`/`tb_mdm_vehicle_node`（不动）；约束「同一实例/同一车同一节点位 仅一条 active 绑定」；权限点 `vehicle_part` 沿用 `completeVehicle:vehicle:vehiclePart:*`、新增 `completeVehicle:vehicle:partInfo:*`，**均留 `vehicle` 命名空间不迁 product**；错误码接续新增 `202016 PART_INSTANCE_ALREADY_EXISTS`/`202017 PART_BINDING_CONFLICT`/`202018 PART_INSTANCE_NOT_EXIST`。§2 新增 G12、物理实例层语义统一补充、N15；§3.6 重构 US-017 + 新增 US-032~036；§4 新增「PartInfo / VehiclePart 物理实例层模型约束」；§5 新增 O83~O90。**本 CR 仅处理 VMD 自有物理实例层，不动字典/类型层（CR-011~021 MDM 投影）、不切断「车辆→零件→设备→生命周期」链路；无历史数据，结构干净重建、不做数据回迁；Flyway 建议 V18 删死表 / V19 建 part_info / V20 重建 vehicle_part（接续 V17），脚本与 active 约束物理实现（MySQL 生成列/NULL 技巧）等细节留 design.md / tasks.md**。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |
| 2026-06-11 | CR-023 | Modified | **零件实例数据入站统一为两入口 + 共用入站内核**：§3.7「车辆数据导入域」重定位为「零件实例数据入站域」；新增 US-037（入口①上游系统对接，独立链路 / 异步事件为主 + 批量兜底 / 入站回执 + 错误通知 / 不硬绑 MES）、US-038（共用入站内核，合并 US-034 的两步落库与乱序兜底，六步：校验 / 标准化 / 幂等 / 去重 / 落库 / 触发事件，按 `part_type` 适配源差异）、US-039（入站异常隔离 / 重放 / 对账）；US-018 重定位为入口②（复用并挂接入站内核，不旁路）；US-020~024 收敛为入站内核来源适配器（移除 `bindOrg=MES` 与 `vehicleNodeCode=BTM_M` 硬编码）；**US-025 / US-032 反转 CR-022 O86：SIM 纳入物理实例层（`part_type=SIM` 落 `part_info`，`sn`=ICCID，IMSI/MSISDN/MNO 入 `extra`），落库后触发 TSP 连接 / 激活事件**；US-032 新增 `source`（入站来源枚举 MES/MANUAL/WMS/IQC/OTHER）/ `part_type` 快照 / 入站溯源键（`inbound_batch_no`/`source_event_id`）/ `last_inbound_time`，`vehicle_node_code` 明确为可空（车载节点对零件可选）；US-017 绑定支持无车载节点零件（安装位置以 `device_item` 表达、节点位可空）、`bind_org` 取自 `source`；US-036 新增权限点 `completeVehicle:vehicle:partInbound:{list/query/export/retry}` 与错误码 `202019 PART_INBOUND_VALIDATE_FAILED` / `202020 PART_TYPE_SCHEMA_NOT_FOUND`；§1 新增 CR-023 兼容说明、§2 新增 G13 / N16、§4 新增「零件实例数据入站约束」与「来源标记语义区分」、§5 新增 O91~O95。边界声明 MDM（字典投影只读）/ TSP（激活 / 连接回写另一条写路径）/ MES（数据源，仅定义接收契约）。**本 CR 仅改 requirements.md；design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |

| 2026-06-11 | CR-024 | New | **MDM Part 同步通道补全：Kafka 消费者 + 定时同步 + 监控告警**：当前设计文档（D13）明确 MDM 同步策略为「Kafka 事件订阅为主 + Feign 全量快照兜底」，但实现中存在三个关键缺失：① Kafka 消费者未实现（增量同步通道缺失）；② Bootstrap 同步仅启动时执行一次（无法持续同步新增数据）；③ 同步失败无监控告警（运维无法及时发现异常）。本 CR 补全这三个能力：新增 US-014e（Kafka 消费者实现，接收 MDM Part 事件并转换为本地 `MdmPartEvent`）、US-014f（定时同步，定期调用 MDM 全量快照接口作为 Kafka 事件的补充和兜底）、US-014g（监控告警，记录同步指标、失败告警、延迟告警、恢复通知）。§2 新增 G14（MDM 同步可靠性目标）；§4 新增「MDM 同步可靠性约束」；§5 新增 O96~O100。**design.md / tasks.md 需按 SPEC 工作流后续同步落地本 CR** |