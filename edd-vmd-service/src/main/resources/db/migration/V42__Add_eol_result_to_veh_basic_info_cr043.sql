-- CR-043: 为车辆基础信息表添加电检放行状态字段
-- eol_result: 电检放行状态，取值为PASS/NG/REWORK

-- 检查eol_result列是否存在，如果不存在则添加
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_veh_basic_info' 
                   AND column_name = 'eol_result');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_veh_basic_info` ADD COLUMN `eol_result` varchar(16) NULL COMMENT ''电检放行状态（CR-043）：PASS/NG/REWORK'' AFTER `vehicle_base_version`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
