package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartBindingConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TolEcuListParserV1_0 单元测试
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@ExtendWith(MockitoExtension.class)
class TolEcuListParserV1_0Test {

    @Mock
    private ImportDataParserRegistry parserRegistry;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Mock
    private PartInfoAppService partInfoAppService;

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    private TolEcuListParserV1_0 parser;

    @BeforeEach
    void setUp() {
        parser = new TolEcuListParserV1_0(
                parserRegistry, vehBasicInfoRepository, partInfoAppService, vehiclePartAppService);
    }

    @Test
    @DisplayName("解析器类型应为TOL")
    void testGetType() {
        assertEquals("TOL", parser.getType());
    }

    @Test
    @DisplayName("解析器版本应为1.0")
    void testGetVersion() {
        assertEquals("1.0", parser.getVersion());
    }

    @Test
    @DisplayName("正常ECU绑定应成功")
    void testSuccessfulEcuBinding() {
        // Given
        String batchNum = "BATCH_001";
        String vin = "TEST_VIN_001";
        String ecuSn = "ECU_SN_001";
        String partCode = "PART_001";
        String vehicleNodeCode = "TBOX_5G";
        JSONObject dataJson = buildDataJson(vin, ecuSn, partCode, vehicleNodeCode);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(partInfoAppService).upsertPartInfo(any(PartInfo.class));
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }

    @Test
    @DisplayName("VIN不存在时应计入failureCount")
    void testVinNotExistShouldIncrementFailureCount() {
        // Given
        String batchNum = "BATCH_002";
        String vin = "NOT_EXIST_VIN";
        String ecuSn = "ECU_SN_002";
        String partCode = "PART_002";
        String vehicleNodeCode = "TBOX_5G";
        JSONObject dataJson = buildDataJson(vin, ecuSn, partCode, vehicleNodeCode);

        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(null);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        assertNotNull(result.getDescription());
        assertTrue(result.getDescription().contains("VIN[" + vin + "]不存在"));

        verify(partInfoAppService, never()).upsertPartInfo(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    @DisplayName("ECU跨VIN冲突时应计入failureCount")
    void testEcuBindingConflictShouldIncrementFailureCount() {
        // Given
        String batchNum = "BATCH_003";
        String vin = "TEST_VIN_003";
        String ecuSn = "ECU_SN_003";
        String partCode = "PART_003";
        String vehicleNodeCode = "TBOX_5G";
        JSONObject dataJson = buildDataJson(vin, ecuSn, partCode, vehicleNodeCode);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class)))
                .thenThrow(new PartBindingConflictException("ECU已绑定其他VIN"));

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        assertNotNull(result.getDescription());
        assertTrue(result.getDescription().contains("ECU[" + ecuSn + "]已绑定其他VIN"));
    }

    @Test
    @DisplayName("必填字段为空时应计入invalidCount")
    void testBlankRequiredFieldsShouldIncrementInvalidCount() {
        // Given
        String batchNum = "BATCH_004";
        JSONObject dataJson = buildDataJson("", "ECU_SN_004", "PART_004", "TBOX_5G");

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());

        verify(vehBasicInfoRepository, never()).selectByVin(any());
    }

    @Test
    @DisplayName("ITEMS为空时应返回空结果")
    void testEmptyItemsShouldReturnEmptyResult() {
        // Given
        String batchNum = "BATCH_005";
        JSONObject dataJson = buildEmptyDataJson();

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
    }

    @Test
    @DisplayName("多条记录混合场景应正确计数")
    void testMixedScenarioWithMultipleRecords() {
        // Given
        String batchNum = "BATCH_006";
        String vin1 = "VIN_001";
        String vin2 = "VIN_002";
        String vin3 = "";
        JSONObject dataJson = buildDataJsonWithMultipleRecords(vin1, vin2, vin3);

        VehicleBasicInfo vehicleBasicInfo1 = VehicleBasicInfo.builder().vin(vin1).build();
        when(vehBasicInfoRepository.selectByVin(vin1)).thenReturn(vehicleBasicInfo1);
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class))).thenReturn(1);

        when(vehBasicInfoRepository.selectByVin(vin2)).thenReturn(null);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getInvalidCount());
    }

    private JSONObject buildDataJson(String vin, String ecuSn, String partCode, String vehicleNodeCode) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("ECU_SN", ecuSn);
        item.set("PART_CODE", partCode);
        item.set("VEHICLE_NODE_CODE", vehicleNodeCode);
        item.set("POSITION", "POS_001");
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildEmptyDataJson() {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMultipleRecords(String vin1, String vin2, String vin3) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        // Valid record 1
        JSONObject item1 = new JSONObject();
        item1.set("VIN", vin1);
        item1.set("ECU_SN", "ECU_SN_001");
        item1.set("PART_CODE", "PART_001");
        item1.set("VEHICLE_NODE_CODE", "TBOX_5G");
        item1.set("POSITION", "POS_001");
        items.add(item1);

        // VIN not exist
        JSONObject item2 = new JSONObject();
        item2.set("VIN", vin2);
        item2.set("ECU_SN", "ECU_SN_002");
        item2.set("PART_CODE", "PART_002");
        item2.set("VEHICLE_NODE_CODE", "TBOX_5G");
        item2.set("POSITION", "POS_002");
        items.add(item2);

        // Invalid - empty VIN
        JSONObject item3 = new JSONObject();
        item3.set("VIN", vin3);
        item3.set("ECU_SN", "ECU_SN_003");
        item3.set("PART_CODE", "PART_003");
        item3.set("VEHICLE_NODE_CODE", "TBOX_5G");
        item3.set("POSITION", "POS_003");
        items.add(item3);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }
}
