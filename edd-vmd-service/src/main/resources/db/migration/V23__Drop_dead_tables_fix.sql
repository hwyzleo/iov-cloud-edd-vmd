-- V23__Drop_dead_tables_fix.sql
-- CR-022: 修复V18遗漏的死表删除（V18表名缺少tb_前缀）
-- 删除无任何PO/Mapper/写入路径的死表
--
-- 接续 CR-023 的 V22

-- 使用存储过程来安全删除表，避免表不存在时的警告
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS drop_table_if_exists(IN table_name VARCHAR(255))
BEGIN
    DECLARE CONTINUE HANDLER FOR 1051 BEGIN END; -- Unknown table
    SET @sql = CONCAT('DROP TABLE IF EXISTS `', table_name, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //

DELIMITER ;

-- 修复V18遗漏：删除外饰表（V18错误使用veh_exterior，实际表名为tb_veh_exterior）
CALL drop_table_if_exists('tb_veh_exterior');

-- 修复V18遗漏：删除内饰表
CALL drop_table_if_exists('tb_veh_interior');

-- 修复V18遗漏：删除轮毂表
CALL drop_table_if_exists('tb_veh_wheel');

-- 修复V18遗漏：删除选装表
CALL drop_table_if_exists('tb_veh_optional');

-- 修复V18遗漏：删除ECU表
CALL drop_table_if_exists('tb_veh_ecu');

-- 修复V18遗漏：删除激活表
CALL drop_table_if_exists('tb_veh_activation');

-- 修复V18遗漏：删除用户表
CALL drop_table_if_exists('tb_veh_user');

-- 清理存储过程
DROP PROCEDURE IF EXISTS drop_table_if_exists;

-- 回滚DDL（仅供参考，不自动执行）：
-- 回滚步骤需要从备份恢复这些表的结构和数据
-- 这些表在CR-022中被认定为死表，无活跃的代码路径
