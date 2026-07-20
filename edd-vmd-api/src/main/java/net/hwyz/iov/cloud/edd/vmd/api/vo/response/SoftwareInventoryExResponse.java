package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 对外服务软件实装清单信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareInventoryExResponse {

    /**
     * 绑定ID（= vehicle_part.id）
     */
    private Long bindingId;

    /**
     * 零件ID（= part_info.id）
     */
    private Long partId;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 车载节点代码
     */
    private String vehicleNodeCode;

    /**
     * 软件目标代码
     */
    private String softwareTargetCode;

    /**
     * 软件零件号
     */
    private String softwarePartNo;

    /**
     * 软件版本
     */
    private String softwareVersion;

    /**
     * 槽位（可空）
     */
    private String slot;

    /**
     * 是否已确认
     */
    private Boolean isConfirmed;

    /**
     * 软件清单版本
     */
    private Long inventoryVersion;

    /**
     * 来源
     */
    private String source;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 版本有效开始时间
     */
    private Instant effectiveFrom;

}
