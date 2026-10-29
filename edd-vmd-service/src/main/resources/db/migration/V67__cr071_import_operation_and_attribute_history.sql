-- V67__cr071_import_operation_and_attribute_history.sql
-- CR-071: 统一导入操作（REPLAY/REIMPORT）与零件/车辆属性历史
-- 新增四张表：tb_import_operation / tb_import_operation_item / tb_part_info_change_log / tb_vehicle_info_change_log
--
-- 相对设计分册 §7.2 DDL 草案的适配说明：
-- 1) row_version 采用 INT NOT NULL DEFAULT 1，与框架 BasePo @Version 乐观锁（Integer）对齐；值域足够。
-- 2) 补充 BasePo 审计列（description/create_by/create_time/modify_by/modify_time/row_valid），
--    与项目既有 PO 继承 BasePo 的惯例一致；row_valid 承担 @TableLogic 逻辑删除。
-- 3) 仅新增不删除（expand-only）；MySQL DDL 隐式提交，不承诺同一文件事务回滚。
-- 4) 不建立物理外键；operation_id 区分大小写（ascii_bin）；摘要用 BINARY(32)。

-- 统一导入操作主表
CREATE TABLE IF NOT EXISTS `tb_import_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'UUID 操作号（区分大小写）',
  `domain` VARCHAR(16) NOT NULL COMMENT '操作域：VEHICLE/PART',
  `operation_type` VARCHAR(16) NOT NULL COMMENT '操作类型：IMPORT/REIMPORT/REPLAY/POST_PROCESS',
  `import_data_id` BIGINT NOT NULL COMMENT '来源导入数据ID（veh_import_data/part_import_data）',
  `batch_num` VARCHAR(128) NOT NULL COMMENT '来源批次号',
  `original_import_data_id` BIGINT NULL COMMENT '纠错批次引用的原始导入数据ID',
  `payload_digest` BINARY(32) NOT NULL COMMENT '原始载荷版本化规范化摘要（SHA-256）',
  `request_digest` BINARY(32) NOT NULL COMMENT '执行请求摘要（幂等复用/冲突判定）',
  `plan_digest` BINARY(32) NOT NULL COMMENT '预览计划摘要（token 校验）',
  `plan_json` JSON NOT NULL COMMENT '服务端持久化预览计划',
  `operator_id` VARCHAR(64) NOT NULL COMMENT '操作人ID（认证上下文注入）',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人姓名',
  `operation_source` VARCHAR(32) NOT NULL COMMENT '操作来源：MPT_REIMPORT/MPT_REPLAY 等',
  `reason` VARCHAR(512) NOT NULL COMMENT '操作原因（REIMPORT 必填）',
  `ticket_no` VARCHAR(128) NULL COMMENT '工单号',
  `status` VARCHAR(32) NOT NULL COMMENT '状态：PENDING/RUNNING/SUCCEEDED/PARTIAL_FAILED/FAILED/RECOVERING',
  `counters_json` JSON NULL COMMENT '阶段计数：changed/unchanged/skipped/failed',
  `legacy_replay_id` VARCHAR(64) NULL COMMENT '关联旧补发 replayId（执行明细权威为旧审计）',
  `lease_owner` VARCHAR(128) NULL COMMENT '执行器租约持有者',
  `lease_until` DATETIME(6) NULL COMMENT '租约到期时间（UTC）',
  `fence_token` BIGINT NOT NULL DEFAULT 0 COMMENT '接管 fence 令牌（单调递增）',
  `row_version` INT NOT NULL DEFAULT 1 COMMENT '记录版本',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  `created_at` DATETIME(6) NOT NULL COMMENT '创建时刻（UTC，审计用）',
  `started_at` DATETIME(6) NULL COMMENT '开始时刻',
  `finished_at` DATETIME(6) NULL COMMENT '结束时刻',
  `active_import_id` BIGINT GENERATED ALWAYS AS (
      CASE WHEN status IN ('PENDING','RUNNING','RECOVERING')
        THEN import_data_id ELSE NULL END
    ) STORED COMMENT '同 domain+import_data 互斥的 active 生成列',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_id` (`operation_id`),
  UNIQUE KEY `uk_active_import` (`domain`, `active_import_id`),
  KEY `idx_import_time` (`domain`, `import_data_id`, `created_at`, `id`),
  KEY `idx_recovery` (`status`, `lease_until`),
  KEY `idx_operator_time` (`operator_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一导入操作主表（CR-071）';

-- 统一导入操作明细表
CREATE TABLE IF NOT EXISTS `tb_import_operation_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '操作号',
  `item_key` BINARY(32) NOT NULL COMMENT '逐项幂等键：SHA-256(domain,稳定行键,entity,action,计划事实版本)',
  `candidate_key` VARCHAR(512) NOT NULL COMMENT '候选标识（如 VIN / partCode:sn）',
  `entity_type` VARCHAR(32) NOT NULL COMMENT '实体类型：VEHICLE_BASIC/PART_INSTANCE/VEHICLE_PART/SOFTWARE_INSTALLATION 等',
  `entity_id` BIGINT NULL COMMENT '实体ID',
  `vin` VARCHAR(32) NULL COMMENT 'VIN（候选标识之一）',
  `action_type` VARCHAR(64) NOT NULL COMMENT '动作类型（与既有 replay/后置动作枚举复用）',
  `phase` VARCHAR(32) NOT NULL COMMENT '阶段：PLAN/BASIC/BINDING/SOFTWARE/SECURITY/LIFECYCLE/FORWARD/EVENT',
  `status` VARCHAR(32) NOT NULL COMMENT '状态：PENDING/RUNNING/QUEUED/SUCCEEDED/NOOP/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL',
  `source` VARCHAR(32) NULL COMMENT '来源系统',
  `source_event_id` VARCHAR(128) NULL COMMENT '来源事件ID',
  `content_digest` BINARY(32) NULL COMMENT '计划事实内容摘要',
  `expected_row_version` BIGINT NULL COMMENT '执行前预期业务本体版本',
  `result_row_version` BIGINT NULL COMMENT '执行后业务本体版本',
  `changed_fields` JSON NULL COMMENT '实际变更字段差异',
  `event_id` VARCHAR(64) NULL COMMENT '预冻结事件ID（REPLAY/Outbox）',
  `outbox_id` BIGINT NULL COMMENT '关联 vmd_outbox 主键',
  `legacy_item_ref` VARCHAR(128) NULL COMMENT '旧补发逐项审计引用',
  `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
  `row_version` INT NOT NULL DEFAULT 1 COMMENT '记录版本',
  `skip_reason` VARCHAR(128) NULL COMMENT '跳过原因（BINDING_NOT_ACTIVE 等）',
  `error_code` VARCHAR(32) NULL COMMENT '错误码',
  `error_message` VARCHAR(1024) NULL COMMENT '错误信息',
  `started_at` DATETIME(6) NULL COMMENT '开始时刻',
  `finished_at` DATETIME(6) NULL COMMENT '结束时刻',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_item` (`operation_id`, `item_key`),
  KEY `idx_operation_status` (`operation_id`, `status`, `id`),
  KEY `idx_source_event` (`source`, `source_event_id`),
  KEY `idx_entity_time` (`entity_type`, `entity_id`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一导入操作明细表（CR-071）';

-- 零件实例属性变更历史表
CREATE TABLE IF NOT EXISTS `tb_part_info_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `part_id` BIGINT NOT NULL COMMENT '零件实例ID（关联tb_part_info.id）',
  `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型：INBOUND/BIND/UNBIND/ATTRIBUTE_UPDATE/SOFTWARE_SNAPSHOT/STATE_CHANGE/BASELINE_CAPTURE',
  `before_json` JSON NULL COMMENT '变更前白名单属性快照（创建为 null）',
  `after_json` JSON NOT NULL COMMENT '变更后白名单属性快照',
  `changed_fields` JSON NOT NULL COMMENT '变更字段差异',
  `source` VARCHAR(32) NOT NULL COMMENT '事实来源（MANUAL/MES/EOL/TOL/PRODUCE 等）',
  `operation_source` VARCHAR(32) NOT NULL COMMENT '操作来源（MPT_REIMPORT/MPT_REPLAY/APP 等）',
  `operator_id` VARCHAR(64) NOT NULL COMMENT '操作人ID（认证上下文注入）',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人姓名',
  `change_time` DATETIME(6) NOT NULL COMMENT '变更时刻（UTC）',
  `source_event_time` DATETIME(6) NULL COMMENT '来源事件时刻（UTC，可证明时填充）',
  `source_event_id` VARCHAR(128) NULL COMMENT '来源事件ID',
  `inbound_batch_no` VARCHAR(128) NULL COMMENT '入站批次号',
  `operation_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '操作号（普通导入为生成的操作号）',
  `mutation_key` BINARY(32) NOT NULL COMMENT '同事实变更唯一键：SHA-256(operationId,entityId,phase,变更序号)',
  `row_version` INT NOT NULL COMMENT '业务本体提交版本（tb_part_info.row_version）',
  `metadata_json` JSON NULL COMMENT '扩展元数据（如软件投影 target/slot/inventoryVersion）',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_part_version` (`part_id`, `row_version`),
  UNIQUE KEY `uk_part_mutation` (`part_id`, `mutation_key`),
  KEY `idx_part_time` (`part_id`, `change_time`, `id`),
  KEY `idx_operation` (`operation_id`),
  KEY `idx_source_event` (`source`, `source_event_id`),
  KEY `idx_operator_time` (`operator_id`, `change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='零件实例属性变更历史表（CR-071）';

-- 车辆基础信息属性变更历史表
CREATE TABLE IF NOT EXISTS `tb_vehicle_info_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vehicle_id` BIGINT NOT NULL COMMENT '车辆基础信息ID（关联tb_veh_basic_info.id）',
  `vin` VARCHAR(32) NOT NULL COMMENT '车架号',
  `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型：INBOUND/ATTRIBUTE_UPDATE/STATE_CHANGE/BASELINE_CAPTURE',
  `before_json` JSON NULL COMMENT '变更前快照（basic/detail/options 命名空间）',
  `after_json` JSON NOT NULL COMMENT '变更后快照',
  `changed_fields` JSON NOT NULL COMMENT '变更字段差异',
  `source` VARCHAR(32) NOT NULL COMMENT '事实来源',
  `operation_source` VARCHAR(32) NOT NULL COMMENT '操作来源',
  `operator_id` VARCHAR(64) NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人姓名',
  `change_time` DATETIME(6) NOT NULL COMMENT '变更时刻（UTC）',
  `source_event_time` DATETIME(6) NULL COMMENT '来源事件时刻',
  `source_event_id` VARCHAR(128) NULL COMMENT '来源事件ID',
  `operation_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '操作号',
  `mutation_key` BINARY(32) NOT NULL COMMENT '同事实变更唯一键',
  `row_version` INT NOT NULL COMMENT '业务本体提交版本（tb_veh_basic_info.row_version）',
  `metadata_json` JSON NULL COMMENT '扩展元数据',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_version` (`vehicle_id`, `row_version`),
  UNIQUE KEY `uk_vehicle_mutation` (`vehicle_id`, `mutation_key`),
  KEY `idx_vin_time` (`vin`, `change_time`, `id`),
  KEY `idx_operation` (`operation_id`),
  KEY `idx_source_event` (`source`, `source_event_id`),
  KEY `idx_operator_time` (`operator_id`, `change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆基础信息属性变更历史表（CR-071）';

-- 回滚DDL（仅供参考，不自动执行；expand-only 迁移不做降级 DROP）：
-- DROP TABLE IF EXISTS `tb_vehicle_info_change_log`;
-- DROP TABLE IF EXISTS `tb_part_info_change_log`;
-- DROP TABLE IF EXISTS `tb_import_operation_item`;
-- DROP TABLE IF EXISTS `tb_import_operation`;
