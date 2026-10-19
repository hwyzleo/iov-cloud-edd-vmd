-- CR-062: VMD 消费 PAR-PROTO CR-008 每用途 Import Attestation 与 FW-SEC CR-016 ImportProofVerifier。
-- 批次明细新增/明确以下非敏感元数据，用于 ACK 全字段绑定与对账：
--   * envelope_format      信封格式（首期 ECDH_ES_P256_V1）
--   * wrapping_suite       封装套件（allowlist）
--   * envelope_digest      FW-SEC CR-014 canonical envelope 摘要（32B，不可日志化）
--   * key_check_value      KCV（3B，不可作认证依据）
--   * recipient_cert_digest 收件证书策略摘要（32B，与 operation 摘要绑定）
-- 禁止新增 wrapped_key、parameters、canonical_aad 或 import_proof 持久化列。
-- operation_id 作为原 operation 对账主键，建立唯一索引；历史 NULL 行不受影响。

ALTER TABLE `tb_business_key_provisioning_item`
  ADD COLUMN `envelope_format` varchar(64) DEFAULT NULL COMMENT '信封格式（首期ECDH_ES_P256_V1）' AFTER `recipient_cert_fingerprint_digest`,
  ADD COLUMN `wrapping_suite` varchar(96) DEFAULT NULL COMMENT '封装套件（allowlist）' AFTER `envelope_format`,
  ADD COLUMN `envelope_digest` binary(32) DEFAULT NULL COMMENT 'canonical envelope摘要（32B，不可日志化）' AFTER `wrapping_suite`,
  ADD COLUMN `key_check_value` binary(3) DEFAULT NULL COMMENT '密钥校验值KCV（3B，不可作认证依据）' AFTER `envelope_digest`,
  ADD COLUMN `recipient_cert_digest` binary(32) DEFAULT NULL COMMENT '收件证书策略摘要（32B）' AFTER `key_check_value`,
  ADD UNIQUE KEY `uk_provisioning_item_operation_id` (`operation_id`);
