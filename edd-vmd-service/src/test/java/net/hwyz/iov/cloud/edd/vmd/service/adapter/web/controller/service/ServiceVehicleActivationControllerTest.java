package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleActivationNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationQueryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactQueryUnavailableException;
import net.hwyz.iov.cloud.framework.web.exception.InputParamInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 车辆首次激活事实只读端点单元测试（VMD-DSN-CR-077 §7）
 * <p>
 * 覆盖 TC-VMD-077-11/12：三态映射、参数非法与「无记录」严格区分、
 * 技术失败 fail-closed（不返回 NOT_RECORDED）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceVehicleActivationController 测试")
class ServiceVehicleActivationControllerTest {

    private static final String VIN = "LSVAA1234567890AB";

    @Mock
    private VehicleActivationQueryAppService queryAppService;

    private ServiceVehicleActivationController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceVehicleActivationController(queryAppService);
    }

    @Test
    @DisplayName("TC-077-11：RECORDED 按契约映射（含 factRef 与 vinResolved）")
    void recorded_mapped() {
        Instant firstActivatedAt = Instant.parse("2026-11-22T09:45:01.234Z");
        when(queryAppService.queryFirstActivation(VIN)).thenReturn(VehicleActivationDto.builder()
                .state(VehicleActivationState.RECORDED)
                .vin(VIN)
                .firstActivatedAt(firstActivatedAt)
                .bindingOperationId("BOP1")
                .sourceFactRef("vso:order:SO1#delivery")
                .eventId("UVA1")
                .occurredAt(firstActivatedAt)
                .receivedAt(Instant.parse("2026-11-22T09:45:12.008Z"))
                .vinResolved(true)
                .factRef("vehicleActivation:" + VIN + ":UVA1:" + firstActivatedAt)
                .build());

        VehicleActivationNodeExResponse response = controller.getFirstActivation(" " + VIN + " ");

        assertEquals(VehicleActivationState.RECORDED, response.getState());
        assertEquals(VIN, response.getVin());
        assertEquals(firstActivatedAt, response.getFirstActivatedAt());
        assertEquals("UVA1", response.getEventId());
        assertEquals(Boolean.TRUE, response.getVinResolved());
        assertTrue(response.getFactRef().startsWith("vehicleActivation:"));
    }

    @Test
    @DisplayName("TC-077-11：PENDING 不返回 firstActivatedAt/factRef")
    void pending_mapped() {
        when(queryAppService.queryFirstActivation(VIN)).thenReturn(VehicleActivationDto.builder()
                .state(VehicleActivationState.PENDING)
                .vin(VIN)
                .eventId("UVA-QUARANTINED")
                .occurredAt(Instant.parse("2026-11-22T09:45:01.234Z"))
                .receivedAt(Instant.parse("2026-11-22T09:45:12.008Z"))
                .build());

        VehicleActivationNodeExResponse response = controller.getFirstActivation(VIN);

        assertEquals(VehicleActivationState.PENDING, response.getState());
        assertNull(response.getFirstActivatedAt());
        assertNull(response.getFactRef());
    }

    @Test
    @DisplayName("TC-077-11：NOT_RECORDED 全字段空")
    void notRecorded_mapped() {
        when(queryAppService.queryFirstActivation(VIN)).thenReturn(VehicleActivationDto.builder()
                .state(VehicleActivationState.NOT_RECORDED)
                .vin(VIN)
                .build());

        VehicleActivationNodeExResponse response = controller.getFirstActivation(VIN);

        assertEquals(VehicleActivationState.NOT_RECORDED, response.getState());
        assertNull(response.getFactRef());
        assertNull(response.getVinResolved());
    }

    @Test
    @DisplayName("VIN 空 / 超长 → 框架参数错误（100001），不混同为「无记录」")
    void invalidVin_parameterError() {
        assertThrows(InputParamInvalidException.class, () -> controller.getFirstActivation(" "));
        assertThrows(InputParamInvalidException.class,
                () -> controller.getFirstActivation("LSVAA1234567890ABCDEF"));
        verifyNoInteractions(queryAppService);
    }

    @Test
    @DisplayName("TC-077-12：查询不可用 → 806137 上抛，不返回 NOT_RECORDED")
    void unavailable_failClosed() {
        when(queryAppService.queryFirstActivation(VIN))
                .thenThrow(new ActivationFactQueryUnavailableException("db down"));

        ActivationFactQueryUnavailableException ex = assertThrows(
                ActivationFactQueryUnavailableException.class, () -> controller.getFirstActivation(VIN));
        assertEquals("806137", ex.getErrorCode().getCode());
    }

}
