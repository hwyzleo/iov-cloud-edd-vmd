package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactQueryUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 首次激活事实只读查询单元测试（VMD-DSN-CR-077 §7.2 / TC-077-11 / TC-077-12）
 * <p>
 * 三态判定、factRef 格式、fail-closed（806137，绝不降级为 NOT_RECORDED）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleActivationQueryAppService 测试")
class VehicleActivationQueryAppServiceTest {

    private static final String VIN = "LSVAA1234567890AB";

    @Mock
    private VehicleActivationNodeRepository nodeRepository;
    @Mock
    private VehicleActivationFactConsumeRepository consumeRepository;

    private VehicleActivationQueryAppService queryService;

    @BeforeEach
    void setUp() {
        queryService = new VehicleActivationQueryAppService(nodeRepository, consumeRepository);
    }

    private LocalDateTime toLocal(String instant) {
        return LocalDateTime.ofInstant(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("TC-077-11：投影存在 → RECORDED，返回首次激活事实与 factRef")
    void recorded() {
        when(nodeRepository.selectByVin(VIN)).thenReturn(VehicleActivationNode.builder()
                .id(1L)
                .vin(VIN)
                .firstActivatedAt(toLocal("2026-11-22T09:45:01.234Z"))
                .bindingOperationId("BOP1")
                .sourceFactRef("vso:order:SO1#delivery")
                .eventId("UVA1")
                .occurredAt(toLocal("2026-11-22T09:45:01.234Z"))
                .receivedAt(toLocal("2026-11-22T09:45:12.008Z"))
                .vinResolved(1)
                .build());

        VehicleActivationDto dto = queryService.queryFirstActivation(VIN);

        assertEquals(VehicleActivationState.RECORDED, dto.getState());
        assertEquals(VIN, dto.getVin());
        assertEquals(Instant.parse("2026-11-22T09:45:01.234Z"), dto.getFirstActivatedAt());
        assertEquals(Instant.parse("2026-11-22T09:45:12.008Z"), dto.getReceivedAt());
        assertEquals(Boolean.TRUE, dto.getVinResolved());
        assertEquals("vehicleActivation:" + VIN + ":UVA1:2026-11-22T09:45:01.234Z", dto.getFactRef());
    }

    @Test
    @DisplayName("TC-077-11：仅有未生效台账 → PENDING，firstActivatedAt=null 且不暴露 factRef")
    void pending() {
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(consumeRepository.selectLatestIneffectiveByVin(VIN)).thenReturn(VehicleActivationFactConsume.builder()
                .id(2L)
                .eventId("UVA-QUARANTINED")
                .vin(VIN)
                .bindingOperationId("BOP1")
                .occurredAt(toLocal("2026-11-22T09:45:01.234Z"))
                .receivedAt(toLocal("2026-11-22T09:45:12.008Z"))
                .status("QUARANTINED")
                .applied(0)
                .build());

        VehicleActivationDto dto = queryService.queryFirstActivation(VIN);

        assertEquals(VehicleActivationState.PENDING, dto.getState());
        assertNull(dto.getFirstActivatedAt(), "PENDING 必须显式 firstActivatedAt=null");
        assertNull(dto.getFactRef());
        assertEquals("UVA-QUARANTINED", dto.getEventId());
        assertEquals(Instant.parse("2026-11-22T09:45:12.008Z"), dto.getReceivedAt());
    }

    @Test
    @DisplayName("TC-077-11：无投影且无台账 → NOT_RECORDED，vinResolved 为 null")
    void notRecorded() {
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(consumeRepository.selectLatestIneffectiveByVin(VIN)).thenReturn(null);

        VehicleActivationDto dto = queryService.queryFirstActivation(VIN);

        assertEquals(VehicleActivationState.NOT_RECORDED, dto.getState());
        assertNull(dto.getVinResolved(), "NOT_RECORDED 不额外查主档，不得混淆未建档与未激活");
        assertNull(dto.getFactRef());
    }

    @Test
    @DisplayName("TC-077-12：投影查询异常 → 806137 fail-closed，绝不返回 NOT_RECORDED")
    void storageFailure_failClosed() {
        when(nodeRepository.selectByVin(VIN)).thenThrow(new IllegalStateException("db down"));

        ActivationFactQueryUnavailableException ex = assertThrows(
                ActivationFactQueryUnavailableException.class,
                () -> queryService.queryFirstActivation(VIN));

        assertEquals(VmdErrorCode.ACTIVATION_FACT_QUERY_UNAVAILABLE.getCode(), ex.getErrorCode().getCode());
        assertEquals("806137", ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("TC-077-12：台账查询异常（投影不存在）→ 同样 fail-closed 806137")
    void consumeQueryFailure_failClosed() {
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(consumeRepository.selectLatestIneffectiveByVin(VIN))
                .thenThrow(new IllegalStateException("timeout"));

        ActivationFactQueryUnavailableException ex = assertThrows(
                ActivationFactQueryUnavailableException.class,
                () -> queryService.queryFirstActivation(VIN));
        assertEquals(VmdErrorCode.ACTIVATION_FACT_QUERY_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("vinResolved=0（未建档）→ 返回 false 而非 null，供调用方区分")
    void unresolvedVin_mappedToFalse() {
        when(nodeRepository.selectByVin(VIN)).thenReturn(VehicleActivationNode.builder()
                .vin(VIN)
                .firstActivatedAt(toLocal("2026-11-22T09:45:01.234Z"))
                .occurredAt(toLocal("2026-11-22T09:45:01.234Z"))
                .eventId("UVA1")
                .vinResolved(0)
                .build());

        VehicleActivationDto dto = queryService.queryFirstActivation(VIN);

        assertNotNull(dto.getVinResolved());
        assertFalse(dto.getVinResolved());
        assertTrue(dto.getFactRef().startsWith("vehicleActivation:" + VIN + ":"));
    }

}
