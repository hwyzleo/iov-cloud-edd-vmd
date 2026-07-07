-- CR-032 补漏：veh_security_constant 补 kms_provider / key_spec / algorithm 列
-- 对齐 part_security_constant (V34) 与设计文档 §3.1

ALTER TABLE tb_veh_security_constant
    ADD COLUMN kms_provider VARCHAR(64) DEFAULT NULL COMMENT 'KMS/HSM提供方标识' AFTER kms_key_ref,
    ADD COLUMN key_spec VARCHAR(64) DEFAULT NULL COMMENT '密钥规格' AFTER kms_provider,
    ADD COLUMN algorithm VARCHAR(64) DEFAULT NULL COMMENT '算法标识' AFTER key_spec;
