package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

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
public class PartInfoResponse {

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
     * 首次入库时间
     */
    private Date firstSeenTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
