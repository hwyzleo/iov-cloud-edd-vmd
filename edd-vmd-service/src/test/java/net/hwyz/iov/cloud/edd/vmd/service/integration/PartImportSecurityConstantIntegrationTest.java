package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.HsmUidFieldResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityBizTypeResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityPresetPolicy;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.SecurityPresetMetrics;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 零件导入触发安全常量生成集成测试（CR-049）
 * <p>
 * 验证零件导入链路与器件级安全常量预置的端到端集成行为，覆盖设计测试矩阵：
 * - HSM_FULL/HSM_LIGHT 触发预置；NONE/SHE 明确跳过（info）
 * - null 能力走旧注册表兜底（warn + 指标）；未知枚举视为契约错误（失败不静默）
 * - 新节点变体仅靠主数据（deviceCategory + hsmCapability）即可生效，无需改注册表
 * - BizType 按 deviceCategory 路由；不可解析时按失败语义处理
 * - chipUid 缺失计入失败；重复导入幂等跳过；存量 TBOX/BTM/CCU/DCU_COCKPIT 回归
 *
 * @author hwyz_leo
 */
class PartImportSecurityConstantIntegrationTest {

    private PartImportDataRepository partImportDataRepository;
    private MdmPartRepository mdmPartRepository;
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private PartInboundAppService partInboundAppService;
    private DownstreamProcessorRegistry downstreamProcessorRegistry;
    private PartSecurityConstantRepository partSecurityConstantRepository;
    private KeyProvisioningTemplate keyProvisioningTemplate;

    private VehicleNodeSchemaRegistry vehicleNodeSchemaRegistry;
    private SecurityPresetMetrics securityPresetMetrics;
    private SecurityPresetPolicy securityPresetPolicy;
    private HsmUidFieldResolver hsmUidFieldResolver;
    private SecurityBizTypeResolver securityBizTypeResolver;
    private PartSecurityPresetAppService partSecurityPresetAppService;
    private PartImportDataAppService partImportDataAppService;

    private ProvisioningResult mockProvisioningResult(String keyRef) {
        ProvisioningResult result = new ProvisioningResult();
        result.setKmsKeyRef(keyRef);
        result.setKeySpec("256-bit");
        result.setProvider("Vault-Transit");
        result.setAlgorithm("HMAC-SHA256");
        result.setKcv(new byte[]{1, 2, 3, 4});
        result.setWrappedMaterial(null);
        return result;
    }

    private VehicleNode vehicleNode(String code, String deviceCategory, String hsmCapability) {
        return VehicleNode.builder()
                .code(code)
                .deviceCategory(deviceCategory)
                .hsmCapability(hsmCapability)
                .build();
    }

    private PartImportData importData(String batchNum, String partCode, String vehicleNodeCode,
                                      String sn, String chipUid) {
        String chipField = chipUid != null ? ",\"HSM\":\"" + chipUid + "\"" : "";
        return PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode(partCode)
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"" + vehicleNodeCode
                        + "\",\"ITEMS\":[{\"SN\":\"" + sn + "\"" + chipField + ",\"HARDWARE_PART_NO\":\"" + partCode + "\"}]}}}")
                .handle(false)
                .build();
    }

    private void stubCommon(PartImportData importData, String partCode, String vehicleNodeCode,
                            String deviceCategory, String hsmCapability) {
        when(partImportDataRepository.selectByBatchNum(importData.getBatchNum())).thenReturn(importData);
        when(mdmPartRepository.selectByCode(partCode)).thenReturn(Part.builder()
                .code(partCode)
                .partType(vehicleNodeCode)
                .vehicleNodeCode(vehicleNodeCode)
                .build());
        when(mdmVehicleNodeRepository.selectByCode(vehicleNodeCode))
                .thenReturn(vehicleNode(vehicleNodeCode, deviceCategory, hsmCapability));
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
    }

    @BeforeEach
    void setUp() {
        partImportDataRepository = mock(PartImportDataRepository.class);
        mdmPartRepository = mock(MdmPartRepository.class);
        mdmVehicleNodeRepository = mock(MdmVehicleNodeRepository.class);
        partInboundAppService = mock(PartInboundAppService.class);
        downstreamProcessorRegistry = mock(DownstreamProcessorRegistry.class);
        partSecurityConstantRepository = mock(PartSecurityConstantRepository.class);
        keyProvisioningTemplate = mock(KeyProvisioningTemplate.class);

        // 真实注册表（迁移期兜底）+ 真实决策组件，仅 mock 外部依赖与指标
        vehicleNodeSchemaRegistry = new VehicleNodeSchemaRegistry();
        securityPresetMetrics = mock(SecurityPresetMetrics.class);
        securityPresetPolicy = new SecurityPresetPolicy(vehicleNodeSchemaRegistry, securityPresetMetrics);
        hsmUidFieldResolver = new HsmUidFieldResolver();
        securityBizTypeResolver = new SecurityBizTypeResolver(vehicleNodeSchemaRegistry, securityPresetMetrics);

        partSecurityPresetAppService = new PartSecurityPresetAppService(
                partSecurityConstantRepository, partImportDataRepository, keyProvisioningTemplate);

        partImportDataAppService = new PartImportDataAppService(
                partImportDataRepository, mdmPartRepository, mdmVehicleNodeRepository, partInboundAppService,
                downstreamProcessorRegistry, partSecurityPresetAppService,
                securityPresetPolicy, hsmUidFieldResolver, securityBizTypeResolver);
    }

    @Test
    @DisplayName("HSM_FULL 零件导入应触发安全常量预置并写入数据库")
    void hsmFullPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_HSM_FULL_001";
        stubCommon(importData(batchNum, "TBOX_5G_001", "TBOX_5G", "SN_TBOX_001", "HSM_UID_001"),
                "TBOX_5G_001", "TBOX_5G", "TBOX", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_001", "SN_TBOX_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_001", BizType.TBOX_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        verify(partSecurityConstantRepository).insert(argThat(entity ->
                entity.getPartCode().equals("TBOX_5G_001")
                        && entity.getSn().equals("SN_TBOX_001")
                        && entity.getChipUid().equals("HSM_UID_001")
                        && entity.getConstantType().equals("ROOT")));
        verify(partSecurityConstantRepository).update(argThat(entity ->
                entity.getPresetState() == SecurityConstantState.PRESET
                        && entity.getKmsKeyRef().equals("dev-root-master:sn:HSM_UID_001")));
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_001", BizType.TBOX_DEVICE_ROOT);
    }

    @Test
    @DisplayName("HSM_LIGHT 零件导入应触发安全常量预置（BTM → PEPS_DEVICE_ROOT）")
    void hsmLightPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_HSM_LIGHT_001";
        stubCommon(importData(batchNum, "BTM_001", "BTM", "SN_BTM_001", "HSM_UID_BTM_001"),
                "BTM_001", "BTM", "BTM", "HSM_LIGHT");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("BTM_001", "SN_BTM_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_BTM_001", BizType.PEPS_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_BTM_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_BTM_001", BizType.PEPS_DEVICE_ROOT);
    }

    @Test
    @DisplayName("NONE 能力零件导入应跳过安全常量预置并输出明确原因")
    void noneCapability_shouldSkipSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_NONE_001";
        stubCommon(importData(batchNum, "SIM_001", "TSP", "SN_SIM_001", null),
                "SIM_001", "TSP", "TSP", "NONE");

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("显式 SHE 能力应覆盖旧白名单，不触发预置（RD-049-1）")
    void sheCapability_shouldOverrideLegacyWhitelistAndSkip() throws Exception {
        String batchNum = "INT_BATCH_SHE_001";
        // BTM 在旧注册表中登记为需预置，但显式 SHE 应优先关闭预置
        stubCommon(importData(batchNum, "BTM_SHE_001", "BTM", "SN_BTM_SHE_001", "HSM_UID_BTM_SHE_001"),
                "BTM_SHE_001", "BTM", "BTM", "SHE");

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("null 能力且命中旧注册表（TBOX_5G）应走兼容兜底触发预置")
    void nullCapability_shouldFallbackToLegacyRegistryAndTrigger() throws Exception {
        String batchNum = "INT_BATCH_FALLBACK_001";
        stubCommon(importData(batchNum, "TBOX_5G_002", "TBOX_5G", "SN_TBOX_002", "HSM_UID_002"),
                "TBOX_5G_002", "TBOX_5G", "TBOX", null);
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_002", "SN_TBOX_002")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_002", BizType.TBOX_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_002"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_002", BizType.TBOX_DEVICE_ROOT);
        verify(securityPresetMetrics).recordLegacyFallback("TBOX_5G");
    }

    @Test
    @DisplayName("null 能力且未注册节点应跳过预置（不触发）")
    void nullCapability_shouldSkipWhenNotInLegacyRegistry() throws Exception {
        String batchNum = "INT_BATCH_NULL_SKIP_001";
        stubCommon(importData(batchNum, "CAM_001", "CAM_FRONT", "SN_CAM_001", null),
                "CAM_001", "CAM_FRONT", "CAM", null);

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("未知能力枚举应视为契约错误，记录失败不静默跳过（RD-049-2）")
    void unknownCapability_shouldFailWithoutSilentSkip() throws Exception {
        String batchNum = "INT_BATCH_INVALID_001";
        stubCommon(importData(batchNum, "TBOX_5G_003", "TBOX_5G", "SN_INVALID_001", "HSM_UID_INVALID_001"),
                "TBOX_5G_003", "TBOX_5G", "TBOX", "HSM_SUPER");

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription() != null && result.getDescription().contains("安全常量预置未执行"));
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
        verify(securityPresetMetrics).recordInvalidCapability("HSM_SUPER");
    }

    @Test
    @DisplayName("新节点变体 CCU_GEN2 仅靠主数据（deviceCategory=CCU + HSM_FULL）即可触发，无需改注册表（R-049-1）")
    void newVariantByMasterDataOnly_shouldTriggerPresetWithoutRegistryChange() throws Exception {
        String batchNum = "INT_BATCH_CCU_GEN2_001";
        // 注册表不含 CCU_GEN2，仅主数据配置完整即可生效
        assertFalse(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("CCU_GEN2"));

        stubCommon(importData(batchNum, "CCU_GEN2_001", "CCU_GEN2", "SN_CCU2_001", "HSM_UID_CCU2_001"),
                "CCU_GEN2_001", "CCU_GEN2", "CCU", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("CCU_GEN2_001", "SN_CCU2_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_CCU2_001", BizType.CCU_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_CCU2_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_CCU2_001", BizType.CCU_DEVICE_ROOT);
    }

    @Test
    @DisplayName("需预置但 BizType 不可解析时应按失败语义处理（RD-049-5）")
    void presetRequiredButBizTypeUnresolved_shouldFail() throws Exception {
        String batchNum = "INT_BATCH_BIZTYPE_001";
        stubCommon(importData(batchNum, "XYZ_001", "NODE_XYZ", "SN_XYZ_001", "HSM_UID_XYZ_001"),
                "XYZ_001", "NODE_XYZ", "XYZ_UNKNOWN", "HSM_FULL");

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription() != null && result.getDescription().contains("deviceCategory=XYZ_UNKNOWN"));
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("需预置但缺少 chipUid 时应计入失败，不静默跳过")
    void missingChipUid_shouldCountAsFailure() throws Exception {
        String batchNum = "INT_BATCH_NO_CHIP_001";
        stubCommon(importData(batchNum, "TBOX_5G_004", "TBOX_5G", "SN_NO_HSM_001", null),
                "TBOX_5G_004", "TBOX_5G", "TBOX", "HSM_FULL");

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription() != null && result.getDescription().contains("缺少安全芯片标识"));
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("已预置的安全常量应跳过重复预置（幂等）")
    void alreadyPresetedSecurityConstant_shouldSkipPreset() throws Exception {
        String batchNum = "INT_BATCH_SKIP_001";
        stubCommon(importData(batchNum, "TBOX_5G_005", "TBOX_5G", "SN_SKIP_001", "HSM_UID_SKIP_001"),
                "TBOX_5G_005", "TBOX_5G", "TBOX", "HSM_FULL");

        PartSecurityConstant existing = PartSecurityConstant.builder()
                .partCode("TBOX_5G_005")
                .sn("SN_SKIP_001")
                .presetState(SecurityConstantState.PRESET)
                .build();
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_005", "SN_SKIP_001")).thenReturn(existing);

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(partSecurityConstantRepository, never()).update(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("KMS/HSM 失败应记录错误且不影响零件入站成功计数（批次保持未处理）")
    void securityConstantPresetFailure_shouldRecordErrorWithoutAffectingInboundCount() throws Exception {
        String batchNum = "INT_BATCH_FAIL_001";
        stubCommon(importData(batchNum, "TBOX_5G_006", "TBOX_5G", "SN_FAIL_001", "HSM_UID_FAIL_001"),
                "TBOX_5G_006", "TBOX_5G", "TBOX", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_006", "SN_FAIL_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_FAIL_001", BizType.TBOX_DEVICE_ROOT))
                .thenThrow(new RuntimeException("KMS/HSM服务不可用"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription() != null && result.getDescription().contains("KMS/HSM服务不可用"));

        verify(partSecurityConstantRepository).update(argThat(entity ->
                entity.getPresetState() == SecurityConstantState.FAILED
                        && entity.getFailReason().contains("KMS/HSM服务不可用")));

        // 预置失败后导入应保持未处理状态（已处理=否），便于修复后重试
        ArgumentCaptor<PartImportData> importDataCaptor = ArgumentCaptor.forClass(PartImportData.class);
        verify(partImportDataRepository, atLeastOnce()).update(importDataCaptor.capture());
        java.util.List<PartImportData> updates = importDataCaptor.getAllValues();
        assertFalse(updates.get(updates.size() - 1).getHandle());
    }

    @Test
    @DisplayName("回归：DCU_COCKPIT 按 deviceCategory 路由到座舱域 BizType（CPT_DCU_DEVICE_ROOT）")
    void dcuCockpitRegression_shouldRouteToCockpitBizType() throws Exception {
        String batchNum = "INT_BATCH_DCU_001";
        stubCommon(importData(batchNum, "DCU_COCKPIT_001", "DCU_COCKPIT", "SN_DCU_001", "HSM_UID_DCU_001"),
                "DCU_COCKPIT_001", "DCU_COCKPIT", "DCU_COCKPIT", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("DCU_COCKPIT_001", "SN_DCU_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_DCU_001", BizType.CPT_DCU_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_DCU_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_DCU_001", BizType.CPT_DCU_DEVICE_ROOT);
    }

    @Test
    @DisplayName("智驾域控 DCU_ADAS_GEN1 按 deviceCategory=DCU_ADAS 路由到 AD_DCU_DEVICE_ROOT（HSM_FULL 触发预置）")
    void dcuAdasGen1_shouldRouteToAdasBizType() throws Exception {
        String batchNum = "INT_BATCH_DCU_ADAS_001";
        stubCommon(importData(batchNum, "DCU_ADAS_001", "DCU_ADAS_GEN1", "SN_ADAS_001", "HSM_UID_ADAS_001"),
                "DCU_ADAS_001", "DCU_ADAS_GEN1", "DCU_ADAS", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("DCU_ADAS_001", "SN_ADAS_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_ADAS_001", BizType.AD_DCU_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_ADAS_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_ADAS_001", BizType.AD_DCU_DEVICE_ROOT);
    }

    @Test
    @DisplayName("智驾域控 DCU_ADAS_GEN1 能力缺失时旧注册表未登记，按主数据 deviceCategory 仍可路由预置（R-049-1）")
    void dcuAdasGen1_nullCapability_shouldFallbackByMasterDataCategory() throws Exception {
        String batchNum = "INT_BATCH_DCU_ADAS_002";
        // 注册表不含 DCU_ADAS_GEN1，能力缺失时策略判定不触发预置（旧注册表未登记）
        assertFalse(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("DCU_ADAS_GEN1"));
        stubCommon(importData(batchNum, "DCU_ADAS_002", "DCU_ADAS_GEN1", "SN_ADAS_002", "HSM_UID_ADAS_002"),
                "DCU_ADAS_002", "DCU_ADAS_GEN1", "DCU_ADAS", null);

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        // 能力缺失 + 注册表未登记 → 不触发预置（显式主数据 HSM_FULL 才是权威路径）
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    @DisplayName("回归：CCU 按 deviceCategory 路由到 CCU_DEVICE_ROOT")
    void ccuRegression_shouldRouteToCcuBizType() throws Exception {
        String batchNum = "INT_BATCH_CCU_001";
        stubCommon(importData(batchNum, "CCU_001", "CCU", "SN_CCU_001", "HSM_UID_CCU_001"),
                "CCU_001", "CCU", "CCU", "HSM_FULL");
        when(partSecurityConstantRepository.selectByPartCodeAndSn("CCU_001", "SN_CCU_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(keyProvisioningTemplate.deriveByUid("HSM_UID_CCU_001", BizType.CCU_DEVICE_ROOT))
                .thenReturn(mockProvisioningResult("dev-root-master:sn:HSM_UID_CCU_001"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(keyProvisioningTemplate).deriveByUid("HSM_UID_CCU_001", BizType.CCU_DEVICE_ROOT);
    }
}
