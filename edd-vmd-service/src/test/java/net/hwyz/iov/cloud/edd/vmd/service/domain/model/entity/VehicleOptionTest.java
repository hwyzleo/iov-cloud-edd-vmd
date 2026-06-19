package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class VehicleOptionTest {

    @Test
    void shouldCreateVehicleOptionWithBuilder() {
        LocalDateTime now = LocalDateTime.now();
        VehicleOption option = VehicleOption.builder()
            .vin("WVWZZZ3CZWE123456")
            .optionFamilyCode("COLOR")
            .optionCode("RED")
            .source("PRODUCE")
            .batchNum("BATCH001")
            .snapshotTime(now)
            .build();

        assertEquals("WVWZZZ3CZWE123456", option.getVin());
        assertEquals("COLOR", option.getOptionFamilyCode());
        assertEquals("RED", option.getOptionCode());
        assertEquals("PRODUCE", option.getSource());
        assertEquals("BATCH001", option.getBatchNum());
        assertEquals(now, option.getSnapshotTime());
    }

    @Test
    void shouldImplementDomainObject() {
        LocalDateTime now = LocalDateTime.now();
        VehicleOption option = VehicleOption.builder()
            .id(1L)
            .vin("WVWZZZ3CZWE123456")
            .optionFamilyCode("COLOR")
            .optionCode("RED")
            .source("PRODUCE")
            .batchNum("BATCH001")
            .snapshotTime(now)
            .build();

        VehicleOption copy = option.copy();
        assertNotSame(option, copy);
        assertEquals(option.getId(), copy.getId());
        assertEquals(option.getVin(), copy.getVin());
        assertEquals(option.getOptionFamilyCode(), copy.getOptionFamilyCode());
        assertEquals(option.getOptionCode(), copy.getOptionCode());
        assertEquals(option.getSource(), copy.getSource());
        assertEquals(option.getBatchNum(), copy.getBatchNum());
        assertEquals(option.getSnapshotTime(), copy.getSnapshotTime());
    }
}
