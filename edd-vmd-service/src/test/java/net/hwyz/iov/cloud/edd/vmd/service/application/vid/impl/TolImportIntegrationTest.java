package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOL 导入集成测试
 * <p>
 * VMD-DSN-CR-029: 总装上线 ECU 清单导入与 ECU↔VIN 幂等绑定。
 * 覆盖：真实 MES 报文字段解析、同零件重导幂等、跨 VIN 绑定冲突、装车槽位冲突。
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TolImportIntegrationTest {

    @Autowired
    private VehImportDataAppService vehImportDataAppService;

    @Autowired
    private VehImportDataRepository vehImportDataRepository;

    @Autowired
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Autowired
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;

    @Autowired
    private VehiclePartAppService vehiclePartAppService;

    private static final String NODE_TBOX = "TBOX_5G";

    @Test
    @DisplayName("TOL导入应正确解析真实MES报文格式ECU清单")
    void testTolImportShouldParseEcuList() {
        // Given
        String batchNum = "TOL_BATCH_001";
        String vin = "TEST_VIN_001";
        prepareVin(vin);
        prepareNode(NODE_TBOX, "TBOX");
        insertImportData(batchNum, buildTolDataJson(vin,
                partJson("00000001AA", "00000002AA", "00", "SUP00000001", "00000002AA00000001", NODE_TBOX, "TBOX", "VEHICLE")));

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        // 生成一条 active 绑定
        VehiclePart binding = vehiclePartAppService.getActiveBindingByVinAndNodeCode(vin, NODE_TBOX);
        assertNotNull(binding);
        assertEquals("VEHICLE", binding.getPosition());
    }

    @Test
    @DisplayName("同零件重导应幂等命中不产生重复active绑定")
    void testReimportSamePartIsIdempotent() {
        // Given
        String vin = "TEST_VIN_002";
        prepareVin(vin);
        prepareNode(NODE_TBOX, "TBOX");
        String dataJson = buildTolDataJson(vin,
                partJson("00000001AA", "00000002AA", "00", "SUP00000001", "00000002AA00000001", NODE_TBOX, "TBOX", "VEHICLE"));

        // 第一次导入
        insertImportData("TOL_BATCH_IDEMPOTENT_001", dataJson);
        ImportResult first = vehImportDataAppService.parseVehImportData("TOL_BATCH_IDEMPOTENT_001");
        assertEquals(1, first.getSuccessCount());
        assertEquals(0, first.getFailureCount());

        // When：同一零件（同 SN）用新批次重导
        insertImportData("TOL_BATCH_IDEMPOTENT_002", dataJson);
        ImportResult second = vehImportDataAppService.parseVehImportData("TOL_BATCH_IDEMPOTENT_002");

        // Then：幂等命中视为成功，不产生第二条 active 绑定
        assertEquals(1, second.getSuccessCount());
        assertEquals(0, second.getFailureCount());
        VehiclePart binding = vehiclePartAppService.getActiveBindingByVinAndNodeCode(vin, NODE_TBOX);
        assertNotNull(binding);
        assertEquals(1, vehiclePartAppService.getActiveBindingsByVin(vin).size());
    }

    @Test
    @DisplayName("零件已绑定其他VIN应报绑定冲突且不落库")
    void testPartBoundToOtherVinConflicts() {
        // Given
        String vinA = "TEST_VIN_003A";
        String vinB = "TEST_VIN_003B";
        prepareVin(vinA);
        prepareVin(vinB);
        prepareNode(NODE_TBOX, "TBOX");
        String sn = "00000002AA00000001";

        // 零件先绑定到 VIN_A
        insertImportData("TOL_BATCH_CONFLICT_001", buildTolDataJson(vinA,
                partJson("00000001AA", "00000002AA", "00", "SUP00000001", sn, NODE_TBOX, "TBOX", "VEHICLE")));
        ImportResult first = vehImportDataAppService.parseVehImportData("TOL_BATCH_CONFLICT_001");
        assertEquals(1, first.getSuccessCount());

        // When：同一零件（同 SN）尝试绑定到 VIN_B
        insertImportData("TOL_BATCH_CONFLICT_002", buildTolDataJson(vinB,
                partJson("00000001AA", "00000002AA", "00", "SUP00000001", sn, NODE_TBOX, "TBOX", "VEHICLE")));
        ImportResult second = vehImportDataAppService.parseVehImportData("TOL_BATCH_CONFLICT_002");

        // Then：跨 VIN 冲突计入 failure，VIN_B 无绑定
        assertEquals(1, second.getTotalCount());
        assertEquals(0, second.getSuccessCount());
        assertEquals(1, second.getFailureCount());
        assertNull(vehiclePartAppService.getActiveBindingByVinAndNodeCode(vinB, NODE_TBOX));
        assertNotNull(vehiclePartAppService.getActiveBindingByVinAndNodeCode(vinA, NODE_TBOX));
    }

    @Test
    @DisplayName("装车槽位已被其他零件占用应报绑定冲突")
    void testSlotOccupiedByOtherPartConflicts() {
        // Given
        String vin = "TEST_VIN_004";
        prepareVin(vin);
        prepareNode(NODE_TBOX, "TBOX");

        // 槽位先被零件 SN1 占用
        insertImportData("TOL_BATCH_SLOT_001", buildTolDataJson(vin,
                partJson("00000001AA", "00000002AA", "00", "SUP00000001", "00000002AA00000001", NODE_TBOX, "TBOX", "VEHICLE")));
        ImportResult first = vehImportDataAppService.parseVehImportData("TOL_BATCH_SLOT_001");
        assertEquals(1, first.getSuccessCount());

        // When：同一槽位（同VIN同节点）被零件 SN2 占用
        insertImportData("TOL_BATCH_SLOT_002", buildTolDataJson(vin,
                partJson("00000001AA", "00000002AB", "00", "SUP00000001", "00000002AB00000001", NODE_TBOX, "TBOX", "VEHICLE")));
        ImportResult second = vehImportDataAppService.parseVehImportData("TOL_BATCH_SLOT_002");

        // Then：槽位冲突计入 failure，active 绑定仍为 1 条（SN1）
        assertEquals(1, second.getFailureCount());
        assertEquals(0, second.getSuccessCount());
        assertEquals(1, vehiclePartAppService.getActiveBindingsByVin(vin).size());
    }

    @Test
    @DisplayName("重复批次号应跳过处理")
    void testDuplicateBatchNumShouldSkip() {
        // Given
        String batchNum = "TOL_BATCH_DUP";
        insertImportData(batchNum, "{}", true);

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getDescription().contains("已处理"));
    }

    private void insertImportData(String batchNum, String dataJson) {
        insertImportData(batchNum, dataJson, false);
    }

    private void insertImportData(String batchNum, String dataJson, boolean handled) {
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData vehImportData =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData.builder()
                        .batchNum(batchNum)
                        .type("TOL")
                        .version("1.0")
                        .data(dataJson)
                        .handle(handled)
                        .build();
        vehImportDataRepository.insert(vehImportData);
    }

    /**
     * 准备车辆基础信息（TOL 导入要求 VIN 已存在）
     */
    private void prepareVin(String vin) {
        VehicleBasicInfo vehicleBasicInfo = VehicleBasicInfo.builder()
                .vin(vin)
                .plantCode("P001")
                .brandCode("B001")
                .platformCode("PL001")
                .carLineCode("CL001")
                .modelCode("M001")
                .variantCode("V001")
                .configurationCode("C001")
                .build();
        vehBasicInfoRepository.insert(vehicleBasicInfo);
    }

    /**
     * 准备车载节点（TOL 导入要求 VEHICLE_NODE 已存在于 MDM 投影）
     */
    private void prepareNode(String code, String deviceCategory) {
        if (mdmVehicleNodeRepository.selectByCode(code) != null) {
            return;
        }
        VehicleNode vehicleNode = VehicleNode.builder()
                .code(code)
                .name(code)
                .deviceCategory(deviceCategory)
                .funcDomain("VEHICLE")
                .nodeType("ECU")
                .otaSupport("OTA")
                .sort(1)
                .source(SourceType.MDM)
                .build();
        mdmVehicleNodeRepository.insert(vehicleNode);
    }

    /**
     * 构建真实 MES 报文格式的 TOL 数据
     */
    private String buildTolDataJson(String vin, JSONObject... partJsonArray) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();
        cn.hutool.json.JSONArray parts = new cn.hutool.json.JSONArray();
        for (JSONObject part : partJsonArray) {
            parts.add(part);
        }

        JSONObject item = new JSONObject();
        item.set("UUID", "33E7101BAE76681CE063660CFF0A25FA");
        item.set("VIN", vin);
        item.set("PARTS", parts);
        items.add(item);

        dataObj.set("BATCH_NO", "20260101000000");
        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }

    /**
     * 构建真实 MES 报文格式的 PART 节点
     */
    private JSONObject partJson(String assemblyPartNo, String hardwarePartNo, String hardwareVersion,
                                String supplierCode, String sn, String vehicleNode, String deviceItem,
                                String installPosition) {
        JSONObject part = new JSONObject();
        part.set("ASSEMBLY_PART_NO", assemblyPartNo);
        part.set("HARDWARE_PART_NO", hardwarePartNo);
        part.set("HARDWARE_VERSION", hardwareVersion);
        part.set("SUPPLIER_CODE", supplierCode);
        part.set("SN", sn);
        part.set("VEHICLE_NODE", vehicleNode);
        part.set("DEVICE_ITEM", deviceItem);
        part.set("INSTALL_POSITION", installPosition);
        return part;
    }
}
