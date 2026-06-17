package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * VehImportDataAppService 单元测试
 * <p>
 * VMD-DSN-CR-027: 车辆数据导入域六步流水线测试
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@ExtendWith(MockitoExtension.class)
class VehImportDataAppServiceTest {

    @Mock
    private VehImportDataRepository vehImportDataRepository;

    @Mock
    private ImportDataParserRegistry parserRegistry;

    @Mock
    private VehicleImportDataParser importDataParser;

    private VehImportDataAppService vehImportDataAppService;

    @BeforeEach
    void setUp() {
        vehImportDataAppService = new VehImportDataAppService(vehImportDataRepository, parserRegistry);
    }

    @Test
    @DisplayName("六步流水线应成功处理PRODUCE类型导入")
    void testSixStepPipelineSuccess() {
        // 准备测试数据
        String batchNum = "TEST_BATCH_001";
        VehImportData importData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":[{\"VIN\":\"TEST_VIN_001\"}]}}}")
                .handle(false)
                .createTime(LocalDateTime.now())
                .build();

        ImportResult expectedResult = ImportResult.builder()
                .totalCount(1)
                .successCount(1)
                .failureCount(0)
                .build();

        // 设置mock行为
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(parserRegistry.getParser("PRODUCE", "1.0")).thenReturn(importDataParser);
        when(importDataParser.parse(eq(batchNum), any(JSONObject.class))).thenReturn(expectedResult);

        // 执行测试
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 验证解析器被调用
        verify(importDataParser).parse(eq(batchNum), any(JSONObject.class));

        // 验证handle被设置为true
        verify(vehImportDataRepository).update(argThat(veh -> Boolean.TRUE.equals(veh.getHandle())));
    }

    @Test
    @DisplayName("六步流水线应处理批次号不存在的情况")
    void testSixStepPipelineBatchNumNotFound() {
        // 准备测试数据
        String batchNum = "NON_EXIST_BATCH";

        // 设置mock行为
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // 执行测试
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription().contains("不存在"));

        // 验证解析器未被调用
        verify(parserRegistry, never()).getParser(anyString(), anyString());
    }

    @Test
    @DisplayName("六步流水线应跳过已处理的记录（幂等检查）")
    void testSixStepPipelineIdempotentCheck() {
        // 准备测试数据 - 已处理的记录
        String batchNum = "PROCESSED_BATCH";
        VehImportData importData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(true)  // 已处理
                .createTime(LocalDateTime.now())
                .build();

        // 设置mock行为
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);

        // 执行测试
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getDescription().contains("已处理"));

        // 验证解析器未被调用
        verify(parserRegistry, never()).getParser(anyString(), anyString());
    }

    @Test
    @DisplayName("六步流水线应处理解析器异常")
    void testSixStepPipelineParserException() {
        // 准备测试数据
        String batchNum = "EXCEPTION_BATCH";
        VehImportData importData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[{\"VIN\":\"VIN001\"}]}}}")
                .handle(false)
                .createTime(LocalDateTime.now())
                .build();

        // 设置mock行为
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(parserRegistry.getParser("PRODUCE", "1.0")).thenReturn(importDataParser);
        when(importDataParser.parse(eq(batchNum), any(JSONObject.class)))
                .thenThrow(new RuntimeException("解析失败"));

        // 执行测试
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getDescription().contains("解析失败"));

        // 验证description被截断并保存
        verify(vehImportDataRepository).update(argThat(veh -> 
                veh.getDescription() != null && veh.getDescription().contains("解析失败")));
    }

    @Test
    @DisplayName("description截断应正确工作")
    void testDescriptionTruncation() {
        // 准备测试数据 - 超长description
        String batchNum = "LONG_DESC_BATCH";
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longDesc.append("A");
        }
        
        VehImportData importData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(false)
                .createTime(LocalDateTime.now())
                .build();

        // 设置mock行为
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(importData);
        when(parserRegistry.getParser("PRODUCE", "1.0")).thenReturn(importDataParser);
        when(importDataParser.parse(eq(batchNum), any(JSONObject.class)))
                .thenThrow(new RuntimeException(longDesc.toString()));

        // 执行测试
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getFailureCount());

        // 验证description被截断到500字符以内
        verify(vehImportDataRepository).update(argThat(veh -> 
                veh.getDescription() != null && veh.getDescription().length() <= 500));
    }

    @Test
    @DisplayName("查询应返回正确的DTO列表")
    void testSearch() {
        // 准备测试数据
        VehImportData importData1 = VehImportData.builder()
                .id(1L)
                .batchNum("BATCH_001")
                .type("PRODUCE")
                .version("1.0")
                .handle(false)
                .createTime(LocalDateTime.now())
                .build();

        VehImportData importData2 = VehImportData.builder()
                .id(2L)
                .batchNum("BATCH_002")
                .type("PRODUCE")
                .version("1.0")
                .handle(true)
                .createTime(LocalDateTime.now())
                .build();

        // 设置mock行为
        when(vehImportDataRepository.selectList(any(VehImportData.class)))
                .thenReturn(Arrays.asList(importData1, importData2));

        // 执行测试
        var query = net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehImportDataQuery.builder()
                .type("PRODUCE")
                .build();
        List<VehImportDataDto> result = vehImportDataAppService.search(query);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("BATCH_001", result.get(0).getBatchNum());
        assertEquals("BATCH_002", result.get(1).getBatchNum());
    }

    @Test
    @DisplayName("批量删除应正确调用仓储")
    void testDeleteByIds() {
        // 准备测试数据
        Long[] ids = {1L, 2L, 3L};

        // 设置mock行为
        when(vehImportDataRepository.deleteByIds(ids)).thenReturn(3);

        // 执行测试
        int result = vehImportDataAppService.deleteVehImportDataByIds(ids);

        // 验证结果
        assertEquals(3, result);
        verify(vehImportDataRepository).deleteByIds(ids);
    }

    @Test
    @DisplayName("批次号唯一性检查应正确工作")
    void testCheckBatchNumUnique() {
        // 设置mock行为
        when(vehImportDataRepository.checkBatchNumUnique(null, "NEW_BATCH")).thenReturn(true);
        when(vehImportDataRepository.checkBatchNumUnique(null, "EXIST_BATCH")).thenReturn(false);

        // 执行测试
        assertTrue(vehImportDataAppService.checkBatchNumUnique(null, "NEW_BATCH"));
        assertFalse(vehImportDataAppService.checkBatchNumUnique(null, "EXIST_BATCH"));
    }
}
