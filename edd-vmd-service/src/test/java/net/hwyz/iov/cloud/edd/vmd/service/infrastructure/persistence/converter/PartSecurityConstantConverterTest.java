package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSecurityConstantPo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零件安全常量转换器测试
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
class PartSecurityConstantConverterTest {

    private final PartSecurityConstantConverter converter = PartSecurityConstantConverter.INSTANCE;

    @Test
    void toDomain_shouldConvertPoToDomain() {
        PartSecurityConstantPo po = PartSecurityConstantPo.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_123456")
                .chipUid("CHIP_UID_789")
                .constantType("ROOT")
                .kmsProvider("TestKMS")
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .algorithm("AES")
                .kcv("01020304")
                .presetState("PRESET")
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .failReason(null)
                .batchNum("BATCH_001")
                .build();

        PartSecurityConstant domain = converter.toDomain(po);

        assertNotNull(domain);
        assertEquals(po.getId(), domain.getId());
        assertEquals(po.getPartCode(), domain.getPartCode());
        assertEquals(po.getSn(), domain.getSn());
        assertEquals(po.getChipUid(), domain.getChipUid());
        assertEquals(po.getConstantType(), domain.getConstantType());
        assertEquals(po.getKmsProvider(), domain.getKmsProvider());
        assertEquals(po.getKmsKeyRef(), domain.getKmsKeyRef());
        assertEquals(po.getKeySpec(), domain.getKeySpec());
        assertEquals(po.getAlgorithm(), domain.getAlgorithm());
        assertEquals(po.getKcv(), domain.getKcv());
        assertEquals(SecurityConstantState.PRESET, domain.getPresetState());
        assertEquals(po.getGenTime(), domain.getGenTime());
        assertEquals(po.getLastAttemptTime(), domain.getLastAttemptTime());
        assertEquals(po.getFailReason(), domain.getFailReason());
        assertEquals(po.getBatchNum(), domain.getBatchNum());
    }

    @Test
    void toDomain_shouldHandleNullPresetState() {
        PartSecurityConstantPo po = PartSecurityConstantPo.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_123456")
                .presetState(null)
                .build();

        PartSecurityConstant domain = converter.toDomain(po);

        assertNotNull(domain);
        assertNull(domain.getPresetState());
    }

    @Test
    void fromDomain_shouldConvertDomainToPo() {
        PartSecurityConstant domain = PartSecurityConstant.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_123456")
                .chipUid("CHIP_UID_789")
                .constantType("ROOT")
                .kmsProvider("TestKMS")
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .algorithm("AES")
                .kcv("01020304")
                .presetState(SecurityConstantState.PRESET)
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .failReason(null)
                .batchNum("BATCH_001")
                .build();

        PartSecurityConstantPo po = converter.fromDomain(domain);

        assertNotNull(po);
        assertEquals(domain.getId(), po.getId());
        assertEquals(domain.getPartCode(), po.getPartCode());
        assertEquals(domain.getSn(), po.getSn());
        assertEquals(domain.getChipUid(), po.getChipUid());
        assertEquals(domain.getConstantType(), po.getConstantType());
        assertEquals(domain.getKmsProvider(), po.getKmsProvider());
        assertEquals(domain.getKmsKeyRef(), po.getKmsKeyRef());
        assertEquals(domain.getKeySpec(), po.getKeySpec());
        assertEquals(domain.getAlgorithm(), po.getAlgorithm());
        assertEquals(domain.getKcv(), po.getKcv());
        assertEquals("PRESET", po.getPresetState());
        assertEquals(domain.getGenTime(), po.getGenTime());
        assertEquals(domain.getLastAttemptTime(), po.getLastAttemptTime());
        assertEquals(domain.getFailReason(), po.getFailReason());
        assertEquals(domain.getBatchNum(), po.getBatchNum());
    }

    @Test
    void fromDomain_shouldHandleNullPresetState() {
        PartSecurityConstant domain = PartSecurityConstant.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_123456")
                .presetState(null)
                .build();

        PartSecurityConstantPo po = converter.fromDomain(domain);

        assertNotNull(po);
        assertNull(po.getPresetState());
    }

    @Test
    void toDomainList_shouldConvertPoListToDomainList() {
        PartSecurityConstantPo po1 = PartSecurityConstantPo.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_001")
                .presetState("PENDING")
                .build();
        PartSecurityConstantPo po2 = PartSecurityConstantPo.builder()
                .id(2L)
                .partCode("TBOX_002")
                .sn("SN_002")
                .presetState("FAILED")
                .build();

        var domainList = converter.toDomainList(java.util.List.of(po1, po2));

        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(SecurityConstantState.PENDING, domainList.get(0).getPresetState());
        assertEquals(SecurityConstantState.FAILED, domainList.get(1).getPresetState());
    }

    @Test
    void fromDomainList_shouldConvertDomainListToPoList() {
        PartSecurityConstant domain1 = PartSecurityConstant.builder()
                .id(1L)
                .partCode("TBOX_001")
                .sn("SN_001")
                .presetState(SecurityConstantState.PENDING)
                .build();
        PartSecurityConstant domain2 = PartSecurityConstant.builder()
                .id(2L)
                .partCode("TBOX_002")
                .sn("SN_002")
                .presetState(SecurityConstantState.PRESET)
                .build();

        var poList = converter.fromDomainList(java.util.List.of(domain1, domain2));

        assertNotNull(poList);
        assertEquals(2, poList.size());
        assertEquals("PENDING", poList.get(0).getPresetState());
        assertEquals("PRESET", poList.get(1).getPresetState());
    }
}
