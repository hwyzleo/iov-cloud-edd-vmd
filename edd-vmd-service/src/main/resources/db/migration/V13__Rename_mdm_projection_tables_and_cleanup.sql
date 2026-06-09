-- V13__Rename_mdm_projection_tables_and_cleanup.sql
-- CR-019: MDM投影表统一前缀 tb_veh_ → tb_mdm_ + 关联表重命名 + 废弃列清理
-- 所有有 MDM 投影字段(source/external_ref_id/external_version/last_sync_time)的表统一使用 tb_mdm_ 前缀

-- ============================================================
-- Part 1: 主数据投影表重命名 tb_veh_ → tb_mdm_
-- ============================================================

-- 1.1 品牌表
RENAME TABLE `tb_veh_brand` TO `tb_mdm_brand`;

-- 1.2 平台表
RENAME TABLE `tb_veh_platform` TO `tb_mdm_platform`;

-- 1.3 车系表
RENAME TABLE `tb_veh_car_line` TO `tb_mdm_car_line`;

-- 1.4 车型表
RENAME TABLE `tb_veh_model` TO `tb_mdm_model`;

-- 1.5 版本表（原tb_veh_base_model → tb_veh_variant → tb_mdm_variant）
RENAME TABLE `tb_veh_variant` TO `tb_mdm_variant`;

-- 1.6 配置表（原tb_veh_build_config → tb_veh_configuration → tb_mdm_configuration）
RENAME TABLE `tb_veh_configuration` TO `tb_mdm_configuration`;

-- 1.7 工厂表（原tb_veh_manufacturer → tb_veh_plant → tb_mdm_plant）
RENAME TABLE `tb_veh_plant` TO `tb_mdm_plant`;

-- 1.8 选项族表（原tb_veh_feature_family → tb_veh_option_family → tb_mdm_option_family）
RENAME TABLE `tb_veh_option_family` TO `tb_mdm_option_family`;

-- 1.9 选项值表（原tb_veh_feature_code → tb_veh_option_code → tb_mdm_option_code）
RENAME TABLE `tb_veh_option_code` TO `tb_mdm_option_code`;

-- ============================================================
-- Part 2: 关联表重命名（与主表名对齐）
-- ============================================================

-- 2.1 配置选项关系表（原tb_veh_build_config_feature_code → tb_mdm_configuration_option_code）
RENAME TABLE `tb_veh_build_config_feature_code` TO `tb_mdm_configuration_option_code`;

-- 2.2 版本选项关系表（原tb_veh_base_model_feature_code → tb_mdm_variant_option_code）
RENAME TABLE `tb_veh_base_model_feature_code` TO `tb_mdm_variant_option_code`;

-- ============================================================
-- Part 3: 列重命名（关联表中的旧列名 → 新列名）
-- ============================================================

-- 3.1 tb_mdm_configuration_option_code: build_config_code → configuration_code
-- V10已添加configuration_code列并回填，此处删除旧列
ALTER TABLE `tb_mdm_configuration_option_code` DROP COLUMN `build_config_code`;

-- 3.2 tb_mdm_variant_option_code: base_model_code → variant_code
-- V8已添加variant_code列并回填，此处删除旧列
ALTER TABLE `tb_mdm_variant_option_code` DROP COLUMN `base_model_code`;

-- ============================================================
-- Part 4: 废弃列清理（tb_veh_basic_info）
-- ============================================================

-- 4.1 删除 manufacturer_code（已被 plant_code 替代，V5已添加并回填）
ALTER TABLE `tb_veh_basic_info` DROP COLUMN `manufacturer_code`;

-- 4.2 删除 base_model_code（已被 variant_code 替代，V8已添加并回填）
ALTER TABLE `tb_veh_basic_info` DROP COLUMN `base_model_code`;

-- 4.3 删除 build_config_code（已被 configuration_code 替代，V10已添加并回填）
ALTER TABLE `tb_veh_basic_info` DROP COLUMN `build_config_code`;

-- ============================================================
-- Part 5: 废弃列清理（tb_mdm_configuration）
-- ============================================================

-- 5.1 删除 base_model_code（已被 variant_code 替代，V8已添加并回填）
ALTER TABLE `tb_mdm_configuration` DROP COLUMN `base_model_code`;

-- ============================================================
-- Part 6: 更新表注释
-- ============================================================

ALTER TABLE `tb_mdm_brand` COMMENT = '品牌主数据本地投影（原tb_veh_brand）';
ALTER TABLE `tb_mdm_platform` COMMENT = '平台主数据本地投影（原tb_veh_platform）';
ALTER TABLE `tb_mdm_car_line` COMMENT = '车系主数据本地投影（原tb_veh_series→tb_veh_car_line）';
ALTER TABLE `tb_mdm_model` COMMENT = '车型主数据本地投影（原tb_veh_model）';
ALTER TABLE `tb_mdm_variant` COMMENT = '版本主数据本地投影（原tb_veh_base_model→tb_veh_variant）';
ALTER TABLE `tb_mdm_configuration` COMMENT = '配置主数据本地投影（原tb_veh_build_config→tb_veh_configuration）';
ALTER TABLE `tb_mdm_plant` COMMENT = '工厂主数据本地投影（原tb_veh_manufacturer→tb_veh_plant）';
ALTER TABLE `tb_mdm_option_family` COMMENT = '选项族主数据本地投影（原tb_veh_feature_family→tb_veh_option_family）';
ALTER TABLE `tb_mdm_option_code` COMMENT = '选项值主数据本地投影（原tb_veh_feature_code→tb_veh_option_code）';
ALTER TABLE `tb_mdm_configuration_option_code` COMMENT = '配置选项关系表（原tb_veh_build_config_feature_code）';
ALTER TABLE `tb_mdm_variant_option_code` COMMENT = '版本选项关系表（原tb_veh_base_model_feature_code）';
