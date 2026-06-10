-- V19__Create_part_info.sql
-- CR-022: 新建物理零件实例本体表
-- 新建 part_info 表，UK (part_code, sn)
-- 允许未绑定 VIN 时独立存在（游离零件）
-- 仅持引用键、不建指向 part/mdm_vehicle_node 的物理外键
--
-- 接续 CR-021 的 V17

CREATE TABLE IF NOT EXISTS `tb_part_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `part_code` VARCHAR(20) NOT NULL COMMENT '零件编码（关联tb_mdm_part.pn）',
  `sn` VARCHAR(255) DEFAULT NULL COMMENT '零件序列号',
  `vehicle_node_code` VARCHAR(20) DEFAULT NULL COMMENT '车载节点代码（关联tb_mdm_vehicle_node.code）',
  `config_word` VARCHAR(255) DEFAULT NULL COMMENT '配置字',
  `supplier_code` VARCHAR(255) DEFAULT NULL COMMENT '供应商编码',
  `batch_num` VARCHAR(255) DEFAULT NULL COMMENT '批次号',
  `hardware_ver` VARCHAR(255) DEFAULT NULL COMMENT '硬件版本号',
  `software_ver` VARCHAR(255) DEFAULT NULL COMMENT '软件版本号',
  `hardware_pn` VARCHAR(255) DEFAULT NULL COMMENT '硬件零件号',
  `software_pn` VARCHAR(255) DEFAULT NULL COMMENT '软件零件号',
  `extra` TEXT COMMENT '附加信息',
  `instance_state` SMALLINT NOT NULL DEFAULT 0 COMMENT '实例状态：0-在库，1-在用，2-待更换，3-已报废',
  `first_seen_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次入库时间',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `row_version` INT DEFAULT '1' COMMENT '记录版本',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_part_code_sn` (`part_code`, `sn`),
  KEY `idx_part_code` (`part_code`),
  KEY `idx_vehicle_node_code` (`vehicle_node_code`),
  KEY `idx_instance_state` (`instance_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物理零件实例本体表';

-- 回滚DDL（仅供参考，不自动执行）：
-- DROP TABLE IF EXISTS `tb_part_info`;
