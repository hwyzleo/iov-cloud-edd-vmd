package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleNodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVehicleNodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVehicleNodePo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdmVehicleNodeRepositoryImplTest {

    @Mock
    private MdmVehicleNodeMapper mdmVehicleNodeMapper;

    @InjectMocks
    private MdmVehicleNodeRepositoryImpl mdmVehicleNodeRepository;

    @Test
    @DisplayName("selectByMap应返回车载节点列表")
    void testSelectByMap() {
        MdmVehicleNodePo po1 = MdmVehicleNodePo.builder().id(1L).code("VN001").name("车载节点1").build();
        MdmVehicleNodePo po2 = MdmVehicleNodePo.builder().id(2L).code("VN002").name("车载节点2").build();

        when(mdmVehicleNodeMapper.selectPoByMap(any(Map.class))).thenReturn(Arrays.asList(po1, po2));

        Map<String, Object> map = new HashMap<>();
        map.put("code", "VN");
        List<VehicleNode> result = mdmVehicleNodeRepository.selectByMap(map);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmVehicleNodeMapper).selectPoByMap(any(Map.class));
    }

    @Test
    @DisplayName("countByMap应返回数量")
    void testCountByMap() {
        when(mdmVehicleNodeMapper.countPoByMap(any(Map.class))).thenReturn(5);

        Map<String, Object> map = new HashMap<>();
        int result = mdmVehicleNodeRepository.countByMap(map);

        assertEquals(5, result);
        verify(mdmVehicleNodeMapper).countPoByMap(any(Map.class));
    }

    @Test
    @DisplayName("selectById应返回车载节点")
    void testSelectById() {
        Long id = 1L;
        MdmVehicleNodePo po = MdmVehicleNodePo.builder().id(id).code("VN001").name("车载节点1").build();

        when(mdmVehicleNodeMapper.selectPoById(id)).thenReturn(po);

        VehicleNode result = mdmVehicleNodeRepository.selectById(id);

        assertNotNull(result);
        assertEquals("VN001", result.getCode());
        verify(mdmVehicleNodeMapper).selectPoById(id);
    }

    @Test
    @DisplayName("selectByCode应返回车载节点")
    void testSelectByCode() {
        String code = "VN001";
        MdmVehicleNodePo po = MdmVehicleNodePo.builder().id(1L).code(code).name("车载节点1").build();

        when(mdmVehicleNodeMapper.selectPoByCode(code)).thenReturn(po);

        VehicleNode result = mdmVehicleNodeRepository.selectByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(mdmVehicleNodeMapper).selectPoByCode(code);
    }

    @Test
    @DisplayName("selectByExternalRefId应返回车载节点")
    void testSelectByExternalRefId() {
        String externalRefId = "ext-001";
        MdmVehicleNodePo po = MdmVehicleNodePo.builder().id(1L).code("VN001").externalRefId(externalRefId).build();

        when(mdmVehicleNodeMapper.selectPoByExternalRefId(externalRefId)).thenReturn(po);

        VehicleNode result = mdmVehicleNodeRepository.selectByExternalRefId(externalRefId);

        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        verify(mdmVehicleNodeMapper).selectPoByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的数量")
    void testCountBySource() {
        when(mdmVehicleNodeMapper.countPoBySource("MDM")).thenReturn(10L);

        long result = mdmVehicleNodeRepository.countBySource(SourceType.MDM);

        assertEquals(10L, result);
        verify(mdmVehicleNodeMapper).countPoBySource("MDM");
    }

    @Test
    @DisplayName("insert应成功插入车载节点")
    void testInsert() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .code("VN001")
                .name("车载节点1")
                .source(SourceType.MANUAL)
                .build();

        when(mdmVehicleNodeMapper.insertPo(any(MdmVehicleNodePo.class))).thenReturn(1);

        int result = mdmVehicleNodeRepository.insert(vehicleNode);

        assertEquals(1, result);
        verify(mdmVehicleNodeMapper).insertPo(any(MdmVehicleNodePo.class));
    }

    @Test
    @DisplayName("update应成功更新车载节点")
    void testUpdate() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("更新后的车载节点")
                .source(SourceType.MANUAL)
                .build();

        when(mdmVehicleNodeMapper.updatePo(any(MdmVehicleNodePo.class))).thenReturn(1);

        int result = mdmVehicleNodeRepository.update(vehicleNode);

        assertEquals(1, result);
        verify(mdmVehicleNodeMapper).updatePo(any(MdmVehicleNodePo.class));
    }

    @Test
    @DisplayName("updateById应成功更新车载节点")
    void testUpdateById() {
        VehicleNode vehicleNode = VehicleNode.builder()
                .id(1L)
                .code("VN001")
                .name("更新后的车载节点")
                .source(SourceType.MDM)
                .externalRefId("ext-001")
                .externalVersion(2L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        when(mdmVehicleNodeMapper.updatePo(any(MdmVehicleNodePo.class))).thenReturn(1);

        int result = mdmVehicleNodeRepository.updateById(vehicleNode);

        assertEquals(1, result);
        verify(mdmVehicleNodeMapper).updatePo(any(MdmVehicleNodePo.class));
    }

    @Test
    @DisplayName("batchPhysicalDelete应成功批量删除车载节点")
    void testBatchPhysicalDelete() {
        Long[] ids = {1L, 2L, 3L};

        when(mdmVehicleNodeMapper.batchPhysicalDeletePo(ids)).thenReturn(3);

        int result = mdmVehicleNodeRepository.batchPhysicalDelete(ids);

        assertEquals(3, result);
        verify(mdmVehicleNodeMapper).batchPhysicalDeletePo(ids);
    }
}
