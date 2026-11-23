-- =============================================================================
-- VMD-DSN-CR-077 / 持久化迁移批次 078（R-19）：车辆首次激活事实消费
-- 形态：纯新增（expand-only），仅新建 2 张表 + 索引，不改任何既有表/列/索引，无数据回填。
-- 说明：
--   1. tb_veh_activation_node        per-VIN 首次激活节点投影（唯一键 vin）
--   2. tb_veh_activation_fact_consume 消费审计与幂等台账（eventId + (vin,bindingOperationId) 双重去重）
--   3. vin 列宽 varchar(20) 与 VMD 既有全表一致（R-12）；超长事件隔离不截断
--   4. status 枚举仅 RECORDED/DUPLICATE/QUARANTINED/FAILED（R-10 取消 CONFLICT 终态）
--   5. 不复用已被 V23 删除的 tb_veh_activation；不写 vmd_outbox / tb_veh_lifecycle
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 新增表 A：首次激活节点投影
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_veh_activation_node`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                  varchar(20)  NOT NULL COMMENT '车架号（Kafka Key；与VMD既有全表一致，超长事件隔离不截断）',
    `first_activated_at`   datetime(3)  NOT NULL COMMENT '首次激活绝对时刻（UTC，取已消费事实occurred_at最小值）',
    `binding_operation_id` varchar(64)  NOT NULL COMMENT '决定首次激活的绑定操作ID',
    `source_fact_ref`      varchar(512) NULL COMMENT '来源事实引用（不含PII）',
    `payload_digest`       varchar(64)  NOT NULL COMMENT '载荷摘要sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与eventId无关（R-10）',
    `event_id`             varchar(64)  NOT NULL COMMENT '决定该投影的事件ID',
    `occurred_at`          datetime(3)  NOT NULL COMMENT '事实发生绝对时刻（UTC）',
    `received_at`          datetime(3)  NOT NULL COMMENT 'VMD接收该事实的绝对时刻（UTC）',
    `schema_version`       int          NOT NULL DEFAULT 1 COMMENT '事件契约版本',
    `digest_source`        varchar(8)   NOT NULL DEFAULT 'EVENT' COMMENT 'EVENT=事件提供 / LOCAL=本地回算（仅观测，不影响判定）',
    `vin_resolved`         tinyint      NOT NULL DEFAULT 1 COMMENT '0=VIN未在VMD建档（仅事实投影，不建车）',
    `description`          varchar(255) NULL COMMENT '备注（如CORRECTED/DIGEST_SOURCE_MISMATCH）',
    `create_time`          datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`            varchar(64)  NULL COMMENT '创建者',
    `modify_time`          datetime(3)  NULL COMMENT '修改时间',
    `modify_by`            varchar(64)  NULL COMMENT '修改者',
    `row_version`          int          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    `row_valid`            tinyint      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_veh_activation_node_vin` (`vin`),
    KEY `idx_veh_activation_node_event` (`event_id`),
    KEY `idx_veh_activation_node_first_at` (`first_activated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆首次激活生命周期节点投影（VMD-DSN-CR-077）';

-- -----------------------------------------------------------------------------
-- 新增表 B：消费审计与幂等台账
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_veh_activation_fact_consume`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id`             varchar(64)  NOT NULL COMMENT '事件ID（幂等键1）',
    `schema_version`       int          NOT NULL DEFAULT 1 COMMENT '事件契约版本',
    `vin`                  varchar(20)  NOT NULL COMMENT '车架号（与VMD既有全表一致，R-12）',
    `binding_operation_id` varchar(64)  NOT NULL COMMENT '绑定操作ID（幂等键2）',
    `source_fact_ref`      varchar(512) NULL COMMENT '来源事实引用',
    `payload_digest`       varchar(64)  NOT NULL COMMENT '载荷摘要sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与eventId无关（R-10）',
    `digest_source`        varchar(8)   NOT NULL DEFAULT 'EVENT' COMMENT 'EVENT/LOCAL（仅观测）',
    `occurred_at`          datetime(3)  NOT NULL COMMENT '事实发生绝对时刻（UTC）',
    `received_at`          datetime(3)  NOT NULL COMMENT '接收绝对时刻（UTC）',
    `status`               varchar(16)  NOT NULL COMMENT 'RECORDED/DUPLICATE/QUARANTINED/FAILED（R-10 取消CONFLICT终态）',
    `applied`              tinyint      NOT NULL DEFAULT 0 COMMENT '是否作用于投影（0/1）',
    `retry_count`          int          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `last_error`           varchar(512) NULL COMMENT '错误/隔离原因摘要（脱敏、截断）',
    `topic`                varchar(128) NOT NULL COMMENT '来源Topic（重放定位）',
    `partition_no`         int          NOT NULL COMMENT '分区',
    `record_offset`        bigint       NOT NULL COMMENT '位点',
    `description`          varchar(255) NULL COMMENT '备注（如DIGEST_MISMATCH/EVENT_ID_BUSINESS_KEY_MISMATCH）',
    `create_time`          datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `create_by`            varchar(64)  NULL COMMENT '创建者',
    `modify_time`          datetime(3)  NULL COMMENT '修改时间',
    `modify_by`            varchar(64)  NULL COMMENT '修改者',
    `row_version`          int          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    `row_valid`            tinyint      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_veh_act_consume_event` (`event_id`),
    UNIQUE KEY `uk_veh_act_consume_business` (`vin`, `binding_operation_id`),
    KEY `idx_veh_act_consume_status` (`status`, `received_at`),
    KEY `idx_veh_act_consume_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆首次激活候选事实消费审计与幂等台账（VMD-DSN-CR-077）';
