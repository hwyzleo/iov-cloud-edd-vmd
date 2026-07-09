-- VMD-DSN-CR-037: veh_security_constant UK 由 (vin) 扩为 (vin, constant_type)
-- 支撑防盗根 IMMO 与车云通信根 ROOT 并存，幂等键 (VIN, constant_type)

-- 删除旧唯一索引 uk_vin
SET @old_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_veh_security_constant'
    AND INDEX_NAME = 'uk_vin'
);

SET @sql = IF(@old_index_exists > 0,
    'ALTER TABLE tb_veh_security_constant DROP INDEX uk_vin',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新建复合唯一索引 uk_vin_constant_type
SET @new_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_veh_security_constant'
    AND INDEX_NAME = 'uk_vin_constant_type'
);

SET @sql2 = IF(@new_index_exists = 0,
    'ALTER TABLE tb_veh_security_constant ADD UNIQUE KEY uk_vin_constant_type (vin, constant_type)',
    'SELECT 1'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
