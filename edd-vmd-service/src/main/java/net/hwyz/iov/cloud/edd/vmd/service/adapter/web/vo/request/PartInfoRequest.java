package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台物理零件实例
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PartInfoRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件编码
     */
    private String partCode;

    /**
     * 零件序列号
     */
    private String sn;

    /**
     * 车载节点代码
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
     * 实例状态：0-在库，1-在用，2-待更换，3-已报废
     */
    private Integer instanceState;

    /**
     * 节点设备类别（CR-072）：只读识别字段——不允许人工写入/纠错；显式赋值或清空请求均拒绝。
     * 不进入实体映射（PartInfoCmd 无此字段，overposting 无效）
     */
    private String deviceCategory;

    /**
     * 是否显式提供了 deviceCategory（区分“未提供”与“显式提供 NULL/空串”；仅识别用，不参与序列化）
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean deviceCategoryProvided;

    public void setDeviceCategory(String deviceCategory) {
        this.deviceCategory = deviceCategory;
        this.deviceCategoryProvided = true;
    }

    /**
     * 创建时间
     */
    private Date createTime;

}
