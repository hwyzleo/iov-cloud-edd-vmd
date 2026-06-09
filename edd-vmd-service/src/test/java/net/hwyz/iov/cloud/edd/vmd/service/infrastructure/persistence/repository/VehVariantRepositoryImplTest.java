package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehVariantOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehVariantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehVariantRepositoryImplTest {

    @Mock
    private VehVariantMapper vehVariantMapper;

    @Mock
    private VehVariantOptionCodeMapper vehVariantOptionCodeMapper;

    @InjectMocks
    private VehVariantRepositoryImpl vehVariantRepositoryImpl;

    private VehVariantPo buildPo(Long id, String code) {
        return VehVariantPo.builder()
                .id(id)
                .code(code)
                .name("测试版本")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .source("MANUAL")
                .build();
    }

    @Test
    @DisplayName("selectByCode应返回版本领域对象")
    void testSelectByCode() {
        String code = "V001";
        VehVariantPo po = buildPo(1L, code);

        when(vehVariantMapper.selectPoByCode(code)).thenReturn(po);

        Variant result = vehVariantRepositoryImpl.selectByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(1L, result.getId());
        verify(vehVariantMapper).selectPoByCode(code);
    }

    @Test
    @DisplayName("selectByCode应返回null当代码不存在时")
    void testSelectByCode_notFound() {
        String code = "NONEXISTENT";

        when(vehVariantMapper.selectPoByCode(code)).thenReturn(null);

        Variant result = vehVariantRepositoryImpl.selectByCode(code);

        assertNull(result);
        verify(vehVariantMapper).selectPoByCode(code);
    }

    @Test
    @DisplayName("selectByExternalRefId应返回MDM投影版本")
    void testSelectByExternalRefId() {
        String externalRefId = "ext-001";
        VehVariantPo po = VehVariantPo.builder()
                .id(1L)
                .code("V001")
                .name("MDM版本")
                .source("MDM")
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        when(vehVariantMapper.selectPoByExternalRefId(externalRefId)).thenReturn(po);

        Variant result = vehVariantRepositoryImpl.selectByExternalRefId(externalRefId);

        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
        verify(vehVariantMapper).selectPoByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的版本数量")
    void testCountBySource() {
        when(vehVariantMapper.countPoBySource("MDM")).thenReturn(5L);

        long result = vehVariantRepositoryImpl.countBySource(SourceType.MDM);

        assertEquals(5L, result);
        verify(vehVariantMapper).countPoBySource("MDM");
    }

    @Test
    @DisplayName("insert应成功插入版本")
    void testInsert() {
        Variant variant = Variant.builder()
                .code("V001")
                .name("新版本")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .source(SourceType.MANUAL)
                .build();

        when(vehVariantMapper.insertPo(any(VehVariantPo.class))).thenReturn(1);

        int result = vehVariantRepositoryImpl.insert(variant);

        assertEquals(1, result);
        verify(vehVariantMapper).insertPo(any(VehVariantPo.class));
    }

    @Test
    @DisplayName("update应成功更新版本")
    void testUpdate() {
        Variant variant = Variant.builder()
                .id(1L)
                .code("V001")
                .name("更新后的版本")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .source(SourceType.MANUAL)
                .build();

        when(vehVariantMapper.updatePo(any(VehVariantPo.class))).thenReturn(1);

        int result = vehVariantRepositoryImpl.update(variant);

        assertEquals(1, result);
        verify(vehVariantMapper).updatePo(any(VehVariantPo.class));
    }

    @Test
    @DisplayName("updateById应成功更新版本")
    void testUpdateById() {
        Variant variant = Variant.builder()
                .id(1L)
                .code("V001")
                .name("更新后的版本")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .source(SourceType.MANUAL)
                .build();

        when(vehVariantMapper.updatePo(any(VehVariantPo.class))).thenReturn(1);

        int result = vehVariantRepositoryImpl.updateById(variant);

        assertEquals(1, result);
        verify(vehVariantMapper).updatePo(any(VehVariantPo.class));
    }
}
