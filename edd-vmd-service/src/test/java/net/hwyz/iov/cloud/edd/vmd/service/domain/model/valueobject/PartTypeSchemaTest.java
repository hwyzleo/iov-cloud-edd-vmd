package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartTypeSchemaTest {

    @Test
    void shouldCreatePartTypeSchemaWithSecurityFields() {
        PartTypeSchema schema = PartTypeSchema.builder()
                .partType("TBOX")
                .hsmUid("hsm_uid_field")
                .needsSecurityConstantPreset(true)
                .build();

        assertEquals("TBOX", schema.getPartType());
        assertEquals("hsm_uid_field", schema.getHsmUid());
        assertTrue(schema.isNeedsSecurityConstantPreset());
    }

    @Test
    void shouldReturnFalseForNonSecurityPartType() {
        PartTypeSchema schema = PartTypeSchema.builder()
                .partType("ECU")
                .needsSecurityConstantPreset(false)
                .build();

        assertFalse(schema.isNeedsSecurityConstantPreset());
    }
}
