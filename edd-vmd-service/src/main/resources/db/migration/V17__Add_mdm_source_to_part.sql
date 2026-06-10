-- V17__Add_mdm_source_to_part.sql
-- CR-021: Part 本地投影
-- 为 tb_part 新增 MDM 投影字段 source / external_ref_id / external_version / last_sync_time
-- 添加唯一约束 UK(external_ref_id)，并回填历史数据 source='MANUAL'
--
-- 说明：V3__Add_mdm_source_to_product_tree.sql 仅覆盖 veh_brand / veh_series / veh_platform，
-- 未覆盖 part，故 CR-021 必须新增本迁移（与 Model/CR-015 的 V6 同构）。
-- 注：Part 实体命名不变、partCode 关联键沿用 pn 列、不重命名（区别于 CR-016~CR-020 的表/列重命名）。

-- 1. 添加 MDM 投影字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND column_name = 'source');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_part` ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''数据来源：MDM=来自MDM系统，MANUAL=本地手动维护'' AFTER `description`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND column_name = 'external_ref_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_part` ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT ''MDM侧实体主键ID'' AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND column_name = 'external_version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_part` ADD COLUMN `external_version` BIGINT NULL DEFAULT 0 COMMENT ''MDM侧实体版本号'' AFTER `external_ref_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND column_name = 'last_sync_time');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_part` ADD COLUMN `last_sync_time` DATETIME NULL COMMENT ''最后一次同步时间'' AFTER `external_version`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加唯一约束 UK(external_ref_id)（如果不存在）
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND index_name = 'uk_external_ref_id');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_part` ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. DML 回填：现有记录 source 默认 'MANUAL'
UPDATE `tb_part` SET `source` = 'MANUAL' WHERE `source` IS NULL;
