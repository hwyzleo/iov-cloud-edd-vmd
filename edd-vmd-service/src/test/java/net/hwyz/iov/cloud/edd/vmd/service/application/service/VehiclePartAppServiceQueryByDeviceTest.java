package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleByDeviceDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehiclePartBindingPublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.DeviceBindingFact;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 设备→当前绑定车辆查询应用服务单元测试（VMD-DSN-CR-076）
 * <p>
 * 覆盖 TC-VMD-76-01～04：唯一绑定、无绑定、跨 VIN 冲突、只读无副作用。
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
@ExtendWith(MockitoExtension.class)
class VehiclePartAppServiceQueryByDeviceTest {

    private static final String DEVICE_ID = "IVI0001ABCD0001";
    private static final String DEVICE_CATEGORY = "IVI";
    private static final String VIN = "LSVAA1234567890AB";

    @Mock
    private VehiclePartRepository vehiclePartRepository;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private VehiclePartBindingPublisher vehiclePartBindingPublisher;

    @InjectMocks
    private VehiclePartAppService vehiclePartAppService;

    @Test
    @DisplayName("TC-VMD-76-01：唯一 active 绑定 → BOUND + VIN + factRef + updatedAt")
    void shouldReturnBoundWhenSingleActiveBinding() {
        Instant updatedAt = Instant.parse("2026-11-20T02:10:00Z");
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(Collections.singletonList(DeviceBindingFact.builder()
                        .vin(VIN).bindingCount(1L).updatedAt(updatedAt).build()));

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.BOUND, result.getBindingState());
        assertEquals(VIN, result.getVin());
        assertEquals("vehiclePartBinding:" + VIN + ":1:2026-11-20T02:10:00Z", result.getFactRef());
        assertEquals(updatedAt, result.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-VMD-76-01：同 VIN 多条 active 事实 → BOUND，factRef 携带事实条数")
    void shouldReturnBoundWithFactCountWhenMultipleFactsForSameVin() {
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(Collections.singletonList(DeviceBindingFact.builder()
                        .vin(VIN).bindingCount(2L).updatedAt(Instant.parse("2026-11-20T02:10:00Z")).build()));

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.BOUND, result.getBindingState());
        assertEquals("vehiclePartBinding:" + VIN + ":2:2026-11-20T02:10:00Z", result.getFactRef());
    }

    @Test
    @DisplayName("TC-VMD-76-02：无 active 绑定（仅有失效记录）→ NO_ACTIVE_BINDING，无 VIN")
    void shouldReturnNoActiveBindingWhenNoFacts() {
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(Collections.emptyList());

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.NO_ACTIVE_BINDING, result.getBindingState());
        assertNull(result.getVin());
        assertNull(result.getFactRef());
        assertNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-VMD-76-02：仓储返回 null → NO_ACTIVE_BINDING（防御式）")
    void shouldReturnNoActiveBindingWhenRepositoryReturnsNull() {
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(null);

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.NO_ACTIVE_BINDING, result.getBindingState());
        assertNull(result.getVin());
    }

    @Test
    @DisplayName("TC-VMD-76-03：同设备多 VIN active → BINDING_CONFLICT 且不返回任何 VIN")
    void shouldReturnConflictWithoutVinWhenMultipleVins() {
        List<DeviceBindingFact> facts = Arrays.asList(
                DeviceBindingFact.builder().vin("VIN_A").bindingCount(1L)
                        .updatedAt(Instant.parse("2026-11-20T02:10:00Z")).build(),
                DeviceBindingFact.builder().vin("VIN_B").bindingCount(1L)
                        .updatedAt(Instant.parse("2026-11-20T03:10:00Z")).build());
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(facts);

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.BINDING_CONFLICT, result.getBindingState());
        assertNull(result.getVin());
        assertNull(result.getFactRef());
        assertNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-VMD-76-04：查询只读——不写绑定、不发布绑定变更事件")
    void shouldBeReadOnlyWithoutSideEffects() {
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(Collections.singletonList(DeviceBindingFact.builder()
                        .vin(VIN).bindingCount(1L).updatedAt(Instant.parse("2026-11-20T02:10:00Z")).build()));

        vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        verify(vehiclePartRepository).selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY);
        verify(vehiclePartRepository, never()).insert(any());
        verify(vehiclePartRepository, never()).update(any());
        verify(vehiclePartRepository, never()).batchInsert(any());
        verify(vehiclePartRepository, never()).batchPhysicalDelete(any());
        verify(vehiclePartRepository, never()).physicalDeleteByVin(any());
        verifyNoInteractions(vehiclePartBindingPublisher);
        verifyNoInteractions(partInfoRepository);
    }

    @Test
    @DisplayName("factRef：updatedAt 为空时降级为 vin+条数，不输出 null 文本")
    void shouldBuildFactRefWithoutUpdatedAtWhenNull() {
        when(vehiclePartRepository.selectActiveBindingFactsBySnAndDeviceCategory(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(Collections.singletonList(DeviceBindingFact.builder()
                        .vin(VIN).bindingCount(1L).updatedAt(null).build()));

        VehicleByDeviceDto result = vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.BOUND, result.getBindingState());
        assertEquals("vehiclePartBinding:" + VIN + ":1", result.getFactRef());
        assertNull(result.getUpdatedAt());
    }

}
