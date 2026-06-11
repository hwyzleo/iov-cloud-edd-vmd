-- V22__Add_supplier_code_and_fota_upgradeable_to_part.sql
-- CR-021: Part 投影补充 P0 业务字段
-- 为 tb_mdm_part 表添加 supplier_code 和 fota_upgradeable 字段
--
-- 说明：V17 迁移遗漏了这两个 P0 必投字段，本迁移补充添加。

-- 1. 添加 supplier_code 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'supplier_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `supplier_code` VARCHAR(255) NULL COMMENT ''供应商编码'' AFTER `device_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 fota_upgradeable 字段（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_part' AND column_name = 'fota_upgradeable');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_part` ADD COLUMN `fota_upgradeable` TINYINT(1) NULL DEFAULT 0 COMMENT ''是否支持FOTA升级'' AFTER `supplier_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
