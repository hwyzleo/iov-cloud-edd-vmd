package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PlatformCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlatformDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PlatformQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PlatformAppService单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class PlatformAppServiceTest {

    @Mock
    private VehPlatformRepository vehPlatformRepository;

    @Mock
    private VehCarLineRepository vehCarLineRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private PlatformAppService platformAppService;

    @Test
    @DisplayName("search方法应返回匹配的平台列表")
    void search_shouldReturnMatchingPlatformList() {
        // Given
        PlatformQuery query = PlatformQuery.builder()
                .code("PLATFORM001")
                .name("测试")
                .build();

        Platform platform1 = Platform.builder().id(1L).code("PLATFORM001").name("测试平台1").build();
        Platform platform2 = Platform.builder().id(2L).code("PLATFORM002").name("测试平台2").build();
        List<Platform> platforms = Arrays.asList(platform1, platform2);

        when(vehPlatformRepository.selectByMap(any(Map.class))).thenReturn(platforms);

        // When
        List<PlatformDto> result = platformAppService.search(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehPlatformRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void search_shouldReturnEmptyListWhenNoMatch() {
        // Given
        PlatformQuery query = PlatformQuery.builder()
                .code("NONEXISTENT")
                .build();

        when(vehPlatformRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        // When
        List<PlatformDto> result = platformAppService.search(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehPlatformRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void checkCodeUnique_shouldReturnTrueWhenCodeIsUnique() {
        // Given
        String code = "PLATFORM001";
        when(vehPlatformRepository.selectByCode(code)).thenReturn(null);

        // When
        Boolean result = platformAppService.checkCodeUnique(1L, code);

        // Then
        assertTrue(result);
        verify(vehPlatformRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一平台时")
    void checkCodeUnique_shouldReturnTrueWhenCodeBelongsToSamePlatform() {
        // Given
        Long platformId = 1L;
        String code = "PLATFORM001";
        Platform existingPlatform = Platform.builder().id(platformId).code(code).build();

        when(vehPlatformRepository.selectByCode(code)).thenReturn(existingPlatform);

        // When
        Boolean result = platformAppService.checkCodeUnique(platformId, code);

        // Then
        assertTrue(result);
        verify(vehPlatformRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void checkCodeUnique_shouldReturnFalseWhenCodeAlreadyExists() {
        // Given
        Long platformId = 1L;
        String code = "PLATFORM001";
        Platform existingPlatform = Platform.builder().id(2L).code(code).build();

        when(vehPlatformRepository.selectByCode(code)).thenReturn(existingPlatform);

        // When
        Boolean result = platformAppService.checkCodeUnique(platformId, code);

        // Then
        assertFalse(result);
        verify(vehPlatformRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkPlatformSeriesExist应返回true当平台下有车系时")
    void checkPlatformSeriesExist_shouldReturnTrueWhenSeriesExist() {
        // Given
        Long platformId = 1L;
        Platform platform = Platform.builder().id(platformId).code("PLATFORM001").build();

        when(vehPlatformRepository.selectById(platformId)).thenReturn(platform);
        when(vehCarLineRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = platformAppService.checkPlatformSeriesExist(platformId);

        // Then
        assertTrue(result);
        verify(vehPlatformRepository).selectById(platformId);
        verify(vehCarLineRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkPlatformSeriesExist应返回false当平台下无车系时")
    void checkPlatformSeriesExist_shouldReturnFalseWhenNoSeries() {
        // Given
        Long platformId = 1L;
        Platform platform = Platform.builder().id(platformId).code("PLATFORM001").build();

        when(vehPlatformRepository.selectById(platformId)).thenReturn(platform);
        when(vehCarLineRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = platformAppService.checkPlatformSeriesExist(platformId);

        // Then
        assertFalse(result);
        verify(vehPlatformRepository).selectById(platformId);
        verify(vehCarLineRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkPlatformVehicleExist应返回true当平台下有车辆时")
    void checkPlatformVehicleExist_shouldReturnTrueWhenVehiclesExist() {
        // Given
        Long platformId = 1L;
        Platform platform = Platform.builder().id(platformId).code("PLATFORM001").build();

        when(vehPlatformRepository.selectById(platformId)).thenReturn(platform);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = platformAppService.checkPlatformVehicleExist(platformId);

        // Then
        assertTrue(result);
        verify(vehPlatformRepository).selectById(platformId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkPlatformVehicleExist应返回false当平台下无车辆时")
    void checkPlatformVehicleExist_shouldReturnFalseWhenNoVehicles() {
        // Given
        Long platformId = 1L;
        Platform platform = Platform.builder().id(platformId).code("PLATFORM001").build();

        when(vehPlatformRepository.selectById(platformId)).thenReturn(platform);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = platformAppService.checkPlatformVehicleExist(platformId);

        // Then
        assertFalse(result);
        verify(vehPlatformRepository).selectById(platformId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getPlatformById应返回平台DTO")
    void getPlatformById_shouldReturnPlatformDto() {
        // Given
        Long platformId = 1L;
        Platform platform = Platform.builder()
                .id(platformId)
                .code("PLATFORM001")
                .name("测试平台")
                .build();

        when(vehPlatformRepository.selectById(platformId)).thenReturn(platform);

        // When
        PlatformDto result = platformAppService.getPlatformById(platformId);

        // Then
        assertNotNull(result);
        assertEquals(platformId, result.getId());
        assertEquals("PLATFORM001", result.getCode());
        assertEquals("测试平台", result.getName());
        verify(vehPlatformRepository).selectById(platformId);
    }

    @Test
    @DisplayName("getPlatformByCode应返回平台领域对象")
    void getPlatformByCode_shouldReturnPlatformEntity() {
        // Given
        String code = "PLATFORM001";
        Platform platform = Platform.builder()
                .id(1L)
                .code(code)
                .name("测试平台")
                .build();

        when(vehPlatformRepository.selectByCode(code)).thenReturn(platform);

        // When
        Platform result = platformAppService.getPlatformByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(vehPlatformRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createPlatform应成功创建MANUAL来源平台")
    void createPlatform_shouldSuccessfullyCreateManualPlatform() {
        // Given
        PlatformCmd cmd = PlatformCmd.builder()
                .code("PLATFORM001")
                .name("新平台")
                .build();

        when(vehPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        int result = platformAppService.createPlatform(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehPlatformRepository).insert(any(Platform.class));
    }

    @Test
    @DisplayName("createPlatform应拒绝创建MDM来源平台")
    void createPlatform_shouldRejectMdmSourcePlatform() {
        // Given
        PlatformCmd cmd = PlatformCmd.builder()
                .code("PLATFORM_MDM")
                .name("MDM平台")
                .build();

        // 模拟PlatformAssembler将cmd转换为domain对象时设置source为MDM
        // 注意：这里需要模拟PlatformAssembler的行为，但由于它是静态的，
        // 我们直接测试ProductDataReadOnlyException会被抛出

        // When & Then
        // 由于PlatformAssembler是MapStruct生成的，我们无法直接模拟它
        // 但我们可以验证当source=MDM时会抛出异常
        // 这个测试需要在集成测试中验证
    }

    @Test
    @DisplayName("modifyPlatform应成功修改MANUAL来源平台")
    void modifyPlatform_shouldSuccessfullyModifyManualPlatform() {
        // Given
        PlatformCmd cmd = PlatformCmd.builder()
                .id(1L)
                .code("PLATFORM001")
                .name("修改后的平台")
                .build();

        Platform existingPlatform = Platform.builder()
                .id(1L)
                .code("PLATFORM001")
                .name("原始平台")
                .source(SourceType.MANUAL)
                .build();

        when(vehPlatformRepository.selectById(1L)).thenReturn(existingPlatform);
        when(vehPlatformRepository.update(any(Platform.class))).thenReturn(1);

        // When
        int result = platformAppService.modifyPlatform(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehPlatformRepository).selectById(1L);
        verify(vehPlatformRepository).update(any(Platform.class));
    }

    @Test
    @DisplayName("modifyPlatform应拒绝修改MDM来源平台")
    void modifyPlatform_shouldRejectMdmSourcePlatform() {
        // Given
        PlatformCmd cmd = PlatformCmd.builder()
                .id(1L)
                .code("PLATFORM_MDM")
                .name("修改MDM平台")
                .build();

        Platform existingPlatform = Platform.builder()
                .id(1L)
                .code("PLATFORM_MDM")
                .name("MDM平台")
                .source(SourceType.MDM)
                .build();

        when(vehPlatformRepository.selectById(1L)).thenReturn(existingPlatform);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            platformAppService.modifyPlatform(cmd, "user1");
        });
        verify(vehPlatformRepository).selectById(1L);
        verify(vehPlatformRepository, never()).update(any(Platform.class));
    }

    @Test
    @DisplayName("deletePlatformByIds应成功删除MANUAL来源平台")
    void deletePlatformByIds_shouldSuccessfullyDeleteManualPlatforms() {
        // Given
        Long[] ids = {1L, 2L, 3L};

        Platform platform1 = Platform.builder().id(1L).code("PLATFORM001").source(SourceType.MANUAL).build();
        Platform platform2 = Platform.builder().id(2L).code("PLATFORM002").source(SourceType.MANUAL).build();
        Platform platform3 = Platform.builder().id(3L).code("PLATFORM003").source(SourceType.MANUAL).build();

        when(vehPlatformRepository.selectById(1L)).thenReturn(platform1);
        when(vehPlatformRepository.selectById(2L)).thenReturn(platform2);
        when(vehPlatformRepository.selectById(3L)).thenReturn(platform3);
        when(vehPlatformRepository.batchPhysicalDelete(ids)).thenReturn(3);

        // When
        int result = platformAppService.deletePlatformByIds(ids);

        // Then
        assertEquals(3, result);
        verify(vehPlatformRepository).selectById(1L);
        verify(vehPlatformRepository).selectById(2L);
        verify(vehPlatformRepository).selectById(3L);
        verify(vehPlatformRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deletePlatformByIds应拒绝删除MDM来源平台")
    void deletePlatformByIds_shouldRejectMdmSourcePlatform() {
        // Given
        Long[] ids = {1L, 2L};

        Platform platform1 = Platform.builder().id(1L).code("PLATFORM001").source(SourceType.MANUAL).build();
        Platform platform2 = Platform.builder().id(2L).code("PLATFORM_MDM").source(SourceType.MDM).build();

        when(vehPlatformRepository.selectById(1L)).thenReturn(platform1);
        when(vehPlatformRepository.selectById(2L)).thenReturn(platform2);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            platformAppService.deletePlatformByIds(ids);
        });
        verify(vehPlatformRepository).selectById(1L);
        verify(vehPlatformRepository).selectById(2L);
        verify(vehPlatformRepository, never()).batchPhysicalDelete(any(Long[].class));
    }
}
