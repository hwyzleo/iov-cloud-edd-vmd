-- CR-055 修复：tb_vmd_outbox.aggregate_id 列宽从 varchar(64) 加宽到 varchar(128)。
-- 原因：BusinessKeyChangedPublisher（CR-055 §7.4）将 Keyprov 官方服务返回的 keyId
-- 写入 aggregate_id；keyId 为不透明标识，权威列宽为 tb_device_business_key.key_id
-- 的 varchar(128)（V55），部分 keyId 超过 64 字符导致
-- "Data too long for column 'aggregate_id'"（ACK 事务整体回滚，官方按原 requestId 重试）。
-- 与 key_id 权威长度对齐后不再截断/溢出；uk_event_id 与 idx_aggregate 均不受影响。

ALTER TABLE `tb_vmd_outbox`
  MODIFY COLUMN `aggregate_id` varchar(128) NOT NULL COMMENT '聚合ID（如 VIN / Keyprov keyId，最长128）';
