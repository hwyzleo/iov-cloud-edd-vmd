-- V25__Align_option_projection_columns_with_mdm.sql
-- 对齐 tb_mdm_option_family / tb_mdm_option_code 投影列与 MDM 原始表结构
-- 1. 扩展列长度以匹配 MDM 原始表
-- 2. name_en → name_local 重命名
-- 3. 删除 MDM 不需要的遗留列

-- ============================================================
-- Part 1: tb_mdm_option_family 列长度扩展
-- ============================================================

ALTER TABLE `tb_mdm_option_family`
  MODIFY COLUMN `code` varchar(64) NOT NULL COMMENT '选项族代码';

ALTER TABLE `tb_mdm_option_family`
  MODIFY COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源: MDM / MANUAL';

ALTER TABLE `tb_mdm_option_family`
  MODIFY COLUMN `external_ref_id` VARCHAR(128) DEFAULT NULL COMMENT 'MDM实体ID';

ALTER TABLE `tb_mdm_option_family`
  MODIFY COLUMN `description` varchar(512) DEFAULT NULL COMMENT '备注';

-- ============================================================
-- Part 2: tb_mdm_option_family name_en → name_local
-- ============================================================

ALTER TABLE `tb_mdm_option_family`
  CHANGE COLUMN `name_en` `name_local` varchar(255) DEFAULT NULL COMMENT '选项族本地名称';

-- ============================================================
-- Part 3: tb_mdm_option_family 删除遗留列
-- ============================================================

ALTER TABLE `tb_mdm_option_family` DROP COLUMN `mandatory`;
ALTER TABLE `tb_mdm_option_family` DROP COLUMN `enable`;
ALTER TABLE `tb_mdm_option_family` DROP COLUMN `sort`;

-- ============================================================
-- Part 4: tb_mdm_option_code 列长度扩展
-- ============================================================

ALTER TABLE `tb_mdm_option_code`
  MODIFY COLUMN `code` varchar(64) NOT NULL COMMENT '选项值代码';

ALTER TABLE `tb_mdm_option_code`
  MODIFY COLUMN `option_family_code` varchar(64) NOT NULL COMMENT '归属选项族代码';

ALTER TABLE `tb_mdm_option_code`
  MODIFY COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源: MDM / MANUAL';

ALTER TABLE `tb_mdm_option_code`
  MODIFY COLUMN `external_ref_id` VARCHAR(128) DEFAULT NULL COMMENT 'MDM实体ID';

ALTER TABLE `tb_mdm_option_code`
  MODIFY COLUMN `description` varchar(512) DEFAULT NULL COMMENT '备注';

-- ============================================================
-- Part 5: tb_mdm_option_code name_en → name_local
-- ============================================================

ALTER TABLE `tb_mdm_option_code`
  CHANGE COLUMN `name_en` `name_local` varchar(255) DEFAULT NULL COMMENT '选项值本地名称';

-- ============================================================
-- Part 6: tb_mdm_option_code 删除遗留列
-- ============================================================

ALTER TABLE `tb_mdm_option_code` DROP COLUMN `val`;
ALTER TABLE `tb_mdm_option_code` DROP COLUMN `enable`;
ALTER TABLE `tb_mdm_option_code` DROP COLUMN `sort`;

-- 清理 V12 废弃的 family_code 旧列（已被 option_family_code 替代）
ALTER TABLE `tb_mdm_option_code` DROP COLUMN `family_code`;
