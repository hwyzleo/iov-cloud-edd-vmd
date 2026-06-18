package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartBindingConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
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
 * VehicleTolDataParserV1_0 单元测试
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
    private VehicleNodeAppService vehicleNodeAppService;

    @Mock
    private PartInfoAppService partInfoAppService;

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    private VehicleTolDataParserV1_0 parser;

    @BeforeEach
    void setUp() {
        parser = new VehicleTolDataParserV1_0(
                parserRegistry, vehBasicInfoRepository, vehicleNodeAppService, partInfoAppService, vehiclePartAppService);
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
    @DisplayName("正常零件绑定应成功")
    void testSuccessfulPartBinding() {
        // Given
        String batchNum = "BATCH_001";
        String vin = "TEST_VIN_001";
        String partNo = "17300011AA";
        String sn = "SN000000001";
        String deviceCode = "IBCM";
        JSONObject dataJson = buildDataJson(vin, partNo, sn, deviceCode);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        VehicleNode vehicleNode = VehicleNode.builder().code(deviceCode).deviceItem("IBCM_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode(deviceCode)).thenReturn(vehicleNode);
        // 模拟 upsertPartInfo 设置 ID
        doAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
        }).when(partInfoAppService).upsertPartInfo(any(PartInfo.class));

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
        String partNo = "17300011AA";
        String sn = "SN000000001";
        String deviceCode = "IBCM";
        JSONObject dataJson = buildDataJson(vin, partNo, sn, deviceCode);

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

        verify(vehicleNodeAppService, never()).getVehicleNodeByCode(any());
        verify(partInfoAppService, never()).upsertPartInfo(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    @DisplayName("车载节点不存在时应计入failureCount")
    void testVehicleNodeNotExistShouldIncrementFailureCount() {
        // Given
        String batchNum = "BATCH_003";
        String vin = "TEST_VIN_003";
        String partNo = "17300011AA";
        String sn = "SN000000001";
        String deviceCode = "INVALID_CODE";
        JSONObject dataJson = buildDataJson(vin, partNo, sn, deviceCode);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        when(vehicleNodeAppService.getVehicleNodeByCode(deviceCode)).thenReturn(null);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        assertNotNull(result.getDescription());
        assertTrue(result.getDescription().contains("车载节点[" + deviceCode + "]不存在"));

        verify(partInfoAppService, never()).upsertPartInfo(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    @DisplayName("零件跨VIN冲突时应计入failureCount")
    void testPartBindingConflictShouldIncrementFailureCount() {
        // Given
        String batchNum = "BATCH_004";
        String vin = "TEST_VIN_004";
        String partNo = "17300011AA";
        String sn = "SN000000001";
        String deviceCode = "IBCM";
        JSONObject dataJson = buildDataJson(vin, partNo, sn, deviceCode);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        VehicleNode vehicleNode = VehicleNode.builder().code(deviceCode).deviceItem("IBCM_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode(deviceCode)).thenReturn(vehicleNode);
        when(partInfoAppService.upsertPartInfo(any(PartInfo.class)))
                .thenThrow(new PartBindingConflictException("零件已绑定其他VIN"));

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
        assertNotNull(result.getDescription());
        assertTrue(result.getDescription().contains("零件[" + partNo + "]已绑定其他VIN"));
    }

    @Test
    @DisplayName("必填字段为空时应计入invalidCount")
    void testBlankRequiredFieldsShouldIncrementInvalidCount() {
        // Given
        String batchNum = "BATCH_004";
        String vin = "TEST_VIN_004";
        JSONObject dataJson = buildDataJson(vin, "", "SN000000001", "IBCM");

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
    @DisplayName("单个ITEM多个PARTS应正确计数")
    void testSingleItemWithMultipleParts() {
        // Given
        String batchNum = "BATCH_006";
        String vin = "TEST_VIN_006";
        JSONObject dataJson = buildDataJsonWithMultipleParts(vin);

        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder().vin(vin).build();
        when(vehBasicInfoRepository.selectByVin(vin)).thenReturn(vehicleBasicInfo);
        VehicleNode vehicleNode = VehicleNode.builder().code("IBCM").deviceItem("IBCM_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("IBCM")).thenReturn(vehicleNode);
        VehicleNode vehicleNode2 = VehicleNode.builder().code("AVAS").deviceItem("AVAS_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("AVAS")).thenReturn(vehicleNode2);
        VehicleNode vehicleNode3 = VehicleNode.builder().code("WCM_L").deviceItem("WCM_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("WCM_L")).thenReturn(vehicleNode3);
        // 模拟 upsertPartInfo 设置 ID
        doAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
        }).when(partInfoAppService).upsertPartInfo(any(PartInfo.class));

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
    }

    @Test
    @DisplayName("多ITEM混合场景应正确计数")
    void testMixedScenarioWithMultipleItems() {
        // Given
        String batchNum = "BATCH_007";
        String vin1 = "VIN_001";
        String vin2 = "VIN_002";
        JSONObject dataJson = buildDataJsonWithMultipleItems(vin1, vin2);

        VehicleBasicInfo vehicleBasicInfo1 = VehicleBasicInfo.builder().vin(vin1).build();
        when(vehBasicInfoRepository.selectByVin(vin1)).thenReturn(vehicleBasicInfo1);
        VehicleNode vehicleNode = VehicleNode.builder().code("IBCM").deviceItem("IBCM_TYPE").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("IBCM")).thenReturn(vehicleNode);
        // 模拟 upsertPartInfo 设置 ID
        doAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return 1;
        }).when(partInfoAppService).upsertPartInfo(any(PartInfo.class));

        when(vehBasicInfoRepository.selectByVin(vin2)).thenReturn(null);

        // When
        ImportResult result = parser.parse(batchNum, dataJson);

        // Then
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());
    }

    private JSONObject buildDataJson(String vin, String partNo, String sn, String deviceCode) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        cn.hutool.json.JSONArray parts = new cn.hutool.json.JSONArray();

        JSONObject part = new JSONObject();
        part.set("VIN", vin);
        part.set("PART_NO", partNo);
        part.set("SN", sn);
        part.set("DEVICE_CODE", deviceCode);
        part.set("INSTALL_POSITION", "VEHICLE");
        part.set("SUPPLIER_CODE", "S2002");
        part.set("HARDWARE_VERSION", "00");
        part.set("HARDWARE_NO", "17300013AA");
        parts.add(part);

        item.set("PARTS", parts);
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

    private JSONObject buildDataJsonWithMultipleParts(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        cn.hutool.json.JSONArray parts = new cn.hutool.json.JSONArray();

        // Part 1 - IBCM
        JSONObject part1 = new JSONObject();
        part1.set("VIN", vin);
        part1.set("PART_NO", "17300011AA");
        part1.set("SN", "SN000000001");
        part1.set("DEVICE_CODE", "IBCM");
        part1.set("INSTALL_POSITION", "VEHICLE");
        parts.add(part1);

        // Part 2 - AVAS
        JSONObject part2 = new JSONObject();
        part2.set("VIN", vin);
        part2.set("PART_NO", "17200033AA");
        part2.set("SN", "SN000000002");
        part2.set("DEVICE_CODE", "AVAS");
        part2.set("INSTALL_POSITION", "VEHICLE");
        parts.add(part2);

        // Part 3 - WCM_L
        JSONObject part3 = new JSONObject();
        part3.set("VIN", vin);
        part3.set("PART_NO", "17000687AA");
        part3.set("SN", "SN000000003");
        part3.set("DEVICE_CODE", "WCM_L");
        part3.set("INSTALL_POSITION", "VEHICLE");
        parts.add(part3);

        item.set("PARTS", parts);
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }

    private JSONObject buildDataJsonWithMultipleItems(String vin1, String vin2) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        // Item 1 - valid
        JSONObject item1 = new JSONObject();
        item1.set("VIN", vin1);
        cn.hutool.json.JSONArray parts1 = new cn.hutool.json.JSONArray();
        JSONObject part1 = new JSONObject();
        part1.set("VIN", vin1);
        part1.set("PART_NO", "17300011AA");
        part1.set("SN", "SN000000001");
        part1.set("DEVICE_CODE", "IBCM");
        part1.set("INSTALL_POSITION", "VEHICLE");
        parts1.add(part1);
        item1.set("PARTS", parts1);
        items.add(item1);

        // Item 2 - VIN not exist
        JSONObject item2 = new JSONObject();
        item2.set("VIN", vin2);
        cn.hutool.json.JSONArray parts2 = new cn.hutool.json.JSONArray();
        JSONObject part2 = new JSONObject();
        part2.set("VIN", vin2);
        part2.set("PART_NO", "17200033AA");
        part2.set("SN", "SN000000002");
        part2.set("DEVICE_CODE", "AVAS");
        part2.set("INSTALL_POSITION", "VEHICLE");
        parts2.add(part2);
        item2.set("PARTS", parts2);
        items.add(item2);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }
}
