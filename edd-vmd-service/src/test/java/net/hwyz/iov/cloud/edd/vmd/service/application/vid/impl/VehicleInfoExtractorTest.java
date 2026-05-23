package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VehicleInfoExtractorTest {

    @InjectMocks
    private VehicleInfoExtractor extractor;

    private JSONObject itemJson;
    private static final String BATCH_NUM = "BATCH001";
    private static final String VIN = "HWYZTEST000000001";

    @BeforeEach
    void setUp() {
        itemJson = new JSONObject();
        itemJson.set("VIN", VIN);
        itemJson.set("MANUFACTURER", "M001");
        itemJson.set("BRAND", "B001");
        itemJson.set("PLATFORM", "P001");
        itemJson.set("SERIES", "S001");
        itemJson.set("MODEL", "MOD01");
        itemJson.set("BASE_MODEL", "BM01");
        itemJson.set("BUILD_CONFIG", "BC01");
        itemJson.set("VEHICLE_BASE_VERSION", "V1.0");
    }

    @Test
    void extractBasicInfo_newVehicle_createsBuilder() {
        VehicleBasicInfo result = extractor.extractBasicInfo(itemJson, null, BATCH_NUM, VIN);

        assertNotNull(result);
        assertEquals(VIN, result.getVin());
        assertEquals("M001", result.getManufacturerCode());
        assertEquals("B001", result.getBrandCode());
        assertEquals("P001", result.getPlatformCode());
        assertEquals("S001", result.getSeriesCode());
        assertEquals("MOD01", result.getModelCode());
        assertEquals("BM01", result.getBaseModelCode());
        assertEquals("BC01", result.getBuildConfigCode());
        assertEquals("V1.0", result.getVehicleBaseVersion());
    }

    @Test
    void extractBasicInfo_existingVehicle_updatesFields() {
        VehicleBasicInfo existing = VehicleBasicInfo.builder()
                .id(1L)
                .vin(VIN)
                .manufacturerCode("OLD_MFG")
                .build();

        VehicleBasicInfo result = extractor.extractBasicInfo(itemJson, existing, BATCH_NUM, VIN);

        assertEquals(1L, result.getId());
        assertEquals("OLD_MFG", result.getManufacturerCode());
    }

    @Test
    void extractDetails_newDetails_createsEntries() {
        Map<String, VehicleDetail> existingMap = new HashMap<>();
        itemJson.set("PRODUCTION_ORDER", "PO001");
        itemJson.set("MATNR", "MAT001");

        List<VehicleDetail> result = extractor.extractDetails(itemJson, existingMap, BATCH_NUM, VIN);

        assertFalse(result.isEmpty());
        Map<String, VehicleDetail> resultMap = new HashMap<>();
        for (VehicleDetail d : result) {
            resultMap.put(d.getType(), d);
        }
        assertEquals("PO001", resultMap.get("PRODUCTION_ORDER").getVal());
        assertEquals("MAT001", resultMap.get("MATNR").getVal());
    }

    @Test
    void extractDetails_existingDetails_doesNotOverwrite() {
        Map<String, VehicleDetail> existingMap = new HashMap<>();
        existingMap.put("PRODUCTION_ORDER", VehicleDetail.builder()
                .id(1L).vin(VIN).type("PRODUCTION_ORDER").val("OLD_PO").build());
        itemJson.set("PRODUCTION_ORDER", "NEW_PO");

        List<VehicleDetail> result = extractor.extractDetails(itemJson, existingMap, BATCH_NUM, VIN);

        VehicleDetail poDetail = result.stream()
                .filter(d -> "PRODUCTION_ORDER".equals(d.getType()))
                .findFirst().orElseThrow();
        assertEquals("OLD_PO", poDetail.getVal());
    }

    @Test
    void extractEolDate_withDate_parsesCorrectly() {
        itemJson.set("EOL_DATE", "20260523");

        Instant result = extractor.extractEolDate(itemJson);

        assertNotNull(result);
        assertEquals(2026, result.atZone(java.time.ZoneId.systemDefault()).getYear());
        assertEquals(5, result.atZone(java.time.ZoneId.systemDefault()).getMonthValue());
    }

    @Test
    void extractEolDate_withoutDate_returnsNow() {
        Instant before = Instant.now();
        Instant result = extractor.extractEolDate(itemJson);
        Instant after = Instant.now();

        assertNotNull(result);
        assertFalse(result.isBefore(before));
        assertFalse(result.isAfter(after));
    }

    @Test
    void extractCertDateStr_withDate_returnsString() {
        itemJson.set("CERT_DATE", "20260523");

        String result = extractor.extractCertDateStr(itemJson);

        assertEquals("20260523", result);
    }

    @Test
    void extractCertDateStr_withoutDate_returnsNull() {
        String result = extractor.extractCertDateStr(itemJson);

        assertNull(result);
    }
}
