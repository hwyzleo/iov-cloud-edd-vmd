-- CR-046: OTA 车辆软件观测消费与 swinv 退役
-- 1) part_software_installation 增列：is_active_slot / observation 跨服务审计字段
ALTER TABLE `tb_part_software_installation`
  ADD COLUMN `is_active_slot` tinyint NOT NULL DEFAULT 1 COMMENT '同一Target多Slot中当前启动槽（1=active，0=standby；与install_state分离，CR-046）' AFTER `is_confirmed`,
  ADD COLUMN `observation_key` varchar(128) DEFAULT NULL COMMENT 'OTA FULL观测幂等身份（CR-046）' AFTER `is_active_slot`,
  ADD COLUMN `canonicalization_version` int DEFAULT NULL COMMENT 'canonicalization版本（跨服务审计，CR-046）' AFTER `observation_key`,
  ADD COLUMN `canonical_digest` varchar(128) DEFAULT NULL COMMENT 'canonical摘要（跨服务审计，CR-046）' AFTER `canonicalization_version`,
  ADD COLUMN `source_accepted_at` datetime(3) DEFAULT NULL COMMENT 'IOV-OTA成功受理时间（仅链路审计，CR-046）' AFTER `canonical_digest`;

-- 2) 新增 OTA 车辆软件观测消费审计表（事件级幂等 + item 隔离 + DLQ 监控）
CREATE TABLE IF NOT EXISTS `tb_software_inventory_consume_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` varchar(64) NOT NULL COMMENT 'OTA观测事件ID（幂等键）',
  `observation_key` varchar(128) NOT NULL COMMENT 'OTA FULL观测身份（幂等键）',
  `vin_hash` varchar(64) NOT NULL COMMENT 'VIN哈希（SHA-256，脱敏审计）',
  `status` varchar(16) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/SUCCESS/PARTIAL/QUARANTINED/FAILED',
  `item_total` int NOT NULL DEFAULT 0 COMMENT '事件item总数',
  `item_applied` int NOT NULL DEFAULT 0 COMMENT '成功写入数',
  `item_ignored` int NOT NULL DEFAULT 0 COMMENT '幂等/版本gate忽略数',
  `item_quarantined` int NOT NULL DEFAULT 0 COMMENT '隔离数（无绑定/多绑定/非法item）',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `last_error_code` varchar(16) DEFAULT NULL COMMENT '最近错误码',
  `quarantine_detail` text COMMENT '隔离明细JSON（ecuId/target/slot/脱敏VIN/原因）',
  `received_at` datetime(3) NOT NULL COMMENT '接收时间',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  UNIQUE KEY `uk_observation_key` (`observation_key`),
  KEY `idx_vin_hash` (`vin_hash`),
  KEY `idx_status` (`status`),
  KEY `idx_received_at` (`received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OTA车辆软件观测消费审计表';
