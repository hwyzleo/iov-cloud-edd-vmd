-- V4__Rename_series_to_car_line.sql
-- 将series相关表和列重命名为carLine

-- 1. 重命名表
RENAME TABLE `tb_veh_series` TO `tb_veh_car_line`;

-- 2. 重命名索引（RENAME TABLE会自动重命名表，但索引名需要手动处理）
ALTER TABLE `tb_veh_car_line` DROP INDEX `idx_series_brand_code`;
ALTER TABLE `tb_veh_car_line` ADD INDEX `idx_car_line_brand_code` (`brand_code`);

-- 3. 重命名列 - 所有包含series_code的表
ALTER TABLE `tb_veh_model` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_base_model` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_basic_info` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_build_config` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_ecu` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_exterior` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_interior` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_optional` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';
ALTER TABLE `tb_veh_wheel` CHANGE COLUMN `series_code` `car_line_code` varchar(255) NOT NULL COMMENT '车系代码';

-- 4. 更新表注释
ALTER TABLE `tb_veh_car_line` COMMENT = '车系表（原tb_veh_series）';
