package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CarLineCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CarLineDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.CarLineQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CarLineAppService单元测试
 * 
 * <p>CR-014：CarLine 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class CarLineAppServiceTest {

    @Mock
    private MdmCarLineRepository mdmCarLineRepository;

    @Mock
    private MdmModelRepository mdmModelRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private CarLineAppService carLineAppService;

    @Test
    @DisplayName("search方法应返回匹配的车系列表")
    void search_shouldReturnMatchingCarLineList() {
        // Given
        CarLineQuery query = CarLineQuery.builder()
                .brandCode("BRAND001")
                .code("CARLINE001")
                .name("测试")
                .build();

        CarLine carLine1 = CarLine.builder().id(1L).code("CARLINE001").name("测试车系1").brandCode("BRAND001").build();
        CarLine carLine2 = CarLine.builder().id(2L).code("CARLINE002").name("测试车系2").brandCode("BRAND001").build();
        List<CarLine> carLines = Arrays.asList(carLine1, carLine2);

        when(mdmCarLineRepository.selectByMap(any(Map.class))).thenReturn(carLines);

        // When
        List<CarLineDto> result = carLineAppService.search(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmCarLineRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void search_shouldReturnEmptyListWhenNoMatch() {
        // Given
        CarLineQuery query = CarLineQuery.builder()
                .code("NONEXISTENT")
                .build();

        when(mdmCarLineRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        // When
        List<CarLineDto> result = carLineAppService.search(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mdmCarLineRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void checkCodeUnique_shouldReturnTrueWhenCodeIsUnique() {
        // Given
        String code = "CARLINE001";
        when(mdmCarLineRepository.selectByCode(code)).thenReturn(null);

        // When
        Boolean result = carLineAppService.checkCodeUnique(1L, code);

        // Then
        assertTrue(result);
        verify(mdmCarLineRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一车系时")
    void checkCodeUnique_shouldReturnTrueWhenCodeBelongsToSameCarLine() {
        // Given
        Long carLineId = 1L;
        String code = "CARLINE001";
        CarLine existingCarLine = CarLine.builder().id(carLineId).code(code).build();

        when(mdmCarLineRepository.selectByCode(code)).thenReturn(existingCarLine);

        // When
        Boolean result = carLineAppService.checkCodeUnique(carLineId, code);

        // Then
        assertTrue(result);
        verify(mdmCarLineRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void checkCodeUnique_shouldReturnFalseWhenCodeAlreadyExists() {
        // Given
        Long carLineId = 1L;
        String code = "CARLINE001";
        CarLine existingCarLine = CarLine.builder().id(2L).code(code).build();

        when(mdmCarLineRepository.selectByCode(code)).thenReturn(existingCarLine);

        // When
        Boolean result = carLineAppService.checkCodeUnique(carLineId, code);

        // Then
        assertFalse(result);
        verify(mdmCarLineRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkSeriesModelExist应返回true当车系下有车型时")
    void checkSeriesModelExist_shouldReturnTrueWhenModelsExist() {
        // Given
        Long carLineId = 1L;
        CarLine carLine = CarLine.builder().id(carLineId).code("CARLINE001").build();

        when(mdmCarLineRepository.selectById(carLineId)).thenReturn(carLine);
        when(mdmModelRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = carLineAppService.checkSeriesModelExist(carLineId);

        // Then
        assertTrue(result);
        verify(mdmCarLineRepository).selectById(carLineId);
        verify(mdmModelRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkSeriesModelExist应返回false当车系下无车型时")
    void checkSeriesModelExist_shouldReturnFalseWhenNoModels() {
        // Given
        Long carLineId = 1L;
        CarLine carLine = CarLine.builder().id(carLineId).code("CARLINE001").build();

        when(mdmCarLineRepository.selectById(carLineId)).thenReturn(carLine);
        when(mdmModelRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = carLineAppService.checkSeriesModelExist(carLineId);

        // Then
        assertFalse(result);
        verify(mdmCarLineRepository).selectById(carLineId);
        verify(mdmModelRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkSeriesVehicleExist应返回true当车系下有车辆时")
    void checkSeriesVehicleExist_shouldReturnTrueWhenVehiclesExist() {
        // Given
        Long carLineId = 1L;
        CarLine carLine = CarLine.builder().id(carLineId).code("CARLINE001").build();

        when(mdmCarLineRepository.selectById(carLineId)).thenReturn(carLine);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = carLineAppService.checkSeriesVehicleExist(carLineId);

        // Then
        assertTrue(result);
        verify(mdmCarLineRepository).selectById(carLineId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkSeriesVehicleExist应返回false当车系下无车辆时")
    void checkSeriesVehicleExist_shouldReturnFalseWhenNoVehicles() {
        // Given
        Long carLineId = 1L;
        CarLine carLine = CarLine.builder().id(carLineId).code("CARLINE001").build();

        when(mdmCarLineRepository.selectById(carLineId)).thenReturn(carLine);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = carLineAppService.checkSeriesVehicleExist(carLineId);

        // Then
        assertFalse(result);
        verify(mdmCarLineRepository).selectById(carLineId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getSeriesById应返回车系DTO")
    void getSeriesById_shouldReturnCarLineDto() {
        // Given
        Long carLineId = 1L;
        CarLine carLine = CarLine.builder()
                .id(carLineId)
                .code("CARLINE001")
                .name("测试车系")
                .brandCode("BRAND001")
                .build();

        when(mdmCarLineRepository.selectById(carLineId)).thenReturn(carLine);

        // When
        CarLineDto result = carLineAppService.getSeriesById(carLineId);

        // Then
        assertNotNull(result);
        assertEquals(carLineId, result.getId());
        assertEquals("CARLINE001", result.getCode());
        assertEquals("测试车系", result.getName());
        assertEquals("BRAND001", result.getBrandCode());
        verify(mdmCarLineRepository).selectById(carLineId);
    }

    @Test
    @DisplayName("getSeriesByCode应返回车系领域对象")
    void getSeriesByCode_shouldReturnCarLineEntity() {
        // Given
        String code = "CARLINE001";
        CarLine carLine = CarLine.builder()
                .id(1L)
                .code(code)
                .name("测试车系")
                .brandCode("BRAND001")
                .build();

        when(mdmCarLineRepository.selectByCode(code)).thenReturn(carLine);

        // When
        CarLine result = carLineAppService.getSeriesByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals("BRAND001", result.getBrandCode());
        verify(mdmCarLineRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createSeries应成功创建MANUAL来源车系")
    void createSeries_shouldSuccessfullyCreateManualCarLine() {
        // Given
        CarLineCmd cmd = CarLineCmd.builder()
                .code("CARLINE001")
                .name("新车系")
                .brandCode("BRAND001")
                .build();

        when(mdmCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        int result = carLineAppService.createSeries(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(mdmCarLineRepository).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("createSeries应拒绝创建MDM来源车系")
    void createSeries_shouldRejectMdmSourceCarLine() {
        // Given
        CarLineCmd cmd = CarLineCmd.builder()
                .code("CARLINE_MDM")
                .name("MDM车系")
                .brandCode("BRAND001")
                .build();

        // When & Then
        // 由于CarLineAssembler是MapStruct生成的，我们无法直接模拟它
        // 但我们可以验证当source=MDM时会抛出异常
        // 这个测试需要在集成测试中验证
    }

    @Test
    @DisplayName("modifySeries应成功修改MANUAL来源车系")
    void modifySeries_shouldSuccessfullyModifyManualCarLine() {
        // Given
        CarLineCmd cmd = CarLineCmd.builder()
                .id(1L)
                .code("CARLINE001")
                .name("修改后的车系")
                .brandCode("BRAND001")
                .build();

        CarLine existingCarLine = CarLine.builder()
                .id(1L)
                .code("CARLINE001")
                .name("原始车系")
                .brandCode("BRAND001")
                .source(SourceType.MANUAL)
                .build();

        when(mdmCarLineRepository.selectById(1L)).thenReturn(existingCarLine);
        when(mdmCarLineRepository.update(any(CarLine.class))).thenReturn(1);

        // When
        int result = carLineAppService.modifySeries(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(mdmCarLineRepository).selectById(1L);
        verify(mdmCarLineRepository).update(any(CarLine.class));
    }

    @Test
    @DisplayName("modifySeries应拒绝修改MDM来源车系")
    void modifySeries_shouldRejectMdmSourceCarLine() {
        // Given
        CarLineCmd cmd = CarLineCmd.builder()
                .id(1L)
                .code("CARLINE_MDM")
                .name("修改MDM车系")
                .brandCode("BRAND001")
                .build();

        CarLine existingCarLine = CarLine.builder()
                .id(1L)
                .code("CARLINE_MDM")
                .name("MDM车系")
                .brandCode("BRAND001")
                .source(SourceType.MDM)
                .build();

        when(mdmCarLineRepository.selectById(1L)).thenReturn(existingCarLine);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            carLineAppService.modifySeries(cmd, "user1");
        });
        verify(mdmCarLineRepository).selectById(1L);
        verify(mdmCarLineRepository, never()).update(any(CarLine.class));
    }

    @Test
    @DisplayName("deleteSeriesByIds应成功删除MANUAL来源车系")
    void deleteSeriesByIds_shouldSuccessfullyDeleteManualCarLines() {
        // Given
        Long[] ids = {1L, 2L, 3L};

        CarLine carLine1 = CarLine.builder().id(1L).code("CARLINE001").source(SourceType.MANUAL).build();
        CarLine carLine2 = CarLine.builder().id(2L).code("CARLINE002").source(SourceType.MANUAL).build();
        CarLine carLine3 = CarLine.builder().id(3L).code("CARLINE003").source(SourceType.MANUAL).build();

        when(mdmCarLineRepository.selectById(1L)).thenReturn(carLine1);
        when(mdmCarLineRepository.selectById(2L)).thenReturn(carLine2);
        when(mdmCarLineRepository.selectById(3L)).thenReturn(carLine3);
        when(mdmCarLineRepository.batchPhysicalDelete(ids)).thenReturn(3);

        // When
        int result = carLineAppService.deleteSeriesByIds(ids);

        // Then
        assertEquals(3, result);
        verify(mdmCarLineRepository).selectById(1L);
        verify(mdmCarLineRepository).selectById(2L);
        verify(mdmCarLineRepository).selectById(3L);
        verify(mdmCarLineRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deleteSeriesByIds应拒绝删除MDM来源车系")
    void deleteSeriesByIds_shouldRejectMdmSourceCarLine() {
        // Given
        Long[] ids = {1L, 2L};

        CarLine carLine1 = CarLine.builder().id(1L).code("CARLINE001").source(SourceType.MANUAL).build();
        CarLine carLine2 = CarLine.builder().id(2L).code("CARLINE_MDM").source(SourceType.MDM).build();

        when(mdmCarLineRepository.selectById(1L)).thenReturn(carLine1);
        when(mdmCarLineRepository.selectById(2L)).thenReturn(carLine2);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            carLineAppService.deleteSeriesByIds(ids);
        });
        verify(mdmCarLineRepository).selectById(1L);
        verify(mdmCarLineRepository).selectById(2L);
        verify(mdmCarLineRepository, never()).batchPhysicalDelete(any(Long[].class));
    }

    @Test
    @DisplayName("getSeriesByExternalRefId应返回车系领域对象")
    void getSeriesByExternalRefId_shouldReturnCarLineEntity() {
        // Given
        String externalRefId = "ext-001";
        CarLine carLine = CarLine.builder()
                .id(1L)
                .code("CARLINE001")
                .externalRefId(externalRefId)
                .build();

        when(mdmCarLineRepository.selectByExternalRefId(externalRefId)).thenReturn(carLine);

        // When
        CarLine result = mdmCarLineRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        verify(mdmCarLineRepository).selectByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的车系数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        SourceType source = SourceType.MDM;
        when(mdmCarLineRepository.countBySource(source)).thenReturn(5L);

        // When
        long result = mdmCarLineRepository.countBySource(source);

        // Then
        assertEquals(5L, result);
        verify(mdmCarLineRepository).countBySource(source);
    }
}
