package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;

import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 车辆生命周期节点领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleLifecycleNode extends BaseDo<String> implements DomainObj<VehicleLifecycleNode> {

    /**
     * 车架号
     */
    private String vin;
    /**
     * 生命周期节点
     */
    private VehicleLifecycleNodeEnum node;
    /**
     * 触达时间
     */
    private Instant reachTime;
    /**
     * 排序
     */
    private Integer sort;

    public void init() {
        stateInit();
    }

}
