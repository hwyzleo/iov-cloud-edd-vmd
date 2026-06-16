package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private PartInboundAppService partInboundAppService;

    @Mock
    private DownstreamProcessorRegistry downstreamProcessorRegistry;

    private PartImportDataAppService partImportDataAppService;

    @BeforeEach
    void setUp() {
        partImportDataAppService = new PartImportDataAppService(
                partImportDataRepository, mdmPartRepository, partInboundAppService, downstreamProcessorRegistry);
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
                .data("{\"vin\":\"TEST_VIN\",\"sn\":\"TEST_SN\",\"deviceItem\":\"TSP\",\"vehicleNodeCode\":\"TSP\",\"supplier\":\"SUP001\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\"}]}")
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
                .data("{\"vin\":\"TEST_VIN\",\"sn\":\"TEST_SN\",\"deviceItem\":\"TSP\",\"vehicleNodeCode\":\"TSP\",\"supplier\":\"SUP001\",\"ITEMS\":[{\"SN\":\"SN001\",\"vehicleNodeCode\":\"TSP\",\"deviceItem\":\"TSP\"}]}")
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
    @DisplayName("两段式导入应跳过vehicleNodeCode为空的下游联动")
    void testTwoStageImportSkipDownstreamWhenVehicleNodeCodeEmpty() {
        // 准备测试数据（vehicleNodeCode为空）
        PartImportData importDataWithoutVehicleNodeCode = PartImportData.builder()
                .id(1L)
                .batchNum("TEST_BATCH_001")
                .partCode("TEST_PART_001")
                .version("1.0")
                .data("{\"vin\":\"TEST_VIN\",\"sn\":\"TEST_SN\",\"deviceItem\":\"TSP\",\"supplier\":\"SUP001\",\"ITEMS\":[{\"SN\":\"SN001\",\"deviceItem\":\"TSP\"}]}")
                .handle(false)
                .build();

        String batchNum = "TEST_BATCH_001";
        Part mdmPart = Part.builder()
                .code("TEST_PART_001")
                .vehicleNodeCode("TSP")
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
}
