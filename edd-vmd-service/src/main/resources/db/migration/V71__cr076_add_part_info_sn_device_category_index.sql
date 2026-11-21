-- V71__cr076_add_part_info_sn_device_category_index.sql
-- CR-076：设备→VIN 只读反查（GET /api/service/vehiclePartBinding/v1/vehicleByDevice）复合索引
-- 查询形态（实际实现，虚拟表名 vehicle_part_binding 在设计 §5 为示意）：
--   tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id AND pi.row_valid = 1
--   WHERE vp.row_valid = 1 AND vp.bind_state = 1
--     AND pi.sn = ?
--     AND COALESCE(pi.device_category, <按 pi.vehicle_node_code 关联的 MDM 节点类别>) = ?
-- 既有 uk_part_code_sn(part_code, sn) 前导列为 part_code，无法服务「仅 sn」反查；
-- 本迁移仅新增索引，不改列、不改数据（expand-only，可回滚）。

ALTER TABLE `tb_part_info`
  ADD INDEX `idx_sn_device_category` (`sn`, `device_category`);

-- 回滚DDL（仅供参考，不自动执行）：
-- ALTER TABLE `tb_part_info` DROP INDEX `idx_sn_device_category`;
