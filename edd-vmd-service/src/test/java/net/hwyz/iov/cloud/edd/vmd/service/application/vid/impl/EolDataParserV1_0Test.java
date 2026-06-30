package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePublish;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
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

    private EolDataParserV1_0 parser;

    private static final String VIN = "HWYZTEST000000001";
    private static final String BATCH_NUM = "BATCH001";

    @BeforeEach
    void setUp() {
        parser = new EolDataParserV1_0(
                vehiclePublish, vehBasicInfoRepository, vehicleInfoExtractor,
                vehicleInfoPersister, vehiclePartBinder, vehicleLifecycleAppService,
                vehicleSecurityPresetAppService, parserRegistry);
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
        item.set("PARTS", new JSONArray());
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data;
    }
}
