-- CR-058: 多用途 V2C 业务密钥开通批次、设备导入 ACK 明细与 CR-055 目录兼容调整
-- 业务密钥目录 ACTIVE 不再等同于“设备已完成件内导入”：
--   * tb_business_key_provisioning        开通批次（requestId 聚合 UPLINK_AEAD + DOWNLINK_AEAD）
--   * tb_business_key_provisioning_item   批次逐用途 ACK 明细（不含 Wrapped Key/parameters 原文）
-- 禁止保存明文密钥、完整 Wrapped Key、设备私钥或可恢复密钥材料。

CREATE TABLE IF NOT EXISTS `tb_business_key_provisioning` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` varchar(128) NOT NULL COMMENT 'TBOX 稳定 requestId（批次幂等键）',
  `request_digest` varchar(512) DEFAULT NULL COMMENT '规范化请求摘要，用于幂等冲突识别',
  `ack_digest` varchar(512) DEFAULT NULL COMMENT '规范化 ACK 摘要；未确认时为空',
  `device_sn` varchar(64) NOT NULL COMMENT 'TBOX 序列号',
  `binding_id` bigint DEFAULT NULL COMMENT '发起时 active vehicle_part.id 快照',
  `part_id` bigint DEFAULT NULL COMMENT '设备物理实例，关联 part_info.id',
  `hsm_uid_digest` varchar(128) DEFAULT NULL COMMENT 'HSM UID 摘要，仅一致性校验与审计',
  `certificate_sn_digest` varchar(128) DEFAULT NULL COMMENT '设备证书序列号摘要',
  `business_domain` varchar(64) NOT NULL COMMENT '业务域；首期固定 V2C_COMM',
  `status` varchar(32) NOT NULL DEFAULT 'PREPARING' COMMENT 'PREPARING/WAIT_DEVICE_ACK/COMPLETED/FAILED/RECONCILE_REQUIRED/EXPIRED',
  `open_flag` tinyint GENERATED ALWAYS AS (CASE WHEN `status` IN ('PREPARING','WAIT_DEVICE_ACK','RECONCILE_REQUIRED') THEN 1 ELSE NULL END) STORED COMMENT '未终结批次生成列（配合 uk_device_domain_open）',
  `fail_reason` varchar(512) DEFAULT NULL COMMENT '非敏感失败原因',
  `started_at` datetime DEFAULT NULL COMMENT '开通开始时间',
  `acknowledged_at` datetime DEFAULT NULL COMMENT '首次完整 ACK 时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  UNIQUE KEY `uk_device_domain_open` (`device_sn`, `business_domain`, `open_flag`),
  KEY `idx_device_domain_status` (`device_sn`, `business_domain`, `status`),
  KEY `idx_status_modify` (`status`, `modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务密钥多用途开通批次表';

CREATE TABLE IF NOT EXISTS `tb_business_key_provisioning_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provisioning_id` bigint NOT NULL COMMENT '批次外键',
  `purpose` varchar(64) NOT NULL COMMENT 'UPLINK_AEAD 或 DOWNLINK_AEAD',
  `key_id` varchar(128) DEFAULT NULL COMMENT '对应 device_business_key.key_id',
  `business_key_version` bigint DEFAULT NULL COMMENT 'VMD 业务版本',
  `directory_key_id` bigint DEFAULT NULL COMMENT 'device_business_key.id',
  `device_result` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCEEDED/FAILED/UNKNOWN',
  `import_proof_digest` varchar(128) DEFAULT NULL COMMENT '非敏感导入证明摘要，可空',
  `confirmed_at` datetime DEFAULT NULL COMMENT '单用途确认时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provisioning_purpose` (`provisioning_id`, `purpose`),
  UNIQUE KEY `uk_provisioning_key` (`provisioning_id`, `key_id`),
  KEY `idx_directory_key_id` (`directory_key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务密钥开通批次 ACK 明细表';

-- CR-055 兼容：批次级幂等统一由 tb_business_key_provisioning.request_id 承担；
-- device_business_key 的 request_id 退化为审计引用，唯一约束调整为 (request_id, purpose) 以支持多用途同 requestId。
ALTER TABLE `tb_device_business_key`
  DROP INDEX `uk_request_id`,
  ADD UNIQUE KEY `uk_request_purpose` (`request_id`, `purpose`);
