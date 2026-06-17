-- VMD-DSN-CR-027: 新建车辆导入批次表
-- 与 part_import_data 并列，支撑车辆数据导入域独立化
CREATE TABLE `tb_veh_import_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_num` varchar(64) NOT NULL COMMENT '批次号',
  `type` varchar(32) NOT NULL COMMENT '车辆生命周期节点类型（本轮仅PRODUCE）',
  `version` varchar(16) NOT NULL DEFAULT '1.0' COMMENT '数据版本',
  `data` text NOT NULL COMMENT '原始报文',
  `handle` tinyint(1) NOT NULL DEFAULT 0 COMMENT '处理状态：0-未处理，1-已处理',
  `description` varchar(500) DEFAULT NULL COMMENT '失败原因（按列长截断）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `row_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `row_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '逻辑删除标志：0-已删除，1-有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_num` (`batch_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车辆导入数据表';
