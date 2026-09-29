-- CR-054: 证书身份校验口径修正（US-053/US-060）
-- 1) tb_veh_certificate 增加权威 HSM UID 与规范化 SPKI 快照（签发时登记，供幂等/换钥/审计）
-- 2) 业务幂等键改为 vin + hsm_uid + public_key_sha256 + certificate_profile（CR-015 §5）
-- 3) 修正 device_sn 列注释：其为物理实例序列号（定位绑定/安装确认），不再是证书 CN 期望值

ALTER TABLE `tb_veh_certificate`
    ADD COLUMN `hsm_uid` varchar(128) DEFAULT NULL COMMENT '签发时权威HSM UID快照（CSR/证书 Subject CN，CR-054）' AFTER `device_sn`,
    ADD COLUMN `public_key_sha256` varchar(128) DEFAULT NULL COMMENT '规范化SubjectPublicKeyInfo SHA-256（幂等/换钥判定，CR-054）' AFTER `hsm_uid`,
    ADD KEY `idx_vin_uid_spki_profile` (`vin`, `hsm_uid`, `certificate_profile`, `public_key_sha256`);

ALTER TABLE `tb_veh_certificate`
    MODIFY COLUMN `device_sn` varchar(64) NOT NULL COMMENT 'TBOX物理实例序列号（定位active绑定/安装确认，非证书CN期望值）';
