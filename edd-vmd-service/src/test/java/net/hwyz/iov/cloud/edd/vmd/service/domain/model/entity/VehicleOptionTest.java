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
        VehicleOption option = VehicleOption.builder()
            .vin("WVWZZZ3CZWE123456")
            .optionFamilyCode("COLOR")
            .optionCode("RED")
            .build();

        VehicleOption copy = option.copy();
        assertNotSame(option, copy);
        assertEquals(option.getVin(), copy.getVin());
        assertEquals(option.getOptionFamilyCode(), copy.getOptionFamilyCode());
        assertEquals(option.getOptionCode(), copy.getOptionCode());
    }
}
