package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
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
        when(vehicleInfoExtractor.extractEolDate(any())).thenReturn(Instant.now());
        when(vehicleInfoExtractor.extractCertDateStr(any())).thenReturn(null);
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
        when(vehicleInfoExtractor.extractEolDate(any())).thenReturn(Instant.now());
        when(vehicleInfoExtractor.extractCertDateStr(any())).thenReturn(null);
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
        when(vehicleInfoExtractor.extractEolDate(any())).thenReturn(Instant.now());
        when(vehicleInfoExtractor.extractCertDateStr(any())).thenReturn(null);
        when(vehicleInfoPersister.persist(any(), anyList())).thenReturn(true);
        when(vehiclePartBinder.bindParts(any(), eq(VIN), eq(BATCH_NUM))).thenReturn(Collections.emptyList());

        ImportResult result = parser.parse(BATCH_NUM, dataJson);

        assertEquals(1, result.getSuccessCount());

        verify(vehiclePublish).produce(VIN, "EOL-" + BATCH_NUM);
        verify(vehicleSecurityPresetAppService, never()).preset(anyString(), anyString());
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
        
        // ECU_BASELINE
        JSONArray ecuBaseline = new JSONArray();
        JSONObject ecu1 = new JSONObject();
        ecu1.set("VEHICLE_NODE", "TBOX_5G");
        ecu1.set("DEVICE_ITEM", "TBOX");
        ecu1.set("SN", "00000005AA00000001");
        ecu1.set("ASSEMBLY_PART_NO", "00000001AA");
        ecu1.set("HARDWARE_PART_NO", "00000005AA");
        ecu1.set("HARDWARE_VERSION", "00");
        JSONArray sw1 = new JSONArray();
        JSONObject swItem1 = new JSONObject();
        swItem1.set("SOFTWARE_TYPE", "APP");
        swItem1.set("SOFTWARE_PART_NO", "00000009AA");
        swItem1.set("SOFTWARE_VERSION", "V1.0.0");
        swItem1.set("FLASH_RESULT", "OK");
        sw1.add(swItem1);
        ecu1.set("SOFTWARE", sw1);
        JSONObject security1 = new JSONObject();
        security1.set("CERT_INJECTED", true);
        security1.set("V2C_COMM_ROOT", "PROVISIONED");
        security1.set("TBOX_DEVICE_ROOT", "PROVISIONED");
        ecu1.set("SECURITY", security1);
        ecuBaseline.add(ecu1);
        item.set("ECU_BASELINE", ecuBaseline);
        
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
}
