package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * VehicleSoftwareBindingResolver 单元测试
 * <p>
 * VMD-DSN-CR-046: 按 vin + ecuId 解析唯一 active 物理绑定；无/多绑定隔离
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class VehicleSoftwareBindingResolverTest {

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    @InjectMocks
    private VehicleSoftwareBindingResolver resolver;

    private static final String VIN = "HWYZTEST000000001";
    private static final String ECU_ID = "ECU_TBOX";

    private VehiclePart binding(Long id, Long partId, String nodeCode) {
        return VehiclePart.builder()
                .id(id)
                .partId(partId)
                .vin(VIN)
                .vehicleNodeCode(nodeCode)
                .build();
    }

    @Test
    void resolve_uniqueActive_shouldReturnBinding() {
        // Given
        when(vehiclePartAppService.getActiveBindingsByVin(VIN))
                .thenReturn(List.of(binding(100L, 200L, ECU_ID)));

        // When
        VehicleSoftwareBindingResolver.BindingResolution result = resolver.resolve(VIN, ECU_ID);

        // Then
        assertEquals(100L, result.bindingId());
        assertEquals(200L, result.partId());
    }

    @Test
    void resolve_noActiveBindings_shouldThrow() {
        // Given
        when(vehiclePartAppService.getActiveBindingsByVin(VIN)).thenReturn(List.of());

        // When / Then
        assertThrows(VehicleSoftwareBindingResolver.ActiveBindingNotFoundException.class,
                () -> resolver.resolve(VIN, ECU_ID));
    }

    @Test
    void resolve_ecuIdNotMatched_shouldThrow() {
        // Given
        when(vehiclePartAppService.getActiveBindingsByVin(VIN))
                .thenReturn(List.of(binding(100L, 200L, "ECU_OTHER")));

        // When / Then
        assertThrows(VehicleSoftwareBindingResolver.ActiveBindingNotFoundException.class,
                () -> resolver.resolve(VIN, ECU_ID));
    }

    @Test
    void resolve_multipleActiveForEcu_shouldThrow() {
        // Given
        when(vehiclePartAppService.getActiveBindingsByVin(VIN))
                .thenReturn(List.of(binding(100L, 200L, ECU_ID), binding(101L, 201L, ECU_ID)));

        // When / Then
        assertThrows(VehicleSoftwareBindingResolver.MultipleActiveBindingException.class,
                () -> resolver.resolve(VIN, ECU_ID));
    }
}
