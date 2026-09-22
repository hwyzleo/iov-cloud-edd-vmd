-- V50__Add_hsm_capability_to_vehicle_node_cr049.sql
-- CR-049：VehicleNode 投影透传 hsmCapability（MDM 主数据驱动器件级安全常量预置）
-- 迁移期允许 NULL（滚动发布）；禁止用 NONE 默认值掩盖缺失主数据（RD-049 / R-049-1）
ALTER TABLE `tb_mdm_vehicle_node`
    ADD COLUMN `hsm_capability` VARCHAR(32) NULL COMMENT 'HSM 能力：NONE/SHE/HSM_LIGHT/HSM_FULL（CR-049 主数据驱动预置）' AFTER `device_category`;
