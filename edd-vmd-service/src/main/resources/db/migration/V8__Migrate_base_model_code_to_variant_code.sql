-- V8__Migrate_base_model_code_to_variant_code.sql
-- CR-016: base_model_code→variant_code 外键迁移
-- 为 tb_veh_basic_info / tb_veh_build_config / tb_veh_base_model_feature_code 新增 variant_code 列
-- 回填历史数据 variant_code = base_model_code
-- 旧列 base_model_code 保留，过渡期兼容

-- 1. tb_veh_basic_info: 添加 variant_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_basic_info' AND column_name = 'variant_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_basic_info` ADD COLUMN `variant_code` varchar(255) NULL COMMENT ''基础车型代码（原base_model_code）'' AFTER `model_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. tb_veh_basic_info: 回填历史数据
UPDATE `tb_veh_basic_info` SET `variant_code` = `base_model_code` WHERE `variant_code` IS NULL;

-- 3. tb_veh_build_config: 添加 variant_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_build_config' AND column_name = 'variant_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_build_config` ADD COLUMN `variant_code` varchar(255) NULL COMMENT ''基础车型代码（原base_model_code）'' AFTER `model_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. tb_veh_build_config: 回填历史数据
UPDATE `tb_veh_build_config` SET `variant_code` = `base_model_code` WHERE `variant_code` IS NULL;

-- 5. tb_veh_base_model_feature_code: 添加 variant_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_base_model_feature_code' AND column_name = 'variant_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_base_model_feature_code` ADD COLUMN `variant_code` varchar(255) NULL COMMENT ''基础车型代码（原base_model_code）'' AFTER `id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. tb_veh_base_model_feature_code: 回填历史数据
UPDATE `tb_veh_base_model_feature_code` SET `variant_code` = `base_model_code` WHERE `variant_code` IS NULL;
