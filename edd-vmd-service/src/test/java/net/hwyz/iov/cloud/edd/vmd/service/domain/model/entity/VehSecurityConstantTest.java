package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VehSecurityConstantTest {

    @Test
    void shouldCreateVehSecurityConstantWithKmsKeyRef() {
        VehSecurityConstant constant = VehSecurityConstant.builder()
                .vin("TEST_VIN_123")
                .batchNum("BATCH_001")
                .presetState(SecurityConstantState.PENDING)
                .kmsKeyRef("test_key_ref")
                .constantType("SECURITY_KEY")
                .createTime(LocalDateTime.now())
                .build();

        assertEquals("TEST_VIN_123", constant.getVin());
        assertEquals("test_key_ref", constant.getKmsKeyRef());
    }
}
