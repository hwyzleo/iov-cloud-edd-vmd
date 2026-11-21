package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleByDeviceDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 设备→当前绑定车辆内部服务端点单元测试（VMD-DSN-CR-076）
 * <p>
 * 覆盖 TC-VMD-76-05：参数缺失/非法返回框架参数错误（100001），不混同为「无绑定」；
 * 以及响应字段映射与入参规范化。
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
@ExtendWith(MockitoExtension.class)
class ServiceVehiclePartBindingControllerTest {

    private static final String DEVICE_ID = "IVI0001ABCD0001";
    private static final String DEVICE_CATEGORY = "IVI";

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    @Mock
    private VehicleNodeAppService vehicleNodeAppService;

    private ServiceVehiclePartBindingController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceVehiclePartBindingController(vehiclePartAppService, vehicleNodeAppService);
    }

    @Test
    @DisplayName("TC-VMD-76-01：BOUND 结果按契约映射为响应字段")
    void shouldMapBoundResult() {
        Instant updatedAt = Instant.parse("2026-11-20T02:10:00Z");
        when(vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(VehicleByDeviceDto.builder()
                        .bindingState(VehicleBindingState.BOUND)
                        .vin("LSVAA1234567890AB")
                        .factRef("vehiclePartBinding:LSVAA1234567890AB:1:2026-11-20T02:10:00Z")
                        .updatedAt(updatedAt)
                        .build());

        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.BOUND, response.getBindingState());
        assertEquals("LSVAA1234567890AB", response.getVin());
        assertEquals("vehiclePartBinding:LSVAA1234567890AB:1:2026-11-20T02:10:00Z", response.getFactRef());
        assertEquals(updatedAt, response.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-VMD-76-02：无绑定仍返回 200 语义（状态非空、VIN 为空）")
    void shouldMapNoActiveBinding() {
        when(vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(VehicleByDeviceDto.builder()
                        .bindingState(VehicleBindingState.NO_ACTIVE_BINDING)
                        .build());

        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);

        assertEquals(VehicleBindingState.NO_ACTIVE_BINDING, response.getBindingState());
        assertNull(response.getVin());
        assertNull(response.getFactRef());
        assertNull(response.getUpdatedAt());
    }

    @Test
    @DisplayName("入参规范化：首尾空白被裁剪，空串视为缺失")
    void shouldTrimParams() {
        when(vehiclePartAppService.queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY))
                .thenReturn(VehicleByDeviceDto.builder()
                        .bindingState(VehicleBindingState.NO_ACTIVE_BINDING).build());

        controller.getVehicleByDevice("  " + DEVICE_ID + "  ", " " + DEVICE_CATEGORY + " ");

        verify(vehiclePartAppService).queryVehicleByDevice(DEVICE_ID, DEVICE_CATEGORY);
    }

    @Test
    @DisplayName("TC-VMD-76-05：deviceId 缺失/空白 → 框架参数错误，不查库")
    void shouldRejectMissingDeviceId() {
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(null, DEVICE_CATEGORY));
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice("   ", DEVICE_CATEGORY));
        verifyNoInteractions(vehiclePartAppService);
    }

    @Test
    @DisplayName("TC-VMD-76-05：deviceCategory 缺失/空白 → 框架参数错误，不查库")
    void shouldRejectMissingDeviceCategory() {
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(DEVICE_ID, null));
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(DEVICE_ID, "  "));
        verifyNoInteractions(vehiclePartAppService);
    }

    @Test
    @DisplayName("TC-VMD-76-05：超长 deviceId / deviceCategory → 框架参数错误")
    void shouldRejectOverLengthParams() {
        String longDeviceId = "D".repeat(256);
        String longCategory = "C".repeat(21);

        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(longDeviceId, DEVICE_CATEGORY));
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(DEVICE_ID, longCategory));
        verifyNoInteractions(vehiclePartAppService);
    }

    @Test
    @DisplayName("框架参数错误码为入参无效 100001，不占用 VMD 806xxx 业务码")
    void shouldUseFrameworkInputParamInvalidCode() {
        InputParamInvalidException exception = assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(null, DEVICE_CATEGORY));

        assertEquals(100001, exception.getCode());
    }

}
