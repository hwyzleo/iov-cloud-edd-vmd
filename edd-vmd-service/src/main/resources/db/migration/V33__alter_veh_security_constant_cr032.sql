-- CR-032: 收口安全常量存储分层
-- 1. 删除 cipher_blob 列
-- 2. 将 key_handle 重命名为 kms_key_ref

-- 删除 cipher_blob 列（如果存在）
-- 注意：MySQL 不支持 DROP COLUMN IF EXISTS，需要先检查列是否存在
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_veh_security_constant'
    AND COLUMN_NAME = 'cipher_blob'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE tb_veh_security_constant DROP COLUMN cipher_blob',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将 key_handle 重命名为 kms_key_ref
-- 先检查旧列是否存在
SET @old_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_veh_security_constant'
    AND COLUMN_NAME = 'key_handle'
);

SET @sql2 = IF(@old_column_exists > 0,
    'ALTER TABLE tb_veh_security_constant CHANGE COLUMN key_handle kms_key_ref VARCHAR(255) COMMENT \'KMS密钥引用（keyId/alias，仅为指针）\'',
    'SELECT 1'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
