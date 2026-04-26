DROP TABLE IF EXISTS `db_vmd`.`tb_veh_basic_info`;
CREATE TABLE `db_vmd`.`tb_veh_basic_info`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                  VARCHAR(20)  NOT NULL COMMENT '车架号',
    `manufacturer_code`    VARCHAR(255) NOT NULL COMMENT '工厂代码',
    `brand_code`           VARCHAR(255) NOT NULL COMMENT '品牌代码',
    `platform_code`        VARCHAR(255) NOT NULL COMMENT '平台代码',
    `series_code`          VARCHAR(255) NOT NULL COMMENT '车系代码',
    `model_code`           VARCHAR(255) NOT NULL COMMENT '车型代码',
    `base_model_code`      VARCHAR(255) NOT NULL COMMENT '基础车型代码',
    `build_config_code`    VARCHAR(255) NOT NULL COMMENT '生产配置代码',
    `eol_time`             TIMESTAMP             DEFAULT NULL COMMENT '车辆下线时间',
    `pdi_time`             TIMESTAMP             DEFAULT NULL COMMENT '最后一次PDI时间',
    `order_num`            VARCHAR(50)           DEFAULT NULL COMMENT '订单编码',
    `vehicle_base_version` VARCHAR(255)          DEFAULT NULL COMMENT '整车基线版本',
    `description`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`            VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`          INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`            TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`vin`),
    INDEX `idx_order_num` (`order_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆基础信息表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_detail_info`;
CREATE TABLE `db_vmd`.`tb_veh_detail_info`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`         VARCHAR(20) NOT NULL COMMENT '车架号',
    `type`        VARCHAR(50) NOT NULL COMMENT '分类',
    `val`         VARCHAR(255)         DEFAULT NULL COMMENT '值',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version` INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆详细信息表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_manufacturer`;
CREATE TABLE `db_vmd`.`tb_veh_manufacturer`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        VARCHAR(255) NOT NULL COMMENT '工厂代码',
    `name`        VARCHAR(255) NOT NULL COMMENT '工厂名称',
    `name_en`     VARCHAR(255)          DEFAULT NULL COMMENT '工厂英文名称',
    `enable`      TINYINT      NOT NULL COMMENT '是否启用',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆生产厂商表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_brand`;
CREATE TABLE `db_vmd`.`tb_veh_brand`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        VARCHAR(255) NOT NULL COMMENT '品牌代码',
    `name`        VARCHAR(255) NOT NULL COMMENT '品牌名称',
    `name_en`     VARCHAR(255)          DEFAULT NULL COMMENT '品牌英文名称',
    `enable`      TINYINT      NOT NULL COMMENT '是否启用',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆品牌表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_platform`;
CREATE TABLE `db_vmd`.`tb_veh_platform`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        VARCHAR(255) NOT NULL COMMENT '平台代码',
    `name`        VARCHAR(255) NOT NULL COMMENT '平台名称',
    `name_en`     VARCHAR(255)          DEFAULT NULL COMMENT '平台英文名称',
    `enable`      TINYINT      NOT NULL COMMENT '是否启用',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆平台表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_series`;
CREATE TABLE `db_vmd`.`tb_veh_series`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `platform_code` VARCHAR(255) NOT NULL COMMENT '平台代码',
    `code`          VARCHAR(255) NOT NULL COMMENT '车系代码',
    `name`          VARCHAR(255) NOT NULL COMMENT '车系名称',
    `name_en`       VARCHAR(255)          DEFAULT NULL COMMENT '车系英文名称',
    `enable`        TINYINT      NOT NULL COMMENT '是否启用',
    `sort`          INT          NOT NULL COMMENT '排序',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆车系表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_model`;
CREATE TABLE `db_vmd`.`tb_veh_model`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `platform_code` VARCHAR(255) NOT NULL COMMENT '平台代码',
    `series_code`   VARCHAR(255) NOT NULL COMMENT '车系代码',
    `code`          VARCHAR(255) NOT NULL COMMENT '车型代码',
    `name`          VARCHAR(255) NOT NULL COMMENT '车型名称',
    `name_en`       VARCHAR(255)          DEFAULT NULL COMMENT '车型英文名称',
    `enable`        TINYINT      NOT NULL COMMENT '是否启用',
    `sort`          INT          NOT NULL COMMENT '排序',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆车型表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_base_model`;
CREATE TABLE `db_vmd`.`tb_veh_base_model`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `platform_code` VARCHAR(255) NOT NULL COMMENT '平台代码',
    `series_code`   VARCHAR(255) NOT NULL COMMENT '车系代码',
    `model_code`    VARCHAR(255) NOT NULL COMMENT '车型代码',
    `code`          VARCHAR(255) NOT NULL COMMENT '基础车型代码',
    `name`          VARCHAR(255) NOT NULL COMMENT '基础车型名称',
    `name_en`       VARCHAR(255)          DEFAULT NULL COMMENT '基础车型英文名称',
    `enable`        TINYINT      NOT NULL COMMENT '是否启用',
    `sort`          INT          NOT NULL COMMENT '排序',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆基础车型表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_feature_family`;
CREATE TABLE `db_vmd`.`tb_veh_feature_family`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        VARCHAR(10)  NOT NULL COMMENT '特征族代码',
    `name`        VARCHAR(255) NOT NULL COMMENT '特征族名称',
    `name_en`     VARCHAR(255)          DEFAULT NULL COMMENT '特征族英文名称',
    `type`        VARCHAR(50)           DEFAULT NULL COMMENT '特征族分类',
    `mandatory`   TINYINT               DEFAULT NULL COMMENT '是否强制',
    `enable`      TINYINT      NOT NULL COMMENT '是否启用',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆特征族表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_feature_code`;
CREATE TABLE `db_vmd`.`tb_veh_feature_code`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `family_code` VARCHAR(10)  NOT NULL COMMENT '特征族代码',
    `code`        VARCHAR(10)  NOT NULL COMMENT '特征值代码',
    `name`        VARCHAR(255) NOT NULL COMMENT '特征值名称',
    `name_en`     VARCHAR(255)          DEFAULT NULL COMMENT '特征值英文名称',
    `val`         VARCHAR(255)          DEFAULT NULL COMMENT '特征值代表值',
    `enable`      TINYINT      NOT NULL COMMENT '是否启用',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`),
    INDEX `idx_family` (`family_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆特征值表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_base_model_feature_code`;
CREATE TABLE `db_vmd`.`tb_veh_base_model_feature_code`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `base_model_code` VARCHAR(255) NOT NULL COMMENT '基础车型代码',
    `family_code`     VARCHAR(255) NOT NULL COMMENT '特征族代码',
    `feature_code`    VARCHAR(255) NOT NULL COMMENT '特征值代码',
    `feature_type`    VARCHAR(20)           DEFAULT NULL COMMENT '特征值类型',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`       VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`       VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`     INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`       TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_base_model` (`base_model_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆基础车型特征值关系表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_build_config`;
CREATE TABLE `db_vmd`.`tb_veh_build_config`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `platform_code`      VARCHAR(255) NOT NULL COMMENT '平台代码',
    `series_code`        VARCHAR(255) NOT NULL COMMENT '车系代码',
    `model_code`         VARCHAR(255) NOT NULL COMMENT '车型代码',
    `base_model_code`    VARCHAR(255) NOT NULL COMMENT '基础车型代码',
    `code`               VARCHAR(255) NOT NULL COMMENT '生产配置代码',
    `name`               VARCHAR(255) NOT NULL COMMENT '生产配置名称',
    `name_en`            VARCHAR(255)          DEFAULT NULL COMMENT '车型配置英文名称',
    `vehicle_stage_code` VARCHAR(50)           DEFAULT NULL COMMENT '车辆阶段代码',
    `exterior_code`      VARCHAR(50)           DEFAULT NULL COMMENT '外饰代码',
    `interior_code`      VARCHAR(50)           DEFAULT NULL COMMENT '内饰代码',
    `wheel_code`         VARCHAR(50)           DEFAULT NULL COMMENT '轮毂代码',
    `tire_code`          VARCHAR(50)           DEFAULT NULL COMMENT '轮胎代码',
    `spare_tire_code`    VARCHAR(50)           DEFAULT NULL COMMENT '备胎代码',
    `adas_code`          VARCHAR(50)           DEFAULT NULL COMMENT '智驾代码',
    `seat_code`          VARCHAR(50)           DEFAULT NULL COMMENT '座椅代码',
    `enable`             TINYINT               DEFAULT 1 COMMENT '是否启用',
    `sort`               INT                   DEFAULT 99 COMMENT '排序',
    `description`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`          VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`        INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`          TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆生产配置表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_lifecycle`;
CREATE TABLE `db_vmd`.`tb_veh_lifecycle`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`         VARCHAR(20)  NOT NULL COMMENT '车架号',
    `node`        VARCHAR(255) NOT NULL COMMENT '生命周期节点',
    `reach_time`  TIMESTAMP             DEFAULT NULL COMMENT '触达时间',
    `sort`        INT          NOT NULL COMMENT '排序',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆生命周期表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_preset_owner`;
CREATE TABLE `db_vmd`.`tb_veh_preset_owner`
(
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`                 VARCHAR(20) NOT NULL COMMENT '车架号',
    `real_name`           VARCHAR(100)         DEFAULT NULL COMMENT '车主真实姓名',
    `country_region_code` VARCHAR(20) NOT NULL COMMENT '手机所属国家或地区',
    `mobile`              VARCHAR(15) NOT NULL COMMENT '手机号',
    `description`         VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`           VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`           VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`         INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`           TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆预设车主表';

DROP TABLE IF EXISTS `db_vmd`.`tb_veh_import_data`;
CREATE TABLE `db_vmd`.`tb_veh_import_data`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `batch_num`   VARCHAR(255) NOT NULL COMMENT '批次号',
    `type`        VARCHAR(100) NOT NULL COMMENT '数据类型',
    `version`     VARCHAR(100) NOT NULL COMMENT '数据版本',
    `data`        TEXT         NOT NULL COMMENT '车辆导入数据',
    `handle`      TINYINT               DEFAULT 0 COMMENT '是否处理',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`batch_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆导入数据表';

DROP TABLE IF EXISTS `db_vmd`.`tb_supplier`;
CREATE TABLE `db_vmd`.`tb_supplier`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`               VARCHAR(255) NOT NULL COMMENT '供应商代码',
    `name`               VARCHAR(255) NOT NULL COMMENT '供应商名称',
    `name_short`         VARCHAR(255)          DEFAULT NULL COMMENT '供应商名称缩写',
    `name_en`            VARCHAR(255)          DEFAULT NULL COMMENT '供应商英文名称',
    `type`               SMALLINT              DEFAULT NULL COMMENT '供应商类型',
    `province`           VARCHAR(255)          DEFAULT NULL COMMENT '省',
    `city`               VARCHAR(255)          DEFAULT NULL COMMENT '市',
    `county`             VARCHAR(255)          DEFAULT NULL COMMENT '区',
    `subdistrict`        VARCHAR(255)          DEFAULT NULL COMMENT '街道',
    `address`            VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    `zipcode`            VARCHAR(20)           DEFAULT NULL COMMENT '邮编',
    `fax`                VARCHAR(50)           DEFAULT NULL COMMENT '供应商传真',
    `tel`                VARCHAR(50)           DEFAULT NULL COMMENT '供应商电话',
    `website`            VARCHAR(255)          DEFAULT NULL COMMENT '供应商网站',
    `email`              VARCHAR(255)          DEFAULT NULL COMMENT '供应商邮箱',
    `contact_person`     VARCHAR(255)          DEFAULT NULL COMMENT '联系人',
    `contact_person_tel` VARCHAR(50)           DEFAULT NULL COMMENT '联系人电话',
    `legal_person`       VARCHAR(255)          DEFAULT NULL COMMENT '法人',
    `bank_name`          VARCHAR(255)          DEFAULT NULL COMMENT '供应商银行',
    `account_no`         VARCHAR(255)          DEFAULT NULL COMMENT '供应商账号',
    `tax_no`             VARCHAR(255)          DEFAULT NULL COMMENT '供应商税号',
    `enable`             TINYINT      NOT NULL COMMENT '是否启用',
    `sort`               INT          NOT NULL COMMENT '排序',
    `description`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`          VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`        INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`          TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='供应商表';

DROP TABLE IF EXISTS `db_vmd`.`tb_device`;
CREATE TABLE `db_vmd`.`tb_device`
(
    `id`                             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`                           VARCHAR(20)  NOT NULL COMMENT '设备编码',
    `name`                           VARCHAR(255) NOT NULL COMMENT '设备名称',
    `name_en`                        VARCHAR(255)          DEFAULT NULL COMMENT '设备英文名称',
    `type`                           VARCHAR(20)  NOT NULL COMMENT '设备类型',
    `device_item`                    VARCHAR(20)  NOT NULL COMMENT '设备项',
    `func_domain`                    VARCHAR(20)  NOT NULL COMMENT '功能域',
    `node_type`                      VARCHAR(100) NOT NULL COMMENT '节点类型',
    `ota_support`                    VARCHAR(20)  NOT NULL COMMENT 'OTA支持类型',
    `partition_type`                 VARCHAR(20)           DEFAULT NULL COMMENT '分区类型',
    `lock_unlock_security_component` SMALLINT              DEFAULT NULL COMMENT '解闭锁安全件：0-无，1-包含解锁安全件，2-包含闭锁安全件，3-包含解/闭锁安全件',
    `link_config_source`             VARCHAR(50)           DEFAULT NULL COMMENT '链路配置源',
    `link_flash_target`              VARCHAR(50)           DEFAULT NULL COMMENT '链路生效目标',
    `comm_protocol`                  VARCHAR(50)           DEFAULT NULL COMMENT '通信协议',
    `flash_protocol`                 VARCHAR(50)           DEFAULT NULL COMMENT '刷写协议',
    `can_tx_id`                      VARCHAR(20)           DEFAULT NULL COMMENT 'CAN/CANFD总线发送标识',
    `can_rx_id`                      VARCHAR(20)           DEFAULT NULL COMMENT 'CAN/CANFD总线接收标识',
    `ethernet_ip`                    VARCHAR(20)           DEFAULT NULL COMMENT '以太网的业务IP',
    `doip_gateway_id`                VARCHAR(20)           DEFAULT NULL COMMENT 'DoIP协议网关标识',
    `doip_entity_id`                 VARCHAR(20)           DEFAULT NULL COMMENT 'DoIP协议设备标识',
    `core`                           TINYINT               DEFAULT 0 COMMENT '是否核心设备',
    `sort`                           INT          NOT NULL COMMENT '排序',
    `description`                    VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`                      VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`                      VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`                    INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`                      TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备信息表';


DROP TABLE IF EXISTS `db_vmd`.`tb_part`;
CREATE TABLE `db_vmd`.`tb_part`
(
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pn`                     VARCHAR(20)  NOT NULL COMMENT '零件号',
    `name`                   VARCHAR(255) NOT NULL COMMENT '零件中文名称',
    `name_en`                VARCHAR(255)          DEFAULT NULL COMMENT '零件英文名称',
    `type`                   VARCHAR(20)           DEFAULT NULL COMMENT '零件类型',
    `ffa`                    VARCHAR(20)           DEFAULT NULL COMMENT '零件分类',
    `status`                 VARCHAR(20)           DEFAULT NULL COMMENT '零件状态：PRODUCTION-量产，TRIAL-试生产，DISCONTINUE-停用',
    `digital_model`          VARCHAR(20)           DEFAULT NULL COMMENT '数字模型：NO-无，PART-零件级，PRODUCT-总成级',
    `unit`                   VARCHAR(10)           DEFAULT NULL COMMENT '单位：EA-个，KG-千克，ML-毫升，M2-平方米，M-米',
    `frame_part`             TINYINT               DEFAULT 0 COMMENT '是否是架构件',
    `nature_part`            TINYINT               DEFAULT 0 COMMENT '是否是本色件',
    `color_area`             VARCHAR(20)           DEFAULT NULL COMMENT '颜色区域：INTERNAL-内部，EXTERNAL-外部',
    `nature_pn`              VARCHAR(20)           DEFAULT NULL COMMENT '本色件零件号',
    `regulatory_part`        TINYINT               DEFAULT 0 COMMENT '是否是法规件',
    `key_part`               VARCHAR(20)           DEFAULT NULL COMMENT '关键程度：KEY-关键，MAJOR-主要，SIMPLE-普通',
    `accurately_traced`      TINYINT               DEFAULT 0 COMMENT '是否精准追溯',
    `aftersale_part`         TINYINT               DEFAULT 0 COMMENT '是否是配件',
    `standard_part_class`    VARCHAR(20)           DEFAULT NULL COMMENT '标准件分类',
    `wrench_type`            VARCHAR(20)           DEFAULT NULL COMMENT '扳拧形式',
    `rod_type`               VARCHAR(20)           DEFAULT NULL COMMENT '杆部形式',
    `head_shape`             VARCHAR(20)           DEFAULT NULL COMMENT '头部形状',
    `end_shape`              VARCHAR(20)           DEFAULT NULL COMMENT '末端形状',
    `washer`                 TINYINT               DEFAULT 0 COMMENT '是否带垫圈',
    `washer_type`            VARCHAR(20)           DEFAULT NULL COMMENT '垫圈类型',
    `diameter`               VARCHAR(20)           DEFAULT NULL COMMENT '直径',
    `length`                 VARCHAR(20)           DEFAULT NULL COMMENT '长度',
    `pitch`                  VARCHAR(20)           DEFAULT NULL COMMENT '螺距',
    `dental_form`            VARCHAR(20)           DEFAULT NULL COMMENT '牙型',
    `strength_grade`         VARCHAR(20)           DEFAULT NULL COMMENT '强度等级',
    `mechanical_property`    VARCHAR(20)           DEFAULT NULL COMMENT '机械性能',
    `surface_treatment`      VARCHAR(20)           DEFAULT NULL COMMENT '表面处理',
    `structure_character`    VARCHAR(20)           DEFAULT NULL COMMENT '结构特征',
    `device_form`            VARCHAR(20)           DEFAULT NULL COMMENT '设备形态',
    `device_code`            VARCHAR(20)           DEFAULT NULL COMMENT '设备代码',
    `designer`               VARCHAR(20)           DEFAULT NULL COMMENT '设计工程师',
    `designer_dept`          VARCHAR(20)           DEFAULT NULL COMMENT '设计工程师部门',
    `non_repair_reason`      VARCHAR(20)           DEFAULT NULL COMMENT '不作为备件原因',
    `color_repair`           TINYINT               DEFAULT 0 COMMENT '是否颜色件维修',
    `primer_repair`          TINYINT               DEFAULT 0 COMMENT '是否底漆件维修',
    `electrophoresis_repair` TINYINT               DEFAULT 0 COMMENT '是否电泳件维修',
    `production_code`        VARCHAR(20)           DEFAULT NULL COMMENT '对应生产件号',
    `spare_property`         VARCHAR(20)           DEFAULT NULL COMMENT '售后配件属性',
    `sale_note`              VARCHAR(20)           DEFAULT NULL COMMENT '售后备注',
    `first_production_date`  VARCHAR(20)           DEFAULT NULL COMMENT '首次投产时间',
    `initial_model`          VARCHAR(20)           DEFAULT NULL COMMENT '初始车型',
    `description`            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`              VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`              VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`            INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`              TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='零件信息表';

DROP TABLE IF EXISTS `db_vmd`.`tb_vehicle_part`;
CREATE TABLE `db_vmd`.`tb_vehicle_part`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pn`            VARCHAR(20) NOT NULL COMMENT '零件编号',
    `vin`           VARCHAR(20)          DEFAULT NULL COMMENT '车架号',
    `device_code`   VARCHAR(20)          DEFAULT NULL COMMENT '设备代码',
    `device_item`   VARCHAR(20)          DEFAULT NULL COMMENT '设备项',
    `sn`            VARCHAR(255)         DEFAULT NULL COMMENT '零件序列号',
    `config_word`   VARCHAR(255)         DEFAULT NULL COMMENT '配置字',
    `supplier_code` VARCHAR(255)         DEFAULT NULL COMMENT '供应商编码',
    `batch_num`     VARCHAR(255)         DEFAULT NULL COMMENT '批次号',
    `hardware_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '软件版本号',
    `hardware_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '硬件零件号',
    `software_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '软件零件号',
    `extra`         TEXT                 DEFAULT NULL COMMENT '附加信息',
    `bind_time`     TIMESTAMP   NULL     DEFAULT NULL COMMENT '绑定时间',
    `bind_type`     VARCHAR(20)          DEFAULT NULL COMMENT '绑定类型',
    `bind_by`       VARCHAR(64)          DEFAULT NULL COMMENT '绑定者',
    `bind_org`      VARCHAR(64)          DEFAULT NULL COMMENT '绑定机构',
    `unbind_time`   TIMESTAMP   NULL     DEFAULT NULL COMMENT '解绑时间',
    `unbind_reason` VARCHAR(50)          DEFAULT NULL COMMENT '解绑理由',
    `unbind_by`     VARCHAR(64)          DEFAULT NULL COMMENT '解绑者',
    `unbind_org`    VARCHAR(64)          DEFAULT NULL COMMENT '解绑机构',
    `part_state`    SMALLINT             DEFAULT NULL COMMENT '零件状态：1-在用，2-待更换，3-已报废',
    `description`   VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`),
    INDEX `idx_pn` (`pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零件表';

DROP TABLE IF EXISTS `db_vmd`.`tb_vehicle_part_history`;
CREATE TABLE `db_vmd`.`tb_vehicle_part_history`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pn`            VARCHAR(20) NOT NULL COMMENT '零件编号',
    `vin`           VARCHAR(20)          DEFAULT NULL COMMENT '车架号',
    `device_code`   VARCHAR(20)          DEFAULT NULL COMMENT '设备代码',
    `device_item`   VARCHAR(20)          DEFAULT NULL COMMENT '设备项',
    `sn`            VARCHAR(255)         DEFAULT NULL COMMENT '零件序列号',
    `config_word`   VARCHAR(255)         DEFAULT NULL COMMENT '配置字',
    `supplier_code` VARCHAR(255)         DEFAULT NULL COMMENT '供应商编码',
    `batch_num`     VARCHAR(255)         DEFAULT NULL COMMENT '批次号',
    `hardware_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '硬件版本号',
    `software_ver`  VARCHAR(255)         DEFAULT NULL COMMENT '软件版本号',
    `hardware_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '硬件零件号',
    `software_pn`   VARCHAR(255)         DEFAULT NULL COMMENT '软件零件号',
    `extra`         TEXT                 DEFAULT NULL COMMENT '附加信息',
    `bind_time`     TIMESTAMP   NULL     DEFAULT NULL COMMENT '绑定时间',
    `bind_type`     VARCHAR(20)          DEFAULT NULL COMMENT '绑定类型',
    `bind_by`       VARCHAR(64)          DEFAULT NULL COMMENT '绑定者',
    `bind_org`      VARCHAR(64)          DEFAULT NULL COMMENT '绑定机构',
    `unbind_time`   TIMESTAMP   NULL     DEFAULT NULL COMMENT '解绑时间',
    `unbind_reason` VARCHAR(50)          DEFAULT NULL COMMENT '解绑理由',
    `unbind_by`     VARCHAR(64)          DEFAULT NULL COMMENT '解绑者',
    `unbind_org`    VARCHAR(64)          DEFAULT NULL COMMENT '解绑机构',
    `part_state`    SMALLINT             DEFAULT NULL COMMENT '零件状态：0-待绑定，1-在用，2-待更换，3-已报废',
    `description`   VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`     VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`   INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`     TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`),
    INDEX `idx_vin` (`vin`),
    INDEX `idx_pn` (`pn`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='车辆零件变更历史表';

DROP TABLE IF EXISTS `db_vmd`.`tb_config_item`;
CREATE TABLE `db_vmd`.`tb_config_item`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `family`      VARCHAR(50)  NOT NULL COMMENT '配置项大类',
    `code`        VARCHAR(50)  NOT NULL COMMENT '配置项编码',
    `name`        VARCHAR(255) NOT NULL COMMENT '配置项名称',
    `type`        VARCHAR(32)  NOT NULL COMMENT '配置项类型',
    `unit`        VARCHAR(16)           DEFAULT NULL COMMENT '配置项单位',
    `capability`  TINYINT               DEFAULT 0 COMMENT '是否车辆能力',
    `display`     TINYINT               DEFAULT 0 COMMENT '端上是否展示',
    `cache`       TINYINT               DEFAULT 0 COMMENT '端上是否缓存',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version` INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '配置项表';

DROP TABLE IF EXISTS `db_vmd`.`tb_config_item_option`;
CREATE TABLE `db_vmd`.`tb_config_item_option`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_item_code` VARCHAR(50)  NOT NULL COMMENT '配置项编码',
    `code`             VARCHAR(50)  NOT NULL COMMENT '枚举值编码',
    `name`             VARCHAR(255) NOT NULL COMMENT '枚举值名称',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`        VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`        VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`      INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`        TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_config_item` (`config_item_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '配置项枚举值表';

DROP TABLE IF EXISTS `db_vmd`.`tb_config_item_mapping`;
CREATE TABLE `db_vmd`.`tb_config_item_mapping`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_item_code`   VARCHAR(50) NOT NULL COMMENT '配置项代码',
    `source_system`      VARCHAR(50) NOT NULL COMMENT '源系统',
    `source_code`        VARCHAR(50) NOT NULL COMMENT '源系统代码',
    `source_value`       VARCHAR(255)         DEFAULT NULL COMMENT '源系统值',
    `target_option_code` VARCHAR(50)          DEFAULT NULL COMMENT '映射的枚举值编码',
    `target_value`       VARCHAR(255)         DEFAULT NULL COMMENT '映射值',
    `description`        VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`          VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time`        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`          VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version`        INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`          TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_config_item` (`config_item_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '配置项映射表';

DROP TABLE IF EXISTS `db_vmd`.`tb_vehicle_config`;
CREATE TABLE `db_vmd`.`tb_vehicle_config`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`         VARCHAR(20) NOT NULL COMMENT '车架号',
    `version`     VARCHAR(64) NOT NULL COMMENT '配置版本',
    `state`       VARCHAR(20) NOT NULL COMMENT '配置状态',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`   VARCHAR(64)          DEFAULT NULL COMMENT '创建者',
    `modify_time` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`   VARCHAR(64)          DEFAULT NULL COMMENT '修改者',
    `row_version` INT                  DEFAULT 1 COMMENT '记录版本',
    `row_valid`   TINYINT              DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY (`vin`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '车辆配置表';

DROP TABLE IF EXISTS `db_vmd`.`tb_vehicle_config_item`;
CREATE TABLE `db_vmd`.`tb_vehicle_config_item`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `vin`               VARCHAR(20)  NOT NULL COMMENT '车架号',
    `version`           VARCHAR(64)  NOT NULL COMMENT '配置版本',
    `config_item_code`  VARCHAR(50)  NOT NULL COMMENT '配置项代码',
    `config_item_value` VARCHAR(255) NOT NULL COMMENT '配置项值',
    `source_value`      VARCHAR(255) NOT NULL COMMENT '源系统值',
    `source_system`     VARCHAR(50)  NOT NULL COMMENT '源系统',
    `description`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`         VARCHAR(64)           DEFAULT NULL COMMENT '创建者',
    `modify_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `modify_by`         VARCHAR(64)           DEFAULT NULL COMMENT '修改者',
    `row_version`       INT                   DEFAULT 1 COMMENT '记录版本',
    `row_valid`         TINYINT               DEFAULT 1 COMMENT '记录是否有效',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY (`vin`, `version`, `config_item_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '车辆配置项表';