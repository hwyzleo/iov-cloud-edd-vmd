-- CR-019: 供应商本地维护下线（方案B - 直接清退）
-- 供应商主数据SSOT上移至edd-mdm Party子域（MDM CR-006）
-- VMD不再保留供应商本地维护能力，且不建本地只读投影

-- 前置要求：
-- 1. 已完成VMD历史供应商数据与edd-mdm Party子域一致性核对
-- 2. 调用方（如SRM）已切换至edd-mdm Party子域或仅使用supplier_code透传

-- 保留边界：
-- part.supplier_code 及导入链路 ods_vmd_* 的 supplier_code 不在本迁移范围
-- 6类离线导入（PRODUCE/EOL/BTM/CCP/IDCM/TBOX/SIM）写入 supplier_code 的逻辑不变

DROP TABLE IF EXISTS tb_supplier;

-- 回滚DDL（仅供参考，不自动执行）：
-- CREATE TABLE IF NOT EXISTS tb_supplier (
--   id BIGINT AUTO_INCREMENT PRIMARY KEY,
--   code VARCHAR(64) NOT NULL COMMENT '供应商编码',
--   name VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
--   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--   UNIQUE KEY uk_code (code)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表（已由CR-019清退）';
--
-- 回滚步骤：
-- 1. 执行上述CREATE TABLE重建表结构
-- 2. 从备份数据恢复历史供应商数据
-- 3. 重新启用Supplier相关代码（需回退代码版本）
