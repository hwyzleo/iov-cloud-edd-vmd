-- VMD-DSN-CR-039: 修改车辆导入事件补发审计表
-- 将 success_count 改为 queued_count，表示已写入 Outbox 的数量
ALTER TABLE `tb_veh_import_event_replay` 
CHANGE COLUMN `success_count` `queued_count` int NOT NULL DEFAULT 0 COMMENT '已入队数（写入Outbox）';
