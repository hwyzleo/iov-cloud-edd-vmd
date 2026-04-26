package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 车辆预设车主领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehiclePresetOwner implements DomainObj<VehiclePresetOwner> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 车主真实姓名
     */
    private String realName;

    /**
     * 手机所属国家或地区
     */
    private String countryRegionCode;

    /**
     * 手机号
     */
    private String mobile;

}
