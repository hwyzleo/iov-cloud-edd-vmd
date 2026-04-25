package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.util.Date;

/**
 * 车辆配置项领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleConfigItem extends BaseDo<Long> implements DomainObj<VehicleConfigItem> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 修改时间
     */
    private Date modifyTime;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 配置版本
     */
    private String version;

    /**
     * 配置项代码
     */
    private String configItemCode;

    /**
     * 配置项值
     */
    private String configItemValue;

    /**
     * 源系统值
     */
    private String sourceValue;

    /**
     * 源系统
     */
    private String sourceSystem;

}
