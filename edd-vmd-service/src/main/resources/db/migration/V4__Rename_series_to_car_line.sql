-- V4__Rename_series_to_car_line.sql
-- 将series相关表和列重命名为carLine

-- 1. 重命名表（如果已重命名则跳过）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_veh_series');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE `tb_veh_series` TO `tb_veh_car_line`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 重命名索引（如果索引存在则处理）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_car_line' AND index_name = 'idx_series_brand_code');
SET @sql = IF(@idx_exists > 0, 'ALTER TABLE `tb_veh_car_line` DROP INDEX `idx_series_brand_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_veh_car_line' AND index_name = 'idx_car_line_brand_code');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_veh_car_line` ADD INDEX `idx_car_line_brand_code` (`brand_code`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 重命名列 - 所有包含series_code的表（表不存在则创建，列已改则跳过）

-- tb_veh_model
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_model' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_model` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_base_model
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_base_model' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_base_model` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_basic_info
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_basic_info' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_basic_info` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_build_config
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_build_config' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_build_config` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_ecu（表不存在则创建，列已改则跳过）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tb_veh_ecu');
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_ecu' AND column_name = 'series_code');
SET @sql = IF(@table_exists = 0, 'CREATE TABLE IF NOT EXISTS `tb_veh_ecu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键'',
  `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码'',
  `code` varchar(255) NOT NULL COMMENT ''ECU代码'',
  `name` varchar(255) NOT NULL COMMENT ''ECU名称'',
  `name_en` varchar(255) DEFAULT NULL COMMENT ''ECU英文名称'',
  `func_domain` varchar(255) NOT NULL COMMENT ''功能域'',
  `node_type` varchar(255) NOT NULL COMMENT ''节点类型'',
  `diag_eth` varchar(255) DEFAULT NULL COMMENT ''诊断口'',
  `burn_eth` varchar(255) DEFAULT NULL COMMENT ''刷写口'',
  `ota` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否支持OTA'',
  `config_word` tinyint NOT NULL DEFAULT ''0'' COMMENT ''是否需要读写配置字'',
  `sort` int NOT NULL DEFAULT ''0'' COMMENT ''排序'',
  `description` varchar(255) DEFAULT NULL COMMENT ''描述'',
  `create_by` bigint DEFAULT NULL COMMENT ''创建者'',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
  `modify_by` bigint DEFAULT NULL COMMENT ''更新者'',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
  `row_version` int NOT NULL DEFAULT ''1'' COMMENT ''版本号'',
  `valid` tinyint NOT NULL DEFAULT ''1'' COMMENT ''是否有效'',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT=''车辆ECU表''', IF(@col_exists > 0, 'ALTER TABLE `tb_veh_ecu` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_exterior
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_exterior' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_exterior` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_interior
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_interior' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_interior` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_optional
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_optional' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_optional` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tb_veh_wheel
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_veh_wheel' AND column_name = 'series_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_veh_wheel` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT ''车系代码''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 更新表注释
ALTER TABLE `tb_veh_car_line` COMMENT = '车系表（原tb_veh_series）';
