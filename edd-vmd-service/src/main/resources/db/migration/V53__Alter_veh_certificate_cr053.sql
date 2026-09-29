-- CR-053: MPT 设备证书申请补偿（US-060）
-- 1) tb_veh_certificate 增加人工补偿审计字段（source_system 已有；乐观锁复用 row_version，不新增 version 列）
-- 2) 新增 tb_veh_certificate_operation 人工操作审计表（不参与证书状态权威判定）

ALTER TABLE `tb_veh_certificate`
    ADD COLUMN `original_request_id` varchar(64) DEFAULT NULL COMMENT 'MES原请求号（人工补偿关联，MPT_COMPENSATION 来源可空）' AFTER `source_system`,
    ADD COLUMN `compensation_reason` varchar(512) DEFAULT NULL COMMENT '人工补偿原因（MPT 写操作必填，不保存 CSR 全文）' AFTER `line_code`,
    ADD COLUMN `ticket_no` varchar(64) DEFAULT NULL COMMENT '关联工单号' AFTER `compensation_reason`,
    ADD COLUMN `last_operator` varchar(64) DEFAULT NULL COMMENT '最后人工操作人' AFTER `ticket_no`,
    ADD COLUMN `last_operation_at` datetime DEFAULT NULL COMMENT '最后人工操作时间' AFTER `last_operator`,
    ADD KEY `idx_original_request_id` (`original_request_id`);

CREATE TABLE IF NOT EXISTS `tb_veh_certificate_operation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_id` varchar(64) NOT NULL COMMENT '操作记录ID（幂等键）',
  `request_id` varchar(64) NOT NULL COMMENT '关联证书申请 request_id（→tb_veh_certificate.request_id）',
  `action` varchar(32) NOT NULL COMMENT '操作类型：COMPENSATE / RECONCILE / CONFIRM_INSTALLED',
  `operator_id` varchar(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `reason` varchar(512) DEFAULT NULL COMMENT '人工原因',
  `ticket_no` varchar(64) DEFAULT NULL COMMENT '关联工单号',
  `original_request_id` varchar(64) DEFAULT NULL COMMENT 'MES原请求号',
  `before_status` varchar(32) DEFAULT NULL COMMENT '操作前证书状态',
  `after_status` varchar(32) DEFAULT NULL COMMENT '操作后证书状态',
  `request_digest` varchar(512) DEFAULT NULL COMMENT '规范化请求摘要 + CSR SHA-256 指纹（不含 CSR 全文/证书本体/凭据）',
  `result` varchar(32) DEFAULT NULL COMMENT '操作结果：SUCCESS / IDEMPOTENT_HIT / CONFLICT / FAILED / PENDING',
  `error_code` varchar(8) DEFAULT NULL COMMENT '失败错误码（806xxx）',
  `source_ip` varchar(64) DEFAULT NULL COMMENT '来源IP',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '终端 User-Agent',
  `occurred_at` datetime NOT NULL COMMENT '操作发生时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_id` (`operation_id`),
  KEY `idx_request_occurred` (`request_id`, `occurred_at`),
  KEY `idx_operator_occurred` (`operator_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书人工补偿操作审计表';
