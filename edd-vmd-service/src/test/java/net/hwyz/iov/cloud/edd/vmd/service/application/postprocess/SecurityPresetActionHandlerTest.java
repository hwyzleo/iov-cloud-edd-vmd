package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.HsmUidFieldResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityBizTypeResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityPresetPolicy;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityPresetDecision;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 器件安全常量补偿动作处理器单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityPresetActionHandler 测试")
class SecurityPresetActionHandlerTest {

    @Mock
    private SecurityPresetPolicy securityPresetPolicy;
    @Mock
    private SecurityBizTypeResolver securityBizTypeResolver;
    @Mock
    private HsmUidFieldResolver hsmUidFieldResolver;
    @Mock
    private PartSecurityPresetAppService partSecurityPresetAppService;
    @Mock
    private PartSecurityConstantRepository partSecurityConstantRepository;

    private SecurityPresetActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SecurityPresetActionHandler(securityPresetPolicy, securityBizTypeResolver,
                hsmUidFieldResolver, partSecurityPresetAppService, partSecurityConstantRepository);
        lenient().when(hsmUidFieldResolver.resolve(null)).thenReturn("HSM");
    }

    private PartPostProcessActionContext buildContext(PartInfo partInfo, VehicleNode vehicleNode) {
        return PartPostProcessActionContext.builder()
                .replayId("replay-003")
                .partImportDataId(1L)
                .batchNum("B001")
                .partCode("PN001")
                .sn("SN001")
                .partInfo(partInfo)
                .vehicleNode(vehicleNode)
                .operatorId("op1")
                .scope(Set.of(PartPostProcessActionType.SECURITY_PRESET))
                .build();
    }

    private PartInfo buildPartInfo() {
        return PartInfo.builder().partCode("PN001").sn("SN001")
                .vehicleNodeCode("TBOX_5G")
                .extra("{\"HSM\":\"UID0001\"}").build();
    }

    private VehicleNode buildVehicleNode() {
        return VehicleNode.builder().code("TBOX_5G").deviceCategory("TBOX")
                .funcDomain("CONNECTIVITY").hsmCapability("HSM_FULL").build();
    }

    @Test
    @DisplayName("策略不要求预置 → SKIPPED(POLICY_NOT_APPLICABLE)")
    void execute_policyNotApplicable() {
        when(securityPresetPolicy.decide(anyString(), anyString()))
                .thenReturn(SecurityPresetDecision.PRESET_NOT_REQUIRED);
        PartPostProcessActionResult result = handler.execute(buildContext(buildPartInfo(), buildVehicleNode()));
        assertEquals(PartPostProcessActionResult.Outcome.SKIPPED, result.getOutcome());
        assertEquals("POLICY_NOT_APPLICABLE", result.getSkipReason());
        verify(partSecurityPresetAppService, never()).presetForReplay(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("已 PRESET → SKIPPED(ALREADY_PRESET)，不再补偿")
    void execute_alreadyPreset() {
        when(securityPresetPolicy.decide(anyString(), anyString()))
                .thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        PartSecurityConstant existing = PartSecurityConstant.builder()
                .partCode("PN001").sn("SN001")
                .presetState(SecurityConstantState.PRESET).build();
        when(partSecurityConstantRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(existing);

        PartPostProcessActionResult result = handler.execute(buildContext(buildPartInfo(), buildVehicleNode()));
        assertEquals(PartPostProcessActionResult.Outcome.SKIPPED, result.getOutcome());
        assertEquals("ALREADY_PRESET", result.getSkipReason());
        verify(partSecurityPresetAppService, never()).presetForReplay(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("需预置且补偿成功 → SUCCESS，经补偿入口执行")
    void execute_presetSuccess() {
        when(securityPresetPolicy.decide(anyString(), anyString()))
                .thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        when(partSecurityConstantRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(null);
        when(securityBizTypeResolver.resolve("TBOX", "CONNECTIVITY", "TBOX_5G"))
                .thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(partSecurityPresetAppService.presetForReplay(eq("PN001"), eq("SN001"), eq("UID0001"),
                eq("B001"), eq("TBOX_5G"), eq(BizType.TBOX_DEVICE_ROOT))).thenReturn(null);

        PartPostProcessActionResult result = handler.execute(buildContext(buildPartInfo(), buildVehicleNode()));
        assertEquals(PartPostProcessActionResult.Outcome.SUCCESS, result.getOutcome());
        verify(partSecurityPresetAppService).presetForReplay(eq("PN001"), eq("SN001"), eq("UID0001"),
                eq("B001"), eq("TBOX_5G"), eq(BizType.TBOX_DEVICE_ROOT));
    }

    @Test
    @DisplayName("补偿失败 → FAILED_RETRYABLE，可重试")
    void execute_presetFailed() {
        when(securityPresetPolicy.decide(anyString(), anyString()))
                .thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        when(partSecurityConstantRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(null);
        when(securityBizTypeResolver.resolve("TBOX", "CONNECTIVITY", "TBOX_5G"))
                .thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(partSecurityPresetAppService.presetForReplay(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(BizType.class))).thenReturn("安全常量预置失败: KMS不可用");

        PartPostProcessActionResult result = handler.execute(buildContext(buildPartInfo(), buildVehicleNode()));
        assertEquals(PartPostProcessActionResult.Outcome.FAILED_RETRYABLE, result.getOutcome());
    }

    @Test
    @DisplayName("HSM UID 缺失 → FAILED_FINAL(HSM_UID_MISSING)")
    void execute_hsmUidMissing() {
        when(securityPresetPolicy.decide(anyString(), anyString()))
                .thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        when(partSecurityConstantRepository.selectByPartCodeAndSn("PN001", "SN001")).thenReturn(null);
        PartInfo partInfo = PartInfo.builder().partCode("PN001").sn("SN001")
                .vehicleNodeCode("TBOX_5G").extra("{}").build();
        PartPostProcessActionResult result = handler.execute(buildContext(partInfo, buildVehicleNode()));
        assertEquals(PartPostProcessActionResult.Outcome.FAILED_FINAL, result.getOutcome());
        assertEquals("HSM_UID_MISSING", result.getErrorCode());
    }

    @Test
    @DisplayName("idempotencyKey = partCode:sn:constantType（跨请求稳定）")
    void idempotencyKey() {
        assertEquals("PN001:SN001:ROOT", handler.idempotencyKey(buildContext(buildPartInfo(), buildVehicleNode())));
    }
}
