# VMD-DSN-CR-070 JSON java.time 时间契约 · 字段与 mapper 清单（兼容阶段实现记录）

- 变更：VMD-DSN-CR-070（配套需求 VMD-REQ-CR-070，统一框架决策 FW-WEB-DSN-CR-001）
- 状态：**兼容阶段已实现并通过本地验证**；CI 未配置，跨服务 E2E、生产部署与消费方签收未完成；全局 ISO 切换未执行。
- 日期：2026-10-29
- 服务：iov-cloud-edd-vmd（edd-vmd-api / edd-vmd-service）

## 1. 兼容阶段策略（本 CR 已落地）

| 项 | 值 | 说明 |
|---|---|---|
| `iov.web.jackson.date-format` | `epoch-millis`（显式） | 冻结存量毫秒数字输出（旧框架 epoch serializer 行为），不输出数组/秒小数 |
| `iov.web.jackson.zone-id` | `${VMD_JACKSON_ZONE_ID:}`（main，缺省阻断）/ `Asia/Shanghai`（test） | 业务 ZoneId；生产注入值须经 P0 证据核验，禁止猜值通过探针 |
| 双读 | 始终生效（框架 `IovJavaTimeReadModule`） | ISO 字符串 + 10 位 epoch 秒 + 13 位 epoch 毫秒；非法（小数/数组/无 offset/空串/窗口外）fail-closed |
| ISO 输出 | 不切全局 | 留待消费方双读部署并签收后逐依赖边灰度（独立执行阶段） |
| 例外 | 两个事件固定 ISO（见 §3） | 既有边界例外，专用 codec，不随全局开关退回 |

## 2. 时区证据链与 P0 待办

### 2.1 本地实证（已核验）
- 本地 JVM `ZoneId.systemDefault() = Asia/Shanghai`（+08:00）。
- DB→JDBC→Java→JSON 对账链（mysql-connector-j 8.0.32，同 VMD JDBC URL 无 serverTimezone 参数）：
  - DB `tb_veh_certificate.not_before` = `2026-10-20 22:48:44`（DATETIME 墙上时间）
  - JDBC 读出 `LocalDateTime 2026-10-20T22:48:44`（Connector/J 不转换 DATETIME）
  - 旧输出 `atZone(systemDefault).toEpochMilli()` = `1792507724000`
  - 新输出 `atZone(Asia/Shanghai).toEpochMilli()` = `1792507724000` → **逐位一致**（本地 JVM=+08:00）
- 样本：`tb_veh_import_event_replay` started_at(15:01) vs create_time(07:01) 差 8h → 写入 JVM=+08:00（该库数据）。
- 仓库证据：TSP-DSN-CR-013 / EDD-VAGW-DSN-CR-018 均已显式配置 `zone-id: Asia/Shanghai`，TSP 注释记录「本地/生产 +08:00」。

### 2.2 生产证据缺口（P0 阻断项，未确认 → 不猜值）
- 生产容器 TZ：Jenkins `ServiceDockerfile` 在构建主机（仓库不可见），`docker run --env-file nacos.env` 无显式 TZ → **容器 TZ 未核验**。
- 需要部署方/运维提供：生产实例 `ZoneId.systemDefault()`、启动参数 `-Duser.timezone`、容器 `TZ` 环境；多实例一致性。
- 处置：main 配置 `zone-id: ${VMD_JACKSON_ZONE_ID:}` 缺省为空 → 注入前启动探针阻断；注入值须经上述证据 + DB→JDBC→Java→JSON 对账链核验。**配置业务 ZoneId ≠ 修改生产 JVM 默认时区**（本 CR 不改后者）。

## 3. 既有边界例外（受控专用 codec）

| Topic | 事件 | 时间字段 | 写出 | 消费方 | 状态 |
|---|---|---|---|---|---|
| vmd.business-key.changed | BusinessKeyChangedEvent | occurredAt (Instant) | 专用 ISO（`VmdEventJsonCodec`） | 仓库内未发现外部消费方（待确认运行期订阅） | 例外已登记；线上格式未核验（P0） |
| vmd.tbox-v2c-key-provisioned | TboxV2cKeyProvisionedEvent | occurredAt (Instant) | 专用 ISO（`VmdEventJsonCodec`） | TSP（`TboxV2cKeyProvisionedConsumer`，TSP-DSN-CR-013 已双读） | 例外已登记；线上格式未核验（P0） |

- 实现：`VmdEventJsonCodec`（内部 ISO mapper，**非 ObjectMapper bean**，避免 Boot `@ConditionalOnMissingBean(ObjectMapper)` 让位破坏共享装配）；只固定 occurredAt，不修改共享 mapper，不做字符串拼接；`Instant.toString()` 规范化算法不变（不改变 digest/canonicalization 输入）。
- 测试锁定：全局 epoch-millis 下这两个事件仍输出 ISO、其他受管字段保持数字；TSP 双读（ISO/10 位秒/13 位毫秒）等价；`OCCURRED_AT_FUTURE` 未来时间门禁保留（未放宽）。
- 历史 Outbox 载荷：Relay 原样补发，不重编码（`OutboxRelayPayloadPreservationTest` 锁定）。

## 4. 字段级清单

> 状态：✅=已核验（本地实证/测试） · ⚠=待核验（真实样本/线上格式） · 🚫=排除（本 CR 不触碰） · 例=既有边界例外
> 旧输出 = 兼容阶段保持的写出（epoch 毫秒数字，除非注明）

### 4.1 edd-vmd-api（官方 Feign 契约，唯一契约源）

| 类.字段 | Java 类型 | JSON 字段 | 方向 | mapper | 旧输出/单位 | 时区语义 | 消费方 | 状态 |
|---|---|---|---|---|---|---|---|---|
| SoftwareManifestRequest.occurredAt | Instant | occurredAt | 入站 | MVC/Feign（生产 ObjectMapper） | 双读（ISO/数字） | 绝对时刻 UTC | OTA（applySoftwareManifest） | ✅ |
| VehiclePartBindingExResponse.bindTime | Instant | bindTime | 出站 | 同上 | epoch 毫秒 | 绝对时刻 UTC | TSP 等 | ✅ |
| SoftwareInventoryExResponse（时间字段盘点） | Instant 等 | 见源码 | 出站 | 同上 | epoch 毫秒 | 绝对时刻 UTC | OTA | ⚠ 逐字段核验 |
| VagwAuthenticatedDeviceContext.mqttReceivedAt | long | mqttReceivedAt | 入站 | 普通 long | 原样 | 固定单位 | VAGW | 🚫 |
| VmdKeyProvisioningRequest/Response/ACK/Status（外层） | byte[]/String/long | — | 双向 | — | Proto bytes 原样 | — | VAGW | 🚫（内层 Proto/DER/签名/digest 全排除） |

### 4.2 Service/MPT/OAPI Web VO

| 类.字段 | Java 类型 | JSON 字段 | 方向 | mapper | 旧输出/单位 | 时区语义 | 消费方 | 状态 |
|---|---|---|---|---|---|---|---|---|
| VehicleLifecycleNodeResponse.reachTime | Instant | reachTime | 出站 | MVC 生产 mapper | epoch 毫秒 | 绝对时刻 UTC | MPT 前端（先双读） | ✅ |
| VehicleLifecycleResponse.reachTime | java.util.Date | reachTime | 出站 | Date 默认 | epoch 毫秒（Nacos `write-dates-as-timestamps=true`） | — | MPT 前端 | ✅ |
| VehicleLifecycleRequest.reachTime | Date | reachTime | 入站 | 同上 | 数字 | — | MPT | ✅ |
| VehiclePartResponse.bindTime/unbindTime | Date | bindTime/unbindTime | 出站 | 同上 | epoch 毫秒 | — | MPT | ✅ |
| VehiclePartRequest.bindTime/unbindTime | Date | bindTime/unbindTime | 入站 | 同上 | 数字 | — | MPT | ✅ |
| CertificateStatusResponse.notBefore/notAfter | LocalDateTime | notBefore/notAfter | 出站 | MVC 生产 mapper | epoch 毫秒（Asia/Shanghai 墙时语义） | 业务墙上时间（上海） | MPT 前端 | ✅ |
| CertificateDetailResponse.notBefore/notAfter | LocalDateTime | 同上 | 出站 | 同上 | epoch 毫秒 | 墙上时间 | MPT | ✅ |
| CertificateListResponse.notBefore/notAfter | LocalDateTime | 同上 | 出站 | 同上 | epoch 毫秒 | 墙上时间 | MPT | ✅ |
| CertificateApplyResponse.notBefore/notAfter | String | 同上 | 出站 | 字符串原样 | ISO 字符串（证书 DER 直接 toString） | 绝对时刻 | 产线调用方 | ⚠ 独立确认 |
| CertificateCompensateResponse.notBefore/notAfter | String | 同上 | 出站 | 字符串原样 | ISO 字符串 | 绝对时刻 | MPT | ⚠ |
| CertificateOperationResponse.occurredAt | LocalDateTime | occurredAt | 出站 | MVC | epoch 毫秒 | 墙上时间 | MPT | ✅ |
| BusinessKeyProvisionResponse.validFrom/validTo | LocalDateTime | validFrom/validTo | 出站 | MVC | epoch 毫秒 | 墙上时间 | 开放平台调用方 | ✅ |

### 4.3 Kafka 入站（VMD 消费）

| 事件 | 时间字段 | Java 类型 | mapper | 旧输入 | 时区语义 | 生产者 | 状态 |
|---|---|---|---|---|---|---|---|
| VehicleSoftwareInventoryObservedEvent | collectedAt/acceptedAt | Instant | 生产 ObjectMapper readValue | ISO/数字双读 | 绝对时刻 UTC | OTA（写 ISO 待签收） | ✅ |
| MdmBrandEvent 等 11 类（继承 MdmEvent） | occurredAt | LocalDateTime | 生产 ObjectMapper readValue | ISO 墙上时间/数字双读 | 业务墙上时间 | EDD-MDM | ✅ |
| VehiclePartBindingChangedEvent（消费方=TSP 投影，非 VMD） | occurredAt | Instant | — | ISO（VMD 手工 toString 写出） | 绝对时刻 UTC | VMD 自身 | ✅（已有契约） |

### 4.4 Kafka 出站（VMD 生产，Outbox→Relay / 直发）

| 事件 | Topic | 时间字段 | mapper | 当前写出 | 消费方 | 状态 |
|---|---|---|---|---|---|---|
| BusinessKeyChangedEvent | vmd.business-key.changed | occurredAt (Instant) | VmdEventJsonCodec | ISO（例外） | 待确认 | 例/⚠ |
| TboxV2cKeyProvisionedEvent | vmd.tbox-v2c-key-provisioned | occurredAt (Instant) | VmdEventJsonCodec | ISO（例外） | TSP | 例/⚠ |
| VehicleSoftwareInventoryChangedEvent | vmd.vehicle-software-inventory.changed | occurredAt (Instant) | Hutool 手工 `.toString()` | ISO 字符串 | OTA/CCS | ✅ |
| VehiclePartBindingChangedEvent | vmd.vehicle-part-binding.changed | occurredAt (Instant) | ObjectNode 手工 `.toString()` | ISO 字符串 | TSP 等 | ✅（既有） |
| VehicleProduceEventEnvelope | vmd.vehicle-produce | occurredAt/produceTime (LocalDateTime) | Hutool `JSONUtil.toJsonStr` | `{year,month,...}` 对象（旧 hutool 格式） | 待确认 | ⚠ 保留旧格式，逐事件核验 |
| PartInboundEventEnvelope | vmd.part-inbound.changed | occurredAt (LocalDateTime) | Hutool `JSONUtil.toJsonStr` | `{year,month,...}` 对象 | 待确认 | ⚠ |
| BindingFactEvent / BindingReplay | vmd.vehicle-part-binding.changed（重放） | occurredAt (Instant) | Hutool `JSONObject.set`+toString | epoch 毫秒数字 | 待确认 | ⚠ |
| 历史 Outbox/DLT 补发 | — | — | OutboxRelay | **原样字节重发，不重编码** | — | ✅（测试锁定） |

## 5. 排除面（本 CR 不触碰，golden 锁定）

- Keyprov 原始 Proto bytes、deterministic parameters、canonical/wire digest、field4 租约、固定 F/B/E/D、签名、证书 DER、operation 及保存的 Response bytes —— 全部原样（`excludedContracts_unchanged` 测试锁定 payloadBytes 往返不变）。
- 普通 long（mqttReceivedAt、TTL、aggregateVersion 等）不变。
- java.util.Date 保持 Nacos `spring.jackson.serialization.write-dates-as-timestamps=true` 语义（epoch 毫秒）。
- 幂等摘要、成功事件条件、KL1 时钟安全/租约、状态机与历史重放语义不变；不因 ISO 序列化重新映射 frozen item/Outbox payload。

## 6. 测试与验证证据（本地）

| 测试 | 覆盖 | 结果 |
|---|---|---|
| VmdJsonTimeContractIntegrationTest（12 用例） | 生产 mapper 写单一（epoch-millis golden）、双读等值、非法 fail-closed、两事件边界例外、keyprov 排除 golden、时区无关（UTC/上海/纽约）、DTO 双读（SoftwareManifestRequest/OTA observed/MdmBrandEvent/VehicleLifecycleNodeResponse）、HTTP converter（MVC 路径） | ✅ 12/12 |
| BusinessKeyChangedPublisherTest / TboxV2cKeyProvisionedPublisherTest（6 用例） | 真实 codec（非理想化 mapper）：occurredAt ISO-8601 字符串、幂等/空参数跳过 | ✅ 6/6 |
| OutboxRelayPayloadPreservationTest（2 用例） | 历史 payload 原样补发（字节不变）、readiness DOWN 暂停发布 | ✅ 2/2 |
| 全量 edd-vmd-service 回归 | 新框架 + 新配置下既有测试无破坏 | 见 §7 |

执行环境：本地 Maven（offline）、MySQL 8.0.33（docker）、Nacos 2.3.2（127.0.0.1:8848）；依赖：framework-web-starter 0.0.1-SNAPSHOT（FW-WEB-DSN-CR-001 P1，本地 .m2 2026-10-28）。

## 7. 全量回归与状态

### 7.1 全量运行结果（本地 `mvn -o test -pl edd-vmd-service`，2026-10-29）
- **Tests run: 1433，Failures: 0，Errors: 190**。
- 190 个 Error 根因 **100% 为 `PortInUseException`（10806）**：Nacos `edd-vmd.yaml` 固定 `server.port: 10806` + Spring Test 多 ApplicationContext 缓存（多个 `@SpringBootTest` 继承 `BaseTest`/不同 `@MockBean` 上下文并存）在<b>全量运行</b>时端口冲突。
- 单跑验证受影响测试类均通过：VehSecurityConstantRepositoryImplTest 3/3、MdmModelRepositoryTest 13/13、BusinessKeyCr055IntegrationTest 5/5、VehiclePartBindingKafkaProducerTest 3/3、VmdJsonTimeContractIntegrationTest 12/12。
- 0 个断言失败（Failures: 0）；本 CR 未引入任何业务/时间断言回归。
- 端口冲突为<b>预存在测试环境问题</b>（改动前旧框架探针失败同样阻断全部集成测试；与本 CR 无关），另行治理建议：测试覆盖 `server.port=0` 或 Nacos 移除固定端口。

### 7.2 环境依赖与证据级别（如实标注）
- 本地 Kafka broker（127.0.0.1:9094）缺失：Topic Provisioning Coordinator 降级重试（WARN），不阻断非 Kafka 断言测试；未执行需要真实 broker 的消息端到端验证。
- SoftwareInventoryCr046IntegrationTest 单跑失败为 `NoSuchBeanDefinitionException`（OTA observed 消费者为 CR-046 暗部署，`vmd.software-inventory.observed.kafka.enabled` 默认 false 未装配 bean）——预存在测试配置问题，与本 CR 无关。
- CI 未配置；跨服务 E2E、生产部署与消费方签收未完成。

### 7.3 状态表述
兼容阶段已实现并通过所列本地验证；CI 未配置，跨服务 E2E、生产部署与消费方签收未完成；全局 ISO 切换未执行。**不得标记为「CI 已通过」或「具备生产切换条件」。**

## 8. 发布与回滚（CR-070 §5 落实）

- 发布顺序：框架兼容版本（已备）→ VMD 双读、旧写出（本 CR）→ OTA/VAGW/TSP/CCS 及外部实际消费方双读已部署 → 逐流量池/单实例 ISO → 验证事件与 OAPI → 全量。
- 回滚：先退 VMD 输出（静态回滚 `date-format=epoch-millis` + 滚动重启），保留读取 ISO 与历史 Outbox 的能力；冻结事件 bytes 不重新生成，积压/重放窗口结束前不得降级 reader。禁止把尚存 ISO 的链路回退到仅数字 reader。
- 外部消费方无法升级时保留独立版本契约，不做强制同步（另立 CR）。
- 两个例外事件若线上核验与签收未完成：保持受控 ISO 写出 + P0 登记；若发现未知消费方仅支持数字，回退为专用数字 codec 并重新登记。
