-- V59__Backfill_part_security_constant_part_code.sql
-- BUG修复：零件导入的安全常量预置 part_code 必须与 part_info.part_code 同源。
-- part_info.part_code 语义为装车软硬件总成号（ASSEMBLY_PART_NO），
-- 而历史导入路径误将批次/硬件总成号（HARDWARE_PART_NO）写入 part_security_constant.part_code，
-- 导致 part_security_constant 无法与 part_info 关联（keyprov 按 chip_uid 反查设备失败）。
--
-- 注意：tb_part_info（V19）COLLATE=utf8mb4_0900_ai_ci，
--      tb_part_security_constant（V34）COLLATE=utf8mb4_unicode_ci，
--      跨表比较 part_code/sn 需显式 COLLATE，否则报 1267 Illegal mix of collations。

-- 1) 回填：按 (sn, hardware_pn) 定位 part_info，将 part_code 改写为总成零件号。
--    使用 UPDATE IGNORE，跳过会与既有 (part_code,sn) 唯一键冲突的记录（冲突行由第 2 步清理）。
UPDATE IGNORE tb_part_security_constant sc
JOIN tb_part_info pi
  ON pi.sn = sc.sn COLLATE utf8mb4_0900_ai_ci
 AND pi.row_valid = 1
 AND pi.hardware_pn = sc.part_code COLLATE utf8mb4_0900_ai_ci
SET sc.part_code = pi.part_code
WHERE sc.part_code COLLATE utf8mb4_0900_ai_ci <> pi.part_code;

-- 2) 清理：同一 sn 下已存在总成零件号行时，逻辑删除残留的硬件零件号重复行，
--    避免 chip_uid 反查命中多个器件而被判定为数据冲突。
UPDATE tb_part_security_constant sc
JOIN tb_part_info pi
  ON pi.sn = sc.sn COLLATE utf8mb4_0900_ai_ci
 AND pi.row_valid = 1
 AND pi.hardware_pn = sc.part_code COLLATE utf8mb4_0900_ai_ci
JOIN tb_part_security_constant canon
  ON canon.sn = sc.sn
 AND canon.part_code COLLATE utf8mb4_0900_ai_ci = pi.part_code
 AND canon.id <> sc.id
 AND canon.deleted = 0
SET sc.deleted = 1
WHERE sc.deleted = 0
  AND sc.part_code COLLATE utf8mb4_0900_ai_ci <> pi.part_code;
