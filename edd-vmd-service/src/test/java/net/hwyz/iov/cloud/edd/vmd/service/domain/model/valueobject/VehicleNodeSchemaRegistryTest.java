package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleNodeSchemaRegistryTest {

    private VehicleNodeSchemaRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new VehicleNodeSchemaRegistry();
    }

    @Test
    void shouldRegisterBuiltinSchemasOnConstruction() {
        // 内置的TBOX_5G、TBOX、BTM、CCP、IDCM、TSP应该被注册
        assertNotNull(registry.getSchema("TBOX_5G"));
        assertNotNull(registry.getSchema("TBOX"));
        assertNotNull(registry.getSchema("BTM"));
        assertNotNull(registry.getSchema("CCP"));
        assertNotNull(registry.getSchema("IDCM"));
        assertNotNull(registry.getSchema("TSP"));
    }

    @Test
    void shouldReturnTrueForSecurityVehicleNodeCode() {
        assertTrue(registry.needsSecurityConstantPreset("TBOX_5G"));
        assertTrue(registry.needsSecurityConstantPreset("TBOX"));
        assertTrue(registry.needsSecurityConstantPreset("BTM"));
        assertTrue(registry.needsSecurityConstantPreset("CCP"));
        assertTrue(registry.needsSecurityConstantPreset("IDCM"));
    }

    @Test
    void shouldReturnFalseForNonSecurityVehicleNodeCode() {
        assertFalse(registry.needsSecurityConstantPreset("TSP"));
    }

    @Test
    void shouldReturnFalseForUnknownVehicleNodeCode() {
        assertFalse(registry.needsSecurityConstantPreset("UNKNOWN"));
        assertFalse(registry.needsSecurityConstantPreset(null));
    }

    @Test
    void shouldReturnHsmUidFieldForSecurityVehicleNodeCode() {
        assertEquals("HSM", registry.getHsmUidField("TBOX_5G"));
        assertEquals("HSM", registry.getHsmUidField("TBOX"));
        assertEquals("HSM", registry.getHsmUidField("BTM"));
        assertEquals("HSM", registry.getHsmUidField("CCP"));
        assertEquals("HSM", registry.getHsmUidField("IDCM"));
    }

    @Test
    void shouldReturnNullHsmUidFieldForNonSecurityVehicleNodeCode() {
        assertNull(registry.getHsmUidField("TSP"));
    }

    @Test
    void shouldReturnNullHsmUidFieldForUnknownVehicleNodeCode() {
        assertNull(registry.getHsmUidField("UNKNOWN"));
        assertNull(registry.getHsmUidField(null));
    }

    @Test
    void shouldRegisterCustomSchema() {
        // Given
        VehicleNodeSchema customSchema = VehicleNodeSchema.builder()
                .vehicleNodeCode("CUSTOM")
                .hsmUid("custom_hsm_field")
                .needsSecurityConstantPreset(true)
                .description("自定义车辆节点")
                .build();

        // When
        registry.register(customSchema);

        // Then
        assertTrue(registry.needsSecurityConstantPreset("CUSTOM"));
        assertEquals("custom_hsm_field", registry.getHsmUidField("CUSTOM"));
    }

    @Test
    void shouldNotOverwriteExistingSchema() {
        // Given - TBOX_5G已存在
        VehicleNodeSchema duplicateSchema = VehicleNodeSchema.builder()
                .vehicleNodeCode("TBOX_5G")
                .hsmUid("different_field")
                .needsSecurityConstantPreset(false)
                .build();

        // When - 尝试重复注册
        registry.register(duplicateSchema);

        // Then - 原有配置应保持不变
        assertTrue(registry.needsSecurityConstantPreset("TBOX_5G"));
        assertEquals("HSM", registry.getHsmUidField("TBOX_5G"));
    }

    @Test
    void shouldGetSchemaDetails() {
        // Given & When
        VehicleNodeSchema tboxSchema = registry.getSchema("TBOX_5G");

        // Then
        assertNotNull(tboxSchema);
        assertEquals("TBOX_5G", tboxSchema.getVehicleNodeCode());
        assertEquals("HSM", tboxSchema.getHsmUid());
        assertTrue(tboxSchema.isNeedsSecurityConstantPreset());
        assertNotNull(tboxSchema.getDescription());
    }

    @Test
    void shouldReturnNullForUnknownSchema() {
        assertNull(registry.getSchema("UNKNOWN"));
        assertNull(registry.getSchema(null));
    }
}
