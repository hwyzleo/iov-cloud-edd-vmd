package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import com.github.pagehelper.PageHelper;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台车辆接口测试
 * <p>
 * 回归：订单配车弹窗调用 GET /api/mpt/vehicle/v1/listAssignable 时，
 * 若该 GET 端点缺失，请求会被 @DeleteMapping("/{vehicleIds}") 路径模式匹配，
 * 返回 "Request method 'GET' is not supported"（HTTP 405），导致分配车辆失败。
 *
 * @author hwyz_leo
 */
class MptVehicleControllerTest {

    private static final String BASE_PATH = "/api/mpt/vehicle/v1";

    private VehicleAppService vehicleAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        vehicleAppService = mock(VehicleAppService.class);
        VehicleLifecycleAppService vehicleLifecycleAppService = mock(VehicleLifecycleAppService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MptVehicleController(vehicleAppService, vehicleLifecycleAppService))
                .build();
    }

    @AfterEach
    void tearDown() {
        PageHelper.clearPage();
    }

    @Test
    @DisplayName("可分配车辆：GET listAssignable 不应 405，且仅查询已下线未绑单车辆")
    void listAssignable() throws Exception {
        when(vehicleAppService.search(any(VehicleQuery.class)))
                .thenReturn(List.of(vehicle("VIN001"), vehicle("VIN002")));

        mockMvc.perform(get(BASE_PATH + "/listAssignable")
                        .param("modelConfigCode", "CFG001")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2));

        ArgumentCaptor<VehicleQuery> captor = ArgumentCaptor.forClass(VehicleQuery.class);
        verify(vehicleAppService).search(captor.capture());
        VehicleQuery query = captor.getValue();
        assertEquals("CFG001", query.getConfigurationCode());
        assertEquals(Boolean.TRUE, query.getIsEol());
        assertEquals(Boolean.FALSE, query.getIsOrder());
    }

    @Test
    @DisplayName("可分配车辆：未传 modelConfigCode 时回退按 configurationCode 过滤")
    void listAssignableFallsBackToConfigurationCode() throws Exception {
        when(vehicleAppService.search(any(VehicleQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/listAssignable")
                        .param("configurationCode", "CFG002")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<VehicleQuery> captor = ArgumentCaptor.forClass(VehicleQuery.class);
        verify(vehicleAppService).search(captor.capture());
        assertEquals("CFG002", captor.getValue().getConfigurationCode());
    }

    @Test
    @DisplayName("可分配车辆：不传配置代码时不过滤配置")
    void listAssignableWithoutConfigurationCode() throws Exception {
        when(vehicleAppService.search(any(VehicleQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/listAssignable")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<VehicleQuery> captor = ArgumentCaptor.forClass(VehicleQuery.class);
        verify(vehicleAppService).search(captor.capture());
        assertNull(captor.getValue().getConfigurationCode());
    }

    private VehicleDto vehicle(String vin) {
        return VehicleDto.builder().vin(vin).build();
    }
}
