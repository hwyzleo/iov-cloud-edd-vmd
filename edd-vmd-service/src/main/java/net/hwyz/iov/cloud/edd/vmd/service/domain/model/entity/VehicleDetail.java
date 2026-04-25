package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.util.Date;

/**
 * 车辆详细信息领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleDetail extends BaseDo<Long> implements DomainObj<VehicleDetail> {

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
     * 类型
     */
    private String type;

    /**
     * 值
     */
    private String val;

}
