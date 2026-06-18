package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
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

    @Test
    @DisplayName("TOL导入应正确解析ECU清单")
    void testTolImportShouldParseEcuList() {
        // Given
        String batchNum = "TOL_BATCH_001";
        String vin = "TEST_VIN_001";
        String dataJson = buildTolDataJson(vin, "17300011AA", "SN000000001", "IBCM");

        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("TOL")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertNotNull(result);
        assertTrue(result.getTotalCount() > 0);
    }

    @Test
    @DisplayName("重复批次号应跳过处理")
    void testDuplicateBatchNumShouldSkip() {
        // Given
        String batchNum = "TOL_BATCH_002";
        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("TOL")
                .version("1.0")
                .data("{}")
                .handle(true)
                .build();
        vehImportDataRepository.insert(vehImportData);

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getDescription().contains("已处理"));
    }

    private String buildTolDataJson(String vin, String partNo, String sn, String deviceCode) {
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
        return data.toString();
    }
}
