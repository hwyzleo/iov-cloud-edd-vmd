-- V31__Refactor_vehicle_node_projection.sql
-- 重构车载节点投影表，对齐 MDM 投影范围
-- 1. 删除原有的 node_type 字段（为 type 字段重命名让路）
-- 2. 重命名字段
-- 3. 删除其他非投影字段
-- 4. 添加索引
-- 5. 更新表注释

-- ============================================================
-- Part 1: 删除原有的 node_type 字段
-- ============================================================

ALTER TABLE `tb_mdm_vehicle_node`
  DROP COLUMN `node_type`;

-- ============================================================
-- Part 2: 重命名字段
-- ============================================================

ALTER TABLE `tb_mdm_vehicle_node`
  CHANGE COLUMN `type` `node_type` VARCHAR(20) NOT NULL COMMENT '节点类型',
  CHANGE COLUMN `device_item` `device_category` VARCHAR(20) NULL COMMENT '设备分类',
  CHANGE COLUMN `name_en` `name_local` VARCHAR(255) NULL COMMENT '本地化名称';

-- ============================================================
-- Part 3: 删除其他非投影字段
-- ============================================================

ALTER TABLE `tb_mdm_vehicle_node`
  DROP COLUMN `partition_type`,
  DROP COLUMN `lock_unlock_security_component`,
  DROP COLUMN `link_config_source`,
  DROP COLUMN `link_flash_target`,
  DROP COLUMN `comm_protocol`,
  DROP COLUMN `flash_protocol`,
  DROP COLUMN `can_tx_id`,
  DROP COLUMN `can_rx_id`,
  DROP COLUMN `ethernet_ip`,
  DROP COLUMN `doip_gateway_id`,
  DROP COLUMN `doip_entity_id`;

-- ============================================================
-- Part 4: 添加索引
-- ============================================================

ALTER TABLE `tb_mdm_vehicle_node`
  ADD INDEX `idx_vn_ota_status` (`ota_support`, `row_valid`),
  ADD INDEX `idx_vn_type_status` (`node_type`, `row_valid`),
  ADD INDEX `idx_vn_domain_status` (`func_domain`, `row_valid`),
  ADD INDEX `idx_vn_core_status` (`core`, `row_valid`),
  ADD INDEX `idx_vn_device_category` (`device_category`);

-- ============================================================
-- Part 5: 更新表注释
-- ============================================================

ALTER TABLE `tb_mdm_vehicle_node` COMMENT = '车载节点（VehicleNode）字典主数据本地投影（对齐 MDM 投影范围）';
