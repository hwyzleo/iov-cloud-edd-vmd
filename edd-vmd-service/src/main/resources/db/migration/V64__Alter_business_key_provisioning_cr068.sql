-- CR-068: PAR-PROTO AAD profile / 9 字段 wireEnvelopeDigest / 双摘要与 Response 字节级幂等重放。
-- 1) tb_business_key_provisioning_item：
--    * envelope_digest RENAME → core_envelope_digest（显式迁移语义：历史值即 FW-SEC core 内部完整性摘要，
--      不静默解释为 wire 值；历史 in-flight 无 wire 证据保持关闭/对账，不自动启用）
--    * 新增 wire_envelope_digest（Response/ACK/proof 权威）、aad_profile_id、parameters_digest、wrapped_key_digest
--    * item 不再承载最终 Response 事实（response 存储移至批次表，见下）
-- 2) tb_business_key_provisioning：
--    * 新增批次级 Response 字节级幂等重放字段（幂等键=requestId；一次 requestId 对应一份完整
--      KeyProvisioningResponse，两个 purpose item 仅是响应组成部分，不各自持有完整响应）
-- 禁止保存明文密钥、完整 Wrapped Key、设备私钥或可恢复密钥材料；response_payload 为 AES-GCM 加密 BLOB，
-- 密钥经 Secret 注入，不在 Nacos/日志中输出。

ALTER TABLE `tb_business_key_provisioning_item`
  RENAME COLUMN `envelope_digest` TO `core_envelope_digest`,
  ADD COLUMN `wire_envelope_digest` binary(32) DEFAULT NULL COMMENT 'PAR-PROTO 9字段wire摘要（BINARY32，Response/ACK/proof权威，非FW core）' AFTER `core_envelope_digest`,
  ADD COLUMN `aad_profile_id` varchar(64) DEFAULT NULL COMMENT 'AAD profile（固定PAR_PROTO_KEYPROV_AAD_V1）' AFTER `wire_envelope_digest`,
  ADD COLUMN `parameters_digest` binary(32) DEFAULT NULL COMMENT 'deterministic parameters字节摘要（BINARY32）' AFTER `aad_profile_id`,
  ADD COLUMN `wrapped_key_digest` binary(32) DEFAULT NULL COMMENT 'wrappedKey(ciphertext)字节摘要（BINARY32）' AFTER `parameters_digest`;

ALTER TABLE `tb_business_key_provisioning`
  ADD COLUMN `response_payload` longblob DEFAULT NULL COMMENT '最终KeyProvisioningResponse字节（AES-GCM加密BLOB，发送前持久化）' AFTER `ack_digest`,
  ADD COLUMN `response_ref` varchar(256) DEFAULT NULL COMMENT '不可变受控对象引用（与response_payload二选一）' AFTER `response_payload`,
  ADD COLUMN `response_digest` varchar(64) DEFAULT NULL COMMENT 'SHA-256(final response bytes) hex，重放完整性校验' AFTER `response_ref`,
  ADD COLUMN `response_codec_version` varchar(64) DEFAULT NULL COMMENT '响应codec/profile版本（固定PAR_PROTO_KEYPROV_AAD_V1）' AFTER `response_digest`,
  ADD COLUMN `response_persisted_at` datetime DEFAULT NULL COMMENT '首次持久化时间（重放不更新）' AFTER `response_codec_version`;
