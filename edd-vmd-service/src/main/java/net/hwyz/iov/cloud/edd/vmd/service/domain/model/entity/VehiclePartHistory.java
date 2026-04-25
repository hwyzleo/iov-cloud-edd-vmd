package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.util.Date;

/**
 * 车辆零件历史领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehiclePartHistory extends BaseDo<Long> implements DomainObj<VehiclePartHistory> {

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
     * 零件编号
     */
    private String pn;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 设备项
     */
    private String deviceItem;

    /**
     * 零件序列号
     */
    private String sn;

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
     * 绑定时间
     */
    private Date bindTime;

    /**
     * 绑定类型
     */
    private String bindType;

    /**
     * 绑定者
     */
    private String bindBy;

    /**
     * 绑定机构
     */
    private String bindOrg;

    /**
     * 解绑时间
     */
    private Date unbindTime;

    /**
     * 解绑理由
     */
    private String unbindReason;

    /**
     * 解绑者
     */
    private String unbindBy;

    /**
     * 解绑机构
     */
    private String unbindOrg;

    /**
     * 零件状态
     */
    private Integer partState;

}
