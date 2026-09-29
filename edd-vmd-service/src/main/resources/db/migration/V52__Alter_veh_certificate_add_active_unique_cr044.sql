-- CR-044 补漏：落实 §3.1 tb_veh_certificate「同一 device_sn+certificate_profile 最多一条 ACTIVE（生成列 / 部分唯一索引）」
-- 修复点：重签/换钥后旧证书并行有效（生命周期缺口，无吊销时以本地状态机兜底）

-- 1) 清理存量重复 ACTIVE：同 (device_sn, certificate_profile) 保留最新一条，其余置 SUPERSEDED
UPDATE tb_veh_certificate t
JOIN (
    SELECT device_sn, certificate_profile, MAX(id) AS keep_id
    FROM tb_veh_certificate
    WHERE cert_status = 'ACTIVE' AND row_valid = 1
    GROUP BY device_sn, certificate_profile
    HAVING COUNT(*) > 1
) d ON t.device_sn = d.device_sn
   AND t.certificate_profile = d.certificate_profile
   AND t.cert_status = 'ACTIVE'
   AND t.row_valid = 1
   AND t.id <> d.keep_id
SET t.cert_status = 'SUPERSEDED',
    t.row_version = t.row_version + 1,
    t.modify_time = now();

-- 2) 生成列 + 部分唯一索引：仅 ACTIVE 行生成非 NULL 键，其余为 NULL（MySQL 唯一索引允许多 NULL）
ALTER TABLE tb_veh_certificate
    ADD COLUMN active_uk varchar(192) GENERATED ALWAYS AS (
        CASE WHEN cert_status = 'ACTIVE' THEN CONCAT(device_sn, '|', certificate_profile) ELSE NULL END
    ) STORED COMMENT 'ACTIVE部分唯一键（生成列，同一设备同Profile最多一条ACTIVE）',
    ADD UNIQUE KEY uk_device_profile_active (active_uk);
