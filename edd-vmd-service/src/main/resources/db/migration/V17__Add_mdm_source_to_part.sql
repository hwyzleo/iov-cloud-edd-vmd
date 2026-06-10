-- V17__Add_mdm_source_to_part.sql
-- CR-021: Part 本地投影
-- 将 tb_part 表重命名为 tb_mdm_part
-- 添加 MDM 投影字段 source / external_ref_id / external_version / last_sync_time
-- 添加唯一约束 UK(external_ref_id)，并回填历史数据 source='MANUAL'
--
-- 说明：与 VehicleNode/CR-020 的 V15 同构——Part 来自 edd-mdm Part 子域，
-- 表重命名为 tb_mdm_part 以与其他 MDM 投影表保持一致。
-- 注：Part 实体命名不变、partCode 关联键沿用 pn 列、不重命名（区别于 CR-016~CR-020 的列重命名）。

-- 1. 重命名表 tb_part -> tb_mdm_part（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_part');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_part` TO `tb_mdm_part`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源：MDM=来自MDM系统，MANUAL=本地手动维护'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM侧实体主键ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `external_version` BIGINT NULL DEFAULT 0 COMMENT ''MDM侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加唯一约束 UK(external_ref_id)（如果不存在）
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. DML 回填：现有记录 source 默认 'MANUAL'
UPDATE `tb_mdm_part` SET `source` = 'MANUAL' WHERE `source` IS NULL;

-- 5. 更新表注释
ALTER TABLE `tb_mdm_part` COMMENT = '零件（Part）字典主数据本地投影';
