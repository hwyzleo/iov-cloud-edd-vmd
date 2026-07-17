-- VMD-DSN-CR-039: 新建车辆导入成功事件补发审计表
-- 与 veh_import_data 分离，支撑车辆导入成功事件人工补发功能
CREATE TABLE `tb_veh_import_event_replay` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `replay_id` varchar(64) NOT NULL COMMENT '补发请求ID（幂等键）',
  `veh_import_data_id` bigint NOT NULL COMMENT '关联的车辆导入数据ID',
  `batch_num` varchar(64) NOT NULL COMMENT '原批次号',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型（本轮仅PRODUCE）',
  `operator_id` varchar(64) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `reason` varchar(500) DEFAULT NULL COMMENT '补发原因',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/PARTIAL_FAILED/FAILED',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总记录数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `failure_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `failure_detail` text DEFAULT NULL COMMENT '失败详情（JSON，受限长度）',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `row_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `row_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '逻辑删除标志：0-已删除，1-有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_replay_id` (`replay_id`),
  KEY `idx_veh_import_data_id` (`veh_import_data_id`),
  KEY `idx_batch_num` (`batch_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车辆导入成功事件补发审计表';
