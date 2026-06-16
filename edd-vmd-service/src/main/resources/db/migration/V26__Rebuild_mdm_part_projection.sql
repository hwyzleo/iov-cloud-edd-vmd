-- V26__Rebuild_mdm_part_projection.sql
-- 重建 tb_mdm_part 投影表为 P0 最小字段集
-- 1. 扩展列长度
-- 2. 重命名列
-- 3. 新增 is_software 列
-- 4. 删除遗留列

-- ============================================================
-- Part 1: 扩展列长度
-- ============================================================

ALTER TABLE `tb_mdm_part`
  MODIFY COLUMN `pn` varchar(64) NOT NULL COMMENT '零件号';

ALTER TABLE `tb_mdm_part`
  MODIFY COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：MDM=来自MDM系统，MANUAL=本地手动维护';

ALTER TABLE `tb_mdm_part`
  MODIFY COLUMN `external_ref_id` VARCHAR(128) NULL COMMENT 'MDM侧实体主键ID';

-- ============================================================
-- Part 2: type → part_type 重命名
-- ============================================================

ALTER TABLE `tb_mdm_part`
  CHANGE COLUMN `type` `part_type` varchar(32) DEFAULT NULL COMMENT '零件类型';

-- ============================================================
-- Part 3: 新增 is_software 列
-- ============================================================

ALTER TABLE `tb_mdm_part`
  ADD COLUMN `is_software` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软件件' AFTER `part_type`;

-- ============================================================
-- Part 4: device_code → vehicle_node_code 处理
-- V16 已在 tb_part(现 tb_mdm_part) 添加 vehicle_node_code varchar(20)
-- 此处：扩展 vehicle_node_code 长度，删除旧列 device_code
-- ============================================================

ALTER TABLE `tb_mdm_part`
  MODIFY COLUMN `vehicle_node_code` varchar(64) DEFAULT NULL COMMENT '车载节点代码';

ALTER TABLE `tb_mdm_part` DROP COLUMN `device_code`;

-- ============================================================
-- Part 5: accurately_traced → is_accurately_traced 重命名
-- ============================================================

ALTER TABLE `tb_mdm_part`
  CHANGE COLUMN `accurately_traced` `is_accurately_traced` TINYINT(1) DEFAULT 0 COMMENT '是否精准追溯';

-- ============================================================
-- Part 6: 删除遗留列
-- ============================================================

ALTER TABLE `tb_mdm_part` DROP COLUMN `name_en`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `ffa`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `digital_model`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `unit`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `frame_part`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `nature_part`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `color_area`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `nature_pn`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `regulatory_part`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `key_part`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `aftersale_part`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `standard_part_class`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `wrench_type`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `rod_type`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `head_shape`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `end_shape`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `washer`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `washer_type`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `diameter`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `length`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `pitch`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `dental_form`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `strength_grade`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `mechanical_property`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `surface_treatment`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `structure_character`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `device_form`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `designer`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `designer_dept`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `non_repair_reason`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `color_repair`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `primer_repair`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `electrophoresis_repair`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `production_code`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `spare_property`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `sale_note`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `first_production_date`;
ALTER TABLE `tb_mdm_part` DROP COLUMN `initial_model`;
