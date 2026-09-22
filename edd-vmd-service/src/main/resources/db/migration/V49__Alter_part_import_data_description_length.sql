-- V49: 扩宽零件导入数据表 description 列
-- 原因：下游联动（TSP/KMS 等）失败时错误信息（FeignException 完整消息含响应体）拼接后
--       超过原 varchar(255) 列宽，导致 DataIntegrityViolationException 使整个导入批次回滚失败。
-- 处理：列宽扩至 1000，与 PartImportDataAppService 截断上限一致（按列长截断）。
ALTER TABLE `tb_part_import_data`
  MODIFY COLUMN `description` varchar(1000) DEFAULT NULL COMMENT '备注/失败原因（按列长截断）';
