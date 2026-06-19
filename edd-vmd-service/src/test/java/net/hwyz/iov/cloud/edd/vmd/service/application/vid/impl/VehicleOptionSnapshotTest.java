package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleOptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleOptionSnapshotTest extends BaseTest {

    @Autowired
    private VehicleOptionRepository vehicleOptionRepository;

    @Test
    void shouldPersistAndRetrieveVehicleOptions() {
        // Given
        String vin = "TEST_VIN_001";
        List<VehicleOption> options = List.of(
            VehicleOption.builder()
                .vin(vin)
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .source("PRODUCE")
                .batchNum("BATCH_TEST")
                .build(),
            VehicleOption.builder()
                .vin(vin)
                .optionFamilyCode("INTERIOR")
                .optionCode("BLACK")
                .source("PRODUCE")
                .batchNum("BATCH_TEST")
                .build()
        );

        // When
        vehicleOptionRepository.batchUpsert(options);

        // Then
        List<VehicleOption> retrieved = vehicleOptionRepository.findByVin(vin);
        assertEquals(2, retrieved.size());

        VehicleOption colorOption = vehicleOptionRepository.findByVinAndOptionFamilyCode(vin, "COLOR");
        assertNotNull(colorOption);
        assertEquals("RED", colorOption.getOptionCode());
    }

    @Test
    void shouldUpsertIdempotently() {
        // Given
        String vin = "TEST_VIN_002";
        VehicleOption option1 = VehicleOption.builder()
            .vin(vin)
            .optionFamilyCode("COLOR")
            .optionCode("RED")
            .source("PRODUCE")
            .batchNum("BATCH_001")
            .build();

        VehicleOption option2 = VehicleOption.builder()
            .vin(vin)
            .optionFamilyCode("COLOR")
            .optionCode("BLUE")
            .source("PRODUCE")
            .batchNum("BATCH_002")
            .build();

        // When
        vehicleOptionRepository.batchUpsert(List.of(option1));
        vehicleOptionRepository.batchUpsert(List.of(option2));

        // Then
        VehicleOption result = vehicleOptionRepository.findByVinAndOptionFamilyCode(vin, "COLOR");
        assertNotNull(result);
        assertEquals("BLUE", result.getOptionCode()); // Updated value
        assertEquals("BATCH_002", result.getBatchNum());
    }
}
