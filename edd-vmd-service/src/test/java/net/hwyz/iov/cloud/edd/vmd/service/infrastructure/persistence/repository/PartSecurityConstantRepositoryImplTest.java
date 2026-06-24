package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartSecurityConstantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSecurityConstantPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PartSecurityConstantRepository 单元测试
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@ExtendWith(MockitoExtension.class)
class PartSecurityConstantRepositoryImplTest {

    @Mock
    private PartSecurityConstantMapper mapper;

    @InjectMocks
    private PartSecurityConstantRepositoryImpl repository;

    @Test
    @DisplayName("应成功插入零件安全常量")
    void shouldInsertEntity() {
        PartSecurityConstant entity = PartSecurityConstant.builder()
                .partCode("TBOX_001")
                .sn("SN_123456")
                .chipUid("CHIP_UID_789")
                .constantType("ROOT")
                .kmsProvider("TestKMS")
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .algorithm("AES")
                .presetState(SecurityConstantState.PENDING)
                .batchNum("BATCH_001")
                .build();

        when(mapper.insertPo(any(PartSecurityConstantPo.class))).thenReturn(1);

        int result = repository.insert(entity);

        assertEquals(1, result);
        verify(mapper).insertPo(any(PartSecurityConstantPo.class));
    }

    @Test
    @DisplayName("应成功根据零件编码和序列号查询")
    void shouldSelectByPartCodeAndSn() {
        String partCode = "TBOX_001";
        String sn = "SN_123456";
        PartSecurityConstantPo po = PartSecurityConstantPo.builder()
                .id(1L)
                .partCode(partCode)
                .sn(sn)
                .chipUid("CHIP_UID_789")
                .constantType("ROOT")
                .kmsProvider("TestKMS")
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .algorithm("AES")
                .presetState("PRESET")
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .failReason(null)
                .batchNum("BATCH_001")
                .build();

        when(mapper.selectPoByPartCodeAndSn(partCode, sn)).thenReturn(po);

        PartSecurityConstant result = repository.selectByPartCodeAndSn(partCode, sn);

        assertNotNull(result);
        assertEquals(partCode, result.getPartCode());
        assertEquals(sn, result.getSn());
        assertEquals(SecurityConstantState.PRESET, result.getPresetState());
        verify(mapper).selectPoByPartCodeAndSn(partCode, sn);
    }

    @Test
    @DisplayName("当零件编码和序列号不存在时应返回null")
    void shouldReturnNullWhenNotFound() {
        String partCode = "NONEXISTENT";
        String sn = "NONEXISTENT";

        when(mapper.selectPoByPartCodeAndSn(partCode, sn)).thenReturn(null);

        PartSecurityConstant result = repository.selectByPartCodeAndSn(partCode, sn);

        assertNull(result);
        verify(mapper).selectPoByPartCodeAndSn(partCode, sn);
    }

    @Test
    @DisplayName("应成功更新零件安全常量")
    void shouldUpdateEntity() {
        PartSecurityConstant entity = PartSecurityConstant.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_123456")
                .chipUid("CHIP_UID_789")
                .constantType("ROOT")
                .kmsProvider("TestKMS")
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .algorithm("AES")
                .presetState(SecurityConstantState.PRESET)
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .failReason(null)
                .batchNum("BATCH_001")
                .build();

        when(mapper.updatePo(any(PartSecurityConstantPo.class))).thenReturn(1);

        int result = repository.update(entity);

        assertEquals(1, result);
        verify(mapper).updatePo(any(PartSecurityConstantPo.class));
    }

    @Test
    @DisplayName("应成功根据零件编码和序列号删除")
    void shouldDeleteByPartCodeAndSn() {
        String partCode = "TBOX_001";
        String sn = "SN_123456";

        when(mapper.deletePoByPartCodeAndSn(partCode, sn)).thenReturn(1);

        int result = repository.deleteByPartCodeAndSn(partCode, sn);

        assertEquals(1, result);
        verify(mapper).deletePoByPartCodeAndSn(partCode, sn);
    }
}
