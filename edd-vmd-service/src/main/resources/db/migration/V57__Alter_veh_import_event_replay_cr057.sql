-- VMD-DSN-CR-057: 扩展车辆导入事件补发审计为主任务（按 ImportType 路由）+ 逐项动作审计
-- 1) tb_veh_import_event_replay: event_type 改名为 import_type（语义对齐，仅本服务使用），
--    新增 requested_actions / success_count / skip_count
-- 2) 新建 tb_veh_import_event_replay_item 逐项动作审计表，UK (replay_id, action_type, aggregate_type, aggregate_id, aggregate_version)

ALTER TABLE `tb_veh_import_event_replay`
  CHANGE COLUMN `event_type` `import_type` varchar(32) NOT NULL COMMENT '导入类型（PRODUCE/TOL/EOL，动作路由依据）';

ALTER TABLE `tb_veh_import_event_replay`
  ADD COLUMN `requested_actions` varchar(500) DEFAULT NULL COMMENT '请求动作范围：逗号分隔的动作类型子集，空表示全部适用动作' AFTER `import_type`,
  ADD COLUMN `success_count` int NOT NULL DEFAULT 0 COMMENT '成功动作数（生命周期补齐/已入Outbox之外的成功动作）' AFTER `queued_count`,
  ADD COLUMN `skip_count` int NOT NULL DEFAULT 0 COMMENT '跳过动作数' AFTER `success_count`;

CREATE TABLE `tb_veh_import_event_replay_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `replay_id` varchar(64) NOT NULL COMMENT '关联补发任务ID',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型：PRODUCE_EVENT/TOL_LIFECYCLE_ENSURE/EOL_LIFECYCLE_ENSURE/BINDING_EVENT_REPLAY/SOFTWARE_INVENTORY_EVENT_REPLAY',
  `aggregate_type` varchar(32) NOT NULL COMMENT '聚合类型：VEHICLE/VEHICLE_LIFECYCLE/VEHICLE_PART/PART_SOFTWARE_INSTALLATION',
  `aggregate_id` varchar(128) NOT NULL COMMENT '聚合ID（VIN/绑定ID/零件ID）',
  `aggregate_version` bigint NOT NULL DEFAULT 0 COMMENT '聚合版本（事件序/软件清单版本/0）',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '原批次候选记录标识（如 partCode:sn，可空）',
  `event_id` varchar(64) DEFAULT NULL COMMENT '事件动作生成的事件ID',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '动作状态：PENDING/RUNNING/QUEUED/SUCCESS/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL',
  `skip_reason` varchar(255) DEFAULT NULL COMMENT '跳过原因（SKIPPED时记录）',
  `failure_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因（截断）',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `row_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `row_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '逻辑删除标志：0-已删除，1-有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_replay_item` (`replay_id`, `action_type`, `aggregate_type`, `aggregate_id`, `aggregate_version`),
  KEY `idx_replay_id_status` (`replay_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车辆导入事件补发逐项动作审计表';
