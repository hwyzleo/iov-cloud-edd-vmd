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
        String dataJson = buildTolDataJson(vin, "ECU_SN_001", "PART_001", "TBOX_5G");

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

    private String buildTolDataJson(String vin, String ecuSn, String partCode, String vehicleNodeCode) {
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
        return data.toString();
    }
}
