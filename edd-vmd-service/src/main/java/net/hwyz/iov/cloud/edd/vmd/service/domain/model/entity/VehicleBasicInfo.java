package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 车辆基础信息领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleBasicInfo implements DomainObj<VehicleBasicInfo> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 工厂代码
     */
    private String manufacturerCode;

    /**
     * 品牌代码
     */
    private String brandCode;

    /**
     * 平台代码
     */
    private String platformCode;

    /**
     * 车系代码
     */
    private String seriesCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 基础车型代码
     */
    private String baseModelCode;

    /**
     * 生产配置代码
     */
    private String buildConfigCode;

    /**
     * 车辆下线时间
     */
    private Instant eolTime;

    /**
     * 最后一次PDI时间
     */
    private Instant pdiTime;

    /**
     * 订单号
     */
    private String orderNum;

    /**
     * 整车基线版本
     */
    private String vehicleBaseVersion;

}
