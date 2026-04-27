package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台车辆零件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class VehiclePartResponse {

    /**
     * 主键
     */
    private Long id;

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
     * 零件状态：0-待绑定，1-在用，2-待更换，3-已报废
     */
    private Integer partState;

    /**
     * 创建时间
     */
    private Date createTime;

}
