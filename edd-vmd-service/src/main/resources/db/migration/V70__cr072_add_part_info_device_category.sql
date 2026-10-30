-- V70__cr072_add_part_info_device_category.sql
-- CR-072：tb_part_info 新增 device_category 最小快照列
-- 语义：车载节点设备类别快照（按 vehicle_node_code 解析 MDM tb_mdm_vehicle_node.device_category），
--       非物料分类（part_type 保持 MDM Part 来源），不建 FK/唯一约束/索引（首版），无 AFTER 列重排。
-- 发布顺序：先加列（仅 NULL 列，expand-only）→ 全 writer 升级 → reader 兼容 → 开关灰度开启（vmd.part.device-category.enabled）。

ALTER TABLE `tb_part_info`
  ADD COLUMN `device_category` VARCHAR(20) NULL DEFAULT NULL
  COMMENT '车载节点设备类别快照，按vehicle_node_code解析MDM；非物料分类（CR-072）';
