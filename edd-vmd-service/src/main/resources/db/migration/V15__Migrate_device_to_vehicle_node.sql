-- V15__Migrate_device_to_vehicle_node.sql
-- CR-020: Device→VehicleNode 投影化 + 表重命名
-- 将 tb_device 表重命名为 tb_mdm_vehicle_node
-- 添加 MDM 投影字段 source / external_ref_id / external_version / last_sync_time
-- 添加唯一约束 UK(external_ref_id)，并回填历史数据 source='MANUAL'
-- 与 V5 Plant / V7 Variant / V9 Configuration / V11 OptionFamily 同构

-- 1. 重命名表 tb_device -> tb_mdm_vehicle_node（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_device');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_device` TO `tb_mdm_vehicle_node`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_vehicle_node' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_vehicle_node` ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源: MDM/MANUAL'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_vehicle_node' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_vehicle_node` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM侧实体主键ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_vehicle_node' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_vehicle_node` ADD COLUMN `external_version` BIGINT NULL DEFAULT 0 COMMENT ''MDM侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_vehicle_node' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_vehicle_node` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加唯一约束 UK(external_ref_id)（如果不存在）
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_vehicle_node' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_vehicle_node` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 回填历史数据：现有记录 source 默认 'MANUAL'
UPDATE `tb_mdm_vehicle_node` SET `source` = 'MANUAL' WHERE `source` IS NULL;

-- 5. 更新表注释
ALTER TABLE `tb_mdm_vehicle_node` COMMENT = '车载节点（VehicleNode，原Device设备）字典主数据本地投影';
