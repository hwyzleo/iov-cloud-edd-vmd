-- 生产配置特征值关联表迁移
-- 将生产配置表中的硬编码特征字段迁移到独立的关联表

-- 1. 创建生产配置特征值关联表
CREATE TABLE IF NOT EXISTS `tb_veh_build_config_feature_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `build_config_code` varchar(255) NOT NULL COMMENT '生产配置代码',
  `family_code` varchar(255) NOT NULL COMMENT '特征族代码',
  `feature_code` varchar(255) NOT NULL COMMENT '特征值代码',
  `feature_type` varchar(20) DEFAULT NULL COMMENT '特征值类型',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_build_config` (`build_config_code`),
  KEY `idx_family` (`family_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆生产配置特征值关系表';

-- 2. 迁移外饰特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'EXTERIOR', `exterior_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `exterior_code` IS NOT NULL AND `exterior_code` != '' AND `row_valid` = 1;

-- 3. 迁移内饰特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'INTERIOR', `interior_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `interior_code` IS NOT NULL AND `interior_code` != '' AND `row_valid` = 1;

-- 4. 迁移轮毂特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'WHEEL', `wheel_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `wheel_code` IS NOT NULL AND `wheel_code` != '' AND `row_valid` = 1;

-- 5. 迁移轮胎特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'TIRE', `tire_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `tire_code` IS NOT NULL AND `tire_code` != '' AND `row_valid` = 1;

-- 6. 迁移备胎特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'SPARE_TIRE', `spare_tire_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `spare_tire_code` IS NOT NULL AND `spare_tire_code` != '' AND `row_valid` = 1;

-- 7. 迁移智驾特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'ADAS', `adas_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `adas_code` IS NOT NULL AND `adas_code` != '' AND `row_valid` = 1;

-- 8. 迁移座椅特征数据
INSERT INTO `tb_veh_build_config_feature_code` (`build_config_code`, `family_code`, `feature_code`, `feature_type`, `create_by`)
SELECT `code`, 'SEAT', `seat_code`, 'SINGLE', `create_by`
FROM `tb_veh_build_config`
WHERE `seat_code` IS NOT NULL AND `seat_code` != '' AND `row_valid` = 1;

-- 9. 删除原表中的硬编码特征字段
ALTER TABLE `tb_veh_build_config`
  DROP COLUMN `exterior_code`,
  DROP COLUMN `interior_code`,
  DROP COLUMN `wheel_code`,
  DROP COLUMN `tire_code`,
  DROP COLUMN `spare_tire_code`,
  DROP COLUMN `adas_code`,
  DROP COLUMN `seat_code`;