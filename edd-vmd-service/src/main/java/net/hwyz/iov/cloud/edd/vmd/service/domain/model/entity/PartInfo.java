package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 物理零件实例本体领域对象
 * <p>
 * 持有不随装车改变的本体属性
 * 允许未绑定VIN时独立存在（游离零件）
 * UK (partCode, sn)
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class PartInfo implements DomainObj<PartInfo> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件编码（关联tb_mdm_part.pn）
     */
    private String partCode;

    /**
     * 零件序列号
     */
    private String sn;

    /**
     * 车载节点代码（关联tb_mdm_vehicle_node.code）
     */
    private String vehicleNodeCode;

    /**
     * 配置字
     */
    private String configWord;

    /**
     * 供应商编码
     */
    private String supplierCode;

    /**
     * 批次号
     */
    private String batchNum;

    /**
     * 硬件版本号
     */
    private String hardwareVer;

    /**
     * 软件版本号
     */
    private String softwareVer;

    /**
     * 硬件零件号
     */
    private String hardwarePn;

    /**
     * 软件零件号
     */
    private String softwarePn;

    /**
     * 附加信息
     */
    private String extra;

    /**
     * 实例状态
     */
    private Integer instanceState;

    /**
     * 首次入库时间
     */
    private Instant firstSeenTime;

}
