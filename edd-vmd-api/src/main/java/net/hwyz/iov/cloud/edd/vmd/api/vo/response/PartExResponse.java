package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartExResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件号
     */
    private String code;

    /**
     * 零件中文名称
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 零件类型
     */
    private String partType;

    /**
     * 是否是软件
     */
    private Boolean isSoftware;

    /**
     * 零件状态：PRODUCTION-量产，TRIAL-试生产，DISCONTINUE-停用
     */
    private String status;

    /**
     * 是否精准追溯
     */
    private Boolean isAccuratelyTraced;

    /**
     * 车辆节点代码
     */
    private String vehicleNodeCode;

    /**
     * 供应商代码
     */
    private String supplierCode;

    /**
     * 是否支持FOTA升级
     */
    private Boolean fotaUpgradeable;

    /**
     * 数据来源
     */
    private String source;

    /**
     * MDM侧实体主键ID
     */
    private String externalRefId;

    /**
     * MDM侧实体版本号
     */
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    private java.time.LocalDateTime lastSyncTime;

}
