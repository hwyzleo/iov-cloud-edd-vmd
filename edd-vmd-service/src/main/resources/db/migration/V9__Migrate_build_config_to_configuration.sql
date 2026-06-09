-- V9__Migrate_build_config_to_configuration.sql
-- CR-017: BuildConfig → Configuration 投影化 + 表重命名
-- 将 tb_veh_build_config 表重命名为 tb_veh_configuration
-- 添加 MDM 投影字段 source / external_ref_id / external_version / last_sync_time
-- 添加唯一约束 UK(external_ref_id)，并回填历史数据 source='MANUAL'
-- 与 V7 Variant / V5 Plant 同构

-- 1. 重命名表 tb_veh_build_config -> tb_veh_configuration（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_veh_build_config');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_veh_build_config` TO `tb_veh_configuration`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_configuration' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_configuration` ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源: MDM/MANUAL'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_configuration' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_configuration` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM 侧实体主键 ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_configuration' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_configuration` ADD COLUMN `external_version` BIGINT NULL DEFAULT 0 COMMENT ''MDM 侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_configuration' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_configuration` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加唯一约束 UK(external_ref_id)（如果不存在）
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_configuration' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_veh_configuration` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 回填历史数据：现有记录 source 默认 'MANUAL'
UPDATE `tb_veh_configuration` SET `source` = 'MANUAL' WHERE `source` IS NULL;

-- 5. 更新表注释
ALTER TABLE `tb_veh_configuration` COMMENT = '配置（Configuration，原 BuildConfig 生产配置）主数据本地投影';
