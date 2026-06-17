-- VMD-DSN-CR-028: 新建安全常量表
-- 支撑per-VIN安全常量预置
CREATE TABLE `tb_veh_security_constant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vin` varchar(17) NOT NULL COMMENT '车架号',
  `batch_num` varchar(64) DEFAULT NULL COMMENT '导入批次号',
  `preset_state` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '预置状态：PENDING-待预置，PRESET-已预置，FAILED-预置失败',
  `key_handle` varchar(128) DEFAULT NULL COMMENT 'KMS/HSM密钥句柄',
  `cipher_blob` text DEFAULT NULL COMMENT '密文',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因（按列长截断）',
  `gen_time` datetime DEFAULT NULL COMMENT '生成成功时间',
  `last_attempt_time` datetime DEFAULT NULL COMMENT '最后尝试时间',
  `constant_type` varchar(32) DEFAULT NULL COMMENT '常量类型（预留扩展位）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改人',
  `row_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `row_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '逻辑删除标志：0-已删除，1-有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车辆安全常量表';
