-- ============================================================
-- iov-cloud-edd-vmd 全局DDL脚本
-- 基于Flyway迁移脚本合并生成（V0 ~ V2）
-- 生成时间: 2026-05-23
-- ============================================================

-- BOM零件主数据表
CREATE TABLE IF NOT EXISTS `tb_bom_part` (
  `LOGS_ID` varchar(36) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `PART_NUM` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件号',
  `PART_TYPE` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件类型',
  `PART_NAME_ZH` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件中文名称',
  `PART_NAME_EN` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件英文名称',
  `FFA` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'FFC 位置码',
  `FFA_DESC` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'FFC描述',
  `PART_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件备注',
  `PART_STATUS` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件状态',
  `IS_DIGITATE` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否有数模',
  `UNIT` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用量单位',
  `IS_FRAME_PART` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否架构件',
  `IS_NATURE_PART` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否本色件',
  `COLOR_AREA` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色区域',
  `NATURE_PART_NO` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '本色件号',
  `IS_REGULATORY_PART` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法规件',
  `INITIAL_MODEL` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '初始车型',
  `ISKEY_PART` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关重特性',
  `IS_ACCURATELY_TRACED` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否精准追溯',
  `IS_AFTERSALES_PART` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否售后配件',
  `STANDARD_PARTS_CLASS` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标准件分类',
  `STANDARD_PARTS_CLASS_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标准件分类备注',
  `WRENCH_TYPE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扳拧型式',
  `WRENCH_TYPE_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扳拧型式备注',
  `HEAD_SHAPE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头部形状',
  `HEAD_SHAPE_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头部形状备注',
  `END_SHAPE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '末端形状',
  `END_SHAPE_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '末端形状备注',
  `ROD_TYPE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '杆部型式',
  `ROD_TYPE_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '杆部型式备注',
  `IS_WASHER` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否带垫圈',
  `WASHER_TYPE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '垫圈类型',
  `WASHER_TYPE_REMARK` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '垫圈类型备注',
  `DIAMETER` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '直径',
  `LENGTH` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '长度',
  `PITCH` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '螺距',
  `DENTAL_FORM` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '牙型',
  `STRENGTH_GRADE` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '强度等级',
  `MECHANIAL_PROPERTIES` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机械性能',
  `SURFACE_TREATMENT` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表面处理',
  `STRUCTURE_CHARACTER` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结构特征',
  `ECU_IDENTIFICTION` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ECU标识',
  `ECU_ABB` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ECU简称',
  `ECU_NAME_ZH` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ECU中文名称',
  `ECU_NAME_EN` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ECU英文名称',
  `DESIGNER` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设计工程师',
  `DESIGNER_DEPT` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设计工程师部门',
  `NON_REPARE_REASON` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '不作为配件原因',
  `IS_COLOR_REPARE` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色件维修',
  `IS_PRIMER_REPARE` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '底漆件维修',
  `IS_ELECTROPHORES` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电泳件维修',
  `PRODUCTION_CODE` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应生产件号',
  `SPARE_PROPERTY` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '售后配件属性',
  `SALE_NOTE` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '售后备注',
  `handle_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '写入日期',
  `handle_status` varchar(2) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发送状态',
  `err_log` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误日志',
  `receive_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送日期',
  `XAPIBATCHNO` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次编号',
  `FIRST_PRODUCTION_DATE` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次投产时间',
  PRIMARY KEY (`LOGS_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BOM零件主数据表';

-- BOM不随车件表
CREATE TABLE IF NOT EXISTS `tb_bom_part_nove` (
  `LOGS_ID` varchar(36) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `MATERIAL_NUM` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件号',
  `MATERIAL_NAME` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件中文名称',
  `MATERIAL_NAME_EN` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件英文名称',
  `PART_TYPE` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '零件类型',
  `PRODUCT` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '车型',
  `BASE_PRODUCT` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '基础车型',
  `CREATED_BY` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `CREATED_DATE` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建时间',
  `UPDATED_BY` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `UPDATED_DATE` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改时间',
  `handle_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '写入日期',
  `handle_status` varchar(2) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '发送状态',
  `err_log` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误日志',
  `receive_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送日期',
  `XAPIBATCHNO` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次编号',
  PRIMARY KEY (`LOGS_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BOM不随车件表';

-- 配置项表
CREATE TABLE IF NOT EXISTS `tb_config_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family` varchar(50) NOT NULL COMMENT '配置项大类',
  `code` varchar(50) NOT NULL COMMENT '配置项编码',
  `name` varchar(255) NOT NULL COMMENT '配置项名称',
  `type` varchar(32) NOT NULL COMMENT '配置项类型',
  `unit` varchar(16) DEFAULT NULL COMMENT '配置项单位',
  `capability` tinyint DEFAULT '0' COMMENT '是否车辆能力',
  `display` tinyint DEFAULT '0' COMMENT '端上是否展示',
  `cache` tinyint DEFAULT '0' COMMENT '端上是否缓存',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置项表';

-- 配置项映射表
CREATE TABLE IF NOT EXISTS `tb_config_item_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_item_code` varchar(50) NOT NULL COMMENT '配置项代码',
  `source_system` varchar(50) NOT NULL COMMENT '源系统',
  `source_code` varchar(50) NOT NULL COMMENT '源系统代码',
  `source_value` varchar(255) DEFAULT NULL COMMENT '源系统值',
  `target_option_code` varchar(50) DEFAULT NULL COMMENT '映射的枚举值编码',
  `target_value` varchar(255) DEFAULT NULL COMMENT '映射值',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_config_item` (`config_item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置项映射表';

-- 配置项枚举值表
CREATE TABLE IF NOT EXISTS `tb_config_item_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_item_code` varchar(50) NOT NULL COMMENT '配置项编码',
  `code` varchar(50) NOT NULL COMMENT '枚举值编码',
  `name` varchar(255) NOT NULL COMMENT '枚举值名称',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_config_item` (`config_item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配置项枚举值表';

-- 设备信息表
CREATE TABLE IF NOT EXISTS `tb_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(20) NOT NULL COMMENT '设备编码',
  `name` varchar(255) NOT NULL COMMENT '设备名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '设备英文名称',
  `type` varchar(20) NOT NULL COMMENT '设备类型',
  `device_item` varchar(20) DEFAULT NULL COMMENT '设备项',
  `func_domain` varchar(20) NOT NULL COMMENT '功能域',
  `node_type` varchar(100) NOT NULL COMMENT '节点类型',
  `ota_support` varchar(20) NOT NULL COMMENT 'OTA支持类型',
  `partition_type` varchar(20) DEFAULT NULL COMMENT '分区类型',
  `lock_unlock_security_component` smallint DEFAULT NULL COMMENT '解闭锁安全件',
  `link_config_source` varchar(50) DEFAULT NULL COMMENT '链路配置源',
  `link_flash_target` varchar(50) DEFAULT NULL COMMENT '链路生效目标',
  `comm_protocol` varchar(50) DEFAULT NULL COMMENT '通信协议',
  `flash_protocol` varchar(50) DEFAULT NULL COMMENT '刷写协议',
  `can_tx_id` varchar(20) DEFAULT NULL COMMENT 'CAN/CANFD总线发送标识',
  `can_rx_id` varchar(20) DEFAULT NULL COMMENT 'CAN/CANFD总线接收标识',
  `ethernet_ip` varchar(20) DEFAULT NULL COMMENT '以太网的业务IP',
  `doip_gateway_id` varchar(20) DEFAULT NULL COMMENT 'DoIP协议网关标识',
  `doip_entity_id` varchar(20) DEFAULT NULL COMMENT 'DoIP协议设备标识',
  `core` tinyint DEFAULT '0' COMMENT '是否核心设备',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备信息表';

-- MES车辆数据表
CREATE TABLE IF NOT EXISTS `tb_mes_vehicle_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_num` varchar(255) NOT NULL COMMENT '批次号',
  `type` varchar(100) NOT NULL COMMENT '数据类型',
  `version` varchar(100) NOT NULL COMMENT '数据版本',
  `data` text NOT NULL COMMENT 'MES车辆数据',
  `handle` tinyint DEFAULT '0' COMMENT '是否处理',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_num` (`batch_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MES车辆数据表';

-- 零件信息表（MDM投影，CR-021重命名自tb_part）
CREATE TABLE IF NOT EXISTS `tb_mdm_part` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pn` varchar(20) NOT NULL COMMENT '零件号（partCode关联键）',
  `name` varchar(255) NOT NULL COMMENT '零件中文名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '零件英文名称',
  `type` varchar(20) DEFAULT NULL COMMENT '零件类型',
  `ffa` varchar(20) DEFAULT NULL COMMENT '零件分类',
  `status` varchar(20) DEFAULT NULL COMMENT '零件状态',
  `digital_model` varchar(20) DEFAULT NULL COMMENT '数字模型',
  `unit` varchar(10) DEFAULT NULL COMMENT '单位',
  `frame_part` tinyint DEFAULT '0' COMMENT '是否是架构件',
  `nature_part` tinyint DEFAULT '0' COMMENT '是否是本色件',
  `color_area` varchar(20) DEFAULT NULL COMMENT '颜色区域',
  `nature_pn` varchar(20) DEFAULT NULL COMMENT '本色件零件号',
  `regulatory_part` tinyint DEFAULT '0' COMMENT '是否是法规件',
  `key_part` varchar(20) DEFAULT NULL COMMENT '关键程度',
  `accurately_traced` tinyint DEFAULT '0' COMMENT '是否精准追溯',
  `aftersale_part` tinyint DEFAULT '0' COMMENT '是否是配件',
  `standard_part_class` varchar(20) DEFAULT NULL COMMENT '标准件分类',
  `wrench_type` varchar(20) DEFAULT NULL COMMENT '扳拧形式',
  `rod_type` varchar(20) DEFAULT NULL COMMENT '杆部形式',
  `head_shape` varchar(20) DEFAULT NULL COMMENT '头部形状',
  `end_shape` varchar(20) DEFAULT NULL COMMENT '末端形状',
  `washer` tinyint DEFAULT '0' COMMENT '是否带垫圈',
  `washer_type` varchar(20) DEFAULT NULL COMMENT '垫圈类型',
  `diameter` varchar(20) DEFAULT NULL COMMENT '直径',
  `length` varchar(20) DEFAULT NULL COMMENT '长度',
  `pitch` varchar(20) DEFAULT NULL COMMENT '螺距',
  `dental_form` varchar(20) DEFAULT NULL COMMENT '牙型',
  `strength_grade` varchar(20) DEFAULT NULL COMMENT '强度等级',
  `mechanical_property` varchar(20) DEFAULT NULL COMMENT '机械性能',
  `surface_treatment` varchar(20) DEFAULT NULL COMMENT '表面处理',
  `structure_character` varchar(20) DEFAULT NULL COMMENT '结构特征',
  `device_form` varchar(20) DEFAULT NULL COMMENT '设备形态',
  `device_code` varchar(20) DEFAULT NULL COMMENT '设备代码（vehicleNodeCode关联键）',
  `supplier_code` varchar(50) DEFAULT NULL COMMENT '供应商代码（溯源透传）',
  `fota_upgradeable` tinyint DEFAULT '0' COMMENT '是否支持FOTA升级',
  `designer` varchar(20) DEFAULT NULL COMMENT '设计工程师',
  `designer_dept` varchar(20) DEFAULT NULL COMMENT '设计工程师部门',
  `non_repair_reason` varchar(20) DEFAULT NULL COMMENT '不作为备件原因',
  `color_repair` tinyint DEFAULT '0' COMMENT '是否颜色件维修',
  `primer_repair` tinyint DEFAULT '0' COMMENT '是否底漆件维修',
  `electrophoresis_repair` tinyint DEFAULT '0' COMMENT '是否电泳件维修',
  `production_code` varchar(20) DEFAULT NULL COMMENT '对应生产件号',
  `spare_property` varchar(20) DEFAULT NULL COMMENT '售后配件属性',
  `sale_note` varchar(255) DEFAULT NULL COMMENT '售后备注',
  `first_production_date` varchar(20) DEFAULT NULL COMMENT '首次投产时间',
  `initial_model` varchar(20) DEFAULT NULL COMMENT '初始车型',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `source` varchar(16) NOT NULL DEFAULT 'MANUAL' COMMENT '数据来源：MDM=来自MDM系统，MANUAL=本地手动维护',
  `external_ref_id` varchar(64) DEFAULT NULL COMMENT 'MDM侧实体主键ID',
  `external_version` bigint DEFAULT 0 COMMENT 'MDM侧实体版本号',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后一次同步时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pn` (`pn`),
  UNIQUE KEY `uk_external_ref_id` (`external_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='零件（Part）字典主数据本地投影';

-- 供应商表
CREATE TABLE IF NOT EXISTS `tb_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(255) NOT NULL COMMENT '供应商代码',
  `name` varchar(255) NOT NULL COMMENT '供应商名称',
  `name_short` varchar(50) DEFAULT NULL COMMENT '供应商简称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '供应商英文名称',
  `type` smallint DEFAULT NULL COMMENT '供应商类型',
  `province` varchar(255) DEFAULT NULL COMMENT '省',
  `city` varchar(255) DEFAULT NULL COMMENT '市',
  `county` varchar(255) DEFAULT NULL COMMENT '区',
  `subdistrict` varchar(255) DEFAULT NULL COMMENT '街道',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `zipcode` varchar(20) DEFAULT NULL COMMENT '邮编',
  `fax` varchar(50) DEFAULT NULL COMMENT '供应商传真',
  `tel` varchar(50) DEFAULT NULL COMMENT '供应商电话',
  `website` varchar(255) DEFAULT NULL COMMENT '供应商网站',
  `email` varchar(255) DEFAULT NULL COMMENT '供应商邮箱',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '供应商联系人',
  `contact_person_tel` varchar(50) DEFAULT NULL COMMENT '供应商联系人电话',
  `legal_person` varchar(50) DEFAULT NULL COMMENT '供应商法人',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '供应商银行',
  `account_no` varchar(50) DEFAULT NULL COMMENT '供应商账号',
  `tax_no` varchar(50) DEFAULT NULL COMMENT '供应商税号',
  `enable` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` bigint DEFAULT NULL COMMENT '更新者',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `row_version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `row_valid` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='供应商表';

-- 车辆激活信息表
CREATE TABLE IF NOT EXISTS `tb_veh_activation` (
  `id` bigint NOT NULL COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `type` varchar(100) NOT NULL COMMENT '激活类型',
  `sn` varchar(255) DEFAULT NULL COMMENT '序列号',
  `status` int NOT NULL COMMENT '激活状态',
  `activation_time` datetime DEFAULT NULL COMMENT '激活时间',
  `expiration_time` datetime DEFAULT NULL COMMENT '过期时间',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` bigint DEFAULT NULL COMMENT '更新者',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `row_version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `valid` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车辆激活信息表';

-- 车辆基础车型表
CREATE TABLE IF NOT EXISTS `tb_veh_base_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `model_code` varchar(255) NOT NULL COMMENT '车型代码',
  `code` varchar(255) NOT NULL COMMENT '基础车型代码',
  `name` varchar(255) NOT NULL COMMENT '基础车型名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '基础车型英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆基础车型表';

-- 车辆基础车型特征值关系表
CREATE TABLE IF NOT EXISTS `tb_veh_base_model_feature_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `base_model_code` varchar(255) NOT NULL COMMENT '基础车型代码',
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
  KEY `idx_basic_model` (`base_model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆基础车型特征值关系表';

-- 车辆基础信息表
CREATE TABLE IF NOT EXISTS `tb_veh_basic_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `manufacturer_code` varchar(255) NOT NULL COMMENT '工厂代码',
  `brand_code` varchar(255) NOT NULL COMMENT '品牌代码',
  `platform_code` varchar(255) NOT NULL COMMENT '平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `model_code` varchar(255) NOT NULL COMMENT '车型代码',
  `base_model_code` varchar(255) NOT NULL COMMENT '基础车型代码',
  `build_config_code` varchar(255) NOT NULL COMMENT '生产配置代码',
  `eol_time` timestamp NULL DEFAULT NULL COMMENT '车辆下线时间',
  `pdi_time` timestamp NULL DEFAULT NULL COMMENT '最后一次PDI时间',
  `order_num` varchar(50) DEFAULT NULL COMMENT '订单编码',
  `vehicle_base_version` varchar(255) DEFAULT NULL COMMENT '整车基线版本',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `vin` (`vin`),
  KEY `idx_order_num` (`order_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆基础信息表';

-- 车辆品牌表
CREATE TABLE IF NOT EXISTS `tb_veh_brand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(255) NOT NULL COMMENT '品牌代码',
  `name` varchar(255) NOT NULL COMMENT '品牌名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '品牌英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆品牌表';

-- 车辆生产配置表（V1迁移后：特征字段已迁移至tb_veh_build_config_feature_code）
CREATE TABLE IF NOT EXISTS `tb_veh_build_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `model_code` varchar(255) NOT NULL COMMENT '车型代码',
  `base_model_code` varchar(255) NOT NULL COMMENT '基础车型代码',
  `code` varchar(255) NOT NULL COMMENT '生产配置代码',
  `name` varchar(255) NOT NULL COMMENT '生产配置名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '车型配置英文名称',
  `vehicle_stage_code` varchar(50) DEFAULT NULL COMMENT '车辆阶段代码',
  `enable` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '99' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆生产配置表';

-- 车辆生产配置特征值关系表（V1新增）
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

-- 车辆详细信息表
CREATE TABLE IF NOT EXISTS `tb_veh_detail_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `type` varchar(50) NOT NULL COMMENT '分类',
  `val` varchar(255) DEFAULT NULL COMMENT '值',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆详细信息表';

-- 车辆ECU表
CREATE TABLE IF NOT EXISTS `tb_veh_ecu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(255) NOT NULL COMMENT 'ECU代码',
  `name` varchar(255) NOT NULL COMMENT 'ECU名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT 'ECU英文名称',
  `func_domain` varchar(255) NOT NULL COMMENT '功能域',
  `node_type` varchar(255) NOT NULL COMMENT '节点类型',
  `diag_eth` varchar(255) DEFAULT NULL COMMENT '诊断口',
  `burn_eth` varchar(255) DEFAULT NULL COMMENT '刷写口',
  `ota` tinyint NOT NULL DEFAULT '0' COMMENT '是否支持OTA',
  `config_word` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要读写配置字',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` bigint DEFAULT NULL COMMENT '更新者',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `row_version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `valid` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车辆ECU表';

-- 车辆外饰表
CREATE TABLE IF NOT EXISTS `tb_veh_exterior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '车辆平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(100) NOT NULL COMMENT '外饰代码',
  `name` varchar(255) NOT NULL COMMENT '外饰名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '外饰英文名称',
  `enable` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '99' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆外饰表';

-- 车辆特征值表
CREATE TABLE IF NOT EXISTS `tb_veh_feature_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_code` varchar(10) NOT NULL COMMENT '特征族代码',
  `code` varchar(10) NOT NULL COMMENT '特征值代码',
  `name` varchar(255) NOT NULL COMMENT '特征值名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '特征值英文名称',
  `val` varchar(255) DEFAULT NULL COMMENT '特征值代表值',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_family` (`family_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆特征值表';

-- 车辆特征族表
CREATE TABLE IF NOT EXISTS `tb_veh_feature_family` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(10) NOT NULL COMMENT '特征族代码',
  `name` varchar(255) NOT NULL COMMENT '特征族名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '特征族英文名称',
  `type` varchar(50) DEFAULT NULL COMMENT '特征族分类',
  `mandatory` tinyint DEFAULT NULL COMMENT '是否强制',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆特征族表';

-- 车辆导入数据表
CREATE TABLE IF NOT EXISTS `tb_veh_import_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_num` varchar(255) NOT NULL COMMENT '批次号',
  `type` varchar(100) NOT NULL COMMENT '数据类型',
  `version` varchar(100) NOT NULL COMMENT '数据版本',
  `data` text NOT NULL COMMENT '车辆导入数据',
  `handle` tinyint DEFAULT '0' COMMENT '是否处理',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_num` (`batch_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆导入数据表';

-- 车辆内饰表
CREATE TABLE IF NOT EXISTS `tb_veh_interior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '车辆平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(100) NOT NULL COMMENT '内饰代码',
  `name` varchar(255) NOT NULL COMMENT '内饰名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '内饰英文名称',
  `enable` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '99' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆内饰表';

-- 车辆生命周期表
CREATE TABLE IF NOT EXISTS `tb_veh_lifecycle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `node` varchar(255) NOT NULL COMMENT '生命周期节点',
  `reach_time` timestamp NULL DEFAULT NULL COMMENT '触达时间',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vin_node` (`vin`, `node`),
  KEY `idx_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆生命周期表';

-- 车辆生产厂商表
CREATE TABLE IF NOT EXISTS `tb_veh_manufacturer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(255) NOT NULL COMMENT '工厂代码',
  `name` varchar(255) NOT NULL COMMENT '工厂名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '工厂英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆生产厂商表';

-- 车辆车型表
CREATE TABLE IF NOT EXISTS `tb_veh_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(255) NOT NULL COMMENT '车型代码',
  `name` varchar(255) NOT NULL COMMENT '车型名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '车型英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆车型表';

-- 车辆选装表
CREATE TABLE IF NOT EXISTS `tb_veh_optional` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(100) NOT NULL COMMENT '选装代码',
  `name` varchar(255) NOT NULL COMMENT '选装名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '选装英文名称',
  `enable` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '99' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆选装表';

-- 车辆平台表
CREATE TABLE IF NOT EXISTS `tb_veh_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(255) NOT NULL COMMENT '平台代码',
  `name` varchar(255) NOT NULL COMMENT '平台名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '平台英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆平台表';

-- 车辆预设车主表
CREATE TABLE IF NOT EXISTS `tb_veh_preset_owner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `real_name` varchar(100) DEFAULT NULL COMMENT '车主真实姓名',
  `country_region_code` varchar(20) NOT NULL COMMENT '手机所属国家或地区',
  `mobile` varchar(15) NOT NULL COMMENT '手机号',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆预设车主表';

-- 车辆车系表（V2迁移后：platform_code已移除，新增brand_code）
CREATE TABLE IF NOT EXISTS `tb_veh_series` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `brand_code` varchar(32) DEFAULT NULL COMMENT '品牌代码',
  `code` varchar(255) NOT NULL COMMENT '车系代码',
  `name` varchar(255) NOT NULL COMMENT '车系名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '车系英文名称',
  `enable` tinyint NOT NULL COMMENT '是否启用',
  `sort` int NOT NULL COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_series_brand_code` (`brand_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆车系表';

-- 车辆用户表
CREATE TABLE IF NOT EXISTS `tb_veh_user` (
  `id` bigint NOT NULL COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `uid` varchar(100) NOT NULL COMMENT '用户ID',
  `vehicle_nickname` varchar(20) DEFAULT NULL COMMENT '车辆昵称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` bigint DEFAULT NULL COMMENT '更新者',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `row_version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `valid` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车辆用户表';

-- 车辆车轮表
CREATE TABLE IF NOT EXISTS `tb_veh_wheel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(255) NOT NULL COMMENT '车辆平台代码',
  `series_code` varchar(255) NOT NULL COMMENT '车系代码',
  `code` varchar(100) NOT NULL COMMENT '车轮代码',
  `name` varchar(255) NOT NULL COMMENT '车轮名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '车轮英文名称',
  `enable` tinyint DEFAULT '1' COMMENT '是否启用',
  `sort` int DEFAULT '99' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆车轮表';

-- 车辆配置表
CREATE TABLE IF NOT EXISTS `tb_vehicle_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `version` varchar(64) NOT NULL COMMENT '配置版本',
  `state` varchar(20) NOT NULL COMMENT '配置状态',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆配置表';

-- 车辆配置项表
CREATE TABLE IF NOT EXISTS `tb_vehicle_config_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `version` varchar(64) NOT NULL COMMENT '配置版本',
  `config_item_code` varchar(50) NOT NULL COMMENT '配置项代码',
  `config_item_value` varchar(255) NOT NULL COMMENT '配置项值',
  `source_value` varchar(255) NOT NULL COMMENT '源系统值',
  `source_system` varchar(50) NOT NULL COMMENT '源系统',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆配置项表';

-- 车辆零件表
CREATE TABLE IF NOT EXISTS `tb_vehicle_part` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pn` varchar(20) NOT NULL COMMENT '零件编号',
  `vin` varchar(20) DEFAULT NULL COMMENT '车架号',
  `device_code` varchar(20) DEFAULT NULL COMMENT '设备代码',
  `device_item` varchar(20) DEFAULT NULL COMMENT '设备项',
  `sn` varchar(255) DEFAULT NULL COMMENT '零件序列号',
  `config_word` varchar(255) DEFAULT NULL COMMENT '配置字',
  `supplier_code` varchar(255) DEFAULT NULL COMMENT '供应商编码',
  `batch_num` varchar(255) DEFAULT NULL COMMENT '批次号',
  `hardware_ver` varchar(255) DEFAULT NULL COMMENT '硬件版本号',
  `software_ver` varchar(255) DEFAULT NULL COMMENT '软件版本号',
  `hardware_pn` varchar(255) DEFAULT NULL COMMENT '硬件零件号',
  `software_pn` varchar(255) DEFAULT NULL COMMENT '软件零件号',
  `extra` text COMMENT '附加信息',
  `bind_time` timestamp NULL DEFAULT NULL COMMENT '绑定时间',
  `bind_type` varchar(20) DEFAULT NULL COMMENT '绑定类型',
  `bind_by` varchar(64) DEFAULT NULL COMMENT '绑定者',
  `bind_org` varchar(64) DEFAULT NULL COMMENT '绑定机构',
  `unbind_time` timestamp NULL DEFAULT NULL COMMENT '解绑时间',
  `unbind_reason` varchar(50) DEFAULT NULL COMMENT '解绑理由',
  `unbind_by` varchar(64) DEFAULT NULL COMMENT '解绑者',
  `unbind_org` varchar(64) DEFAULT NULL COMMENT '解绑机构',
  `part_state` smallint DEFAULT NULL COMMENT '零件状态：1-在用，2-待更换，3-已报废',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_vin` (`vin`),
  KEY `idx_pn` (`pn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆零件表';

-- 车辆零件变更历史表
CREATE TABLE IF NOT EXISTS `tb_vehicle_part_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pn` varchar(20) NOT NULL COMMENT '零件编号',
  `vin` varchar(20) DEFAULT NULL COMMENT '车架号',
  `device_code` varchar(20) DEFAULT NULL COMMENT '设备代码',
  `device_item` varchar(20) DEFAULT NULL COMMENT '设备项',
  `sn` varchar(255) DEFAULT NULL COMMENT '零件序列号',
  `config_word` varchar(255) DEFAULT NULL COMMENT '配置字',
  `supplier_code` varchar(255) DEFAULT NULL COMMENT '供应商编码',
  `batch_num` varchar(255) DEFAULT NULL COMMENT '批次号',
  `hardware_ver` varchar(255) DEFAULT NULL COMMENT '硬件版本号',
  `software_ver` varchar(255) DEFAULT NULL COMMENT '软件版本号',
  `hardware_pn` varchar(255) DEFAULT NULL COMMENT '硬件零件号',
  `software_pn` varchar(255) DEFAULT NULL COMMENT '软件零件号',
  `extra` text COMMENT '附加信息',
  `bind_time` timestamp NULL DEFAULT NULL COMMENT '绑定时间',
  `bind_type` varchar(20) DEFAULT NULL COMMENT '绑定类型',
  `bind_by` varchar(64) DEFAULT NULL COMMENT '绑定者',
  `bind_org` varchar(64) DEFAULT NULL COMMENT '绑定机构',
  `unbind_time` timestamp NULL DEFAULT NULL COMMENT '解绑时间',
  `unbind_reason` varchar(50) DEFAULT NULL COMMENT '解绑理由',
  `unbind_by` varchar(64) DEFAULT NULL COMMENT '解绑者',
  `unbind_org` varchar(64) DEFAULT NULL COMMENT '解绑机构',
  `part_state` smallint DEFAULT NULL COMMENT '零件状态：1-在用，2-待更换，3-已报废',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `modify_by` varchar(64) DEFAULT NULL COMMENT '修改者',
  `row_version` int DEFAULT '1' COMMENT '记录版本',
  `row_valid` tinyint DEFAULT '1' COMMENT '记录是否有效',
  PRIMARY KEY (`id`),
  KEY `idx_vin` (`vin`),
  KEY `idx_pn` (`pn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆零件变更历史表';

-- 车型配置车身颜色关系表
CREATE TABLE IF NOT EXISTS `tr_veh_model_config_body_color` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_config_code` varchar(255) NOT NULL COMMENT '车型配置代码',
  `body_color_code` varchar(255) NOT NULL COMMENT '车身颜色代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车型配置车身颜色关系表';

-- 车型配置轮毂关系表
CREATE TABLE IF NOT EXISTS `tr_veh_model_config_hub` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_config_code` varchar(255) NOT NULL COMMENT '车型配置代码',
  `hub_code` varchar(255) NOT NULL COMMENT '轮毂代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车型配置轮毂关系表';

-- 车型配置内饰颜色关系表
CREATE TABLE IF NOT EXISTS `tr_veh_model_config_interior_color` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_config_code` varchar(255) NOT NULL COMMENT '车型配置代码',
  `interior_color_code` varchar(255) NOT NULL COMMENT '内饰颜色代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车型配置内饰颜色关系表';

-- 车型配置选装关系表
CREATE TABLE IF NOT EXISTS `tr_veh_model_config_optional` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_config_code` varchar(255) NOT NULL COMMENT '车型配置代码',
  `optional_code` varchar(255) NOT NULL COMMENT '选装代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车型配置选装关系表';

-- 车辆用户关系表
CREATE TABLE IF NOT EXISTS `tr_veh_user_relation` (
  `id` bigint NOT NULL COMMENT '主键',
  `vin` varchar(20) NOT NULL COMMENT '车架号',
  `uid` varchar(100) NOT NULL COMMENT '用户ID',
  `relation` int NOT NULL COMMENT '人车关系',
  `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_by` bigint DEFAULT NULL COMMENT '更新者',
  `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `row_version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `valid` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='车辆用户关系表';
