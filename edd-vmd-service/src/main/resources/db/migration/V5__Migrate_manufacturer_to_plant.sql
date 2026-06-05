-- V5__Migrate_manufacturer_to_plant.sql
-- CR-011: Manufacturer→Plant 迁移
-- 将 tb_veh_manufacturer 表重命名为 tb_veh_plant，列 code/name 重命名为 plant_code/plant_name
-- 添加 MDM 投影字段，为 veh_basic_info 添加 plant_code 列并回填历史数据

-- 1. 重命名表 tb_veh_manufacturer -> tb_veh_plant
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_veh_manufacturer');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_veh_manufacturer` TO `tb_veh_plant`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 重命名列 code -> plant_code
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_plant` CHANGE COLUMN `code` `plant_code` varchar(255) NOT NULL COMMENT ''工厂代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 重命名列 name -> plant_name
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'name');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_plant` CHANGE COLUMN `name` `plant_name` varchar(255) NOT NULL COMMENT ''工厂名称''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_plant` ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源：MDM=来自MDM系统，MANUAL=本地手动维护'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_plant` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM侧实体主键ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_plant` ADD COLUMN `external_version` BIGINT NULL COMMENT ''MDM侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_plant` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 添加唯一约束 UK(external_ref_id)（如果不存在）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_veh_plant` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 更新唯一约束名称（如果存在旧的 code 唯一约束）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND index_name = 'code');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tb_veh_plant` DROP INDEX `code`, ADD UNIQUE KEY `uk_plant_code` (`plant_code`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. 更新表注释
ALTER TABLE `tb_veh_plant` COMMENT = '车辆生产工厂表（原tb_veh_manufacturer）';

-- 8. 在 tb_veh_basic_info 中添加 plant_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_basic_info' AND column_name = 'plant_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_basic_info` ADD COLUMN `plant_code` varchar(255) NULL COMMENT ''生产工厂代码'' AFTER `manufacturer_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 9. 回填历史数据：将 manufacturer_code 的值复制到 plant_code
UPDATE `tb_veh_basic_info` SET `plant_code` = `manufacturer_code` WHERE `plant_code` IS NULL;

-- 10. 更新 tb_veh_manufacturer 表的 source 字段默认值（如果存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_plant' AND column_name = 'source');
SET @sql = IF(@col_exists > 0, 'UPDATE `tb_veh_plant` SET `source` = ''MANUAL'' WHERE `source` IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;