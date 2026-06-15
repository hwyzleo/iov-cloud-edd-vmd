-- V24__Rename_veh_import_data_to_part_import_data.sql
-- 将车辆导入数据表重命名为零件导入数据表
-- 配合后端 MptVehicleImportDataController -> MptPartImportDataController 调整

-- 重命名表
RENAME TABLE `tb_veh_import_data` TO `tb_part_import_data`;

-- 重命名字段 type -> part_code
ALTER TABLE `tb_part_import_data`
  CHANGE COLUMN `type` `part_code` varchar(100) NOT NULL COMMENT '零件编码';

-- 更新表注释
ALTER TABLE `tb_part_import_data` COMMENT = '零件导入数据表';

-- 更新字段注释
ALTER TABLE `tb_part_import_data`
  MODIFY COLUMN `data` text NOT NULL COMMENT '零件导入数据';
