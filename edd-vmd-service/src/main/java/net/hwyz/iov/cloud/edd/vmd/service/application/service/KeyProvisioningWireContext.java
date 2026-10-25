package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * PAR-PROTO Wire Mapper 输入上下文（VMD-DSN-CR-068）。
 * <p>
 * 承载 wire digest 计算所需的批次级事实：requestId 与业务域（V2C_COMM）。
 * 其余 9 段输入（envelopeFormat/purpose/keyId/businessKeyVersion/wrappingSuite/wrappedKey/
 * originalDeterministicParametersBytes）全部取自结构化 view 与 deterministic parameters，
 * 由 {@link ParProtoWireMapperV1} 在映射流水线内读取，禁止调用方传入。
 *
 * @param requestId       TBOX 稳定 requestId（UTF-8）
 * @param businessDomain   业务域（首期 V2C_COMM，UTF-8）
 * @author hwyz_leo
 * @since 2026-10-25
 */
public record KeyProvisioningWireContext(String requestId, String businessDomain) {

    public KeyProvisioningWireContext {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("KeyProvisioningWireContext.requestId 不能为空");
        }
        if (businessDomain == null || businessDomain.isBlank()) {
            throw new IllegalArgumentException("KeyProvisioningWireContext.businessDomain 不能为空");
        }
    }
}
