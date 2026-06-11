-- V21__Add_inbound_columns_to_part_info.sql
-- CR-023: 零件实例入站治理列
-- 为part_info新增入站治理字段，接续CR-022的V20

-- 1. 新增入站来源系统枚举字段
ALTER TABLE `tb_part_info`
ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '入站来源系统：MES-制造执行系统, MANUAL-手动导入, WMS-仓储管理系统, IQC-来料检验, OTHER-其他' AFTER `instance_state`;

-- 2. 新增零件类型快照字段
ALTER TABLE `tb_part_info`
ADD COLUMN `part_type` VARCHAR(20) DEFAULT NULL COMMENT '零件类型快照：TBOX-车载终端, BTM-蓝牙模块, CCP-域控制器, IDCM-智能驾驶控制, SIM-SIM卡, OTHER-其他' AFTER `source`;

-- 3. 新增入站批次号（事件/批次级幂等去重键）
ALTER TABLE `tb_part_info`
ADD COLUMN `inbound_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '入站批次号（批次级幂等去重键）' AFTER `part_type`;

-- 4. 新增源事件ID（事件级幂等去重键）
ALTER TABLE `tb_part_info`
ADD COLUMN `source_event_id` VARCHAR(128) DEFAULT NULL COMMENT '源事件ID（事件级幂等去重键）' AFTER `inbound_batch_no`;

-- 5. 新增最近入站时间
ALTER TABLE `tb_part_info`
ADD COLUMN `last_inbound_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最近一次入站upsert时间' AFTER `source_event_id`;

-- 6. 修改vehicle_node_code为可空（车载节点对零件实例可选）
ALTER TABLE `tb_part_info`
MODIFY COLUMN `vehicle_node_code` VARCHAR(20) DEFAULT NULL COMMENT '车载节点代码（关联tb_mdm_vehicle_node.code，可空，仅联网/可升级/关键件具备）';

-- 7. 创建索引
CREATE INDEX `idx_source` ON `tb_part_info` (`source`);
CREATE INDEX `idx_part_type` ON `tb_part_info` (`part_type`);
CREATE INDEX `idx_inbound_batch_no` ON `tb_part_info` (`inbound_batch_no`);
CREATE INDEX `idx_source_event_id` ON `tb_part_info` (`source_event_id`);

-- 8. 回填既有行source='MANUAL'（历史数据视为手动导入）
UPDATE `tb_part_info` SET `source` = 'MANUAL' WHERE `source` IS NULL OR `source` = '';

-- 回滚DDL（仅供参考，不自动执行）：
-- ALTER TABLE `tb_part_info` DROP COLUMN `source`;
-- ALTER TABLE `tb_part_info` DROP COLUMN `part_type`;
-- ALTER TABLE `tb_part_info` DROP COLUMN `inbound_batch_no`;
-- ALTER TABLE `tb_part_info` DROP COLUMN `source_event_id`;
-- ALTER TABLE `tb_part_info` DROP COLUMN `last_inbound_time`;
