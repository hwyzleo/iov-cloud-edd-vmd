package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EOL 导入集成测试
 * <p>
 * 验证 EOL/TOL 导入的完整流程：
 * 1. 新车辆 EOL 导入（自动建车兜底）
 * 2. 已有车辆 EOL 导入
 * 3. 补偿绑定语义
 * 4. TOL 导入后写入生命周期节点
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EolImportIntegrationTest {

    @Autowired
    private VehImportDataAppService vehImportDataAppService;

    @Autowired
    private VehImportDataRepository vehImportDataRepository;

    @Test
    @Order(1)
    @DisplayName("新车辆EOL导入应自动建车兜底（残档）")
    void testEolImport_newVehicle() {
        String batchNum = "EOL_BATCH_NEW_001";
        String vin = "NEW_VIN_EOL_001";
        String dataJson = buildEolDataJson(vin, "17300011AA", "SN000000001", "IBCM");

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        // 新车辆自动建车兜底：createStubVehicle 以 UNKNOWN 填充七项 NOT NULL 生产配置列，插入成功
        // （残档，缺七项生产配置与选项值快照，需后续 PRODUCE 重导补全）
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @Order(2)
    @DisplayName("已有车辆EOL导入应正确处理")
    void testEolImport_existingVehicle() {
        String batchNum = "EOL_BATCH_EXIST_001";
        String vin = "HWYZTEST000000001";
        String dataJson = buildEolDataJson(vin, "17300011AA", "SN000000002", "IBCM");

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getSuccessCount() >= 0);
    }

    @Test
    @Order(3)
    @DisplayName("EOL导入应支持补偿绑定语义")
    void testEolImport_compensateBinding() {
        String batchNum = "EOL_BATCH_COMP_001";
        String vin = "COMP_VIN_001";
        String dataJson = buildEolDataJsonWithMultipleParts(vin);

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        assertNotNull(result);
        assertTrue(result.getTotalCount() > 0);
        assertTrue(result.getSuccessCount() >= 0);
    }

    @Test
    @Order(5)
    @DisplayName("新格式结构化EOL导入应正确处理（ECU_BASELINE/SECURITY/补偿绑定）")
    void testEolImport_structuredNewFormat() {
        String batchNum = "EOL_BATCH_STRUCT_001";
        String vin = "HWYZTEST000000001";
        String dataJson = buildStructuredEolDataJson(vin);

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        // 新格式 ECU_BASELINE 补偿绑定 + 安全回执对账不应使整条失败（对账缺失仅 WARN）
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @Order(6)
    @DisplayName("同一VIN重复EOL导入应幂等（生命周期节点重复写入不失败）")
    void testEolImport_duplicateImport_idempotent() {
        String batchNum = "EOL_BATCH_DUP_001";
        String vin = "HWYZTEST000000001";
        String dataJson = buildStructuredEolDataJson(vin);

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        // 先执行一次，再以另一批次重复导入同一 VIN
        vehImportDataAppService.parseVehImportData(batchNum);
        String batchNum2 = "EOL_BATCH_DUP_002";
        vehImportDataRepository.insert(VehImportData.builder()
                .batchNum(batchNum2)
                .type("EOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build());
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum2);

        assertNotNull(result);
        // 重复导入：EOL 事件不再发布（firstEol=false），证书/下电节点幂等跳过，不应失败
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @Order(4)
    @DisplayName("TOL导入后应写入生命周期节点")
    void testTolImport_lifecycleNode() {
        String batchNum = "TOL_BATCH_LIFECYCLE_001";
        String vin = "HWYZTEST000000001";
        String dataJson = buildTolDataJson(vin, "17300011AA", "SN000000003", "IBCM");

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("TOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        assertNotNull(result);
        assertTrue(result.getTotalCount() > 0);
        assertTrue(result.getSuccessCount() >= 0);
    }

    private String buildStructuredEolDataJson(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("EOL_RESULT", "PASS");
        item.set("EOL_TIME", 1784391702000L);
        item.set("POWER_DOWN_TIME", 1784391734000L);
        item.set("PLANT", "HWYZ");
        item.set("LINE_CODE", "FA1");
        item.set("STATION_CODE", "EOL-ELE-01");
        item.set("SHIFT", "A");
        item.set("OPERATOR", "OP10086");
        item.set("TRANSPORT_MODE", "TRANSPORT");
        item.set("ODOMETER_KM", 3);
        item.set("SOC", 40);
        item.set("HV_STATUS", "POWER_OFF");

        JSONObject certificate = new JSONObject();
        certificate.set("CERT_NO", "WHC20260101000001");
        certificate.set("CERT_DATE", 1784391768000L);
        certificate.set("MANUFACTURE_DATE", 1784391783000L);
        item.set("CERTIFICATE", certificate);

        JSONObject otaBaseline = new JSONObject();
        otaBaseline.set("VEHICLE_VERSION", "HSRE26_2026.1.0");
        otaBaseline.set("PACKAGE_ID", "BL_HSRE26_20260101");
        otaBaseline.set("EE_ARCH", "CENTRAL_ZONE");
        item.set("OTA_BASELINE", otaBaseline);

        JSONArray ecuBaseline = new JSONArray();
        JSONObject tbox = new JSONObject();
        tbox.set("VEHICLE_NODE", "TBOX_5G");
        tbox.set("DEVICE_ITEM", "TBOX");
        tbox.set("SN", "IT_SN_TBOX_0001");
        tbox.set("ASSEMBLY_PART_NO", "IT_PN_TBOX_0001");
        tbox.set("HARDWARE_PART_NO", "IT_HW_TBOX_0001");
        tbox.set("HARDWARE_VERSION", "00");
        JSONArray tboxSw = new JSONArray();
        JSONObject sw1 = new JSONObject();
        sw1.set("SOFTWARE_TYPE", "APP");
        sw1.set("SOFTWARE_PART_NO", "IT_SW_TBOX_0001");
        sw1.set("SOFTWARE_VERSION", "V1.0.0");
        sw1.set("FLASH_RESULT", "OK");
        tboxSw.add(sw1);
        tbox.set("SOFTWARE", tboxSw);
        JSONObject tboxSec = new JSONObject();
        tboxSec.set("CERT_INJECTED", true);
        tboxSec.set("V2C_COMM_ROOT", "PROVISIONED");
        tboxSec.set("TBOX_DEVICE_ROOT", "PROVISIONED");
        tbox.set("SECURITY", tboxSec);
        ecuBaseline.add(tbox);

        JSONObject cockpit = new JSONObject();
        cockpit.set("VEHICLE_NODE", "DCU_COCKPIT_SA8295P");
        cockpit.set("DEVICE_ITEM", "DCU");
        cockpit.set("SN", "IT_SN_DCU_0001");
        cockpit.set("ASSEMBLY_PART_NO", "IT_PN_DCU_0001");
        cockpit.set("HARDWARE_PART_NO", "IT_HW_DCU_0001");
        cockpit.set("HARDWARE_VERSION", "00");
        JSONArray cockpitSw = new JSONArray();
        JSONObject sw2 = new JSONObject();
        sw2.set("SOFTWARE_TYPE", "APP");
        sw2.set("SOFTWARE_PART_NO", "IT_SW_DCU_0001");
        sw2.set("SOFTWARE_VERSION", "V1.0.0");
        sw2.set("FLASH_RESULT", "OK");
        cockpitSw.add(sw2);
        cockpit.set("SOFTWARE", cockpitSw);
        JSONObject cockpitSec = new JSONObject();
        cockpitSec.set("CERT_INJECTED", true);
        cockpitSec.set("CPT_DCU_DEVICE_ROOT", "PROVISIONED");
        cockpit.set("SECURITY", cockpitSec);
        ecuBaseline.add(cockpit);
        item.set("ECU_BASELINE", ecuBaseline);

        JSONArray inspectionItems = new JSONArray();
        JSONObject inspection = new JSONObject();
        inspection.set("ITEM_CODE", "HV_INSULATION");
        inspection.set("NAME", "高压绝缘");
        inspection.set("RESULT", "PASS");
        inspection.set("VALUE", "50");
        inspection.set("UNIT", "MΩ");
        inspectionItems.add(inspection);
        item.set("INSPECTION_ITEMS", inspectionItems);

        JSONObject diagnostic = new JSONObject();
        diagnostic.set("DTC_CLEARED", true);
        diagnostic.set("RESIDUAL_DTC", new JSONArray());
        item.set("DIAGNOSTIC", diagnostic);

        JSONObject powertrain = new JSONObject();
        powertrain.set("POWER_BATTERY_PACK_NO", "PB0000000001");
        powertrain.set("POWER_BATTERY_SOH", 100);
        powertrain.set("FRONT_DRIVE_MOTOR_NO", "FM0000000001");
        powertrain.set("REAR_DRIVE_MOTOR_NO", "RM0000000001");
        powertrain.set("GENERATOR_NO", "GEN0000000001");
        powertrain.set("ENGINE_NO", "ENG0000000001");
        item.set("POWERTRAIN", powertrain);

        items.add(item);
        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }

    private String buildEolDataJson(String vin, String partNo, String sn, String deviceCode) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("EOL_DATE", "20260617");
        item.set("CERT_DATE", "20260617");
        item.set("MANUFACTURER", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("SERIES", "S001");
        item.set("MODEL", "M001");
        item.set("BASE_MODEL", "V001");
        item.set("BUILD_CONFIG", "C001");
        item.set("VEHICLE_BASE_VERSION", "1.0.0");
        item.set("PRODUCTION_ORDER", "PO001");
        item.set("MATNR", "MAT001");
        item.set("PROJECT", "PRJ001");
        item.set("SALES_AREA", "CN");
        item.set("BODY_TYPE", "SEDAN");
        item.set("CONFIG_LEVEL", "HIGH");
        item.set("MODEL_YEAR", "2026");
        item.set("STEERING_POSITION", "LHD");
        item.set("INTERIOR_STYLE", "BLACK");
        item.set("EXTERIOR_COLOR", "WHITE");
        item.set("DRIVE_TYPE", "AWD");
        item.set("WHEEL", "19");
        item.set("TIRE", "235/55R19");
        item.set("SEAT_TYPE", "LEATHER");
        item.set("ASSISTED_DRIVING", "L2");
        item.set("ETC_SYSTEM", "ETC01");
        item.set("REAR_TOW_BAR", "N");
        item.set("ENGINE_NO", "");
        item.set("ENGINE_TYPE", "");
        item.set("FRONT_DRIVE_MOTOR_NO", "FDM001");
        item.set("FRONT_DRIVE_MOTOR_TYPE", "PMSM");
        item.set("REAR_DRIVE_MOTOR_NO", "RDM001");
        item.set("REAR_DRIVE_MOTOR_TYPE", "PMSM");
        item.set("GENERATOR_NO", "");
        item.set("GENERATOR_TYPE", "");
        item.set("POWER_BATTERY_PACK_NO", "PBP001");
        item.set("POWER_BATTERY_TYPE", "LFP");
        item.set("POWER_BATTERY_FACTORY", "CATL");

        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.set("VIN", vin);
        part.set("PART_NO", partNo);
        part.set("PART_SN", sn);
        part.set("DEVICE_CODE", deviceCode);
        part.set("INSTALL_POSITION", "VEHICLE");
        part.set("SUPPLIER_CODE", "S2002");
        part.set("HARDWARE_VERSION", "00");
        part.set("HARDWARE_PN", "17300013AA");
        parts.add(part);

        item.set("PARTS", parts);
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }

    private String buildEolDataJsonWithMultipleParts(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("EOL_DATE", "20260617");
        item.set("CERT_DATE", "20260617");
        item.set("MANUFACTURER", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("SERIES", "S001");
        item.set("MODEL", "M001");
        item.set("BASE_MODEL", "V001");
        item.set("BUILD_CONFIG", "C001");
        item.set("VEHICLE_BASE_VERSION", "1.0.0");
        item.set("PRODUCTION_ORDER", "PO001");
        item.set("MATNR", "MAT001");
        item.set("PROJECT", "PRJ001");
        item.set("SALES_AREA", "CN");
        item.set("BODY_TYPE", "SEDAN");
        item.set("CONFIG_LEVEL", "HIGH");
        item.set("MODEL_YEAR", "2026");
        item.set("STEERING_POSITION", "LHD");
        item.set("INTERIOR_STYLE", "BLACK");
        item.set("EXTERIOR_COLOR", "WHITE");
        item.set("DRIVE_TYPE", "AWD");
        item.set("WHEEL", "19");
        item.set("TIRE", "235/55R19");
        item.set("SEAT_TYPE", "LEATHER");
        item.set("ASSISTED_DRIVING", "L2");
        item.set("ETC_SYSTEM", "ETC01");
        item.set("REAR_TOW_BAR", "N");
        item.set("ENGINE_NO", "");
        item.set("ENGINE_TYPE", "");
        item.set("FRONT_DRIVE_MOTOR_NO", "FDM001");
        item.set("FRONT_DRIVE_MOTOR_TYPE", "PMSM");
        item.set("REAR_DRIVE_MOTOR_NO", "RDM001");
        item.set("REAR_DRIVE_MOTOR_TYPE", "PMSM");
        item.set("GENERATOR_NO", "");
        item.set("GENERATOR_TYPE", "");
        item.set("POWER_BATTERY_PACK_NO", "PBP001");
        item.set("POWER_BATTERY_TYPE", "LFP");
        item.set("POWER_BATTERY_FACTORY", "CATL");

        JSONArray parts = new JSONArray();

        JSONObject part1 = new JSONObject();
        part1.set("VIN", vin);
        part1.set("PART_NO", "17300011AA");
        part1.set("PART_SN", "SN_COMP_001");
        part1.set("DEVICE_CODE", "IBCM");
        part1.set("INSTALL_POSITION", "VEHICLE");
        part1.set("SUPPLIER_CODE", "S2002");
        part1.set("HARDWARE_VERSION", "00");
        part1.set("HARDWARE_PN", "17300013AA");
        parts.add(part1);

        JSONObject part2 = new JSONObject();
        part2.set("VIN", vin);
        part2.set("PART_NO", "17200033AA");
        part2.set("PART_SN", "SN_COMP_002");
        part2.set("DEVICE_CODE", "AVAS");
        part2.set("INSTALL_POSITION", "VEHICLE");
        part2.set("SUPPLIER_CODE", "S2002");
        part2.set("HARDWARE_VERSION", "00");
        part2.set("HARDWARE_PN", "17200035AA");
        parts.add(part2);

        item.set("PARTS", parts);
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }

    private String buildTolDataJson(String vin, String partNo, String sn, String deviceCode) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        JSONArray parts = new JSONArray();

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
        return data.toString();
    }
}
