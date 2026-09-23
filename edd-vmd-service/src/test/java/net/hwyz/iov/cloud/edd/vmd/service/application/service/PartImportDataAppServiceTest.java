package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityPresetDecision;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PartImportDataAppService单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class PartImportDataAppServiceTest {

    @Mock
    private PartImportDataRepository partImportDataRepository;

    @Mock
    private MdmPartRepository mdmPartRepository;

    @Mock
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;

    @Mock
    private PartInboundAppService partInboundAppService;

    @Mock
    private DownstreamProcessorRegistry downstreamProcessorRegistry;

    @Mock
    private PartSecurityPresetAppService partSecurityPresetAppService;

    @Mock
    private SecurityPresetPolicy securityPresetPolicy;

    @Mock
    private HsmUidFieldResolver hsmUidFieldResolver;

    @Mock
    private SecurityBizTypeResolver securityBizTypeResolver;

    private PartImportDataAppService partImportDataAppService;

    @BeforeEach
    void setUp() {
        partImportDataAppService = new PartImportDataAppService(
                partImportDataRepository, mdmPartRepository, mdmVehicleNodeRepository, partInboundAppService,
                downstreamProcessorRegistry, partSecurityPresetAppService,
                securityPresetPolicy, hsmUidFieldResolver, securityBizTypeResolver);
        // 默认不触发预置；具体能力/类别场景由各测试以更具体的 stub 覆盖
        lenient().when(securityPresetPolicy.decide(any(), any())).thenReturn(SecurityPresetDecision.PRESET_NOT_REQUIRED);
    }

    @Test
    @DisplayName("两段式导入应成功处理通用导入和下游联动")
    void testTwoStageImportSuccess() {
        // 准备测试数据
        String batchNum = "TEST_BATCH_001";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\",\"HARDWARE_PART_NO\":\"TEST_PART_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .vehicleNodeCode("TSP")
                .build();

        ImportResult expectedResult = ImportResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证下游处理器被调用
        verify(mockProcessor).process(eq(batchNum), eq("TEST_PART_001"), eq("TSP"), any(JSONObject.class));
    }

    @Test
    @DisplayName("两段式导入应处理下游联动失败情况")
    void testTwoStageImportDownstreamFailure() {
        // 准备测试数据
        String batchNum = "TEST_BATCH_001";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\",\"HARDWARE_PART_NO\":\"TEST_PART_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .vehicleNodeCode("TSP")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        // 模拟下游处理器抛出异常
        doThrow(new RuntimeException("TSP服务调用失败")).when(mockProcessor)
                .process(eq(batchNum), eq("TEST_PART_001"), eq("TSP"), any(JSONObject.class));

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription().contains("TSP服务调用失败"));
    }

    @Test
    @DisplayName("下游联动失败且错误信息超长时应截断description后写库")
    void testTwoStageImportDownstreamFailureTruncatesDescription() {
        // 准备测试数据
        String batchNum = "TEST_BATCH_001";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\",\"HARDWARE_PART_NO\":\"TEST_PART_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .vehicleNodeCode("TSP")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 构造超长错误信息（模拟 FeignException 完整消息拼接后远超列宽 1000）
        String longErrorMessage = "feign.FeignException$InternalServerError: [500] during [POST] to [http://iov-tsp/api/service/tbox/v1/batchImport] "
                + "[TspTboxInfoService#batchImport(BatchImportTboxRequest)]: [{\"code\":\"100002\",\"message\":\"系统内部错误\","
                + "\"traceId\":\"75481fafb1ba426ebbf4ece78335d832\",\"timestamp\":1790040366827}] "
                + "X".repeat(3000);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        // 模拟下游处理器抛出超长异常
        doThrow(new RuntimeException(longErrorMessage)).when(mockProcessor)
                .process(eq(batchNum), eq("TEST_PART_001"), eq("TSP"), any(JSONObject.class));

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription().length() > 1000);

        // 验证写库时 description 被截断到列宽内，且不抛 Data too long
        ArgumentCaptor<PartImportData> captor = ArgumentCaptor.forClass(PartImportData.class);
        verify(partImportDataRepository).update(captor.capture());
        PartImportData updated = captor.getValue();
        assertNotNull(updated.getDescription());
        assertTrue(updated.getDescription().length() <= 1000, "description 应截断到列宽内");
        assertTrue(updated.getDescription().endsWith("..."));
        assertTrue(updated.getDescription().startsWith("[TSP] 处理失败"));
        // 下游联动失败时保持未处理，便于修复后重试
        assertFalse(updated.getHandle());
    }

    @Test
    @DisplayName("两段式导入全部成功时标记为已处理")
    void testTwoStageImportSuccessMarksHandled() {
        // 准备测试数据（ITEMS 携带 HARDWARE_PART_NO 以保证通用导入成功）
        String batchNum = "TEST_BATCH_OK";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\",\"HARDWARE_PART_NO\":\"TEST_PART_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .vehicleNodeCode("TSP")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(securityPresetPolicy.decide(any(), eq("TSP"))).thenReturn(SecurityPresetDecision.PRESET_NOT_REQUIRED);
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getFailureCount());

        // 验证全部成功时标记为已处理
        ArgumentCaptor<PartImportData> captor = ArgumentCaptor.forClass(PartImportData.class);
        verify(partImportDataRepository).update(captor.capture());
        PartImportData updated = captor.getValue();
        assertTrue(updated.getHandle());
    }

    @Test
    @DisplayName("两段式导入应跳过vehicleNodeCode为空的下游联动")
    void testTwoStageImportSkipDownstreamWhenVehicleNodeCodeEmpty() {
        // 准备测试数据（vehicleNodeCode为空）
        PartImportData importDataWithoutVehicleNodeCode = PartImportData.builder()
                .id(1L)
                .batchNum("TEST_BATCH_001")
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":[{\"SN\":\"SN001\",\"deviceItem\":\"TSP\",\"HARDWARE_PART_NO\":\"TEST_PART_001\"}]}}}")
                .handle(false)
                .build();

        String batchNum = "TEST_BATCH_001";
        // MDM Part 无 vehicleNodeCode：通用导入可从 ITEM 兜底，但下游联动应跳过
        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .build();

        ImportResult expectedResult = ImportResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .build();

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importDataWithoutVehicleNodeCode);
        when(mdmPartRepository.selectByCode("TEST_PART_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证下游处理器未被调用
        verify(downstreamProcessorRegistry, never()).getProcessor(anyString());
    }

    @Test
    @DisplayName("两段式导入应触发TBOX安全常量预置")
    void testTwoStageImportTriggerSecurityConstantPresetForTbox() throws Exception {
        // 准备测试数据 - TBOX类型零件
        String batchNum = "TEST_BATCH_TBOX";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TBOX_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TBOX_5G\",\"ITEMS\":[{\"SN\":\"SN001\",\"HSM\":\"HSM_UID_001\",\"HARDWARE_PART_NO\":\"TBOX_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TBOX_001")
                .partType("TBOX")
                .vehicleNodeCode("TBOX_5G")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TBOX_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        // CR-049：以 VehicleNode 投影主数据（hsmCapability/deviceCategory）驱动判定
        VehicleNode tbox5gNode = VehicleNode.builder()
                .code("TBOX_5G")
                .deviceCategory("TBOX")
                .hsmCapability("HSM_FULL")
                .build();
        when(mdmVehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(tbox5gNode);
        when(securityPresetPolicy.decide("HSM_FULL", "TBOX_5G")).thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        when(securityBizTypeResolver.resolve("TBOX", null, "TBOX_5G")).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(hsmUidFieldResolver.resolve(any())).thenReturn("HSM");
        when(downstreamProcessorRegistry.getProcessor("TBOX_5G")).thenReturn(mockProcessor);

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证安全常量预置服务被调用（BizType 按 deviceCategory 路由）
        verify(partSecurityPresetAppService).preset(eq("TBOX_001"), eq("SN001"), eq("HSM_UID_001"), eq(batchNum), eq("TBOX_5G"), eq(BizType.TBOX_DEVICE_ROOT));
        
        // 验证下游处理器也被调用（与安全常量预置并列）
        verify(mockProcessor).process(eq(batchNum), eq("TBOX_001"), eq("TBOX_5G"), any(JSONObject.class));
    }

    @Test
    @DisplayName("两段式导入应跳过非安全类型的零件的安全常量预置")
    void testTwoStageImportSkipSecurityPresetForNonSecurityType() {
        // 准备测试数据 - SIM类型零件（不需要安全常量预置）
        String batchNum = "TEST_BATCH_SIM";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("SIM_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TSP\",\"ITEMS\":[{\"SN\":\"SN001\",\"iccid\":\"ICCID001\",\"HARDWARE_PART_NO\":\"SIM_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("SIM_001")
                .partType("SIM")
                .vehicleNodeCode("TSP")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("SIM_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        when(securityPresetPolicy.decide(any(), eq("TSP"))).thenReturn(SecurityPresetDecision.PRESET_NOT_REQUIRED);
        when(downstreamProcessorRegistry.getProcessor("TSP")).thenReturn(mockProcessor);

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证安全常量预置服务未被调用
        verify(partSecurityPresetAppService, never()).preset(anyString(), anyString(), anyString(), anyString(), anyString(), any(BizType.class));
        
        // 验证下游处理器被调用
        verify(mockProcessor).process(eq(batchNum), eq("SIM_001"), eq("TSP"), any(JSONObject.class));
    }

    @Test
    @DisplayName("两段式导入应处理安全常量预置失败情况")
    void testTwoStageImportSecurityPresetFailure() throws Exception {
        // 准备测试数据 - TBOX类型零件
        String batchNum = "TEST_BATCH_TBOX_FAIL";
        PartImportData importData = PartImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .partCode("TBOX_001")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"vehicleNodeCode\":\"TBOX_5G\",\"ITEMS\":[{\"SN\":\"SN001\",\"HSM\":\"HSM_UID_001\",\"HARDWARE_PART_NO\":\"TBOX_001\"}]}}}")
                .handle(false)
                .build();

        Part mdmPart = Part.builder()
                .code("TBOX_001")
                .partType("TBOX")
                .vehicleNodeCode("TBOX_5G")
                .build();

        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);

        // 设置mock行为
        when(partImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(mdmPartRepository.selectByCode("TBOX_001")).thenReturn(mdmPart);
        when(partInboundAppService.processInbound(any(), any(), any())).thenReturn(
                PartInboundAppService.PartInboundResult.builder()
                        .totalCount(1)
                        .successCount(1)
                        .failureCount(0)
                        .build());
        // CR-049：以 VehicleNode 投影主数据（hsmCapability/deviceCategory）驱动判定
        VehicleNode tbox5gNode = VehicleNode.builder()
                .code("TBOX_5G")
                .deviceCategory("TBOX")
                .hsmCapability("HSM_FULL")
                .build();
        when(mdmVehicleNodeRepository.selectByCode("TBOX_5G")).thenReturn(tbox5gNode);
        when(securityPresetPolicy.decide("HSM_FULL", "TBOX_5G")).thenReturn(SecurityPresetDecision.PRESET_REQUIRED);
        when(securityBizTypeResolver.resolve("TBOX", null, "TBOX_5G")).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(hsmUidFieldResolver.resolve(any())).thenReturn("HSM");
        when(downstreamProcessorRegistry.getProcessor("TBOX_5G")).thenReturn(mockProcessor);

        // 模拟安全常量预置抛出异常
        doThrow(new RuntimeException("KMS/HSM服务不可用")).when(partSecurityPresetAppService)
                .preset(eq("TBOX_001"), eq("SN001"), eq("HSM_UID_001"), eq(batchNum), eq("TBOX_5G"), eq(BizType.TBOX_DEVICE_ROOT));

        // 执行测试
        ImportResult result = partImportDataAppService.parsePartImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription().contains("安全常量预置失败"));
        assertTrue(result.getDescription().contains("KMS/HSM服务不可用"));
    }
}
