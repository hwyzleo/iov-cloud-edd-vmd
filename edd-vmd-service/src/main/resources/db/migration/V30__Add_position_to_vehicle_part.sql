-- V30__Add_position_to_vehicle_part.sql
-- CR-029: 为 vehicle_part 表添加 position 字段
-- 用于存储零件安装位置（来自 TOL 导入数据的 INSTALL_POSITION）

ALTER TABLE `tb_vehicle_part`
ADD COLUMN `position` VARCHAR(50) DEFAULT NULL COMMENT '安装位置' AFTER `device_item`;
