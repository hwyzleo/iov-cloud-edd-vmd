package net.hwyz.iov.cloud.edd.vmd.service.integration;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.KmsHsmClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 零件导入触发安全常量生成集成测试
 * <p>
 * 验证零件导入流程与安全常量预置的端到端集成行为：
 * - TBOX/BTM/CCP/IDCM 类型零件导入触发安全常量预置
 * - SIM/OTHER 类型零件导入跳过安全常量预置
 * - 安全常量预置失败时正确记录错误信息
 * - VehicleNodeSchemaRegistry 与 PartSecurityPresetAppService 联动
 *
 * @author hwyz_leo
 */
class PartImportSecurityConstantIntegrationTest {

    private PartImportDataRepository partImportDataRepository;
    private MdmPartRepository mdmPartRepository;
    private PartInboundAppService partInboundAppService;
    private DownstreamProcessorRegistry downstreamProcessorRegistry;
    private VehicleNodeSchemaRegistry vehicleNodeSchemaRegistry;
    private PartSecurityPresetAppService partSecurityPresetAppService;
    private PartSecurityConstantRepository partSecurityConstantRepository;
    private KmsHsmClient kmsHsmClient;

    private PartImportDataAppService partImportDataAppService;

    @BeforeEach
    void setUp() {
        partImportDataRepository = mock(PartImportDataRepository.class);
        mdmPartRepository = mock(MdmPartRepository.class);
        partInboundAppService = mock(PartInboundAppService.class);
        downstreamProcessorRegistry = mock(DownstreamProcessorRegistry.class);
        partSecurityConstantRepository = mock(PartSecurityConstantRepository.class);
        kmsHsmClient = mock(KmsHsmClient.class);

        // 使用真实的 VehicleNodeSchemaRegistry，验证内置类型白名单逻辑
        vehicleNodeSchemaRegistry = new VehicleNodeSchemaRegistry();

        // 使用真实的 PartSecurityPresetAppService，仅 mock 外部依赖
        partSecurityPresetAppService = new PartSecurityPresetAppService(
                partSecurityConstantRepository, partImportDataRepository, kmsHsmClient);

        partImportDataAppService = new PartImportDataAppService(
                partImportDataRepository, mdmPartRepository, partInboundAppService,
                downstreamProcessorRegistry, vehicleNodeSchemaRegistry, partSecurityPresetAppService);
    }

    @Test
    @DisplayName("TBOX零件导入应触发安全常量预置并写入数据库")
    void tboxPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_TBOX_001";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TBOX_5G_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TBOX_5G\",\"ITEMS\":[{\"SN\":\"SN_TBOX_001\",\"HSM\":\"HSM_UID_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TBOX_5G_001")
                .partType("TBOX")
                .vehicleNodeCode("TBOX_5G")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TBOX_5G_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(downstreamProcessorRegistry.getProcessor("TBOX_5G")).thenReturn(mockProcessor);
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_001", "SN_TBOX_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(kmsHsmClient.generatePerDeviceConstant("TBOX_5G_001", "SN_TBOX_001", "ROOT", "HSM_UID_001"))
                .thenReturn(KmsHsmResult.builder()
                        .kmsKeyRef("key-ref-001")
                        .keySpec("AES-256")
                        .provider("OpenBao")
                        .algorithm("AES")
                        .build());

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证安全常量记录被创建（使用 ArgumentCaptor 捕获 insert 时的快照）
        ArgumentCaptor<PartSecurityConstant> insertCaptor = ArgumentCaptor.forClass(PartSecurityConstant.class);
        verify(partSecurityConstantRepository).insert(insertCaptor.capture());
        PartSecurityConstant inserted = insertCaptor.getValue();
        assertEquals("TBOX_5G_001", inserted.getPartCode());
        assertEquals("SN_TBOX_001", inserted.getSn());
        assertEquals("HSM_UID_001", inserted.getChipUid());
        assertEquals("ROOT", inserted.getConstantType());

        // 验证安全常量状态更新为已预置
        verify(partSecurityConstantRepository).update(argThat(entity ->
                entity.getPresetState() == SecurityConstantState.PRESET &&
                entity.getKmsKeyRef().equals("key-ref-001") &&
                entity.getKmsProvider().equals("OpenBao")
        ));

        // 验证 KMS/HSM 客户端被调用
        verify(kmsHsmClient).generatePerDeviceConstant("TBOX_5G_001", "SN_TBOX_001", "ROOT", "HSM_UID_001");

        // 验证下游处理器也被调用（与安全常量预置并列）
        verify(mockProcessor).process(eq(batchNum), eq("TBOX_5G_001"), eq("TBOX_5G"), any(JSONObject.class));
    }

    @Test
    @DisplayName("BTM零件导入应触发安全常量预置")
    void btmPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_BTM_001";
        PartImportData importData = PartImportData.builder()
                .id(2L)
                .batchNum(batchNum)
                .partCode("BTM_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP002\"},\"DATA\":{\"vehicleNodeCode\":\"BTM\",\"ITEMS\":[{\"SN\":\"SN_BTM_001\",\"HSM\":\"HSM_UID_BTM_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("BTM_001")
                .partType("BTM")
                .vehicleNodeCode("BTM")
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("BTM_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(partSecurityConstantRepository.selectByPartCodeAndSn("BTM_001", "SN_BTM_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(kmsHsmClient.generatePerDeviceConstant("BTM_001", "SN_BTM_001", "ROOT", "HSM_UID_BTM_001"))
                .thenReturn(KmsHsmResult.builder()
                        .kmsKeyRef("key-ref-btm").keySpec("AES-256").provider("OpenBao").algorithm("AES").build());

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(kmsHsmClient).generatePerDeviceConstant("BTM_001", "SN_BTM_001", "ROOT", "HSM_UID_BTM_001");
    }

    @Test
    @DisplayName("SIM零件导入应跳过安全常量预置")
    void simPartImport_shouldSkipSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_SIM_001";
        PartImportData importData = PartImportData.builder()
                .id(3L)
                .batchNum(batchNum)
                .partCode("SIM_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP003\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN_SIM_001\",\"iccid\":\"ICCID001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("SIM_001")
                .partType("SIM")
                .vehicleNodeCode("TSP")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("SIM_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证安全常量预置服务未被调用
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(kmsHsmClient, never()).generatePerDeviceConstant(any(), any(), any(), any());

        // 验证下游处理器仍被调用
        verify(mockProcessor).process(eq(batchNum), eq("SIM_001"), eq("TSP"), any(JSONObject.class));
    }

    @Test
    @DisplayName("OTHER类型零件导入应跳过安全常量预置")
    void otherPartImport_shouldSkipSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_OTHER_001";
        PartImportData importData = PartImportData.builder()
                .id(4L)
                .batchNum(batchNum)
                .partCode("OTHER_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP004\"},\"DATA\":{\"ITEMS\":[{\"SN\":\"SN_OTHER_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("OTHER_001")
                .partType("OTHER")
                .vehicleNodeCode(null)
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("OTHER_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(kmsHsmClient, never()).generatePerDeviceConstant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("安全常量预置失败应记录错误信息且不影响零件入站成功计数")
    void securityConstantPresetFailure_shouldRecordErrorWithoutAffectingInboundCount() throws Exception {
        String batchNum = "INT_BATCH_FAIL_001";
        PartImportData importData = PartImportData.builder()
                .id(5L)
                .batchNum(batchNum)
                .partCode("TBOX_5G_002")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TBOX_5G\",\"ITEMS\":[{\"SN\":\"SN_FAIL_001\",\"HSM\":\"HSM_UID_FAIL_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TBOX_5G_002")
                .partType("TBOX")
                .vehicleNodeCode("TBOX_5G")
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TBOX_5G_002")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_002", "SN_FAIL_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(kmsHsmClient.generatePerDeviceConstant("TBOX_5G_002", "SN_FAIL_001", "ROOT", "HSM_UID_FAIL_001"))
                .thenThrow(new RuntimeException("KMS/HSM服务不可用"));

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        // 零件入站成功计数不受影响
        assertEquals(1, result.getSuccessCount());
        // 安全常量预置失败由 PartSecurityPresetAppService 内部处理，不抛出异常
        // 因此 failureCount 不变（仍为通用导入阶段的 0）
        assertEquals(0, result.getFailureCount());

        // 验证安全常量状态更新为失败
        ArgumentCaptor<PartSecurityConstant> updateCaptor = ArgumentCaptor.forClass(PartSecurityConstant.class);
        verify(partSecurityConstantRepository).update(updateCaptor.capture());
        PartSecurityConstant updated = updateCaptor.getValue();
        assertEquals(SecurityConstantState.FAILED, updated.getPresetState());
        assertTrue(updated.getFailReason().contains("KMS/HSM服务不可用"));

        // 验证 PartSecurityPresetAppService 尝试更新导入数据描述
        verify(partImportDataRepository, atLeastOnce()).selectByBatchNum(batchNum);
    }

    @Test
    @DisplayName("已预置的安全常量应跳过重复预置")
    void alreadyPresetedSecurityConstant_shouldSkipPreset() throws Exception {
        String batchNum = "INT_BATCH_SKIP_001";
        PartImportData importData = PartImportData.builder()
                .id(6L)
                .batchNum(batchNum)
                .partCode("TBOX_5G_003")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TBOX_5G\",\"ITEMS\":[{\"SN\":\"SN_SKIP_001\",\"HSM\":\"HSM_UID_SKIP_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TBOX_5G_003")
                .partType("TBOX")
                .vehicleNodeCode("TBOX_5G")
                .build();

        PartSecurityConstant existing = PartSecurityConstant.builder()
                .partCode("TBOX_5G_003")
                .sn("SN_SKIP_001")
                .presetState(SecurityConstantState.PRESET)
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TBOX_5G_003")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(partSecurityConstantRepository.selectByPartCodeAndSn("TBOX_5G_003", "SN_SKIP_001")).thenReturn(existing);

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());

        // 验证跳过了插入和KMS调用
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(partSecurityConstantRepository, never()).update(any());
        verify(kmsHsmClient, never()).generatePerDeviceConstant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("VehicleNodeSchemaRegistry应正确识别需要安全常量预置的车辆节点")
    void vehicleNodeSchemaRegistry_shouldIdentifySecurityPresetRequiredTypes() {
        // 需要安全常量预置的节点
        assertTrue(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("TBOX_5G"));
        assertTrue(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("BTM"));
        assertTrue(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("CCP"));
        assertTrue(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("IDCM"));

        // 不需要安全常量预置的节点
        assertFalse(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("TSP"));

        // 未注册节点
        assertFalse(vehicleNodeSchemaRegistry.needsSecurityConstantPreset("UNKNOWN"));
        assertFalse(vehicleNodeSchemaRegistry.needsSecurityConstantPreset(null));
    }

    @Test
    @DisplayName("VehicleNodeSchemaRegistry应正确返回HSM UID字段名")
    void vehicleNodeSchemaRegistry_shouldReturnCorrectHsmUidField() {
        assertEquals("HSM", vehicleNodeSchemaRegistry.getHsmUidField("TBOX_5G"));
        assertEquals("HSM", vehicleNodeSchemaRegistry.getHsmUidField("BTM"));
        assertEquals("HSM", vehicleNodeSchemaRegistry.getHsmUidField("CCP"));
        assertEquals("HSM", vehicleNodeSchemaRegistry.getHsmUidField("IDCM"));

        assertNull(vehicleNodeSchemaRegistry.getHsmUidField("TSP"));
        assertNull(vehicleNodeSchemaRegistry.getHsmUidField("UNKNOWN"));
        assertNull(vehicleNodeSchemaRegistry.getHsmUidField(null));
    }

    @Test
    @DisplayName("CCP零件导入应触发安全常量预置")
    void ccpPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_CCP_001";
        PartImportData importData = PartImportData.builder()
                .id(7L)
                .batchNum(batchNum)
                .partCode("CCP_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP005\"},\"DATA\":{\"vehicleNodeCode\":\"CCP\",\"ITEMS\":[{\"SN\":\"SN_CCP_001\",\"HSM\":\"HSM_UID_CCP_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("CCP_001")
                .partType("CCP")
                .vehicleNodeCode("CCP")
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("CCP_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(partSecurityConstantRepository.selectByPartCodeAndSn("CCP_001", "SN_CCP_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(kmsHsmClient.generatePerDeviceConstant("CCP_001", "SN_CCP_001", "ROOT", "HSM_UID_CCP_001"))
                .thenReturn(KmsHsmResult.builder()
                        .kmsKeyRef("key-ref-ccp").keySpec("AES-256").provider("OpenBao").algorithm("AES").build());

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(kmsHsmClient).generatePerDeviceConstant("CCP_001", "SN_CCP_001", "ROOT", "HSM_UID_CCP_001");
    }

    @Test
    @DisplayName("IDCM零件导入应触发安全常量预置")
    void idcmPartImport_shouldTriggerSecurityConstantPreset() throws Exception {
        String batchNum = "INT_BATCH_IDCM_001";
        PartImportData importData = PartImportData.builder()
                .id(8L)
                .batchNum(batchNum)
                .partCode("IDCM_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP006\"},\"DATA\":{\"vehicleNodeCode\":\"IDCM\",\"ITEMS\":[{\"SN\":\"SN_IDCM_001\",\"HSM\":\"HSM_UID_IDCM_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("IDCM_001")
                .partType("IDCM")
                .vehicleNodeCode("IDCM")
                .build();

        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("IDCM_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1).successCount(1).failureCount(0).build());
        when(partSecurityConstantRepository.selectByPartCodeAndSn("IDCM_001", "SN_IDCM_001")).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(kmsHsmClient.generatePerDeviceConstant("IDCM_001", "SN_IDCM_001", "ROOT", "HSM_UID_IDCM_001"))
                .thenReturn(KmsHsmResult.builder()
                        .kmsKeyRef("key-ref-idcm").keySpec("AES-256").provider("OpenBao").algorithm("AES").build());

        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        assertNotNull(result);
        assertEquals(0, result.getFailureCount());
        verify(partSecurityConstantRepository).insert(any());
        verify(kmsHsmClient).generatePerDeviceConstant("IDCM_001", "SN_IDCM_001", "ROOT", "HSM_UID_IDCM_001");
    }
}
