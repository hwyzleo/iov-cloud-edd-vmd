-- V10__Migrate_build_config_code_to_configuration_code.sql
-- CR-017: 关联键 build_config_code → configuration_code 迁移/回填
-- 在相关表中新增 configuration_code 列，回填历史数据，保留旧列兼容期

-- 1. veh_basic_info 新增 configuration_code 列
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_basic_info' AND column_name = 'configuration_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_basic_info` ADD COLUMN `configuration_code` VARCHAR(64) COMMENT ''配置关联编码（承接 build_config_code 语义）'' AFTER `build_config_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 回填历史车辆 configuration_code = build_config_code
UPDATE `tb_veh_basic_info` SET `configuration_code` = `build_config_code` WHERE `build_config_code` IS NOT NULL;

-- 3. veh_build_config_feature_code 引用键迁移
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_build_config_feature_code' AND column_name = 'configuration_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_veh_build_config_feature_code` ADD COLUMN `configuration_code` VARCHAR(64) COMMENT ''配置编码（承接 build_config_code 语义）'' AFTER `build_config_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填历史数据
UPDATE `tb_veh_build_config_feature_code` SET `configuration_code` = `build_config_code` WHERE `build_config_code` IS NOT NULL;

-- 4. 旧列兼容期保留（标 deprecated，不删除）
-- build_config_code 列保留，待后续清理 CR 下线
