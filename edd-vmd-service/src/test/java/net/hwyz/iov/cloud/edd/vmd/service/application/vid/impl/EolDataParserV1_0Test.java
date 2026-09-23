package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.EolResultGateService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SecurityProvisionConfirmService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSecurityPresetAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EolDataParserV1_0 单元测试
 * <p>
 * VMD-DSN-CR-035: 验证 EOL 解析器的自动建车兜底、补偿绑定、安全预置行为
 * CR-051: 安全回执对账（车辆级 ROOT/IMMO/OTA + 器件级 ROOT 双层对账）、补偿绑定转换
 *
 * @author hwyz_leo
 * @since 2026-06-30
 */
@ExtendWith(MockitoExtension.class)
class EolDataParserV1_0Test {

    @Mock
    private VehiclePublish vehiclePublish;
    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;
    @Mock
    private VehicleInfoExtractor vehicleInfoExtractor;
    @Mock
    private VehicleInfoPersister vehicleInfoPersister;
    @Mock
    private VehiclePartBinder vehiclePartBinder;
    @Mock
    private VehicleLifecycleAppService vehicleLifecycleAppService;
    @Mock
    private VehicleSecurityPresetAppService vehicleSecurityPresetAppService;
    @Mock
    private ImportDataParserRegistry parserRegistry;
    @Mock
    private EolResultGateService eolResultGateService;
    @Mock
    private SoftwareInventoryAppService softwareInventoryAppService;
    @Mock
    private SecurityProvisionConfirmService securityProvisionConfirmService;
    @Mock
    private VehiclePartAppService vehiclePartAppService;

    private EolDataParserV1_0 parser;

    private static final String VIN = "HWYZTEST000000001";
    private static final String BATCH_NUM = "BATCH001";

    @BeforeEach
    void setUp() {
        parser = new EolDataParserV1_0(
                vehiclePublish, vehBasicInfoRepository, vehicleInfoExtractor,
                vehicleInfoPersister, vehiclePartBinder, vehicleLifecycleAppService,
                vehicleSecurityPresetAppService, parserRegistry, eolResultGateService,
                softwareInventoryAppService, securityProvisionConfirmService, vehiclePartAppService);
    }

    @Test
    @DisplayName("已有车辆的 EOL 处理应正常提取、持久化并发布事件")
    void testEolWithExistingVehicle() {
        JSONObject dataJson = buildEolDataJson(VIN);

        VehicleBasicInfo existingInfo = VehicleBasicInfo.builder().id(1L).vin(VIN).build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(existingInfo);
        when(vehBasicInfoRepository.selectDetailByVin(VIN)).thenReturn(Collections.emptyList());

        VehicleBasicInfo extractedInfo = VehicleBasicInfo.builder().id(1L).vin(VIN).build();
        when(vehicleInfoExtractor.extractBasicInfo(any(), eq(existingInfo), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(extractedInfo);
        when(vehicleInfoExtractor.extractDetails(any(), anyMap(), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(Collections.emptyList());
        when(vehicleInfoPersister.persist(any(), anyList())).thenReturn(false);
        when(vehiclePartBinder.bindParts(any(), eq(VIN), eq(BATCH_NUM))).thenReturn(Collections.emptyList());

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getInvalidCount());

        verify(vehicleInfoExtractor).extractBasicInfo(any(), eq(existingInfo), eq(BATCH_NUM), eq(VIN));
        verify(vehicleInfoExtractor, never()).createStubVehicle(any(), any(), any());
        verify(vehiclePublish).eol(eq(VIN), any(Instant.class));
        verify(vehiclePublish, never()).produce(anyString(), anyString());
        verify(vehicleSecurityPresetAppService, never()).preset(anyString(), anyString());
    }

    @Test
    @DisplayName("VIN 不存在时应自动建车兜底（残档）并发布补发 PRODUCE 事件")
    void testEolWithNewVehicle_autoCreateStub() {
        JSONObject dataJson = buildEolDataJson(VIN);

        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        when(vehBasicInfoRepository.selectDetailByVin(VIN)).thenReturn(Collections.emptyList());

        VehicleBasicInfo stubInfo = VehicleBasicInfo.builder().vin(VIN).build();
        when(vehicleInfoExtractor.createStubVehicle(any(), eq(BATCH_NUM), eq(VIN))).thenReturn(stubInfo);
        when(vehicleInfoExtractor.extractDetails(any(), anyMap(), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(Collections.emptyList());
        when(vehicleInfoPersister.persist(any(), anyList())).thenReturn(true);
        when(vehiclePartBinder.bindParts(any(), eq(VIN), eq(BATCH_NUM))).thenReturn(Collections.emptyList());

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        verify(vehicleInfoExtractor).createStubVehicle(any(), eq(BATCH_NUM), eq(VIN));
        verify(vehicleInfoExtractor, never()).extractBasicInfo(any(), any(), any(), any());
        verify(vehiclePublish).produce(VIN, "EOL-" + BATCH_NUM);
        verify(vehiclePublish).eol(eq(VIN), any(Instant.class));
    }

    @Test
    @DisplayName("EOL 补发的 PRODUCE 事件不应触发安全预置")
    void testEolProduceEvent_notTriggerSecurityPreset() {
        JSONObject dataJson = buildEolDataJson(VIN);

        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        when(vehBasicInfoRepository.selectDetailByVin(VIN)).thenReturn(Collections.emptyList());

        VehicleBasicInfo stubInfo = VehicleBasicInfo.builder().vin(VIN).build();
        when(vehicleInfoExtractor.createStubVehicle(any(), eq(BATCH_NUM), eq(VIN))).thenReturn(stubInfo);
        when(vehicleInfoExtractor.extractDetails(any(), anyMap(), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(Collections.emptyList());
        when(vehicleInfoPersister.persist(any(), anyList())).thenReturn(true);
        when(vehiclePartBinder.bindParts(any(), eq(VIN), eq(BATCH_NUM))).thenReturn(Collections.emptyList());

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getSuccessCount());

        verify(vehiclePublish).produce(VIN, "EOL-" + BATCH_NUM);
        verify(vehicleSecurityPresetAppService, never()).preset(anyString(), anyString());
    }

    @Test
    @DisplayName("安全回执对账应按字段分流：车辆级映射 ROOT/IMMO/OTA、器件级按 part_code+sn")
    void testEol_securityConfirm_dispatchesVehicleAndDeviceRoots() {
        JSONObject dataJson = buildEolDataJson(VIN);
        stubExistingVehicleFlow();

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getSuccessCount());

        // 车辆级：TBOX 的 V2C_COMM_ROOT → constant_type=ROOT
        verify(securityProvisionConfirmService)
                .processVehicleSecurityProvision(VIN, "ROOT", "PROVISIONED", "EOL");
        // 车辆级：CCU 的 OTA_VEHICLE_ROOT → constant_type=OTA
        verify(securityProvisionConfirmService)
                .processVehicleSecurityProvision(VIN, "OTA", "PROVISIONED", "EOL");
        // 不得再把 BizType 名当 constant_type 去查 veh_security_constant
        verify(securityProvisionConfirmService, never())
                .processVehicleSecurityProvision(eq(VIN), eq("V2C_COMM_ROOT"), anyString(), anyString());
        verify(securityProvisionConfirmService, never())
                .processVehicleSecurityProvision(eq(VIN), eq("TBOX_DEVICE_ROOT"), anyString(), anyString());
        verify(securityProvisionConfirmService, never())
                .processVehicleSecurityProvision(eq(VIN), eq("CGW_DEVICE_ROOT"), anyString(), anyString());

        // 器件级：TBOX_DEVICE_ROOT（TBOX）
        verify(securityProvisionConfirmService)
                .processPartSecurityProvision("00000001AA", "00000002AA00000001",
                        "TBOX_DEVICE_ROOT", "PROVISIONED", "EOL");
        // 器件级：CPT_DCU_DEVICE_ROOT（座舱域控，新节点名 DCU_COCKPIT_SA8295P 不再依赖节点名匹配）
        verify(securityProvisionConfirmService)
                .processPartSecurityProvision("00000004AA", "00000005AA00000001",
                        "CPT_DCU_DEVICE_ROOT", "PROVISIONED", "EOL");
        // 器件级：CGW_DEVICE_ROOT（CCU_GEN1）
        verify(securityProvisionConfirmService)
                .processPartSecurityProvision("00000007AA", "00000008AA00000001",
                        "CGW_DEVICE_ROOT", "PROVISIONED", "EOL");
        // 器件级：PEPS_DEVICE_ROOT（DCU_ADAS_GEN1）
        verify(securityProvisionConfirmService)
                .processPartSecurityProvision("00000010AA", "00000011AA00000001",
                        "PEPS_DEVICE_ROOT", "PROVISIONED", "EOL");
    }

    @Test
    @DisplayName("补偿绑定应将新格式 ECU_BASELINE 转换为绑定字段并发布绑定事件")
    void testEol_compensateBinding_convertsAndBinds() {
        JSONObject dataJson = buildEolDataJson(VIN);
        stubExistingVehicleFlow();

        List<VehicleEolPartBoundEvent.PartMeta> partMetaList = List.of(
                new VehicleEolPartBoundEvent.PartMeta(
                        "00000002AA00000001", "00000001AA", "TBOX_5G", "TBOX",
                        null, BATCH_NUM, null, "00", "V1.0.0", "00000002AA", "00000003AA",
                        "861000000000001", "89860400000000000001"));
        when(vehiclePartBinder.bindParts(any(), eq(VIN), eq(BATCH_NUM))).thenReturn(partMetaList);

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getSuccessCount());
        // 绑定成功后发布 VehicleEolPartBoundEvent（CR-033 下游供给）
        verify(vehiclePublish).eolPartBound(eq(VIN), eq(partMetaList));
    }

    private void stubExistingVehicleFlow() {
        VehicleBasicInfo existingInfo = VehicleBasicInfo.builder().id(1L).vin(VIN).build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(existingInfo);
        when(vehBasicInfoRepository.selectDetailByVin(VIN)).thenReturn(Collections.emptyList());
        VehicleBasicInfo extractedInfo = VehicleBasicInfo.builder().id(1L).vin(VIN).build();
        when(vehicleInfoExtractor.extractBasicInfo(any(), eq(existingInfo), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(extractedInfo);
        when(vehicleInfoExtractor.extractDetails(any(), anyMap(), eq(BATCH_NUM), eq(VIN)))
                .thenReturn(Collections.emptyList());
        when(vehicleInfoPersister.persist(any(), anyList())).thenReturn(false);
    }

    private JSONObject buildEolDataJson(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("EOL_RESULT", "PASS");
        item.set("EOL_TIME", 1767261000000L);
        item.set("POWER_DOWN_TIME", 1767261600000L);
        item.set("PLANT", "HWYZ");
        item.set("LINE_CODE", "FA1");
        item.set("STATION_CODE", "EOL-ELE-01");
        item.set("SHIFT", "A");
        item.set("OPERATOR", "OP10086");
        item.set("TRANSPORT_MODE", "TRANSPORT");
        item.set("ODOMETER_KM", 3);
        item.set("SOC", 40);
        item.set("HV_STATUS", "POWER_OFF");

        // CERTIFICATE
        JSONObject certificate = new JSONObject();
        certificate.set("CERT_NO", "WHC20260101000001");
        certificate.set("CERT_DATE", 1767312000000L);
        certificate.set("MANUFACTURE_DATE", 1767225600000L);
        item.set("CERTIFICATE", certificate);

        // OTA_BASELINE
        JSONObject otaBaseline = new JSONObject();
        otaBaseline.set("VEHICLE_VERSION", "HSRE26_2026.1.0");
        otaBaseline.set("PACKAGE_ID", "BL_HSRE26_20260101");
        otaBaseline.set("EE_ARCH", "CENTRAL_ZONE");
        item.set("OTA_BASELINE", otaBaseline);

        // ECU_BASELINE（4 节点，与 MES 点检下线样本一致）
        item.set("ECU_BASELINE", buildEcuBaseline());

        // INSPECTION_ITEMS
        JSONArray inspectionItems = new JSONArray();
        JSONObject inspection1 = new JSONObject();
        inspection1.set("ITEM_CODE", "HV_INSULATION");
        inspection1.set("NAME", "高压绝缘");
        inspection1.set("RESULT", "PASS");
        inspection1.set("VALUE", "50");
        inspection1.set("UNIT", "MΩ");
        inspectionItems.add(inspection1);
        item.set("INSPECTION_ITEMS", inspectionItems);

        // DIAGNOSTIC
        JSONObject diagnostic = new JSONObject();
        diagnostic.set("DTC_CLEARED", true);
        diagnostic.set("RESIDUAL_DTC", new JSONArray());
        item.set("DIAGNOSTIC", diagnostic);

        // POWERTRAIN
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
        return data;
    }

    private JSONArray buildEcuBaseline() {
        JSONArray ecuBaseline = new JSONArray();

        // TBOX_5G
        JSONObject tbox = new JSONObject();
        tbox.set("VEHICLE_NODE", "TBOX_5G");
        tbox.set("DEVICE_ITEM", "TBOX");
        tbox.set("SN", "00000002AA00000001");
        tbox.set("ASSEMBLY_PART_NO", "00000001AA");
        tbox.set("HARDWARE_PART_NO", "00000002AA");
        tbox.set("HARDWARE_VERSION", "00");
        tbox.set("IMEI", "861000000000001");
        tbox.set("ICCID1", "89860400000000000001");
        tbox.set("ICCID2", "89860600000000000001");
        tbox.set("SOFTWARE", buildSoftware("00000003AA", "V1.0.0"));
        JSONObject tboxSec = new JSONObject();
        tboxSec.set("CERT_INJECTED", true);
        tboxSec.set("V2C_COMM_ROOT", "PROVISIONED");
        tboxSec.set("TBOX_DEVICE_ROOT", "PROVISIONED");
        tbox.set("SECURITY", tboxSec);
        ecuBaseline.add(tbox);

        // DCU_COCKPIT_SA8295P
        JSONObject cockpit = new JSONObject();
        cockpit.set("VEHICLE_NODE", "DCU_COCKPIT_SA8295P");
        cockpit.set("DEVICE_ITEM", "DCU");
        cockpit.set("SN", "00000005AA00000001");
        cockpit.set("ASSEMBLY_PART_NO", "00000004AA");
        cockpit.set("HARDWARE_PART_NO", "00000005AA");
        cockpit.set("HARDWARE_VERSION", "00");
        cockpit.set("SOFTWARE", buildSoftware("00000006AA", "V1.0.0"));
        JSONObject cockpitSec = new JSONObject();
        cockpitSec.set("CERT_INJECTED", true);
        cockpitSec.set("CPT_DCU_DEVICE_ROOT", "PROVISIONED");
        cockpit.set("SECURITY", cockpitSec);
        ecuBaseline.add(cockpit);

        // CCU_GEN1
        JSONObject ccu = new JSONObject();
        ccu.set("VEHICLE_NODE", "CCU_GEN1");
        ccu.set("DEVICE_ITEM", "CCU");
        ccu.set("SN", "00000008AA00000001");
        ccu.set("ASSEMBLY_PART_NO", "00000007AA");
        ccu.set("HARDWARE_PART_NO", "00000008AA");
        ccu.set("HARDWARE_VERSION", "00");
        ccu.set("SOFTWARE", buildSoftware("00000009AA", "V1.0.0"));
        JSONObject ccuSec = new JSONObject();
        ccuSec.set("CERT_INJECTED", true);
        ccuSec.set("OTA_VEHICLE_ROOT", "PROVISIONED");
        ccuSec.set("CGW_DEVICE_ROOT", "PROVISIONED");
        ccu.set("SECURITY", ccuSec);
        ecuBaseline.add(ccu);

        // DCU_ADAS_GEN1
        JSONObject adas = new JSONObject();
        adas.set("VEHICLE_NODE", "DCU_ADAS_GEN1");
        adas.set("DEVICE_ITEM", "DCU");
        adas.set("SN", "00000011AA00000001");
        adas.set("ASSEMBLY_PART_NO", "00000010AA");
        adas.set("HARDWARE_PART_NO", "00000011AA");
        adas.set("HARDWARE_VERSION", "00");
        adas.set("SOFTWARE", buildSoftware("00000012AA", "V1.0.0"));
        JSONObject adasSec = new JSONObject();
        adasSec.set("CERT_INJECTED", true);
        adasSec.set("PEPS_DEVICE_ROOT", "PROVISIONED");
        adas.set("SECURITY", adasSec);
        ecuBaseline.add(adas);

        return ecuBaseline;
    }

    private JSONArray buildSoftware(String softwarePartNo, String softwareVersion) {
        JSONArray software = new JSONArray();
        JSONObject sw = new JSONObject();
        sw.set("SOFTWARE_TYPE", "APP");
        sw.set("SOFTWARE_PART_NO", softwarePartNo);
        sw.set("SOFTWARE_VERSION", softwareVersion);
        sw.set("FLASH_RESULT", "OK");
        software.add(sw);
        return software;
    }
}
