package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * VehicleProduceDataParserV1_0 单元测试
 * <p>
 * VMD-DSN-CR-028: 验证车辆安全常量预置集成行为
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@ExtendWith(MockitoExtension.class)
class VehicleProduceDataParserV1_0Test {

    @Mock
    private VehiclePublish vehiclePublish;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Mock
    private ImportDataParserRegistry parserRegistry;

    @Mock
    private VehicleSecurityPresetAppService vehicleSecurityPresetAppService;

    @Mock
    private VehicleOptionRepository vehicleOptionRepository;

    private VehicleProduceDataParserV1_0 parser;

    @BeforeEach
    void setUp() {
        parser = new VehicleProduceDataParserV1_0(
                vehiclePublish, vehBasicInfoRepository, parserRegistry, vehicleSecurityPresetAppService, vehicleOptionRepository);
    }

    @Test
    @DisplayName("成功导入后应调用安全常量预置")
    void testPresetCalledAfterSuccessfulProduce() {
        // Given
        String batchNum = "BATCH_001";
        String vin = "TEST_VIN_001";
        JSONObject dataJson = buildDataJson(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(vehBasicInfoRepository).insert(any(VehicleBasicInfo.class));
        verify(vehiclePublish).produce(vin);
        verify(vehicleSecurityPresetAppService).preset(vin, batchNum);
    }

    @Test
    @DisplayName("安全常量预置失败不应影响successCount")
    void testPresetFailureDoesNotAffectSuccessCount() {
        // Given
        String batchNum = "BATCH_002";
        String vin = "TEST_VIN_002";
        JSONObject dataJson = buildDataJson(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);
        doThrow(new RuntimeException("KMS/HSM unavailable"))
                .when(vehicleSecurityPresetAppService).preset(vin, batchNum);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(vehicleSecurityPresetAppService).preset(vin, batchNum);
    }

    @Test
    @DisplayName("VIN为空时应计入invalidCount")
    void testBlankVinIncrementsInvalidCount() {
        // Given
        String batchNum = "BATCH_003";
        JSONObject dataJson = buildDataJson("");

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());

        verify(vehicleSecurityPresetAppService, never()).preset(any(), any());
    }

    @Test
    @DisplayName("车辆数据处理失败时应计入failureCount且不调用预置")
    void testVehicleProcessingFailureDoesNotCallPreset() {
        // Given
        String batchNum = "BATCH_004";
        String vin = "TEST_VIN_004";
        JSONObject dataJson = buildDataJson(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenThrow(new RuntimeException("DB error"));

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(vehicleSecurityPresetAppService, never()).preset(any(), any());
    }

    @Test
    @DisplayName("多条记录混合场景应正确计数")
    void testMixedScenarioWithMultipleRecords() {
        // Given
        String batchNum = "BATCH_005";
        String vin1 = "VIN_001";
        String vin2 = "VIN_002";
        String vin3 = "";
        JSONObject dataJson = buildDataJsonWithMultipleVins(vin1, vin2, vin3);

        when(vehBasicInfoRepository.selectByVin(vin1)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);
        when(vehBasicInfoRepository.selectByVin(vin2)).thenThrow(new RuntimeException("DB error"));
        // vin3 is empty string - will be invalid

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());

        verify(vehicleSecurityPresetAppService).preset(vin1, batchNum);
    }

    @Test
    @DisplayName("成功导入后应保存车辆选项值快照")
    void shouldPersistVehicleOptionsDuringProduce() {
        // Given
        String batchNum = "BATCH_006";
        String vin = "TEST_VIN_006";
        JSONObject dataJson = buildDataJsonWithOptions(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());

        verify(vehicleOptionRepository).batchUpsert(argThat(optList ->
                optList.size() == 2 &&
                "COLOR".equals(optList.get(0).getOptionFamilyCode()) &&
                "RED".equals(optList.get(0).getOptionCode()) &&
                "PRODUCE".equals(optList.get(0).getSource()) &&
                "INTERIOR".equals(optList.get(1).getOptionFamilyCode()) &&
                "LEATHER".equals(optList.get(1).getOptionCode())
        ));
    }

    @Test
    @DisplayName("无OPTIONS数组时不应调用batchUpsert")
    void shouldNotCallBatchUpsertWhenNoOptions() {
        // Given
        String batchNum = "BATCH_007";
        String vin = "TEST_VIN_007";
        JSONObject dataJson = buildDataJson(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehicleOptionRepository, never()).batchUpsert(any());
    }

    @Test
    @DisplayName("选项值快照保存失败不应影响successCount")
    void testOptionPersistenceFailureDoesNotAffectSuccessCount() {
        // Given
        String batchNum = "BATCH_008";
        String vin = "TEST_VIN_008";
        JSONObject dataJson = buildDataJsonWithOptions(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);
        doThrow(new RuntimeException("DB batch upsert error"))
                .when(vehicleOptionRepository).batchUpsert(any());

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(vehicleOptionRepository).batchUpsert(any());
        verify(vehiclePublish).produce(vin);
        verify(vehicleSecurityPresetAppService).preset(vin, batchNum);
    }

    @Test
    @DisplayName("空OPTIONS数组时不应调用batchUpsert")
    void shouldNotCallBatchUpsertWhenEmptyOptions() {
        // Given
        String batchNum = "BATCH_009";
        String vin = "TEST_VIN_009";
        JSONObject dataJson = buildDataJsonWithEmptyOptions(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehicleOptionRepository, never()).batchUpsert(any());
    }

    @Test
    @DisplayName("OPTIONS缺少OPTION_FAMILY时应跳过该选项")
    void shouldSkipOptionWhenFamilyCodeMissing() {
        // Given
        String batchNum = "BATCH_010";
        String vin = "TEST_VIN_010";
        JSONObject dataJson = buildDataJsonWithMissingFamilyCode(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehicleOptionRepository, never()).batchUpsert(any());
    }

    @Test
    @DisplayName("OPTIONS缺少OPTION_CODE时应跳过该选项")
    void shouldSkipOptionWhenCodeMissing() {
        // Given
        String batchNum = "BATCH_011";
        String vin = "TEST_VIN_011";
        JSONObject dataJson = buildDataJsonWithMissingCode(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehicleOptionRepository, never()).batchUpsert(any());
    }

    @Test
    @DisplayName("OPTIONS混合有效和无效选项时应只保存有效选项")
    void shouldOnlyPersistValidOptions() {
        // Given
        String batchNum = "BATCH_012";
        String vin = "TEST_VIN_012";
        JSONObject dataJson = buildDataJsonWithMixedOptions(vin);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);
        when(vehBasicInfoRepository.insert(any(VehicleBasicInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getSuccessCount());
        verify(vehicleOptionRepository).batchUpsert(argThat(optList ->
                optList.size() == 1 &&
                "COLOR".equals(optList.get(0).getOptionFamilyCode()) &&
                "RED".equals(optList.get(0).getOptionCode())
        ));
    }

    private JSONObject buildDataJsonWithOptions(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");

        cn.hutool.json.JSONArray options = new cn.hutool.json.JSONArray();
        JSONObject option1 = new JSONObject();
        option1.set("OPTION_FAMILY", "COLOR");
        option1.set("OPTION_CODE", "RED");
        options.add(option1);
        JSONObject option2 = new JSONObject();
        option2.set("OPTION_FAMILY", "INTERIOR");
        option2.set("OPTION_CODE", "LEATHER");
        options.add(option2);
        item.set("OPTIONS", options);

        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJson(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithEmptyOptions(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");

        cn.hutool.json.JSONArray options = new cn.hutool.json.JSONArray();
        item.set("OPTIONS", options);

        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMissingFamilyCode(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");

        cn.hutool.json.JSONArray options = new cn.hutool.json.JSONArray();
        JSONObject option1 = new JSONObject();
        option1.set("OPTION_CODE", "RED");
        options.add(option1);
        item.set("OPTIONS", options);

        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMissingCode(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");

        cn.hutool.json.JSONArray options = new cn.hutool.json.JSONArray();
        JSONObject option1 = new JSONObject();
        option1.set("OPTION_FAMILY", "COLOR");
        options.add(option1);
        item.set("OPTIONS", options);

        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMixedOptions(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");

        cn.hutool.json.JSONArray options = new cn.hutool.json.JSONArray();
        // Valid option
        JSONObject option1 = new JSONObject();
        option1.set("OPTION_FAMILY", "COLOR");
        option1.set("OPTION_CODE", "RED");
        options.add(option1);
        // Invalid option - missing family code
        JSONObject option2 = new JSONObject();
        option2.set("OPTION_CODE", "BLACK");
        options.add(option2);
        // Invalid option - missing code
        JSONObject option3 = new JSONObject();
        option3.set("OPTION_FAMILY", "INTERIOR");
        options.add(option3);
        item.set("OPTIONS", options);

        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMultipleVins(String vin1, String vin2, String vin3) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        // Valid vin1
        JSONObject item1 = new JSONObject();
        item1.set("VIN", vin1);
        item1.set("PLANT", "P001");
        item1.set("BRAND", "B001");
        item1.set("PLATFORM", "PL001");
        item1.set("CAR_LINE", "CL001");
        item1.set("MODEL", "M001");
        item1.set("VARIANT", "V001");
        item1.set("CONFIGURATION", "C001");
        items.add(item1);

        // vin2 will cause exception
        JSONObject item2 = new JSONObject();
        item2.set("VIN", vin2);
        item2.set("PLANT", "P002");
        items.add(item2);

        // vin3 is empty - invalid
        JSONObject item3 = new JSONObject();
        item3.set("VIN", vin3);
        items.add(item3);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }
}
