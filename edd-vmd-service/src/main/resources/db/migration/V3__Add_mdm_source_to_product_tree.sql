-- V3__Add_mdm_source_to_product_tree.sql
-- 品牌/车系/平台新增 source / external_ref_id / external_version / last_sync_time 字段
-- 用于支持 MDM 主数据同步

-- 1. veh_brand 表新增字段
ALTER TABLE `tb_veh_brand`
ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：MDM=来自MDM系统，MANUAL=本地手动维护' AFTER `description`,
ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT 'MDM侧实体主键ID' AFTER `source`,
ADD COLUMN `external_version` BIGINT NULL COMMENT 'MDM侧实体版本号' AFTER `external_ref_id`,
ADD COLUMN `last_sync_time` DATETIME NULL COMMENT '最后一次同步时间' AFTER `external_version`;

-- 2. veh_series 表新增字段
ALTER TABLE `tb_veh_series`
ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：MDM=来自MDM系统，MANUAL=本地手动维护' AFTER `description`,
ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT 'MDM侧实体主键ID' AFTER `source`,
ADD COLUMN `external_version` BIGINT NULL COMMENT 'MDM侧实体版本号' AFTER `external_ref_id`,
ADD COLUMN `last_sync_time` DATETIME NULL COMMENT '最后一次同步时间' AFTER `external_version`;

-- 3. veh_platform 表新增字段
ALTER TABLE `tb_veh_platform`
ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：MDM=来自MDM系统，MANUAL=本地手动维护' AFTER `description`,
ADD COLUMN `external_ref_id` VARCHAR(64) NULL COMMENT 'MDM侧实体主键ID' AFTER `source`,
ADD COLUMN `external_version` BIGINT NULL COMMENT 'MDM侧实体版本号' AFTER `external_ref_id`,
ADD COLUMN `last_sync_time` DATETIME NULL COMMENT '最后一次同步时间' AFTER `external_version`;

-- 4. 添加唯一约束 UK(external_ref_id)
-- MySQL UNIQUE 允许多 NULL，source=MANUAL 时 external_ref_id=NULL 自动跳过约束
ALTER TABLE `tb_veh_brand`
ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`);

ALTER TABLE `tb_veh_series`
ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`);

ALTER TABLE `tb_veh_platform`
ADD UNIQUE KEY `uk_external_ref_id` (`external_ref_id`);

-- 5. DML 回填：现有记录 source 默认 'MANUAL'
UPDATE `tb_veh_brand` SET `source` = 'MANUAL' WHERE `source` IS NULL;
UPDATE `tb_veh_series` SET `source` = 'MANUAL' WHERE `source` IS NULL;
UPDATE `tb_veh_platform` SET `source` = 'MANUAL' WHERE `source` IS NULL;
