package net.hwyz.iov.cloud.edd.vmd.service.domain.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.util.Date;

/**
 * 车辆生命周期节点领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class VehicleLifecycleNode extends BaseDo<String> {

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
    private Date reachTime;
    /**
     * 排序
     */
    private Integer sort;

    public void init() {
        stateInit();
    }

}
