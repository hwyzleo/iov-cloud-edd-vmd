package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.impl;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockKmsHsmClientTest {

    private final MockKmsHsmClient client = new MockKmsHsmClient();

    @Test
    @DisplayName("Mock 应成功生成 per-VIN 安全常量")
    void testGeneratePerVinConstant() throws Exception {
        // Given
        String vin = "TEST_VIN_001";

        // When
        KmsHsmResult result = client.generatePerVinConstant(vin);

        // Then
        assertNotNull(result);
        assertNotNull(result.getKmsKeyRef());
        assertTrue(result.getKmsKeyRef().contains("vault:v1:"));
        assertEquals("aes256-gcm96", result.getKeySpec());
        assertEquals("Mock", result.getProvider());
        assertEquals("HMAC-SHA256", result.getAlgorithm());
    }

    @Test
    @DisplayName("Mock 应成功生成器件级安全常量")
    void testGeneratePerDeviceConstant() throws Exception {
        // Given
        String partCode = "TBOX_001";
        String sn = "SN123456";
        String constantType = "ROOT";
        String chipUid = "HSM_UID_789";

        // When
        KmsHsmResult result = client.generatePerDeviceConstant(partCode, sn, constantType, chipUid);

        // Then
        assertNotNull(result);
        assertNotNull(result.getKmsKeyRef());
        assertTrue(result.getKmsKeyRef().contains("vault:v1:"));
        assertEquals("aes256-gcm96", result.getKeySpec());
        assertEquals("Mock", result.getProvider());
        assertEquals("HMAC-SHA256", result.getAlgorithm());
    }

    @Test
    @DisplayName("Mock 应返回一致的结果")
    void testReturnsConsistentResults() throws Exception {
        // Given
        String vin = "TEST_VIN_001";

        // When
        KmsHsmResult result1 = client.generatePerVinConstant(vin);
        KmsHsmResult result2 = client.generatePerVinConstant(vin);

        // Then
        assertEquals(result1.getKmsKeyRef(), result2.getKmsKeyRef());
    }
}
