package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVariantOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVariantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantPo;
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
class MdmVariantRepositoryImplTest {

    @Mock
    private MdmVariantMapper mdmVariantMapper;

    @Mock
    private MdmVariantOptionCodeMapper mdmVariantOptionCodeMapper;

    @InjectMocks
    private MdmVariantRepositoryImpl mdmVariantRepositoryImpl;

    private MdmVariantPo buildPo(Long id, String code) {
        return MdmVariantPo.builder()
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
        MdmVariantPo po = buildPo(1L, code);

        when(mdmVariantMapper.selectPoByCode(code)).thenReturn(po);

        Variant result = mdmVariantRepositoryImpl.selectByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(1L, result.getId());
        verify(mdmVariantMapper).selectPoByCode(code);
    }

    @Test
    @DisplayName("selectByCode应返回null当代码不存在时")
    void testSelectByCode_notFound() {
        String code = "NONEXISTENT";

        when(mdmVariantMapper.selectPoByCode(code)).thenReturn(null);

        Variant result = mdmVariantRepositoryImpl.selectByCode(code);

        assertNull(result);
        verify(mdmVariantMapper).selectPoByCode(code);
    }

    @Test
    @DisplayName("selectByExternalRefId应返回MDM投影版本")
    void testSelectByExternalRefId() {
        String externalRefId = "ext-001";
        MdmVariantPo po = MdmVariantPo.builder()
                .id(1L)
                .code("V001")
                .name("MDM版本")
                .source("MDM")
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        when(mdmVariantMapper.selectPoByExternalRefId(externalRefId)).thenReturn(po);

        Variant result = mdmVariantRepositoryImpl.selectByExternalRefId(externalRefId);

        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
        verify(mdmVariantMapper).selectPoByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的版本数量")
    void testCountBySource() {
        when(mdmVariantMapper.countPoBySource("MDM")).thenReturn(5L);

        long result = mdmVariantRepositoryImpl.countBySource(SourceType.MDM);

        assertEquals(5L, result);
        verify(mdmVariantMapper).countPoBySource("MDM");
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

        when(mdmVariantMapper.insertPo(any(MdmVariantPo.class))).thenReturn(1);

        int result = mdmVariantRepositoryImpl.insert(variant);

        assertEquals(1, result);
        verify(mdmVariantMapper).insertPo(any(MdmVariantPo.class));
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

        when(mdmVariantMapper.updatePo(any(MdmVariantPo.class))).thenReturn(1);

        int result = mdmVariantRepositoryImpl.update(variant);

        assertEquals(1, result);
        verify(mdmVariantMapper).updatePo(any(MdmVariantPo.class));
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

        when(mdmVariantMapper.updatePo(any(MdmVariantPo.class))).thenReturn(1);

        int result = mdmVariantRepositoryImpl.updateById(variant);

        assertEquals(1, result);
        verify(mdmVariantMapper).updatePo(any(MdmVariantPo.class));
    }
}
