-- CR-061: V2C keyprov 由独立随机业务密钥改为 Transit/HSM 根派生（deriveAndWrap）。
-- 兼容列全部可 NULL，不阻断历史升级：
--   * tb_device_business_key            增 material_model / root_key_version / derivation_version / operation_id
--   * tb_business_key_provisioning_item 增 operation_id / root_key_version / derivation_version / recipient_cert_fingerprint_digest
-- 历史 material_model 保持 NULL（不猜测为 DERIVED_ROOT/RANDOM_MATERIAL）；material_model IS NULL 的历史行
-- 禁止自动轮换/重建/迁移，须走专项批次重新派生、下发、ACK 后切换。
-- 禁止保存派生明文、Wrapped Key、VIN 原文或可恢复密钥材料。

ALTER TABLE `tb_device_business_key`
  ADD COLUMN `material_model` varchar(32) DEFAULT NULL COMMENT '材料模型：DERIVED_ROOT/RANDOM_MATERIAL；历史记录保持NULL不猜测' AFTER `purpose`,
  ADD COLUMN `root_key_version` bigint DEFAULT NULL COMMENT 'Transit/HSM根版本（仅根派生型；历史可空）' AFTER `kms_key_version`,
  ADD COLUMN `derivation_version` varchar(32) DEFAULT NULL COMMENT '派生格式版本（首期v1；历史可空）' AFTER `root_key_version`,
  ADD COLUMN `operation_id` varchar(64) DEFAULT NULL COMMENT '根派生操作标识（对账主键，不透明）' AFTER `request_id`,
  ADD KEY `idx_material_model` (`material_model`),
  ADD KEY `idx_operation_id` (`operation_id`);

ALTER TABLE `tb_business_key_provisioning_item`
  ADD COLUMN `operation_id` varchar(64) DEFAULT NULL COMMENT '根派生操作标识（对账主键，不透明）' AFTER `directory_key_id`,
  ADD COLUMN `root_key_version` bigint DEFAULT NULL COMMENT 'Transit/HSM根版本（根派生型）' AFTER `operation_id`,
  ADD COLUMN `derivation_version` varchar(32) DEFAULT NULL COMMENT '派生格式版本（首期v1）' AFTER `root_key_version`,
  ADD COLUMN `recipient_cert_fingerprint_digest` varchar(128) DEFAULT NULL COMMENT '收件设备证书指纹摘要（非证书本体）' AFTER `derivation_version`;