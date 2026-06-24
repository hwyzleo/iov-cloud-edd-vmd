package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PartSecurityConstantTest {

    @Test
    void shouldCreatePartSecurityConstant() {
        PartSecurityConstant constant = PartSecurityConstant.builder()
                .partCode("TBOX_001")
                .sn("SN_123456")
                .chipUid("CHIP_UID_789")
                .presetState(SecurityConstantState.PENDING)
                .constantType("ROOT")
                .kmsKeyRef("test_key_ref")
                .batchNum("BATCH_001")
                .createTime(LocalDateTime.now())
                .build();

        assertEquals("TBOX_001", constant.getPartCode());
        assertEquals("SN_123456", constant.getSn());
        assertEquals("CHIP_UID_789", constant.getChipUid());
        assertEquals(SecurityConstantState.PENDING, constant.getPresetState());
        assertEquals("ROOT", constant.getConstantType());
        assertEquals("test_key_ref", constant.getKmsKeyRef());
        assertEquals("BATCH_001", constant.getBatchNum());
    }
}
