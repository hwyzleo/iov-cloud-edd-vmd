-- CR-043: 为安全常量表添加灌注确认列
-- 将「下发即记账」升级为「产线灌注确认对账」

-- 1. 为tb_veh_security_constant添加灌注确认列
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_veh_security_constant' 
                   AND column_name = 'provision_confirmed');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_veh_security_constant` ADD COLUMN `provision_confirmed` tinyint(1) DEFAULT 0 COMMENT ''灌注确认状态（CR-043）：0-未确认，1-已确认'' AFTER `constant_type`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_veh_security_constant' 
                   AND column_name = 'confirm_time');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_veh_security_constant` ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT ''灌注确认时间（CR-043）'' AFTER `provision_confirmed`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_veh_security_constant' 
                   AND column_name = 'confirm_source');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_veh_security_constant` ADD COLUMN `confirm_source` varchar(32) DEFAULT NULL COMMENT ''灌注确认来源（CR-043）'' AFTER `confirm_time`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 为tb_part_security_constant添加灌注确认列
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_part_security_constant' 
                   AND column_name = 'provision_confirmed');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_part_security_constant` ADD COLUMN `provision_confirmed` tinyint(1) DEFAULT 0 COMMENT ''灌注确认状态（CR-043）：0-未确认，1-已确认'' AFTER `batch_num`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_part_security_constant' 
                   AND column_name = 'confirm_time');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_part_security_constant` ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT ''灌注确认时间（CR-043）'' AFTER `provision_confirmed`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
                   WHERE table_schema = DATABASE() 
                   AND table_name = 'tb_part_security_constant' 
                   AND column_name = 'confirm_source');

SET @sql = IF(@col_exists = 0, 
              'ALTER TABLE `tb_part_security_constant` ADD COLUMN `confirm_source` varchar(32) DEFAULT NULL COMMENT ''灌注确认来源（CR-043）'' AFTER `confirm_time`',
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
