-- VMD-DSN-CR-036: 安全常量表新增 kcv 列（KCV 密钥校验值）
-- 可空、可公开、不可逆、非密钥，随 preset_state=PRESET 回填，KMS 现算 VMD 落库

ALTER TABLE tb_veh_security_constant
    ADD COLUMN kcv VARCHAR(128) DEFAULT NULL COMMENT 'KCV密钥校验值（可公开、不可逆、非密钥，hex编码）' AFTER algorithm;

ALTER TABLE tb_part_security_constant
    ADD COLUMN kcv VARCHAR(128) DEFAULT NULL COMMENT 'KCV密钥校验值（可公开、不可逆、非密钥，hex编码）' AFTER algorithm;
