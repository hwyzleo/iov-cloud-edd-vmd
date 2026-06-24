-- CR-032: 收口安全常量存储分层
-- 1. 删除 cipher_blob 列
-- 2. 将 key_handle 重命名为 kms_key_ref

-- 删除 cipher_blob 列
ALTER TABLE tb_veh_security_constant DROP COLUMN IF EXISTS cipher_blob;

-- 将 key_handle 重命名为 kms_key_ref
ALTER TABLE tb_veh_security_constant CHANGE COLUMN key_handle kms_key_ref VARCHAR(255) COMMENT 'KMS密钥引用（keyId/alias，仅为指针）';
