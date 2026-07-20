-- CR-045: 扩展软件实装时态表支持多来源写入与消解
-- 添加 is_confirmed 字段标识是否已确认（provisional/confirmed 语义）
-- 添加 source_event_time 字段记录来源事件时间（版本时序 gate 判定）

ALTER TABLE `tb_part_software_installation`
  ADD COLUMN `is_confirmed` tinyint NOT NULL DEFAULT 1 COMMENT '是否已确认（1=confirmed，0=provisional）' AFTER `flash_result`,
  ADD COLUMN `source_event_time` timestamp NULL DEFAULT NULL COMMENT '来源事件时间（版本时序gate判定用）' AFTER `is_confirmed`;

-- 添加索引支持按 VIN 查询软件清单
CREATE INDEX `idx_vin_target` ON `tb_part_software_installation` (`vin_snapshot`, `software_target_code`);
