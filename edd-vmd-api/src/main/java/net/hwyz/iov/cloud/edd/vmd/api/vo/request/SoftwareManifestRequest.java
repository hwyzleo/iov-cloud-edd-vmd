package net.hwyz.iov.cloud.edd.vmd.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 软件实装清单写入请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareManifestRequest {

    /**
     * 车架号
     */
    private String vin;

    /**
     * 来源（EOL/VEHICLE_REPORT/OTA/AFTER_SALES/MANUAL）
     */
    private String source;

    /**
     * 来源版本（用于版本时序gate判定）
     */
    private String sourceVersion;

    /**
     * 来源事件时间（用于版本时序gate判定）
     */
    private Instant occurredAt;

    /**
     * 请求幂等键（同步Feign请求ID）
     */
    private String requestId;

    /**
     * 软件实装清单条目列表
     */
    private List<SoftwareManifestItem> items;

    /**
     * 软件实装清单条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoftwareManifestItem {

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
         * 制品摘要（可空）
         */
        private String digest;

        /**
         * 变更类型（INITIAL/UPGRADE/ROLLBACK/REFLASH/REPAIR）
         */
        private String changeType;

        /**
         * 是否已确认（默认provisional）
         */
        private Boolean isConfirmed;
    }
}
