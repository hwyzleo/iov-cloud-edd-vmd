package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * PRODUCE 导入集成测试
 * <p>
 * VMD-DSN-CR-050: 同批 VIN 去重、生命周期 PRODUCE 节点幂等、批次收敛回归。
 * 覆盖：同批重复 VIN / 历史已导 VIN 重导 / 首次导入不回归。
 *
 * @author hwyz_leo
 * @since 2026-09-23
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProduceImportIntegrationTest {

    @Autowired
    private VehImportDataAppService vehImportDataAppService;

    @Autowired
    private VehImportDataRepository vehImportDataRepository;

    @Autowired
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Autowired
    private VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    @Autowired
    private VehSecurityConstantRepository vehSecurityConstantRepository;

    /**
     * Mock KMS（本地 OpenBao 可能处于 sealed 状态），保证安全常量预置成功路径可测
     */
    @MockBean
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @BeforeEach
    void stubKms() {
        ProvisioningResult mockResult = new ProvisioningResult();
        mockResult.setKmsKeyRef("dev-root-master:vin:MOCK");
        mockResult.setKeySpec("256-bit");
        mockResult.setProvider("Vault-Transit");
        mockResult.setAlgorithm("HMAC-SHA256");
        mockResult.setKcv(new byte[]{1, 2, 3, 4});
        mockResult.setWrappedMaterial(null);
        when(keyProvisioningTemplate.deriveByVin(anyString(), any(BizType.class))).thenReturn(mockResult);
    }

    @Test
    @DisplayName("同批重复VIN应去重且批次收敛为已处理")
    void testSameBatchDuplicateVin() {
        // Given
        String batchNum = "PRODUCE_BATCH_DUP_001";
        String vin1 = "DUP_ITEG_VIN_001";
        String vin2 = "DUP_ITEG_VIN_002";
        insertImportData(batchNum, buildProduceDataJsonWithDuplicates(vin1, vin2));

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getInvalidCount());
        assertEquals(0, result.getFailureCount());

        // 批次收敛为已处理（invalidCount 不阻断）
        VehImportData persisted = vehImportDataRepository.selectByBatchNum(batchNum);
        assertNotNull(persisted);
        assertTrue(Boolean.TRUE.equals(persisted.getHandle()));

        // 两个 VIN 的 PRODUCE 节点各唯一
        assertEquals(1, vehicleLifecycleNodeRepository.selectByVin(vin1).size());
        assertEquals(1, vehicleLifecycleNodeRepository.selectByVin(vin2).size());
    }

    @Test
    @DisplayName("历史已导VIN重导应幂等且PRODUCE节点保持不变")
    void testReimportExistingVin() {
        // Given
        String batchNum1 = "PRODUCE_BATCH_FIRST_001";
        String batchNum2 = "PRODUCE_BATCH_RE_002";
        String vin = "RE_ITEG_VIN_001";

        insertImportData(batchNum1, buildProduceDataJson(vin));
        ImportResult first = vehImportDataAppService.parseVehImportData(batchNum1);
        assertEquals(1, first.getSuccessCount());
        assertEquals(0, first.getFailureCount());

        List<VehicleLifecycleNode> nodesBefore = vehicleLifecycleNodeRepository.selectByVin(vin);
        assertEquals(1, nodesBefore.size());
        assertEquals(VehicleLifecycleNodeEnum.PRODUCE, nodesBefore.get(0).getNode());
        var reachTimeBefore = nodesBefore.get(0).getReachTime();

        // When：历史已导 VIN 用新批次重导
        insertImportData(batchNum2, buildProduceDataJson(vin));
        ImportResult second = vehImportDataAppService.parseVehImportData(batchNum2);

        // Then
        assertEquals(1, second.getSuccessCount());
        assertEquals(0, second.getFailureCount());
        assertTrue(Boolean.TRUE.equals(vehImportDataRepository.selectByBatchNum(batchNum2).getHandle()));

        // PRODUCE 节点数量不变、reachTime 不变（首次写入胜出）
        List<VehicleLifecycleNode> nodesAfter = vehicleLifecycleNodeRepository.selectByVin(vin);
        assertEquals(1, nodesAfter.size());
        assertEquals(reachTimeBefore, nodesAfter.get(0).getReachTime());
    }

    @Test
    @DisplayName("首次导入应新建车辆、写入PRODUCE节点并预置安全常量")
    void testFirstImportNoRegression() {
        // Given
        String batchNum = "PRODUCE_BATCH_FIRST_003";
        String vin = "NEW_ITEG_VIN_001";
        insertImportData(batchNum, buildProduceDataJson(vin));

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        // 车辆建档
        VehicleBasicInfo vehicle = vehBasicInfoRepository.selectByVin(vin);
        assertNotNull(vehicle);

        // PRODUCE 节点写入
        List<VehicleLifecycleNode> nodes = vehicleLifecycleNodeRepository.selectByVin(vin);
        assertEquals(1, nodes.size());
        assertEquals(VehicleLifecycleNodeEnum.PRODUCE, nodes.get(0).getNode());

        // ROOT/IMMO/OTA 安全常量预置（即使 KMS 不可用也应落记录）
        assertEquals(3, vehSecurityConstantRepository.countByVin(vin));

        // 批次收敛
        assertTrue(Boolean.TRUE.equals(vehImportDataRepository.selectByBatchNum(batchNum).getHandle()));
    }

    @Test
    @DisplayName("安全常量预置失败时批次应保持未处理且description记录错误")
    void testPresetFailureLeavesBatchUnprocessed() {
        // Given：KMS 调用失败（模拟 OpenBao sealed 等不可用场景）
        doThrow(new RuntimeException("KMS unavailable"))
                .when(keyProvisioningTemplate).deriveByVin(anyString(), any(BizType.class));
        String batchNum = "PRODUCE_BATCH_PRESET_FAIL_001";
        String vin = "PRESETFAIL_VIN_01";
        insertImportData(batchNum, buildProduceDataJson(vin));

        // When
        ImportResult result = vehImportDataAppService.parseVehImportData(batchNum);

        // Then：预置失败计入 failureCount
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());

        // 批次保持未处理，允许重试
        VehImportData persisted = vehImportDataRepository.selectByBatchNum(batchNum);
        assertNotNull(persisted);
        assertEquals(Boolean.FALSE, persisted.getHandle());
        assertNotNull(persisted.getDescription());
        assertTrue(persisted.getDescription().contains("预置失败"));
    }

    private void insertImportData(String batchNum, String dataJson) {
        VehImportData vehImportData = VehImportData.builder()
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data(dataJson)
                .handle(false)
                .build();
        vehImportDataRepository.insert(vehImportData);
    }

    private String buildProduceDataJson(String vin) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        JSONObject item = new JSONObject();
        item.set("VIN", vin);
        item.set("PLANT", "P001");
        item.set("BRAND", "B001");
        item.set("PLATFORM", "PL001");
        item.set("CAR_LINE", "CL001");
        item.set("MODEL", "M001");
        item.set("VARIANT", "V001");
        item.set("CONFIGURATION", "C001");
        items.add(item);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }

    private String buildProduceDataJsonWithDuplicates(String vin1, String vin2) {
        JSONObject data = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject dataObj = new JSONObject();
        cn.hutool.json.JSONArray items = new cn.hutool.json.JSONArray();

        // vin1 首次出现
        JSONObject item1 = new JSONObject();
        item1.set("VIN", vin1);
        item1.set("PLANT", "P001");
        item1.set("BRAND", "B001");
        item1.set("PLATFORM", "PL001");
        item1.set("CAR_LINE", "CL001");
        item1.set("MODEL", "M001");
        item1.set("VARIANT", "V001");
        item1.set("CONFIGURATION", "C001");
        items.add(item1);

        // vin1 重复（应被解析器去重跳过）
        JSONObject item2 = new JSONObject();
        item2.set("VIN", vin1);
        item2.set("PLANT", "P001");
        item2.set("BRAND", "B001");
        items.add(item2);

        // vin2 不同 VIN
        JSONObject item3 = new JSONObject();
        item3.set("VIN", vin2);
        item3.set("PLANT", "P002");
        item3.set("BRAND", "B002");
        item3.set("PLATFORM", "PL002");
        item3.set("CAR_LINE", "CL002");
        item3.set("MODEL", "M002");
        item3.set("VARIANT", "V002");
        item3.set("CONFIGURATION", "C002");
        items.add(item3);

        dataObj.set("ITEMS", items);
        request.set("DATA", dataObj);
        data.set("REQUEST", request);
        return data.toString();
    }
}
