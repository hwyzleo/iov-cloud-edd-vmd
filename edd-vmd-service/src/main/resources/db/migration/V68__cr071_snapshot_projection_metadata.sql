-- V68__cr071_snapshot_projection_metadata.sql
-- CR-071: 软件兼容投影元数据（§5 SSOT 主软件投影）
-- tb_part_info 增列，支撑 software_pn/software_ver 作为"当前主软件兼容投影"的可判定状态与来源引用。
-- 设计决策（§5.1）：保留 software_pn/software_ver 两列（兼容读取），不删除、不改型；
-- 完整多 Target/Slot 当前及历史始终查询 tb_part_software_installation（唯一完整 SSOT）。
--
-- 说明：
-- 1) tb_part_info.row_version 已存在于 V19（INT），tb_veh_basic_info.row_version 已存在于 V0，
--    均不重复添加平行版本列（§7.3 门禁）。
-- 2) expand-only，无破坏性 DROP；MySQL DDL 隐式提交，不承诺事务回滚。

ALTER TABLE `tb_part_info`
  ADD COLUMN `software_snapshot_state` VARCHAR(32) NOT NULL DEFAULT 'LEGACY_UNVERIFIED'
    COMMENT '软件兼容投影状态：CURRENT/AMBIGUOUS/PROVISIONAL/UNAVAILABLE/LEGACY_UNVERIFIED（CR-071）'
    AFTER `software_pn`,
  ADD COLUMN `software_snapshot_installation_id` BIGINT NULL
    COMMENT '主软件投影来源安装记录ID（tb_part_software_installation.id）'
    AFTER `software_snapshot_state`,
  ADD COLUMN `software_snapshot_target` VARCHAR(128) NULL
    COMMENT '主软件投影来源 Target（software_target_code）'
    AFTER `software_snapshot_installation_id`,
  ADD COLUMN `software_snapshot_slot` VARCHAR(64) NULL
    COMMENT '主软件投影来源槽位（slot）'
    AFTER `software_snapshot_target`,
  ADD COLUMN `software_snapshot_inventory_version` BIGINT NULL
    COMMENT '主软件投影来源清单版本（inventory_version）'
    AFTER `software_snapshot_slot`;

-- 回滚DDL（仅供参考，不自动执行）：
-- ALTER TABLE `tb_part_info`
--   DROP COLUMN `software_snapshot_inventory_version`,
--   DROP COLUMN `software_snapshot_slot`,
--   DROP COLUMN `software_snapshot_target`,
--   DROP COLUMN `software_snapshot_installation_id`,
--   DROP COLUMN `software_snapshot_state`;
