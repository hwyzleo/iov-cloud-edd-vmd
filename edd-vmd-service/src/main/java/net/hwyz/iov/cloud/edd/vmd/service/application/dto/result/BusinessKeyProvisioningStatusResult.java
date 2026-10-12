package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务密钥开通批次只读查询结果（CR-058 MPT）。
 * <p>仅返回非敏感批次状态与逐用途确认事实，不含密钥材料、Wrapped Key、证书或证明原文。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessKeyProvisioningStatusResult {

    private String requestId;
    private String deviceSn;
    private String businessDomain;
    private String status;
    private String failReason;
    private LocalDateTime startedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime completedAt;
    private List<Item> items;

    /**
     * 逐用途确认明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String purpose;
        private String keyId;
        private Long businessKeyVersion;
        private Long directoryKeyId;
        private String deviceResult;
        private LocalDateTime confirmedAt;
    }
}
