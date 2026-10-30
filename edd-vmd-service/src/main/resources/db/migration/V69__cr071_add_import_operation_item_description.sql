-- V69__cr071_add_import_operation_item_description.sql
-- CR-071 修复：V67 建表时三张新表（tb_import_operation_item / tb_part_info_change_log /
-- tb_vehicle_info_change_log）遗漏 BasePo 的 description 审计列（主表 tb_import_operation 有），
-- 导致 MyBatis-Plus 全字段 SELECT 报 Unknown column 'description'。
-- 列位置与主表一致（row_version 之后、审计列之前）。

ALTER TABLE `tb_import_operation_item`
  ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_version`;

ALTER TABLE `tb_part_info_change_log`
  ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_version`;

ALTER TABLE `tb_vehicle_info_change_log`
  ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT '备注' AFTER `row_version`;
