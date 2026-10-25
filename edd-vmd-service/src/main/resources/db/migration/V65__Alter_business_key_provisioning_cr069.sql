-- CR-069（KEYPROV-LIFECYCLE-1 / KL1）：内容幂等 + 交付租约签发，替换 CR-068 整包回放语义（保留其证据）。
-- 1) tb_business_key_provisioning 新增 KL1 生命周期列：
--    * lifecycle_profile       KL1 标识（nil 表示存量未迁移）
--    * first_issued_at         F：首次完整双用途响应原子持久化时间（不可滑动；迁移仅从可信首次持久化/审计恢复）
--    * ack_deadline_at         B：固定 ACK 截止 = F + 300s（重放不延 B）
--    * immutable_content_digest 响应去掉唯一外层 field4 后的原始内容 C 的 SHA-256（续签不变）
--    * lease_seq               当前最大交付租约序号（原子递增）
--    * terminal_reason/terminal_at 终态治理原因（ACK_TIMEOUT / QUARANTINE_MIGRATION 等）与时间
--    * 乐观锁复用既有 row_version（不新增第二套版本计数）
-- 2) 新增 tb_keyprov_delivery_lease 交付租约表：
--    * provisioning_id + lease_seq 唯一；message_id 唯一（同一 messageId 不得对应不同 bytes）
--    * expire_at E=min(now+60s, B-30s)；delivery_payload_digest 每次应用投递完整 bytes 摘要
--    * content_digest 冗余不可变内容摘要（与批次 immutable_content_digest 一致，用于租约级校验）
--    * payload/payload_ref 受保护原传输字节（AES-GCM 加密 BLOB 或不可变受控对象引用，二选一）
-- 3) tb_business_key_provisioning_item 新增原响应无损切片证据（offset/length，指向最终响应序列化中
--    该 item 子消息 body；不落 wrapped_key/parameters 原文，与 V61 禁止敏感材料列一致）。
-- 只 expand 不 drop；原 response_payload/response_ref/response_digest/codec/persisted_at 保留为历史证据。

ALTER TABLE `tb_business_key_provisioning`
  ADD COLUMN `lifecycle_profile` varchar(64) DEFAULT NULL COMMENT '生命周期profile（KEYPROV-LIFECYCLE-1/KL1；NULL=存量未迁移）' AFTER `response_persisted_at`,
  ADD COLUMN `first_issued_at` datetime DEFAULT NULL COMMENT 'KL1 F：首次完整响应原子持久化时间（不可滑动）' AFTER `lifecycle_profile`,
  ADD COLUMN `ack_deadline_at` datetime DEFAULT NULL COMMENT 'KL1 B：固定ACK截止=F+300s（重放不延B）' AFTER `first_issued_at`,
  ADD COLUMN `immutable_content_digest` varchar(64) DEFAULT NULL COMMENT 'KL1：响应去掉唯一外层field4后原始内容C的SHA-256（续签不变）' AFTER `ack_deadline_at`,
  ADD COLUMN `lease_seq` int DEFAULT NULL COMMENT 'KL1：当前最大交付租约序号' AFTER `immutable_content_digest`,
  ADD COLUMN `terminal_reason` varchar(128) DEFAULT NULL COMMENT '终态治理原因（ACK_TIMEOUT/QUARANTINE_MIGRATION等；公开Status仍既有枚举）' AFTER `lease_seq`,
  ADD COLUMN `terminal_at` datetime DEFAULT NULL COMMENT '终态治理时间' AFTER `terminal_reason`,
  ADD KEY `idx_kl1_pending` (`lifecycle_profile`, `status`, `ack_deadline_at`);

CREATE TABLE IF NOT EXISTS `tb_keyprov_delivery_lease` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provisioning_id` bigint NOT NULL COMMENT '批次外键（tb_business_key_provisioning.id）',
  `lease_seq` int NOT NULL COMMENT '本批次租约序号（从1递增，原子签发）',
  `issued_at` datetime NOT NULL COMMENT '租约签发时间（服务端UTC）',
  `expire_at` datetime NOT NULL COMMENT 'KL1 E=min(now+60s,B-30s)；E-now<20s不签发',
  `message_id` varchar(128) NOT NULL COMMENT '本次应用投递新messageId（唯一；同messageId不得对应不同bytes）',
  `correlation_id` varchar(128) DEFAULT NULL COMMENT '应用投递correlationId（→Request）',
  `delivery_payload_digest` varchar(64) NOT NULL COMMENT '本次完整响应bytes的SHA-256（含本次E）',
  `content_digest` varchar(64) DEFAULT NULL COMMENT '不可变内容摘要（=批次immutable_content_digest，租约级校验）',
  `payload` longblob DEFAULT NULL COMMENT '受保护原传输bytes（AES-GCM加密BLOB；与payload_ref二选一）',
  `payload_ref` varchar(256) DEFAULT NULL COMMENT '不可变受控对象引用（与payload二选一）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本（乐观锁）',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provisioning_lease_seq` (`provisioning_id`, `lease_seq`),
  UNIQUE KEY `uk_lease_message_id` (`message_id`),
  KEY `idx_lease_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='KL1 交付租约表（内容幂等+外层租约签发；批次+租约序号唯一）';

ALTER TABLE `tb_business_key_provisioning_item`
  ADD COLUMN `item_wire_offset` int DEFAULT NULL COMMENT '原响应序列化中该item子消息body的字节偏移（无损切片证据）' AFTER `recipient_cert_digest`,
  ADD COLUMN `item_wire_length` int DEFAULT NULL COMMENT '原响应序列化中该item子消息body的字节长度（无损切片证据）' AFTER `item_wire_offset`;
