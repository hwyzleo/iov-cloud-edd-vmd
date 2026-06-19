-- V32__Create_vehicle_option.sql
-- VMD-DSN-CR-030: 单车选项值快照持久化（US-043）

CREATE TABLE IF NOT EXISTS `tb_vehicle_option` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin` VARCHAR(17) NOT NULL COMMENT '车辆识别码',
    `option_family_code` VARCHAR(50) NOT NULL COMMENT '选项族编码',
    `option_code` VARCHAR(50) NOT NULL COMMENT '选项值编码',
    `source` VARCHAR(50) DEFAULT NULL COMMENT '数据来源',
    `batch_num` VARCHAR(50) DEFAULT NULL COMMENT '导入批次号',
    `snapshot_time` DATETIME DEFAULT NULL COMMENT '快照时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_by` VARCHAR(50) DEFAULT NULL COMMENT '修改人',
    `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vin_option_family` (`vin`, `option_family_code`),
    KEY `idx_vin` (`vin`),
    KEY `idx_batch_num` (`batch_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单车选项值快照表';
