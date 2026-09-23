package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartBindingConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehiclePartBindResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePartBindingPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 车辆-零件绑定关系应用服务单元测试
 * <p>
 * 覆盖 VMD-DSN-CR-029 幂等绑定：新建绑定 / 幂等命中 / 跨VIN冲突 / 装车槽位冲突
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class VehiclePartAppServiceTest {

    @Mock
    private VehiclePartRepository vehiclePartRepository;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private VehiclePartBindingPublisher vehiclePartBindingPublisher;

    @InjectMocks
    private VehiclePartAppService vehiclePartAppService;

    private VehiclePart buildVehiclePart(String vin, Long partId, String nodeCode) {
        return VehiclePart.builder()
                .vin(vin)
                .partId(partId)
                .vehicleNodeCode(nodeCode)
                .build();
    }

    @Test
    @DisplayName("幂等绑定-未绑定且槽位空闲应新建绑定")
    void testBindIdempotentWhenNotBound() {
        // Given
        VehiclePart vehiclePart = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        when(vehiclePartRepository.selectActiveByPartId(1001L)).thenReturn(null);
        when(vehiclePartRepository.selectActiveByVinAndVehicleNodeCode("VIN_A", "TBOX_5G")).thenReturn(null);

        // When
        VehiclePartBindResult result = vehiclePartAppService.bindVehiclePartIdempotent(vehiclePart);

        // Then
        assertEquals(VehiclePartBindResult.BOUND, result);
        verify(vehiclePartRepository).insert(vehiclePart);
        verify(vehiclePartBindingPublisher).publishBindingChanged(eq(vehiclePart), any());
    }

    @Test
    @DisplayName("幂等绑定-零件已绑定同一VIN应幂等命中跳过")
    void testBindIdempotentWhenSameVinBound() {
        // Given
        VehiclePart vehiclePart = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        VehiclePart existing = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        when(vehiclePartRepository.selectActiveByPartId(1001L)).thenReturn(existing);

        // When
        VehiclePartBindResult result = vehiclePartAppService.bindVehiclePartIdempotent(vehiclePart);

        // Then
        assertEquals(VehiclePartBindResult.SKIPPED_IDEMPOTENT, result);
        verify(vehiclePartRepository, never()).insert(any());
        verify(vehiclePartBindingPublisher, never()).publishBindingChanged(any(), any());
    }

    @Test
    @DisplayName("幂等绑定-零件已绑定其他VIN应抛绑定冲突")
    void testBindIdempotentWhenOtherVinBound() {
        // Given
        VehiclePart vehiclePart = buildVehiclePart("VIN_B", 1001L, "TBOX_5G");
        VehiclePart existing = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        when(vehiclePartRepository.selectActiveByPartId(1001L)).thenReturn(existing);

        // When & Then
        assertThrows(PartBindingConflictException.class,
                () -> vehiclePartAppService.bindVehiclePartIdempotent(vehiclePart));
        verify(vehiclePartRepository, never()).insert(any());
    }

    @Test
    @DisplayName("幂等绑定-装车槽位已被其他零件占用应抛绑定冲突")
    void testBindIdempotentWhenSlotOccupied() {
        // Given
        VehiclePart vehiclePart = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        VehiclePart slotOccupied = buildVehiclePart("VIN_A", 1002L, "TBOX_5G");
        when(vehiclePartRepository.selectActiveByPartId(1001L)).thenReturn(null);
        when(vehiclePartRepository.selectActiveByVinAndVehicleNodeCode("VIN_A", "TBOX_5G")).thenReturn(slotOccupied);

        // When & Then
        assertThrows(PartBindingConflictException.class,
                () -> vehiclePartAppService.bindVehiclePartIdempotent(vehiclePart));
        verify(vehiclePartRepository, never()).insert(any());
    }

    @Test
    @DisplayName("幂等绑定-槽位被同一零件占用应幂等命中跳过")
    void testBindIdempotentWhenSamePartInSlot() {
        // Given
        VehiclePart vehiclePart = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        VehiclePart slotSamePart = buildVehiclePart("VIN_A", 1001L, "TBOX_5G");
        when(vehiclePartRepository.selectActiveByPartId(1001L)).thenReturn(null);
        when(vehiclePartRepository.selectActiveByVinAndVehicleNodeCode("VIN_A", "TBOX_5G")).thenReturn(slotSamePart);

        // When
        VehiclePartBindResult result = vehiclePartAppService.bindVehiclePartIdempotent(vehiclePart);

        // Then
        assertEquals(VehiclePartBindResult.SKIPPED_IDEMPOTENT, result);
        verify(vehiclePartRepository, never()).insert(any());
    }
}
