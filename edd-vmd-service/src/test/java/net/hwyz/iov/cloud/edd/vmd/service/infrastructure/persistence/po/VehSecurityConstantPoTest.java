package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehSecurityConstantPoTest {

    @Test
    void shouldCreatePoWithKmsKeyRefColumn() {
        VehSecurityConstantPo po = VehSecurityConstantPo.builder()
                .vin("TEST_VIN_123")
                .kmsKeyRef("test_key_ref")
                .build();

        assertEquals("test_key_ref", po.getKmsKeyRef());
        assertNull(po.getCipherBlob()); // Should not exist
        assertNull(po.getKeyHandle()); // Should not exist
    }
}
