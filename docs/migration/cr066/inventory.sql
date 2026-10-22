-- VMD-DSN-CR-066 §10.1：存量数据只读盘点（维护窗口执行，只读不写）
-- 目标：定位 uk_key_id 冲突 / 同逻辑材料多业务版本行 / 异常状态，输出到受控审计文件，
-- 禁止把高基数标识（keyId/operationId/VIN）打印到普通日志。
--
-- 前置：先执行 V63 迁移（增加 superseded_by_id/logical_identity_digest/recovery_count/last_recovery_request_id）。
-- 全部语句为 SELECT，可重复执行；结果只进入受控审计文件。

-- 1) 同 keyId 多行（历史约束缺失/失效导致；正常受 uk_key_id 保护应为 0）
SELECT key_id, COUNT(*) AS cnt
FROM tb_device_business_key
WHERE key_id IS NOT NULL AND row_valid = 1
GROUP BY key_id HAVING COUNT(*) > 1;

-- 2) 同上下文多业务版本但材料代际相同（root/derivation 相同 → 伪多版本行）
SELECT device_sn, business_domain, purpose,
       root_key_version, derivation_version, COUNT(*) AS cnt
FROM tb_device_business_key
WHERE key_id IS NOT NULL AND row_valid = 1
GROUP BY device_sn, business_domain, purpose, root_key_version, derivation_version
HAVING COUNT(*) > 1;

-- 3) 关键异常状态：FAILED 且已有 keyId 证据；PENDING 且 keyId 为空（占位行）
SELECT id, device_sn, business_domain, purpose, key_state, key_id, root_key_version,
       derivation_version, kms_key_ref, operation_id, request_id, create_time, modify_time
FROM tb_device_business_key
WHERE (key_state = 'FAILED' AND key_id IS NOT NULL)
   OR (key_state = 'PENDING' AND key_id IS NULL)
   AND row_valid = 1
ORDER BY modify_time DESC;

-- 4) 多 ACTIVE（唯一性异常，需人工裁决）
SELECT device_sn, business_domain, purpose, COUNT(*) AS active_cnt
FROM tb_device_business_key
WHERE key_state = 'ACTIVE' AND row_valid = 1
GROUP BY device_sn, business_domain, purpose HAVING COUNT(*) > 1;

-- 5) 批次 item 与目录引用完整性（孤立 item / 断裂引用）
SELECT i.id AS item_id, i.provisioning_id, i.purpose, i.directory_key_id
FROM tb_business_key_provisioning_item i
LEFT JOIN tb_device_business_key k ON k.id = i.directory_key_id
WHERE i.row_valid = 1 AND k.id IS NULL;

-- 说明：实际盘点须同时关联 provisioning item、requestId、operationId、KCV 与更新时间，
-- 见 CR-066 §10.1；canonical 选择规则与修复动作见 10.2/10.3 与 repair.sql。
