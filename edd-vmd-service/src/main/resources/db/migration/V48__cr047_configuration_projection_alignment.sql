-- V48__cr047_configuration_projection_alignment.sql
-- CR-047: 收敛 tb_mdm_configuration 为 Configuration 最小投影
-- 目标字段：code / name / name_local / variant_code / description / source / external_ref_id / external_version / last_sync_time + 审计
-- 删除旧层级冗余列：platform_code / car_line_code / model_code / vehicle_stage_code / enable / sort / name_en
-- 保留：tb_mdm_configuration_option_code（选项值映射）、veh_basic_info 车辆生产代码快照（RD-047-5）
-- 依赖：V8 已将 variant_code 从 NOT NULL 的 base_model_code 回填，存量 variant_code 应非空
-- Phase 顺序：扩展（name_local / 512 / variant_code NOT NULL / 索引）→ 收缩（删旧列）

-- ============================================================
-- Phase 1: 扩展
-- ============================================================

-- 1.1 新增 name_local 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'name_local');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_mdm_configuration` ADD COLUMN `name_local` VARCHAR(512) NULL COMMENT ''配置本地化名称'' AFTER `name`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 名称容量对齐 MDM 512 字符（RD-047-6）
SET @name_len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'name');
SET @sql = IF(@name_len IS NULL OR @name_len < 512, 'ALTER TABLE `tb_mdm_configuration` MODIFY COLUMN `name` VARCHAR(512) NOT NULL COMMENT ''配置名称''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.3 description 容量对齐目标 DDL 512
SET @desc_len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'description');
SET @sql = IF(@desc_len IS NULL OR @desc_len < 512, 'ALTER TABLE `tb_mdm_configuration` MODIFY COLUMN `description` VARCHAR(512) NULL COMMENT ''备注''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.4 variant_code 收缩为 VARCHAR(64) NOT NULL（RD-047-1 唯一直接父引用）
-- 存量 variant_code 若存在 NULL 将由本 MODIFY 确定性 fail-fast（设计 Phase 2 差异核对口径），不吞异常
ALTER TABLE `tb_mdm_configuration` MODIFY COLUMN `variant_code` VARCHAR(64) NOT NULL COMMENT '版本代码';

-- 1.5 索引：配置按 variant 查询 + 同步监控（如果不存在）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND index_name = 'idx_mdm_configuration_variant');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_configuration` ADD KEY `idx_mdm_configuration_variant` (`variant_code`, `row_valid`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND index_name = 'idx_mdm_configuration_sync');
SET @sql = IF(@idx_exists = 0, 'ALTER TABLE `tb_mdm_configuration` ADD KEY `idx_mdm_configuration_sync` (`source`, `last_sync_time`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Phase 3: 收缩（删除旧层级冗余列）
-- ============================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'platform_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `platform_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'car_line_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `car_line_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'model_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `model_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'vehicle_stage_code');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `vehicle_stage_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'enable');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `enable`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'sort');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `sort`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_mdm_configuration' AND column_name = 'name_en');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE `tb_mdm_configuration` DROP COLUMN `name_en`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新表注释
ALTER TABLE `tb_mdm_configuration` COMMENT = '配置（Configuration）主数据本地最小投影（CR-047：仅 variant_code 直接父引用，层级沿产品树派生）';
