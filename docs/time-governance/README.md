# EDD-VMD 时间治理（VMD-DSN-CR-073）G1 证据与实现

- CR：VMD-DSN-CR-073 / VMD-REQ-CR-073（2026-10-30 登记；本批实施 G1，不改生产路径）
- 实施范围（用户确认）：G1 隔离测试 + 测试专用 PO/XML + 配置候选 + 脱敏证据；不选生产表映射、不改审计属主/生产配置/服务 DDL；OQ-1～3 保持原实现并保留证据。
- 仓库基线：main `8849d022b0ccd6853f32f8baa815e84e5b7b9c91`（实施前 HEAD，干净）
- 本批新增文件清单见下方；**生产代码零改动**（唯一生产改动：`edd-vmd-service/pom.xml` surefire `<includes>` 增加 `**/*IT.java`，属测试执行器配置）。

## 1. 交付物

| 路径 | 内容 |
|---|---|
| `edd-vmd-service/src/test/java/net/hwyz/iov/cloud/edd/vmd/service/timeit/` | G1 隔离测试（见 §3） |
| `edd-vmd-service/src/test/resources/mappers-timeit/TimeItMapper.xml` | 候选 XML 映射（隔离表，不指向生产表） |
| `edd-vmd-service/scripts/time-governance/run_time_matrix.sh` | 双 JVM 矩阵脚本（失败返回非零；EXIT trap 清理隔离库） |
| `docs/time-governance/` | 本目录（账本 + 证据） |
| `edd-vmd-service/pom.xml` | surefire includes 增加 `**/*IT.java` |

## 2. 制品/环境锁定（G0 证据）

- 依赖（effective-pom / dependency:tree）：JDK 17；Spring Boot 3.0.2；MyBatis-Plus 3.5.3.1（mybatis 3.5.10）；Connector/J 8.0.32；Druid 1.2.8；Flyway 9.5.1；hutool-all 5.8.19；MapStruct 1.4.2.Final。
- 本地 SNAPSHOT 制品 SHA-256（安装来源：~/.m2，本地安装）：
  - framework-mysql-starter-0.0.1-SNAPSHOT.jar `0352cc68ffed47f6b8d329e5dbc04c0d0eaeaee7102c469c2c77f005ed14e291`
  - framework-common-0.0.1-SNAPSHOT.jar `1f21ddd9e24cbeddcffe4cd7f67c28dada6c5f939bb309ac5d76bb8c3fdaf6e6`
  - framework-web-starter-0.0.1-SNAPSHOT.jar `319a652d0ca983774aff7b7e0639c33716add507eafc784c1408e60d29a7f819`
- 测试运行环境：本机 macOS；本地 MySQL `@@system_time_zone=UTC`、`@@global/session.time_zone=SYSTEM`；测试 JVM 默认 `Asia/Shanghai`（`TimeZone.getDefault()` 实证）；显式 MySQL 地址 `-Dtime.it.mysql.host=127.0.0.1`；口令经 `VMD_IT_MYSQL_PASSWORD` 环境变量注入，仓库不含口令。

## 3. 测试清单与结果（2026-10-31 运行）

### 3.1 隔离 JUnit（`mvn test -Dtest=...`，全部 18/18 通过，隔离库用后 DROP）

| 测试 | 覆盖（CR-073 TC 映射） | 结果 |
|---|---|---|
| `TimeJdbcMatrixIT`（5 用例） | TC-TIME-02/03：候选 UTC 组绝对一致 + 精度按 (0)/(3)/(6) 声明验收；LDT 墙钟原样；默认路径条件断言（上海 JVM + 非亚洲会话复现 +8h）；TIMESTAMP 范围拒绝/边界 | 5/5 |
| `UtcInstantTypeHandlerPrecisionIT`（4 用例） | TC-TIME-03/框架能力核验：框架 handler 在 DATETIME(6) 保留微秒（**未固定毫秒，无缺口**）；(3) 毫秒；null 语义；读路径 | 4/4 |
| `TimeItXmlMappingIT`（3 用例） | TC-TIME-05 子集：XML resultMap + 参数显式 typeHandler 在 (6) 微秒往返；默认 InstantTypeHandler + 候选 UTC 连接=绝对正确（连接层修复证据）；LDT 墙钟原样 | 3/3 |
| `HutoolTimeGoldenIT`（3 用例） | TC-TIME-09 观察（不改 writer）：JSONObject LDT→epoch 毫秒按 JVM 默认时区换算（上海：`14:33:59.123`→`1793342039123`=06:33:59Z）；裸 LDT 对象形式；JSONObject 内 Instant→ISO 字符串 | 3/3 |
| `MapStructTimeConversionIT`（3 用例） | TC-TIME-05/06：**MapStruct 1.4.2 生成代码对 LDT↔Date 固定 UTC**（`Date.from(ldt.toInstant(ZoneOffset.UTC))` / `LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))`），非 systemDefault；Date 承载毫秒（LDT 微秒→Date 截断到毫秒） | 3/3 |

### 3.2 双 JVM 矩阵（`bash scripts/time-governance/run_time_matrix.sh`，exit 0）

- 流程：UTC JVM 写→UTC 自读→上海交叉读；上海 JVM 写→上海自读→UTC 交叉读；同一隔离 fixture。
- 候选验收组（UTC_CANDIDATE 连接：`connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&preserveInstants=true`）：**84 行 checked / 0 failures**——Instant/LDT-UTC墙/Date × DATETIME(6)/TIMESTAMP(6) × 8 样本，任意写/读 JVM 组合下 `readInstant==expected`（Date 按毫秒承载精度）。
- 默认路径差异（风险组，非验收）：**8 条**，全部为「上海 JVM 写默认 URL」的 +8h 偏移（`stored=2026-10-30 08:00:00.123456` / `read=2026-10-30T08:00:00.123456Z` vs `expected=2026-10-30T00:00:00.123456Z`）与 Date 毫秒截断——与 CR-073 报告复现一致；UTC JVM 写默认路径无偏移。
- 证据文件：`target/time-governance/matrix-{utc,sh}-{write,read}*.json`、`diff-default-legacy.txt`、`matrix-summary.txt`（每次运行覆盖；本目录记录要点）。

### 3.3 未执行/阻断项（如实保留）

- ProduceImport/EolImport 等真实 DB 集成测试：未执行（会对 db_vmd 执行 DML，超出 G1 隔离边界；上阶段同因）。
- 生产 JVM/服务器会话 TZ、Nacos 覆盖、历史写方、消费边签收：D，未验证。
- OQ-1/2/3（produceTime 语义、EOL_DATE 语义、软件 sourceEventTime 缺失政策）：保持原实现，未代填/未改语义（用户确认暂缓变更）。

## 4. 关键结论（本批实证）

1. **候选 UTC 连接参数形态可使 Instant/Date/LDT-UTC 全链路 JVM 无关且绝对正确**（含 (6) 微秒），无论写/读 JVM（84/84 交叉验证）。
2. **默认 URL + 上海 JVM + 服务器会话 UTC**：Instant→TIMESTAMP/DATETIME 存储偏移 +8h 可复现（条件断言成立）；UTC JVM 写默认路径无偏移——偏移风险与写方 JVM TZ 绑定。
3. **框架 `UtcInstantTypeHandler` 精度能力核验通过**：DATETIME(6) 微秒往返保留（未固定毫秒）；可安全用于 (6) 候选映射；VMD 无需复制长期 handler。
4. **LDT 墙钟无转换**：纯墙钟原样读写；(0)/(3) 位按 MySQL 小数秒 HALF_UP 舍入到声明位（精度损失为声明预期）。
5. **TIMESTAMP 范围**：1970-01-01 下限之外与 2038-01-19 03:14:07 上限之外 → 拒绝/范围限制；DATETIME 可存且读回原值。
6. **MapStruct 固定 UTC**（修正早期“默认区”推断）：领域 LDT→PO Date 按 `ZoneOffset.UTC` 解释；Date 毫秒承载（LDT 微秒截断）。
7. **Hutool writer**：JSONObject 内 LDT→epoch 毫秒按 JVM 默认时区换算（上海 JVM 输出正确绝对时刻；UTC JVM 将偏移 +8h）；JSONObject 内 Instant→ISO 字符串。writer 保持不动，本批仅固化。

## 5. 上线阻断项（G2/G3 前置，未批准）

- 生产 JVM TZ / 服务器会话 TZ / Nacos 覆盖（D）——阻断 UTC 灰度与配置切换；
- OQ-1～3 业务裁定——阻断事件时间语义修复；
- 首批表列与 owner、完整制品 hash 追溯、审计属主切换、逐表 DDL——待评审（本批不实施）；
- 消费边（OTA/TSP/VAGW/MPT/IDK）签收——阻断 ISO/writer 相关；
- 本批不启用生产探针、不修数据、不替换 writer、不切 ISO、不全局 ±8h。
