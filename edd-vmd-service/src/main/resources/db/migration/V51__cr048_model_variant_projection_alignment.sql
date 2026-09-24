-- V51__cr048_model_variant_projection_alignment.sql
-- CR-048: 收敛 Model/Variant 投影 schema 与字段映射（RD-048-1/2）
-- 目标业务字段（对齐 MDM mdm_model / mdm_variant 契约）：
--   tb_mdm_model:   code / name / name_local / car_line_code / platform_code / description / source / external_ref_id / external_version / last_sync_time + 审计
--   tb_mdm_variant: code / name / name_local / model_code / description / source / external_ref_id / external_version / last_sync_time + 审计
-- 删除旧字段：
--   Model:   enable / sort / name_en
--   Variant: platform_code / car_line_code / enable / sort / name_en
-- 有效性由事件与 row_valid 表达；Variant 平台/车系沿 Model 派生，不再保存重复事实（RD-048-1）。
-- Phase 顺序：扩展（name_local / description 容量 / 产品树索引）→ 核对回填（name_en → name_local）→ 收缩（删旧列）

-- ============================================================
-- Phase 1: 扩展
-- ============================================================

-- 1.1 tb_mdm_model 新增 name_local（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND column_name = 'name_local');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_model` ADD COLUMN `name_local` VARCHAR(128) NULL COMMENT ''车型本地化名称'' AFTER `name`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 tb_mdm_variant 新增 name_local（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'name_local');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_variant` ADD COLUMN `name_local` VARCHAR(128) NULL COMMENT ''版本本地化名称'' AFTER `name`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.3 description 容量对齐 MDM 512 字符（RD-048：与 CR-047 同口径）
SET @desc_len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND column_name = 'description');
SET @sql = IF(@desc_len IS NULL OR @desc_len < 512, 'ALTER TABLE `tb_mdm_model` MODIFY COLUMN `description` VARCHAR(512) NULL COMMENT ''备注''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @desc_len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'description');
SET @sql = IF(@desc_len IS NULL OR @desc_len < 512, 'ALTER TABLE `tb_mdm_variant` MODIFY COLUMN `description` VARCHAR(512) NULL COMMENT ''备注''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.4 产品树查询索引（RD-048-4：Variant 平台/车系过滤经 JOIN Model 完成，过滤与分页在数据库侧执行）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND index_name = 'idx_mdm_model_tree');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_model` ADD KEY `idx_mdm_model_tree` (`platform_code`, `car_line_code`, `row_valid`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND index_name = 'idx_mdm_variant_model');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_variant` ADD KEY `idx_mdm_variant_model` (`model_code`, `row_valid`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Phase 2: 核对与回填
-- ============================================================

-- 2.1 存量 name_en 回填 name_local（仅当 name_local 为空且 name_en 非空；保留历史数据，不丢值）
--     语义核对：VMD 历史 name_en 一直作为名称备用/本地化列使用，回填后删除源列；
--     差异仅告警输出，不得反写 MDM。
UPDATE `tb_mdm_model` SET `name_local` = `name_en` WHERE `name_local` IS NULL AND `name_en` IS NOT NULL;
UPDATE `tb_mdm_variant` SET `name_local` = `name_en` WHERE `name_local` IS NULL AND `name_en` IS NOT NULL;

-- 2.2 Variant 存量平台/车系冗余字段与 Model 推导值差异仅核对告警，不反写（本迁移以注释承载，执行期无输出）

-- ============================================================
-- Phase 3: 收缩（删除旧字段）
-- ============================================================

-- 3.1 tb_mdm_model 删除 enable / sort / name_en
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND column_name = 'enable');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_model` DROP COLUMN `enable`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND column_name = 'sort');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_model` DROP COLUMN `sort`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_model' AND column_name = 'name_en');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_model` DROP COLUMN `name_en`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 tb_mdm_variant 删除 platform_code / car_line_code / enable / sort / name_en
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'platform_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_variant` DROP COLUMN `platform_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'car_line_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_variant` DROP COLUMN `car_line_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'enable');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_variant` DROP COLUMN `enable`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'sort');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_variant` DROP COLUMN `sort`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_variant' AND column_name = 'name_en');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_variant` DROP COLUMN `name_en`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新表注释
ALTER TABLE `tb_mdm_model` COMMENT = '车型主数据本地投影（CR-048：name_local，无 enable/sort/name_en；有效性由事件与 row_valid 表达）';
ALTER TABLE `tb_mdm_variant` COMMENT = '版本主数据本地投影（CR-048：仅 model_code 直接父引用，平台/车系沿 Model 派生）';
