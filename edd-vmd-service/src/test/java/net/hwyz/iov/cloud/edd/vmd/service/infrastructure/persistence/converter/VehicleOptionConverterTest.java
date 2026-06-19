package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单车选项值快照转换器测试
 *
 * @author VMD-DSN-CR-030 / US-043
 */
class VehicleOptionConverterTest {

    private final VehicleOptionConverter converter = VehicleOptionConverter.INSTANCE;

    @Test
    void toDomain_shouldConvertPoToDomain() {
        LocalDateTime now = LocalDateTime.now();
        VehicleOptionPo po = VehicleOptionPo.builder()
                .id(1L)
                .vin("WVWZZZ3CZWE123456")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .source("PRODUCE")
                .batchNum("BATCH001")
                .snapshotTime(now)
                .build();

        VehicleOption domain = converter.toDomain(po);

        assertNotNull(domain);
        assertEquals(po.getId(), domain.getId());
        assertEquals(po.getVin(), domain.getVin());
        assertEquals(po.getOptionFamilyCode(), domain.getOptionFamilyCode());
        assertEquals(po.getOptionCode(), domain.getOptionCode());
        assertEquals(po.getSource(), domain.getSource());
        assertEquals(po.getBatchNum(), domain.getBatchNum());
        assertEquals(po.getSnapshotTime(), domain.getSnapshotTime());
    }

    @Test
    void fromDomain_shouldConvertDomainToPo() {
        LocalDateTime now = LocalDateTime.now();
        VehicleOption domain = VehicleOption.builder()
                .id(1L)
                .vin("WVWZZZ3CZWE123456")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .source("PRODUCE")
                .batchNum("BATCH001")
                .snapshotTime(now)
                .build();

        VehicleOptionPo po = converter.fromDomain(domain);

        assertNotNull(po);
        assertEquals(domain.getId(), po.getId());
        assertEquals(domain.getVin(), po.getVin());
        assertEquals(domain.getOptionFamilyCode(), po.getOptionFamilyCode());
        assertEquals(domain.getOptionCode(), po.getOptionCode());
        assertEquals(domain.getSource(), po.getSource());
        assertEquals(domain.getBatchNum(), po.getBatchNum());
        assertEquals(domain.getSnapshotTime(), po.getSnapshotTime());
    }

    @Test
    void toDomainList_shouldConvertPoListToDomainList() {
        VehicleOptionPo po1 = VehicleOptionPo.builder()
                .id(1L)
                .vin("VIN001")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .build();
        VehicleOptionPo po2 = VehicleOptionPo.builder()
                .id(2L)
                .vin("VIN001")
                .optionFamilyCode("INTERIOR")
                .optionCode("BLACK")
                .build();

        List<VehicleOption> domainList = converter.toDomainList(List.of(po1, po2));

        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals("COLOR", domainList.get(0).getOptionFamilyCode());
        assertEquals("INTERIOR", domainList.get(1).getOptionFamilyCode());
    }

    @Test
    void fromDomainList_shouldConvertDomainListToPoList() {
        VehicleOption domain1 = VehicleOption.builder()
                .id(1L)
                .vin("VIN001")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .build();
        VehicleOption domain2 = VehicleOption.builder()
                .id(2L)
                .vin("VIN001")
                .optionFamilyCode("INTERIOR")
                .optionCode("BLACK")
                .build();

        List<VehicleOptionPo> poList = converter.fromDomainList(List.of(domain1, domain2));

        assertNotNull(poList);
        assertEquals(2, poList.size());
        assertEquals("COLOR", poList.get(0).getOptionFamilyCode());
        assertEquals("INTERIOR", poList.get(1).getOptionFamilyCode());
    }

    @Test
    void toDomain_shouldHandleNullFields() {
        VehicleOptionPo po = VehicleOptionPo.builder()
                .id(1L)
                .vin("VIN001")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .source(null)
                .batchNum(null)
                .snapshotTime(null)
                .build();

        VehicleOption domain = converter.toDomain(po);

        assertNotNull(domain);
        assertNull(domain.getSource());
        assertNull(domain.getBatchNum());
        assertNull(domain.getSnapshotTime());
    }
}
