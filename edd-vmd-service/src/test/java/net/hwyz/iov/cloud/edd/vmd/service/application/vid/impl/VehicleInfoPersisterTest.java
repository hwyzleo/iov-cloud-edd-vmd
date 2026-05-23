package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleInfoPersisterTest {

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private VehicleInfoPersister persister;

    @Test
    void persist_newVehicle_insertsAndReturnsTrue() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().vin("VIN001").build();
        List<VehicleDetail> details = List.of(
                VehicleDetail.builder().vin("VIN001").type("MATNR").val("MAT001").build()
        );

        boolean result = persister.persist(basicInfo, details);

        assertTrue(result);
        verify(vehBasicInfoRepository).insert(basicInfo);
        verify(vehBasicInfoRepository, never()).update(any());
        verify(vehBasicInfoRepository).batchInsertDetail(anyList());
    }

    @Test
    void persist_existingVehicle_updatesAndReturnsFalse() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().id(1L).vin("VIN001").build();
        List<VehicleDetail> details = List.of(
                VehicleDetail.builder().id(10L).vin("VIN001").type("MATNR").val("MAT001").build()
        );

        boolean result = persister.persist(basicInfo, details);

        assertFalse(result);
        verify(vehBasicInfoRepository, never()).insert(any());
        verify(vehBasicInfoRepository).update(basicInfo);
        verify(vehBasicInfoRepository, never()).batchInsertDetail(anyList());
    }

    @Test
    void persist_mixedDetails_insertsOnlyNew() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().id(1L).vin("VIN001").build();
        List<VehicleDetail> details = new ArrayList<>();
        details.add(VehicleDetail.builder().id(10L).vin("VIN001").type("MATNR").val("MAT001").build());
        details.add(VehicleDetail.builder().vin("VIN001").type("PROJECT").val("PRJ001").build());

        persister.persist(basicInfo, details);

        verify(vehBasicInfoRepository).batchInsertDetail(argThat(list -> list.size() == 1
                && "PROJECT".equals(list.get(0).getType())));
    }

    @Test
    void persist_noNewDetails_skipsBatchInsert() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().id(1L).vin("VIN001").build();
        List<VehicleDetail> details = List.of(
                VehicleDetail.builder().id(10L).vin("VIN001").type("MATNR").val("MAT001").build()
        );

        persister.persist(basicInfo, details);

        verify(vehBasicInfoRepository, never()).batchInsertDetail(anyList());
    }
}
