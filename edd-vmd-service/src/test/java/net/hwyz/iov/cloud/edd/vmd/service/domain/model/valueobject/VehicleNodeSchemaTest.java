package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleNodeSchemaTest {

    @Test
    void shouldCreateVehicleNodeSchemaWithSecurityFields() {
        VehicleNodeSchema schema = VehicleNodeSchema.builder()
                .vehicleNodeCode("TBOX")
                .hsmUid("hsm_uid_field")
                .needsSecurityConstantPreset(true)
                .build();

        assertEquals("TBOX", schema.getVehicleNodeCode());
        assertEquals("hsm_uid_field", schema.getHsmUid());
        assertTrue(schema.isNeedsSecurityConstantPreset());
    }

    @Test
    void shouldReturnFalseForNonSecurityVehicleNodeCode() {
        VehicleNodeSchema schema = VehicleNodeSchema.builder()
                .vehicleNodeCode("ECU")
                .needsSecurityConstantPreset(false)
                .build();

        assertFalse(schema.isNeedsSecurityConstantPreset());
    }
}
