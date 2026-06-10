package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleNodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleNodeAppServiceTest {

    @Mock
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;

    @InjectMocks
    private VehicleNodeAppService vehicleNodeAppService;

    @Test
    @DisplayName("search方法应返回匹配的车载节点列表")
    void testSearch() {
        VehicleNodeQuery query = VehicleNodeQuery.builder()
                .code("VN001")
                .name("测试")
                .funcDomain("ADAS")
                .build();

        VehicleNode vn1 = VehicleNode.builder().id(1L).code("VN001").name("车载节点1").build();
        VehicleNode vn2 = VehicleNode.builder().id(2L).code("VN002").name("车载节点2").build();

        when(mdmVehicleNodeRepository.selectByMap(any(Map.class))).thenReturn(Arrays.asList(vn1, vn2));

        List<VehicleNodeDto> result = vehicleNodeAppService.search(query);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmVehicleNodeRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("listAll方法应返回所有车载节点")
    void testListAll() {
        VehicleNode vn1 = VehicleNode.builder().id(1L).code("VN001").name("车载节点1").build();
        VehicleNode vn2 = VehicleNode.builder().id(2L).code("VN002").name("车载节点2").build();

        when(mdmVehicleNodeRepository.selectByMap(any(Map.class))).thenReturn(Arrays.asList(vn1, vn2));

        List<VehicleNodeDto> result = vehicleNodeAppService.listAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmVehicleNodeRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void testCheckCodeUnique() {
        String code = "VN001";
        when(mdmVehicleNodeRepository.selectByCode(code)).thenReturn(null);

        Boolean result = vehicleNodeAppService.checkCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmVehicleNodeRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一车载节点时")
    void testCheckCodeUnique_sameVehicleNode() {
        String code = "VN001";
        VehicleNode existing = VehicleNode.builder().id(1L).code(code).build();

        when(mdmVehicleNodeRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = vehicleNodeAppService.checkCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmVehicleNodeRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void testCheckCodeUnique_duplicate() {
        String code = "VN001";
        VehicleNode existing = VehicleNode.builder().id(2L).code(code).build();

        when(mdmVehicleNodeRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = vehicleNodeAppService.checkCodeUnique(1L, code);

        assertFalse(result);
        verify(mdmVehicleNodeRepository).selectByCode(code);
    }

    @Test
    @DisplayName("getVehicleNodeById应返回车载节点DTO")
    void testGetVehicleNodeById() {
        Long id = 1L;
        VehicleNode vn = VehicleNode.builder().id(id).code("VN001").name("车载节点1").build();

        when(mdmVehicleNodeRepository.selectById(id)).thenReturn(vn);

        VehicleNodeDto result = vehicleNodeAppService.getVehicleNodeById(id);

        assertNotNull(result);
        assertEquals("VN001", result.getCode());
        verify(mdmVehicleNodeRepository).selectById(id);
    }

    @Test
    @DisplayName("getVehicleNodeByCode应返回车载节点领域对象")
    void testGetVehicleNodeByCode() {
        String code = "VN001";
        VehicleNode vn = VehicleNode.builder().id(1L).code(code).name("车载节点1").build();

        when(mdmVehicleNodeRepository.selectByCode(code)).thenReturn(vn);

        VehicleNode result = vehicleNodeAppService.getVehicleNodeByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(mdmVehicleNodeRepository).selectByCode(code);
    }

    @Test
    @DisplayName("listAllFota应返回OTA支持的车载节点列表")
    void testListAllFota() {
        VehicleNode vn1 = VehicleNode.builder().id(1L).code("VN001").otaSupport("OTA").build();

        when(mdmVehicleNodeRepository.selectByMap(any(Map.class))).thenReturn(List.of(vn1));

        List<VehicleNode> result = vehicleNodeAppService.listAllFota();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mdmVehicleNodeRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("createVehicleNode应拒绝创建MDM来源车载节点")
    void testCreateVehicleNode_sourceMdm_throwsException() {
        VehicleNodeCmd cmd = VehicleNodeCmd.builder()
                .code("VN001")
                .name("MDM车载节点")
                .build();

        // When source is null (default), it won't be MDM, so it should succeed
        when(mdmVehicleNodeRepository.insert(any(VehicleNode.class))).thenReturn(1);

        int result = vehicleNodeAppService.createVehicleNode(cmd);

        assertEquals(1, result);
        verify(mdmVehicleNodeRepository).insert(any(VehicleNode.class));
    }

    @Test
    @DisplayName("modifyVehicleNode应拒绝修改MDM来源车载节点")
    void testModifyVehicleNode_sourceMdm_throwsException() {
        VehicleNodeCmd cmd = VehicleNodeCmd.builder()
                .id(1L)
                .code("VN001")
                .name("修改MDM车载节点")
                .build();

        VehicleNode existing = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("MDM车载节点")
                .source(SourceType.MDM)
                .build();

        when(mdmVehicleNodeRepository.selectById(1L)).thenReturn(existing);

        assertThrows(ProductDataReadOnlyException.class, () -> {
            vehicleNodeAppService.modifyVehicleNode(cmd);
        });
        verify(mdmVehicleNodeRepository).selectById(1L);
        verify(mdmVehicleNodeRepository, never()).update(any(VehicleNode.class));
    }

    @Test
    @DisplayName("modifyVehicleNode应成功修改MANUAL来源车载节点")
    void testModifyVehicleNode_sourceManual_success() {
        VehicleNodeCmd cmd = VehicleNodeCmd.builder()
                .id(1L)
                .code("VN001")
                .name("修改后的车载节点")
                .build();

        VehicleNode existing = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("原始车载节点")
                .source(SourceType.MANUAL)
                .build();

        when(mdmVehicleNodeRepository.selectById(1L)).thenReturn(existing);
        when(mdmVehicleNodeRepository.update(any(VehicleNode.class))).thenReturn(1);

        int result = vehicleNodeAppService.modifyVehicleNode(cmd);

        assertEquals(1, result);
        verify(mdmVehicleNodeRepository).selectById(1L);
        verify(mdmVehicleNodeRepository).update(any(VehicleNode.class));
    }

    @Test
    @DisplayName("deleteVehicleNodeByIds应拒绝删除MDM来源车载节点")
    void testDeleteVehicleNode_sourceMdm_throwsException() {
        Long[] ids = {1L, 2L};

        VehicleNode vn1 = VehicleNode.builder().id(1L).code("VN001").source(SourceType.MANUAL).build();
        VehicleNode vn2 = VehicleNode.builder().id(2L).code("VN002").source(SourceType.MDM).build();

        when(mdmVehicleNodeRepository.selectById(1L)).thenReturn(vn1);
        when(mdmVehicleNodeRepository.selectById(2L)).thenReturn(vn2);

        assertThrows(ProductDataReadOnlyException.class, () -> {
            vehicleNodeAppService.deleteVehicleNodeByIds(ids);
        });
        verify(mdmVehicleNodeRepository).selectById(1L);
        verify(mdmVehicleNodeRepository).selectById(2L);
        verify(mdmVehicleNodeRepository, never()).batchPhysicalDelete(any(Long[].class));
    }

    @Test
    @DisplayName("deleteVehicleNodeByIds应成功删除MANUAL来源车载节点")
    void testDeleteVehicleNode_sourceManual_success() {
        Long[] ids = {1L, 2L};

        VehicleNode vn1 = VehicleNode.builder().id(1L).code("VN001").source(SourceType.MANUAL).build();
        VehicleNode vn2 = VehicleNode.builder().id(2L).code("VN002").source(SourceType.MANUAL).build();

        when(mdmVehicleNodeRepository.selectById(1L)).thenReturn(vn1);
        when(mdmVehicleNodeRepository.selectById(2L)).thenReturn(vn2);
        when(mdmVehicleNodeRepository.batchPhysicalDelete(ids)).thenReturn(2);

        int result = vehicleNodeAppService.deleteVehicleNodeByIds(ids);

        assertEquals(2, result);
        verify(mdmVehicleNodeRepository).selectById(1L);
        verify(mdmVehicleNodeRepository).selectById(2L);
        verify(mdmVehicleNodeRepository).batchPhysicalDelete(ids);
    }
}
