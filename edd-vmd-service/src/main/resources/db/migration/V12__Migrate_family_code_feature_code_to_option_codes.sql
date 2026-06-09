-- V12__Migrate_family_code_feature_code_to_option_codes.sql
-- CR-018: Migrate family_code/feature_code keys to option_family_code/option_code

-- 1. veh_option_code: family_code -> option_family_code
ALTER TABLE tb_veh_option_code
    ADD COLUMN option_family_code VARCHAR(50) DEFAULT NULL COMMENT '归属选项族代码(原family_code, CR-018重命名)' AFTER code;
UPDATE tb_veh_option_code SET option_family_code = family_code WHERE option_family_code IS NULL;

-- 2. veh_base_model_feature_code: family_code -> option_family_code, feature_code -> option_code
ALTER TABLE tb_veh_base_model_feature_code
    ADD COLUMN option_family_code VARCHAR(50) DEFAULT NULL COMMENT '选项族代码(原family_code, CR-018重命名)' AFTER variant_code,
    ADD COLUMN option_code VARCHAR(2000) DEFAULT NULL COMMENT '选项值代码(原feature_code, CR-018重命名)' AFTER option_family_code;
UPDATE tb_veh_base_model_feature_code SET option_family_code = family_code, option_code = feature_code WHERE option_family_code IS NULL;

-- 3. veh_build_config_feature_code: family_code -> option_family_code, feature_code -> option_code
ALTER TABLE tb_veh_build_config_feature_code
    ADD COLUMN option_family_code VARCHAR(50) DEFAULT NULL COMMENT '选项族代码(原family_code, CR-018重命名)' AFTER configuration_code,
    ADD COLUMN option_code VARCHAR(2000) DEFAULT NULL COMMENT '选项值代码(原feature_code, CR-018重命名)' AFTER option_family_code;
UPDATE tb_veh_build_config_feature_code SET option_family_code = family_code, option_code = feature_code WHERE option_family_code IS NULL;

-- Note: old columns family_code/feature_code preserved for backward compatibility (deprecated, to be cleaned up later)
