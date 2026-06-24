package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆安全常量转换器测试
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
class VehSecurityConstantConverterTest {

    private final VehSecurityConstantConverter converter = VehSecurityConstantConverter.INSTANCE;

    @Test
    void toDomain_shouldConvertPoToDomain() {
        VehSecurityConstantPo po = VehSecurityConstantPo.builder()
                .id(1L)
                .vin("HWYZTEST000000001")
                .batchNum("BATCH001")
                .presetState("PENDING")
                .kmsKeyRef("kms-key-ref-1")
                .failReason(null)
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .constantType("TYPE1")
                .build();

        VehSecurityConstant domain = converter.toDomain(po);

        assertNotNull(domain);
        assertEquals(po.getId(), domain.getId());
        assertEquals(po.getVin(), domain.getVin());
        assertEquals(po.getBatchNum(), domain.getBatchNum());
        assertEquals(SecurityConstantState.PENDING, domain.getPresetState());
        assertEquals(po.getKmsKeyRef(), domain.getKmsKeyRef());
        assertEquals(po.getFailReason(), domain.getFailReason());
        assertEquals(po.getGenTime(), domain.getGenTime());
        assertEquals(po.getLastAttemptTime(), domain.getLastAttemptTime());
        assertEquals(po.getConstantType(), domain.getConstantType());
    }

    @Test
    void toDomain_shouldHandleNullPresetState() {
        VehSecurityConstantPo po = VehSecurityConstantPo.builder()
                .id(1L)
                .vin("HWYZTEST000000001")
                .presetState(null)
                .build();

        VehSecurityConstant domain = converter.toDomain(po);

        assertNotNull(domain);
        assertNull(domain.getPresetState());
    }

    @Test
    void fromDomain_shouldConvertDomainToPo() {
        VehSecurityConstant domain = VehSecurityConstant.builder()
                .id(1L)
                .vin("HWYZTEST000000001")
                .batchNum("BATCH001")
                .presetState(SecurityConstantState.PRESET)
                .kmsKeyRef("kms-key-ref-1")
                .failReason(null)
                .genTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .constantType("TYPE1")
                .build();

        VehSecurityConstantPo po = converter.fromDomain(domain);

        assertNotNull(po);
        assertEquals(domain.getId(), po.getId());
        assertEquals(domain.getVin(), po.getVin());
        assertEquals(domain.getBatchNum(), po.getBatchNum());
        assertEquals("PRESET", po.getPresetState());
        assertEquals(domain.getKmsKeyRef(), po.getKmsKeyRef());
        assertEquals(domain.getFailReason(), po.getFailReason());
        assertEquals(domain.getGenTime(), po.getGenTime());
        assertEquals(domain.getLastAttemptTime(), po.getLastAttemptTime());
        assertEquals(domain.getConstantType(), po.getConstantType());
    }

    @Test
    void fromDomain_shouldHandleNullPresetState() {
        VehSecurityConstant domain = VehSecurityConstant.builder()
                .id(1L)
                .vin("HWYZTEST000000001")
                .presetState(null)
                .build();

        VehSecurityConstantPo po = converter.fromDomain(domain);

        assertNotNull(po);
        assertNull(po.getPresetState());
    }

    @Test
    void toDomainList_shouldConvertPoListToDomainList() {
        VehSecurityConstantPo po1 = VehSecurityConstantPo.builder()
                .id(1L)
                .vin("VIN001")
                .presetState("PENDING")
                .build();
        VehSecurityConstantPo po2 = VehSecurityConstantPo.builder()
                .id(2L)
                .vin("VIN002")
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
        VehSecurityConstant domain1 = VehSecurityConstant.builder()
                .id(1L)
                .vin("VIN001")
                .presetState(SecurityConstantState.PENDING)
                .build();
        VehSecurityConstant domain2 = VehSecurityConstant.builder()
                .id(2L)
                .vin("VIN002")
                .presetState(SecurityConstantState.PRESET)
                .build();

        var poList = converter.fromDomainList(java.util.List.of(domain1, domain2));

        assertNotNull(poList);
        assertEquals(2, poList.size());
        assertEquals("PENDING", poList.get(0).getPresetState());
        assertEquals("PRESET", poList.get(1).getPresetState());
    }
}
