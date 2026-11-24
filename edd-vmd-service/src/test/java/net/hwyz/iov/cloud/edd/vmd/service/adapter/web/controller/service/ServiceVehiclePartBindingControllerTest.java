package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehiclePartQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 绑定事实对外服务：chipUid 组装单元测试（SEC-VIAM-DSN-CR-006 / D-VIAM-030）
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceVehiclePartBindingControllerTest {

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    @Mock
    private VehicleNodeAppService vehicleNodeAppService;

    @Mock
    private PartSecurityConstantRepository partSecurityConstantRepository;

    @InjectMocks
    private ServiceVehiclePartBindingController controller;

    private static VehiclePartDto dto(long id, String partCode, String sn, String nodeCode) {
        VehiclePartDto dto = new VehiclePartDto();
        dto.setId(id);
        dto.setVin("HWYZTEST900000001");
        dto.setPartCode(partCode);
        dto.setSn(sn);
        dto.setVehicleNodeCode(nodeCode);
        dto.setBindTime(Instant.parse("2026-09-23T03:39:54Z"));
        return dto;
    }

    private static VehicleNode node(String code, String category) {
        return VehicleNode.builder().code(code).deviceCategory(category).build();
    }

    @Test
    @DisplayName("按 VIN 查询：批量组装 chipUid 且类别取 MDM 节点")
    void listActiveBindingsByVin_shouldFillChipUidAndCategory() {
        VehiclePartDto tbox = dto(1L, "00000001AA", "00000002AA00000001", "TBOX_5G");
        VehiclePartDto ivi = dto(2L, "00000004AA", "00000005AA00000001", "DCU_COCKPIT_SA8295P");
        when(vehiclePartAppService.search(any(VehiclePartQuery.class))).thenReturn(List.of(tbox, ivi));
        when(partSecurityConstantRepository.selectChipUidByKeys(anyCollection())).thenReturn(Map.of(
                "00000001AA:00000002AA00000001", "00000000000000000000000000000001",
                "00000004AA:00000005AA00000001", "00000005AA0000000000000000000001"));
        when(vehicleNodeAppService.getVehicleNodeByCode("TBOX_5G")).thenReturn(node("TBOX_5G", "TBOX"));
        when(vehicleNodeAppService.getVehicleNodeByCode("DCU_COCKPIT_SA8295P"))
                .thenReturn(node("DCU_COCKPIT_SA8295P", "IVI"));

        List<VehiclePartBindingExResponse> result = controller.listActiveBindingsByVin("HWYZTEST900000001");

        assertEquals(2, result.size());
        assertEquals("IVI", result.get(1).getDeviceCategory());
        assertEquals("00000005AA0000000000000000000001", result.get(1).getChipUid());
        assertEquals("TBOX", result.get(0).getDeviceCategory());
        assertEquals("00000000000000000000000000000001", result.get(0).getChipUid());
        // 批量查询：一次调用，避免 N+1
        verify(partSecurityConstantRepository, times(1)).selectChipUidByKeys(anyCollection());
    }

    @Test
    @DisplayName("芯片 UID 缺失时返回 null，不回退 SN/VIN")
    void listActiveBindingsByVin_shouldReturnNullChipUidWhenMissing() {
        VehiclePartDto ivi = dto(2L, "00000004AA", "00000005AA00000001", "DCU_COCKPIT_SA8295P");
        when(vehiclePartAppService.search(any(VehiclePartQuery.class))).thenReturn(List.of(ivi));
        when(partSecurityConstantRepository.selectChipUidByKeys(anyCollection())).thenReturn(Map.of());
        when(vehicleNodeAppService.getVehicleNodeByCode(anyString()))
                .thenReturn(node("DCU_COCKPIT_SA8295P", "IVI"));

        List<VehiclePartBindingExResponse> result = controller.listActiveBindingsByVin("HWYZTEST900000001");

        assertNull(result.get(0).getChipUid());
    }

    @Test
    @DisplayName("芯片 UID 查询异常时不阻断绑定事实返回")
    void listActiveBindingsByVin_shouldNotFailWhenChipUidQueryErrors() {
        VehiclePartDto ivi = dto(2L, "00000004AA", "00000005AA00000001", "DCU_COCKPIT_SA8295P");
        when(vehiclePartAppService.search(any(VehiclePartQuery.class))).thenReturn(List.of(ivi));
        when(partSecurityConstantRepository.selectChipUidByKeys(anyCollection()))
                .thenThrow(new IllegalStateException("db down"));
        when(vehicleNodeAppService.getVehicleNodeByCode(anyString()))
                .thenReturn(node("DCU_COCKPIT_SA8295P", "IVI"));

        List<VehiclePartBindingExResponse> result = controller.listActiveBindingsByVin("HWYZTEST900000001");

        assertEquals(1, result.size());
        assertNull(result.get(0).getChipUid());
    }

    @Test
    @DisplayName("bootstrap 分页同样组装 chipUid")
    void listBindingsForBootstrap_shouldFillChipUid() {
        VehiclePartDto ivi = dto(2L, "00000004AA", "00000005AA00000001", "DCU_COCKPIT_SA8295P");
        when(vehiclePartAppService.search(any(VehiclePartQuery.class))).thenReturn(List.of(ivi));
        when(partSecurityConstantRepository.selectChipUidByKeys(anyCollection()))
                .thenReturn(Map.of("00000004AA:00000005AA00000001", "00000005AA0000000000000000000001"));
        when(vehicleNodeAppService.getVehicleNodeByCode(anyString()))
                .thenReturn(node("DCU_COCKPIT_SA8295P", "IVI"));

        List<VehiclePartBindingExResponse> result = controller.listBindingsForBootstrap(null, 100);

        assertEquals(1, result.size());
        assertEquals("00000005AA0000000000000000000001", result.get(0).getChipUid());
    }

    @Test
    @DisplayName("空快照不触发 chipUid 查询")
    void listActiveBindingsByVin_shouldSkipQueryWhenEmpty() {
        when(vehiclePartAppService.search(any(VehiclePartQuery.class))).thenReturn(List.of());

        assertTrue(controller.listActiveBindingsByVin("HWYZTEST900000001").isEmpty());
        verify(partSecurityConstantRepository, never()).selectChipUidByKeys(anyCollection());
    }
}
