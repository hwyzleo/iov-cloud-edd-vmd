-- V18__Drop_dead_tables.sql
-- CR-022: 死表清退
-- 删除无任何PO/Mapper/写入路径的死表
-- 外饰/内饰/轮毂/选装语义由 Variant+Configuration→OptionCode 表达
-- 不触任何 mdm_* 字典投影表
--
-- 接续 CR-021 的 V17

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

-- 1. 删除外饰表
CALL drop_table_if_exists('veh_exterior');

-- 2. 删除内饰表
CALL drop_table_if_exists('veh_interior');

-- 3. 删除轮毂表
CALL drop_table_if_exists('veh_wheel');

-- 4. 删除选装表
CALL drop_table_if_exists('veh_optional');

-- 5. 删除ECU表
CALL drop_table_if_exists('veh_ecu');

-- 6. 删除激活表
CALL drop_table_if_exists('veh_activation');

-- 7. 删除MES车辆数据表
CALL drop_table_if_exists('tb_mes_vehicle_data');

-- 8. 删除BOM零件主数据表
CALL drop_table_if_exists('tb_bom_part');

-- 9. 删除BOM不随车件表
CALL drop_table_if_exists('tb_bom_part_nove');

-- 10. 删除车型配置车身颜色关系表
CALL drop_table_if_exists('tr_veh_model_config_body_color');

-- 11. 删除车型配置轮毂关系表
CALL drop_table_if_exists('tr_veh_model_config_hub');

-- 12. 删除车型配置内饰颜色关系表
CALL drop_table_if_exists('tr_veh_model_config_interior_color');

-- 13. 删除车型配置选装关系表
CALL drop_table_if_exists('tr_veh_model_config_optional');

-- 14. 删除用户车辆关系表
CALL drop_table_if_exists('tr_veh_user_relation');

-- 15. 删除用户表
CALL drop_table_if_exists('veh_user');

-- 清理存储过程
DROP PROCEDURE IF EXISTS drop_table_if_exists;

-- 回滚DDL（仅供参考，不自动执行）：
-- 回滚步骤需要从备份恢复这些表的结构和数据
-- 这些表在CR-022中被认定为死表，无活跃的代码路径
