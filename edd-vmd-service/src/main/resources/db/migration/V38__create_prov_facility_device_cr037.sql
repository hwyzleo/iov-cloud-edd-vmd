-- VMD-DSN-CR-037: 新建安全灌注机注册表（安全域第3张表）
-- 登记安全灌注机并经 deriveByUid(facilityUid, KLD_DEVICE_ROOT) 预置设备根

CREATE TABLE `tb_prov_facility_device` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `facility_uid` varchar(128) NOT NULL COMMENT '灌注机唯一标识',
    `facility_type` varchar(32) NOT NULL DEFAULT 'KLD' COMMENT '灌注机类型：KLD-安全灌注机',
    `preset_state` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '预置状态：PENDING-待预置，PRESET-已预置，FAILED-预置失败',
    `kms_provider` varchar(64) DEFAULT NULL COMMENT 'KMS/HSM提供方标识',
    `kms_key_ref` varchar(255) DEFAULT NULL COMMENT 'KMS密钥引用（keyId/alias，仅为指针）',
    `key_spec` varchar(64) DEFAULT NULL COMMENT '密钥规格',
    `algorithm` varchar(64) DEFAULT NULL COMMENT '算法标识',
    `kcv` varchar(128) DEFAULT NULL COMMENT 'KCV密钥校验值（可公开、不可逆、非密钥，hex编码）',
    `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因（按列长截断）',
    `gen_time` datetime DEFAULT NULL COMMENT '生成成功时间',
    `last_attempt_time` datetime DEFAULT NULL COMMENT '最后尝试时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
    `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by` varchar(64) DEFAULT NULL COMMENT '修改人',
    `row_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `row_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '逻辑删除标志：0-已删除，1-有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_facility_uid` (`facility_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全灌注机注册表';
