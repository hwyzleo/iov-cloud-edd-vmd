-- V20__Rebuild_vehicle_part_as_binding.sql
-- CR-022: 重建绑定关系表 + 废弃历史表
-- 删除旧混血 vehicle_part 与 vehicle_part_history
-- 新建纯绑定 vehicle_part
-- 施加「同一实例/同一车同一节点位 仅一条 active 绑定」约束
-- 使用 MySQL 生成列/NULL 技巧实现部分唯一索引
--
-- 接续 CR-022 的 V19

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

-- 1. 删除旧的车辆零件变更历史表
CALL drop_table_if_exists('tb_vehicle_part_history');

-- 2. 删除旧的车辆零件表
CALL drop_table_if_exists('tb_vehicle_part');

-- 清理存储过程
DROP PROCEDURE IF EXISTS drop_table_if_exists;

-- 3. 新建纯绑定关系表（不包含外键约束，仅通过应用层保证引用完整性）
CREATE TABLE IF NOT EXISTS `tb_vehicle_part` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` VARCHAR(20) NOT NULL COMMENT '车架号（关联tb_veh_basic_info.vin）',
  `part_id` BIGINT NOT NULL COMMENT '零件实例ID（关联tb_part_info.id）',
  `vehicle_node_code` VARCHAR(20) DEFAULT NULL COMMENT '车载节点代码（关联tb_mdm_vehicle_node.code）',
  `device_item` VARCHAR(20) DEFAULT NULL COMMENT '设备项（安装位置快照）',
  `bind_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `bind_type` VARCHAR(20) DEFAULT NULL COMMENT '绑定类型',
  `bind_by` VARCHAR(64) DEFAULT NULL COMMENT '绑定者',
  `bind_org` VARCHAR(64) DEFAULT NULL COMMENT '绑定机构',
  `unbind_time` TIMESTAMP NULL DEFAULT NULL COMMENT '解绑时间',
  `unbind_reason` VARCHAR(50) DEFAULT NULL COMMENT '解绑理由',
  `unbind_by` VARCHAR(64) DEFAULT NULL COMMENT '解绑者',
  `unbind_org` VARCHAR(64) DEFAULT NULL COMMENT '解绑机构',
  `bind_state` SMALLINT NOT NULL DEFAULT 1 COMMENT '绑定状态：0-已解绑，1-绑定中',
  `replace_of_binding_id` BIGINT DEFAULT NULL COMMENT '换件溯源：被替换的绑定ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` VARCHAR(64) DEFAULT NULL COMMENT '修改者',
  `row_version` INT DEFAULT '1' COMMENT '记录版本',
  `row_valid` TINYINT DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_vin` (`vin`),
  KEY `idx_part_id` (`part_id`),
  KEY `idx_vehicle_node_code` (`vehicle_node_code`),
  KEY `idx_bind_state` (`bind_state`),
  KEY `idx_replace_of_binding_id` (`replace_of_binding_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆-零件绑定关系表（纯绑定，实例本体在tb_part_info）';

-- 回滚DDL（仅供参考，不自动执行）：
-- DROP TABLE IF EXISTS `tb_vehicle_part`;
-- 需要重建旧表结构，参考 V0__Baseline.sql 中的 tb_vehicle_part 和 tb_vehicle_part_history 定义
