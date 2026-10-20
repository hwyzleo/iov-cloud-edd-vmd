-- VMD-DSN-CR-063: 逻辑 TBOX ECDH-ES P-256 v1 证书 Profile —— 签发后复验与 Keyprov 前置门禁
-- 1) tb_veh_certificate 增加 SHA-256(原始 leaf DER) 摘要（Keyprov 冻结/ACK 绑定，BINARY(32)）
-- 2) 增加 profile_validated_at 复验审计时间（非认证事实）
-- 3) certificate_profile 放宽 VARCHAR(96) 承载逻辑 Profile 全名（非物理 Provisioner 名）
-- 复用既有：pki_request_id / public_key_sha256 / csr_fingerprint / hsm_uid；
-- ACTIVE 唯一性沿用 uk_device_profile_active（device_sn+certificate_profile），
-- 业务幂等沿用 idx_vin_uid_spki_profile（vin+hsm_uid+public_key_sha256+certificate_profile）。

ALTER TABLE `tb_veh_certificate`
    ADD COLUMN `leaf_certificate_digest` BINARY(32) DEFAULT NULL COMMENT 'SHA-256(原始leaf DER)（CR-063复验登记，CR-062 Keyprov冻结/ACK绑定）' AFTER `certificate_fingerprint`,
    ADD COLUMN `profile_validated_at` datetime DEFAULT NULL COMMENT '逻辑Profile签发后复验通过时间（审计事实，非认证事实）' AFTER `leaf_certificate_digest`;

ALTER TABLE `tb_veh_certificate`
    MODIFY COLUMN `certificate_profile` varchar(96) NOT NULL COMMENT '受治理逻辑证书Profile（非物理Provisioner名）';
