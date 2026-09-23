package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehLifecycleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VehicleLifecycleAppService 单元测试
 * <p>
 * VMD-DSN-CR-050: recordProduceNode 幂等（存在性判断 + 并发唯一键回查兜底）
 *
 * @author hwyz_leo
 * @since 2026-09-23
 */
@ExtendWith(MockitoExtension.class)
class VehicleLifecycleAppServiceTest {

    @Mock
    private VehLifecycleRepository vehLifecycleRepository;

    @Mock
    private VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    private VehicleLifecycleAppService appService;

    @BeforeEach
    void setUp() {
        appService = new VehicleLifecycleAppService(vehLifecycleRepository, vehicleLifecycleNodeRepository);
    }

    @Test
    @DisplayName("PRODUCE节点已存在时应直接返回且不写入")
    void testRecordProduceNodeAlreadyExists() {
        // Given
        String vin = "VIN_EXIST_001";
        when(vehicleLifecycleNodeRepository.existsByVinAndNode(vin, VehicleLifecycleNodeEnum.PRODUCE))
                .thenReturn(true);

        // When
        appService.recordProduceNode(vin);

        // Then
        verify(vehicleLifecycleNodeRepository, never()).save(any(VehicleLifecycleNode.class));
    }

    @Test
    @DisplayName("PRODUCE节点不存在时应正常写入")
    void testRecordProduceNodeNotExists() {
        // Given
        String vin = "VIN_NEW_001";
        when(vehicleLifecycleNodeRepository.existsByVinAndNode(vin, VehicleLifecycleNodeEnum.PRODUCE))
                .thenReturn(false);

        // When
        appService.recordProduceNode(vin);

        // Then
        verify(vehicleLifecycleNodeRepository).save(any(VehicleLifecycleNode.class));
    }

    @Test
    @DisplayName("并发唯一键冲突且回查节点已存在时应视为幂等成功")
    void testRecordProduceNodeConcurrentDuplicate() {
        // Given
        String vin = "VIN_CONCURRENT_001";
        when(vehicleLifecycleNodeRepository.existsByVinAndNode(vin, VehicleLifecycleNodeEnum.PRODUCE))
                .thenReturn(false)   // 前置存在性判断：不存在
                .thenReturn(true);   // 冲突后回查：已存在
        doThrow(new DuplicateKeyException("uk_vin_node conflict"))
                .when(vehicleLifecycleNodeRepository).save(any(VehicleLifecycleNode.class));

        // When / Then
        assertDoesNotThrow(() -> appService.recordProduceNode(vin));

        verify(vehicleLifecycleNodeRepository, times(2))
                .existsByVinAndNode(vin, VehicleLifecycleNodeEnum.PRODUCE);
        verify(vehicleLifecycleNodeRepository).save(any(VehicleLifecycleNode.class));
    }

    @Test
    @DisplayName("并发唯一键冲突但回查节点不存在时应继续抛出异常")
    void testRecordProduceNodeConcurrentDuplicateButNotExist() {
        // Given
        String vin = "VIN_CONCURRENT_002";
        when(vehicleLifecycleNodeRepository.existsByVinAndNode(vin, VehicleLifecycleNodeEnum.PRODUCE))
                .thenReturn(false)   // 前置存在性判断：不存在
                .thenReturn(false);  // 冲突后回查：仍不存在（异常不得被吞掉）
        doThrow(new DuplicateKeyException("uk_vin_node conflict"))
                .when(vehicleLifecycleNodeRepository).save(any(VehicleLifecycleNode.class));

        // When / Then
        assertThrows(DuplicateKeyException.class, () -> appService.recordProduceNode(vin));
    }
}
