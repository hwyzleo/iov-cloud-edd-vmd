-- CR-066: 修复 V2C DERIVED_ROOT logicalKeyId 全局唯一与多业务版本目录冲突。
-- 保留 uk_key_id(key_id) 全局唯一；同一逻辑材料只保留一个 canonical 目录行，
-- 失败重试/重建/换证/换件/无代际变化的轮换均复用该行；只有真实密码学代际变化
-- （rootKeyVersion/derivationVersion/anchor/purpose/bizType）才新建材料行并提升业务版本。
-- 本次仅增加辅助字段与索引（全部可 NULL / 默认值），不改变既有约束、不删数据、不扩 keyId 格式：
--   * superseded_by_id          占位/重复行指向 canonical 目录行（SUPERSEDED 行必填）
--   * logical_identity_digest   非敏感规范化身份摘要（BINARY(32)，辅助预检与审计，不取代 Provider keyId）
--   * recovery_count            同目录材料恢复次数（FAILED→PENDING_RECOVERY→PENDING 计数）
--   * last_recovery_request_id  最近恢复 attempt 审计引用
-- 禁止保存派生明文、Wrapped Key、VIN 原文或可恢复密钥材料。

ALTER TABLE `tb_device_business_key`
  ADD COLUMN `superseded_by_id` bigint DEFAULT NULL COMMENT '被取代后指向canonical目录行（SUPERSEDED行必填）' AFTER `operation_id`,
  ADD COLUMN `logical_identity_digest` binary(32) DEFAULT NULL COMMENT '非敏感规范化身份摘要（BINARY32，辅助预检与审计）' AFTER `superseded_by_id`,
  ADD COLUMN `recovery_count` int NOT NULL DEFAULT 0 COMMENT '同目录材料恢复次数（FAILED→PENDING_RECOVERY→PENDING）' AFTER `logical_identity_digest`,
  ADD COLUMN `last_recovery_request_id` varchar(128) DEFAULT NULL COMMENT '最近恢复attempt审计引用' AFTER `recovery_count`,
  ADD KEY `idx_logical_identity_digest` (`logical_identity_digest`),
  ADD KEY `idx_superseded_by_id` (`superseded_by_id`);
