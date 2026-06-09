-- V11__Migrate_feature_family_code_to_option.sql
-- CR-018: FeatureFamily/FeatureCode -> OptionFamily/OptionCode projection + table rename
-- Isomorphic with V4 (Plant), V7 (Variant), V9 (Configuration)

-- 1. Rename tables
RENAME TABLE tb_veh_feature_family TO tb_veh_option_family;
RENAME TABLE tb_veh_feature_code TO tb_veh_option_code;

-- 2. Add MDM projection fields to veh_option_family
ALTER TABLE tb_veh_option_family
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源: MDM / MANUAL' AFTER sort,
    ADD COLUMN external_ref_id VARCHAR(64) DEFAULT NULL COMMENT 'MDM实体ID' AFTER source,
    ADD COLUMN external_version BIGINT DEFAULT NULL COMMENT 'MDM版本号' AFTER external_ref_id,
    ADD COLUMN last_sync_time DATETIME DEFAULT NULL COMMENT '最近同步时间' AFTER external_version,
    ADD UNIQUE KEY uk_external_ref_id (external_ref_id);

-- 3. Add MDM projection fields to veh_option_code
ALTER TABLE tb_veh_option_code
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源: MDM / MANUAL' AFTER sort,
    ADD COLUMN external_ref_id VARCHAR(64) DEFAULT NULL COMMENT 'MDM实体ID' AFTER source,
    ADD COLUMN external_version BIGINT DEFAULT NULL COMMENT 'MDM版本号' AFTER external_ref_id,
    ADD COLUMN last_sync_time DATETIME DEFAULT NULL COMMENT '最近同步时间' AFTER external_version,
    ADD UNIQUE KEY uk_external_ref_id (external_ref_id);

-- 4. Update table comments
ALTER TABLE tb_veh_option_family COMMENT = '车辆选项族表(原特征族表, CR-018重命名)';
ALTER TABLE tb_veh_option_code COMMENT = '车辆选项值表(原特征值表, CR-018重命名)';
