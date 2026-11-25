package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleInfoChangeLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 建档后置激活生命周期补偿钩子单元测试（VMD-DSN-CR-079 / D079-05）
 * <p>
 * 覆盖：新建车辆提交后触发补偿钩子；已存在车辆的补丁写不触发；补偿在事务提交后才执行
 * （避免与建档主事务耦合，失败不影响建档）。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleInfoMutationAppService 建档补偿钩子测试")
class VehicleInfoMutationAppServiceCr079HookTest {

    private static final String VIN = "CR079HOOK00000001";

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;
    @Mock
    private VehicleInfoChangeLogRepository vehicleInfoChangeLogRepository;
    @Mock
    private VehicleActivationLifecycleCompensationAppService activationLifecycleCompensationAppService;

    private VehicleInfoMutationAppService appService;

    @BeforeEach
    void setUp() {
        appService = new VehicleInfoMutationAppService(vehBasicInfoRepository, vehicleInfoChangeLogRepository,
                activationLifecycleCompensationAppService);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private VehicleInfoPatchCmd ctx() {
        return VehicleInfoPatchCmd.builder()
                .source("PRODUCE")
                .operationSource("VEHICLE_IMPORT")
                .operatorId("SYSTEM")
                .changeType("INBOUND")
                .operationId("NONE")
                .phase("BASIC")
                .seq(1)
                .build();
    }

    @Test
    @DisplayName("新建车辆 → 事务提交后触发补偿（提交前不执行）")
    void newVehicle_registersAfterCommitCompensation() {
        VehicleBasicInfo vehicle = VehicleBasicInfo.builder().vin(VIN).build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        when(vehBasicInfoRepository.insert(vehicle)).thenReturn(1);

        appService.create(vehicle, ctx());

        verify(activationLifecycleCompensationAppService, never()).compensateQuietly(any());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(activationLifecycleCompensationAppService).compensateQuietly(VIN);
        verify(vehicleInfoChangeLogRepository).insert(any());
    }

    @Test
    @DisplayName("已存在车辆（补丁写）→ 不注册补偿钩子")
    void existingVehicle_doesNotRegisterCompensation() {
        VehicleBasicInfo existing = VehicleBasicInfo.builder().id(9L).vin(VIN).build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(existing);
        when(vehBasicInfoRepository.selectById(9L)).thenReturn(existing);

        appService.create(VehicleBasicInfo.builder().vin(VIN).build(), ctx());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(activationLifecycleCompensationAppService, never()).compensateQuietly(any());
    }
}
