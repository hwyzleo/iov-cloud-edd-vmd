# EDD-VMD 时间字段账本（CR-073 G1 归档）

- 状态：脱敏只读归档；用于 G2/G3 首批表列评审的候选依据，**不视为已批准迁移清单**。
- 统计口径：当前代码 PO 字段、迁移脚本、测试脚本分别统计（不混总数）。
- 证据等级沿用 CR-073：A=代码、B=运行、C=文档、D=待确认。

## 1. 统计

- 当前代码：50 个 PO；时间型业务字段见 §2/§3；`db_vmd` information_schema 共 176 个时间列（含审计列，B）。
- 迁移脚本：V0～V70 共 71 条已应用（db_vmd flyway_schema_history，B）；DATETIME(6) 9 列（V67）、DATETIME(3) 6 列（V47）；TIMESTAMP(0)/DATETIME(0) 大量。
- 测试脚本：无独立 SQL 时间列（复用主迁移，B）。

## 2. 业务事件/观测时刻（绝对语义候选，OQ 裁定前不得改）

| PO/字段 | 列 | Java | DDL | 来源（A/B） | 待确认 |
|---|---|---|---|---|---|
| VehBasicInfoPo.eolTime | tb_veh_basic_info.eol_time | Instant | TIMESTAMP(0) | EOL_TIME epoch 毫秒（正确）/ EOL_DATE yyyyMMdd（hutool 系统区）/ 缺失 now() | OQ-2 EOL_DATE 语义 |
| VehBasicInfoPo.pdiTime | tb_veh_basic_info.pdi_time | Instant | TIMESTAMP(0) | 白名单字段，无现成写点 | 写点 |
| VehiclePartPo.bindTime/unbindTime | tb_vehicle_part.* | Instant | TIMESTAMP(0) | Instant.now()（VehiclePartAppService L197/259/276） | 写方 TZ |
| VehLifecyclePo.reachTime | tb_veh_lifecycle.reach_time | Instant | TIMESTAMP(0) | Instant.now()（VehicleLifecycleAppService 多处）；dev 样本 reach_time 与 create_time 差 +8h（B） | 写方 TZ/历史分段 |
| PartInfoPo.firstSeenTime/lastInboundTime | tb_part_info.* | Instant | TIMESTAMP(0) | Instant.now()（PartInfoAppService L182、PartInboundAppService L300） | 写方 TZ |
| PartSoftwareInstallationPo.effectiveFrom/To/reportedAt/sourceEventTime/sourceAcceptedAt | tb_part_software_installation.* | Instant | TIMESTAMP(0) / source_accepted_at DATETIME(3) | EOL 链路 sourceEventTime/reportedAt=Instant.now()（EolDataParserV1_0 L371/372）；OTA observed collectedAt/acceptedAt | OQ-3 软件 gate |
| VehicleOptionPo.snapshotTime | tb_vehicle_option.snapshot_time | LDT | DATETIME(0) | LocalDateTime.now()（VehicleProduceDataParserV1_0 L253） | 快照语义 |
| VehicleCertificatePo.notBefore/notAfter | tb_veh_certificate.* | LDT | DATETIME(0) | X.509 绝对时刻经 systemDefault 转 LDT（CertificateProvisioningAppService L627/628） | 地区语义 |
| VehicleCertificatePo.issuedAt/confirmedAt/profileValidatedAt/lastOperationAt | tb_veh_certificate.* | LDT | DATETIME(0) | LocalDateTime.now() | 写方 TZ |
| MDM 投影 lastSyncTime（11 表） | tb_mdm_*.last_sync_time | LDT | DATETIME(0) | MDM 事件 occurredAt（Asia/Shanghai 墙钟，Nacos zone-id）或 LocalDateTime.now()（MdmSyncAppService） | 已绑定 Asia/Shanghai |

## 3. 接收/处理/审计/截止时刻（分类）

- 接收/处理：VmdOutboxPo.nextRetryTime/publishedAt（LDT，DATETIME(0)，OutboxRelay now()）；SoftwareInventoryConsumeAuditPo.receivedAt/completedAt（LDT，DATETIME(3)，observed consumer now()）；VehImportData/PartImportData 审计列。
- 创建/修改审计（BasePo：description/createBy/createTime/modifyBy/modifyTime/rowVersion/rowValid，java.util.Date）：绝大多数 mapper XML 显式 `now()`（DB 会话时区）；少数 app 显式 `#{modifyTime}`（BusinessKeyProvisioning/Item、DeviceBusinessKey、KeyProvisioningLease）；`modify_time=#{modifyTime}` 兼任乐观并发条件（ConfigItem 等）。**BasePo 编译产物无 fill/@Version/@TableLogic 注解**（A）。
- 业务截止/租约（**不属于普通行审计**，CR-073 RD-TIME-005 保留语义）：BusinessKeyProvisioning*（startedAt/acknowledgedAt/completedAt/confirmedAt/responsePersistedAt/firstIssuedAt/ackDeadlineAt/terminalAt，LDT→DATETIME(0)）；DeviceBusinessKeyPo.validFrom/validTo/decryptUntil/lastAttemptTime；KeyProvisioningLeasePo.issuedAt/expireAt；ImportOperationPo.leaseUntil（DATETIME(6)，注释 UTC）；V67 change_time/source_event_time（DATETIME(6)，注释 UTC，**PO 为 LDT**）。
- 纯日期：无 DATE/LocalDate 业务列（B）；EOL_DATE 以字符串 yyyyMMdd 在载荷中，落库前转 Instant（OQ-2）。

## 4. 精度现状（B 实证）

- DATETIME(6)（V67 9 列）：微秒往返保留（框架 handler 与候选连接均验证）；PO 类型 LDT 与 DDL“UTC”注释不匹配（A）。
- DATETIME(3)（V47 6 列）：毫秒声明精度（微秒样本截断到毫秒，MySQL HALF_UP）。
- TIMESTAMP(0)/DATETIME(0)：秒级；写 Instant 微秒丢失（声明精度损失）；TIMESTAMP 范围 1970-01-01T00:00:01Z～2038-01-19T03:14:07Z（范围外拒绝）。
- java.util.Date（BasePo 审计/MapStruct 目标）：毫秒承载（LDT 微秒→Date 截断）。

## 5. 待确认（D，阻断对应 G2/G3）

- 生产 JVM/容器 TZ、生产 MySQL 会话/服务器 TZ、生产 Nacos 覆盖；
- 各列历史写方 TZ 与时间段分段；
- OQ-1/2/3（produceTime、EOL_DATE、软件 sourceEventTime）；
- 消费边（OTA/TSP/VAGW/MPT/IDK）签收；
- 首批表列与 owner（本批未选，待 G1 结果评审）。
