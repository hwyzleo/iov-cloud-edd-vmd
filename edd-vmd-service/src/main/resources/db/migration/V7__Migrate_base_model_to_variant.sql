-- V7__Migrate_base_model_to_variant.sql
-- CR-016: BaseModel→Variant 迁移
-- 将 tb_veh_base_model 表重命名为 tb_veh_variant
-- 添加 MDM 投影字段 source / external_ref_id / external_version / last_sync_time
-- 添加唯一约束 UK(external_ref_id)，并回填历史数据 source='MANUAL'

-- 1. 重命名表 tb_veh_base_model -> tb_veh_variant（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_veh_base_model');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_veh_base_model` TO `tb_veh_variant`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_variant' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_variant` ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源：MDM=来自MDM系统，MANUAL=本地手动维护'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_variant' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_variant` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM侧实体主键ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_variant' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_variant` ADD COLUMN `external_version` BIGINT NULL COMMENT ''MDM侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_variant' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_variant` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加唯一约束 UK(external_ref_id)（如果不存在）
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_variant' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_veh_variant` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 回填历史数据：现有记录 source 默认 'MANUAL'
UPDATE `tb_veh_variant` SET `source` = 'MANUAL' WHERE `source` IS NULL;

-- 5. 更新表注释
ALTER TABLE `tb_veh_variant` COMMENT = '车辆基础车型表（原tb_veh_base_model）';
