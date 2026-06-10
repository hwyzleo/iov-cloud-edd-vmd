-- V16__Migrate_device_code_to_vehicle_node_code.sql
-- CR-020: 关联键 device_code → vehicle_node_code 迁移/回填
-- 在 part / vehicle_part / vehicle_part_history 中新增 vehicle_node_code 列
-- 回填历史数据 vehicle_node_code = device_code
-- 旧列 device_code 兼容期保留（标 deprecated，不删除）
-- 物理设备实例 + 绑定关系本体不上移、不投影化、保持留在 VMD

-- 1. part: 添加 vehicle_node_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_part' AND column_name = 'vehicle_node_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_part` ADD COLUMN `vehicle_node_code` varchar(20) NULL COMMENT ''车载节点代码（原device_code，CR-020重命名）'' AFTER `device_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. part: 回填历史数据
UPDATE `tb_part` SET `vehicle_node_code` = `device_code` WHERE `vehicle_node_code` IS NULL AND `device_code` IS NOT NULL;

-- 3. vehicle_part: 添加 vehicle_node_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_part' AND column_name = 'vehicle_node_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_vehicle_part` ADD COLUMN `vehicle_node_code` varchar(20) NULL COMMENT ''车载节点代码（原device_code，CR-020重命名）'' AFTER `device_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. vehicle_part: 回填历史数据
UPDATE `tb_vehicle_part` SET `vehicle_node_code` = `device_code` WHERE `vehicle_node_code` IS NULL AND `device_code` IS NOT NULL;

-- 5. vehicle_part_history: 添加 vehicle_node_code 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_part_history' AND column_name = 'vehicle_node_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `tb_vehicle_part_history` ADD COLUMN `vehicle_node_code` varchar(20) NULL COMMENT ''车载节点代码（原device_code，CR-020重命名）'' AFTER `device_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. vehicle_part_history: 回填历史数据
UPDATE `tb_vehicle_part_history` SET `vehicle_node_code` = `device_code` WHERE `vehicle_node_code` IS NULL AND `device_code` IS NOT NULL;

-- 7. 旧列兼容期保留（标 deprecated，不删除）
-- device_code 列保留，待后续清理 CR 下线
