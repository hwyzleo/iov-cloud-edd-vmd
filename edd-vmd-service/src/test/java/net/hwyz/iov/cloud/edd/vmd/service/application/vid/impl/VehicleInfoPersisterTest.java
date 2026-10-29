package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleInfoMutationAppService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 车辆信息持久化组件单元测试（CR-071：经统一变更写服务）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class VehicleInfoPersisterTest {

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Mock
    private VehicleInfoMutationAppService vehicleInfoMutationAppService;

    @InjectMocks
    private VehicleInfoPersister persister;

    @Test
    void persist_newVehicle_returnsTrueAndWritesViaMutation() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().vin("VIN001").build();
        List<VehicleDetail> details = List.of(
                VehicleDetail.builder().vin("VIN001").type("MATNR").val("MAT001").build()
        );
        when(vehicleInfoMutationAppService.create(any(), any()))
                .thenReturn(new VehicleInfoMutationAppService.MutationResult("APPLIED", 1, Map.of()));

        boolean result = persister.persist(basicInfo, details);

        assertTrue(result);
        verify(vehicleInfoMutationAppService).create(any(VehicleBasicInfo.class), any());
        verify(vehBasicInfoRepository).batchInsertDetail(anyList());
    }

    @Test
    void persist_existingVehicle_returnsFalseAndWritesViaMutation() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().id(1L).vin("VIN001").build();
        List<VehicleDetail> details = List.of(
                VehicleDetail.builder().id(10L).vin("VIN001").type("MATNR").val("MAT001").build()
        );
        when(vehicleInfoMutationAppService.create(any(), any()))
                .thenReturn(new VehicleInfoMutationAppService.MutationResult("NOOP", 1, Map.of()));

        boolean result = persister.persist(basicInfo, details);

        assertFalse(result);
        verify(vehicleInfoMutationAppService).create(any(VehicleBasicInfo.class), any());
        verify(vehBasicInfoRepository, never()).batchInsertDetail(anyList());
    }

    @Test
    void persist_mixedDetails_insertsOnlyNew() {
        VehicleBasicInfo basicInfo = VehicleBasicInfo.builder().id(1L).vin("VIN001").build();
        List<VehicleDetail> details = new ArrayList<>();
        details.add(VehicleDetail.builder().id(10L).vin("VIN001").type("MATNR").val("MAT001").build());
        details.add(VehicleDetail.builder().vin("VIN001").type("PROJECT").val("PRJ001").build());
        when(vehicleInfoMutationAppService.create(any(), any()))
                .thenReturn(new VehicleInfoMutationAppService.MutationResult("NOOP", 1, Map.of()));

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
        when(vehicleInfoMutationAppService.create(any(), any()))
                .thenReturn(new VehicleInfoMutationAppService.MutationResult("NOOP", 1, Map.of()));

        persister.persist(basicInfo, details);

        verify(vehBasicInfoRepository, never()).batchInsertDetail(anyList());
    }
}
