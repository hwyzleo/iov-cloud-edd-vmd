-- CR-031: 创建零件安全常量表

CREATE TABLE tb_part_security_constant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    part_code VARCHAR(64) NOT NULL COMMENT '零件编码（→part_info.part_code）',
    sn VARCHAR(64) NOT NULL COMMENT '零件序列号（→part_info.sn）',
    chip_uid VARCHAR(128) COMMENT '安全芯片/HSM唯一标识快照（源自part_info.hsm_uid）',
    constant_type VARCHAR(32) NOT NULL DEFAULT 'ROOT' COMMENT '安全常量用途/类型：ROOT-根密钥，SECOC-安全通信，IMMO-防盗，SEED_KEY-种子密钥',
    kms_provider VARCHAR(64) COMMENT 'KMS/HSM提供方标识',
    kms_key_ref VARCHAR(255) COMMENT 'KMS密钥引用（keyId/alias，仅为指针）',
    key_spec VARCHAR(64) COMMENT '密钥规格',
    algorithm VARCHAR(64) COMMENT '算法标识',
    preset_state VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '预置状态：PENDING-待预置，PRESET-已预置，FAILED-预置失败',
    gen_time DATETIME COMMENT '生成成功时间',
    last_attempt_time DATETIME COMMENT '最后尝试时间',
    fail_reason VARCHAR(500) COMMENT '失败原因（按列长截断）',
    batch_num VARCHAR(64) COMMENT '来源导入批次号（→part_import_data.batch_num）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除',
    UNIQUE KEY uk_part_code_sn (part_code, sn) COMMENT '按器件幂等'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='零件安全常量表';
