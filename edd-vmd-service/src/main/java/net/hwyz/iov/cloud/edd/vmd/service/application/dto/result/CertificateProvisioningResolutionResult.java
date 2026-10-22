package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书维度寻址开通批次结果（VMD-DSN-CR-065 §6.2）。
 * <p>
 * 只读桥接接口：从证书申请 requestId（MPT-CERT-* 等）解析到同设备+业务域唯一未终结
 * Keyprov 批次 requestId（kp-*），供前端取真实批次号后调用 blocking-diagnosis / clear-blocking；
 * 诊断与清理接口不接受证书申请 requestId，两套 ID 不得混用。
 * <p>
 * 不返回证书材料、密钥材料、证书/指纹摘要原文等任何敏感信息。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateProvisioningResolutionResult {

    /** 证书申请 requestId（入参原值） */
    private String certificateRequestId;

    /** TBOX 序列号（由证书记录解析） */
    private String deviceSn;

    /** 业务域 */
    private String businessDomain;

    /** 寻址结果：NO_OPEN_PROVISIONING / OPEN_BATCH_FOUND */
    private Resolution resolution;

    /** 唯一未终结批次摘要；无未终结批次时为 null */
    private ProvisioningSummary openBatch;

    /** 历史终结批次摘要（仅 includeTerminal=true 时返回，仅供审计，不进入清理选择） */
    private List<ProvisioningSummary> terminalHistory;

    /**
     * 寻址结果枚举
     */
    public enum Resolution {
        /** 证书存在但同设备+业务域无未终结开通批次（HTTP 200，非 806083） */
        NO_OPEN_PROVISIONING,
        /** 命中唯一未终结开通批次 */
        OPEN_BATCH_FOUND
    }

    /**
     * 批次摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvisioningSummary {

        /** 真实 Keyprov 批次 requestId（kp-*） */
        private String provisioningRequestId;

        /** 批次状态 */
        private String state;

        /** 批次最近修改时间（clear-blocking 回传防过期页面 STALE_VIEW 校验） */
        private LocalDateTime modifyTime;

        /** 批次冻结证书摘要与当前 ACTIVE 证书摘要是否不一致（“证书已升级”），不含证书材料 */
        private Boolean certificateChanged;

        /** 是否为可清理候选（PREPARING / RECONCILE_REQUIRED；WAIT_DEVICE_ACK 由 ACK 超时任务治理） */
        private Boolean clearCandidate;
    }
}
