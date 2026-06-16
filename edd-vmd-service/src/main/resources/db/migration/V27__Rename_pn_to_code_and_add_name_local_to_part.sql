-- V27__Rename_pn_to_code_and_add_name_local_to_part.sql
-- 零件投影表：pn → code（对齐 MDM mdm_material_part.code）
-- 新增 name_local（对齐 MDM mdm_material_part.name_local，归 P0）

-- 1. pn → code 重命名
ALTER TABLE `tb_mdm_part` CHANGE COLUMN `pn` `code` VARCHAR(64) NOT NULL COMMENT '零件号（对齐MDM code）';

-- 2. 更新唯一约束名
ALTER TABLE `tb_mdm_part` DROP INDEX `pn`, ADD UNIQUE KEY `uk_code` (`code`);

-- 3. 新增 name_local
ALTER TABLE `tb_mdm_part` ADD COLUMN `name_local` VARCHAR(128) NULL DEFAULT NULL COMMENT '本地化名称' AFTER `name`;

-- 4. 更新 part_info 表注释中对 pn 的引用
ALTER TABLE `tb_part_info` MODIFY COLUMN `part_code` VARCHAR(20) NOT NULL COMMENT '零件编码（关联tb_mdm_part.code）';
