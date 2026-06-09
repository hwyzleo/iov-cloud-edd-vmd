package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ModelQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBaseModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
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
 * ModelAppService单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class ModelAppServiceTest {

    @Mock
    private VehModelRepository vehModelRepository;

    @Mock
    private VehCarLineRepository vehCarLineRepository;

    @Mock
    private VehBaseModelRepository vehBaseModelRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private ModelAppService modelAppService;

    @Test
    @DisplayName("search方法应返回匹配的车型列表")
    void search_shouldReturnMatchingModelList() {
        // Given
        ModelQuery query = ModelQuery.builder()
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .code("MODEL001")
                .name("测试")
                .build();

        Model model1 = Model.builder().id(1L).code("MODEL001").name("测试车型1").build();
        Model model2 = Model.builder().id(2L).code("MODEL002").name("测试车型2").build();
        List<Model> models = Arrays.asList(model1, model2);

        when(vehModelRepository.selectByMap(any(Map.class))).thenReturn(models);

        // When
        List<ModelDto> result = modelAppService.search(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehModelRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void search_shouldReturnEmptyListWhenNoMatch() {
        // Given
        ModelQuery query = ModelQuery.builder()
                .code("NONEXISTENT")
                .build();

        when(vehModelRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        // When
        List<ModelDto> result = modelAppService.search(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehModelRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void checkCodeUnique_shouldReturnTrueWhenCodeIsUnique() {
        // Given
        String code = "MODEL001";
        when(vehModelRepository.selectByCode(code)).thenReturn(null);

        // When
        Boolean result = modelAppService.checkCodeUnique(1L, code);

        // Then
        assertTrue(result);
        verify(vehModelRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一车型时")
    void checkCodeUnique_shouldReturnTrueWhenCodeBelongsToSameModel() {
        // Given
        Long modelId = 1L;
        String code = "MODEL001";
        Model existingModel = Model.builder().id(modelId).code(code).build();

        when(vehModelRepository.selectByCode(code)).thenReturn(existingModel);

        // When
        Boolean result = modelAppService.checkCodeUnique(modelId, code);

        // Then
        assertTrue(result);
        verify(vehModelRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void checkCodeUnique_shouldReturnFalseWhenCodeAlreadyExists() {
        // Given
        Long modelId = 1L;
        String code = "MODEL001";
        Model existingModel = Model.builder().id(2L).code(code).build();

        when(vehModelRepository.selectByCode(code)).thenReturn(existingModel);

        // When
        Boolean result = modelAppService.checkCodeUnique(modelId, code);

        // Then
        assertFalse(result);
        verify(vehModelRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkModelBaseModelExist应返回true当车型下有基础车型时")
    void checkModelBaseModelExist_shouldReturnTrueWhenBaseModelsExist() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder().id(modelId).code("MODEL001").build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehBaseModelRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = modelAppService.checkModelBaseModelExist(modelId);

        // Then
        assertTrue(result);
        verify(vehModelRepository).selectById(modelId);
        verify(vehBaseModelRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkModelBaseModelExist应返回false当车型下无基础车型时")
    void checkModelBaseModelExist_shouldReturnFalseWhenNoBaseModels() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder().id(modelId).code("MODEL001").build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehBaseModelRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = modelAppService.checkModelBaseModelExist(modelId);

        // Then
        assertFalse(result);
        verify(vehModelRepository).selectById(modelId);
        verify(vehBaseModelRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkModelVehicleExist应返回true当车型下有车辆时")
    void checkModelVehicleExist_shouldReturnTrueWhenVehiclesExist() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder().id(modelId).code("MODEL001").build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = modelAppService.checkModelVehicleExist(modelId);

        // Then
        assertTrue(result);
        verify(vehModelRepository).selectById(modelId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkModelVehicleExist应返回false当车型下无车辆时")
    void checkModelVehicleExist_shouldReturnFalseWhenNoVehicles() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder().id(modelId).code("MODEL001").build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = modelAppService.checkModelVehicleExist(modelId);

        // Then
        assertFalse(result);
        verify(vehModelRepository).selectById(modelId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getModelById应返回车型DTO")
    void getModelById_shouldReturnModelDto() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder()
                .id(modelId)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();

        CarLine carLine = CarLine.builder()
                .code("CARLINE001")
                .brandCode("BRAND001")
                .build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehCarLineRepository.selectByCode("CARLINE001")).thenReturn(carLine);

        // When
        ModelDto result = modelAppService.getModelById(modelId);

        // Then
        assertNotNull(result);
        assertEquals(modelId, result.getId());
        assertEquals("MODEL001", result.getCode());
        assertEquals("测试车型", result.getName());
        assertEquals("BRAND001", result.getBrandCode());
        verify(vehModelRepository).selectById(modelId);
        verify(vehCarLineRepository).selectByCode("CARLINE001");
    }

    @Test
    @DisplayName("getModelById应返回车型DTO当车系不存在时")
    void getModelById_shouldReturnModelDtoWhenCarLineNotFound() {
        // Given
        Long modelId = 1L;
        Model model = Model.builder()
                .id(modelId)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();

        when(vehModelRepository.selectById(modelId)).thenReturn(model);
        when(vehCarLineRepository.selectByCode("CARLINE001")).thenReturn(null);

        // When
        ModelDto result = modelAppService.getModelById(modelId);

        // Then
        assertNotNull(result);
        assertEquals(modelId, result.getId());
        assertEquals("MODEL001", result.getCode());
        assertNull(result.getBrandCode());
        verify(vehModelRepository).selectById(modelId);
        verify(vehCarLineRepository).selectByCode("CARLINE001");
    }

    @Test
    @DisplayName("getModelByCode应返回车型领域对象")
    void getModelByCode_shouldReturnModelEntity() {
        // Given
        String code = "MODEL001";
        Model model = Model.builder()
                .id(1L)
                .code(code)
                .name("测试车型")
                .build();

        when(vehModelRepository.selectByCode(code)).thenReturn(model);

        // When
        Model result = modelAppService.getModelByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(vehModelRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createModel应成功创建MANUAL来源车型")
    void createModel_shouldSuccessfullyCreateManualModel() {
        // Given
        ModelCmd cmd = ModelCmd.builder()
                .code("MODEL001")
                .name("新车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();

        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        int result = modelAppService.createModel(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehModelRepository).insert(any(Model.class));
    }

    @Test
    @DisplayName("createModel应成功创建MANUAL来源车型当source为null时")
    void createModel_shouldSuccessfullyCreateModelWhenSourceIsNull() {
        // Given
        ModelCmd cmd = ModelCmd.builder()
                .code("MODEL001")
                .name("新车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();

        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        int result = modelAppService.createModel(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehModelRepository).insert(any(Model.class));
    }

    @Test
    @DisplayName("modifyModel应成功修改MANUAL来源车型")
    void modifyModel_shouldSuccessfullyModifyManualModel() {
        // Given
        ModelCmd cmd = ModelCmd.builder()
                .id(1L)
                .code("MODEL001")
                .name("修改后的车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();

        Model existingModel = Model.builder()
                .id(1L)
                .code("MODEL001")
                .name("原始车型")
                .source(SourceType.MANUAL)
                .build();

        when(vehModelRepository.selectById(1L)).thenReturn(existingModel);
        when(vehModelRepository.update(any(Model.class))).thenReturn(1);

        // When
        int result = modelAppService.modifyModel(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehModelRepository).selectById(1L);
        verify(vehModelRepository).update(any(Model.class));
    }

    @Test
    @DisplayName("modifyModel应拒绝修改MDM来源车型")
    void modifyModel_shouldRejectMdmSourceModel() {
        // Given
        ModelCmd cmd = ModelCmd.builder()
                .id(1L)
                .code("MODEL_MDM")
                .name("修改MDM车型")
                .build();

        Model existingModel = Model.builder()
                .id(1L)
                .code("MODEL_MDM")
                .name("MDM车型")
                .source(SourceType.MDM)
                .build();

        when(vehModelRepository.selectById(1L)).thenReturn(existingModel);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            modelAppService.modifyModel(cmd, "user1");
        });
        verify(vehModelRepository).selectById(1L);
        verify(vehModelRepository, never()).update(any(Model.class));
    }

    @Test
    @DisplayName("deleteModelByIds应成功删除MANUAL来源车型")
    void deleteModelByIds_shouldSuccessfullyDeleteManualModels() {
        // Given
        Long[] ids = {1L, 2L, 3L};

        Model model1 = Model.builder().id(1L).code("MODEL001").source(SourceType.MANUAL).build();
        Model model2 = Model.builder().id(2L).code("MODEL002").source(SourceType.MANUAL).build();
        Model model3 = Model.builder().id(3L).code("MODEL003").source(SourceType.MANUAL).build();

        when(vehModelRepository.selectById(1L)).thenReturn(model1);
        when(vehModelRepository.selectById(2L)).thenReturn(model2);
        when(vehModelRepository.selectById(3L)).thenReturn(model3);
        when(vehModelRepository.batchPhysicalDelete(ids)).thenReturn(3);

        // When
        int result = modelAppService.deleteModelByIds(ids);

        // Then
        assertEquals(3, result);
        verify(vehModelRepository).selectById(1L);
        verify(vehModelRepository).selectById(2L);
        verify(vehModelRepository).selectById(3L);
        verify(vehModelRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deleteModelByIds应拒绝删除MDM来源车型")
    void deleteModelByIds_shouldRejectMdmSourceModel() {
        // Given
        Long[] ids = {1L, 2L};

        Model model1 = Model.builder().id(1L).code("MODEL001").source(SourceType.MANUAL).build();
        Model model2 = Model.builder().id(2L).code("MODEL_MDM").source(SourceType.MDM).build();

        when(vehModelRepository.selectById(1L)).thenReturn(model1);
        when(vehModelRepository.selectById(2L)).thenReturn(model2);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            modelAppService.deleteModelByIds(ids);
        });
        verify(vehModelRepository).selectById(1L);
        verify(vehModelRepository).selectById(2L);
        verify(vehModelRepository, never()).batchPhysicalDelete(any(Long[].class));
    }

    @Test
    @DisplayName("getByExternalRefId应返回车型领域对象")
    void getByExternalRefId_shouldReturnModelEntity() {
        // Given
        String externalRefId = "ext-001";
        Model model = Model.builder()
                .id(1L)
                .code("MODEL001")
                .externalRefId(externalRefId)
                .build();

        when(vehModelRepository.selectByExternalRefId(externalRefId)).thenReturn(model);

        // When
        Model result = vehModelRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        verify(vehModelRepository).selectByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的车型数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        SourceType source = SourceType.MDM;
        when(vehModelRepository.countBySource(source)).thenReturn(5L);

        // When
        long result = vehModelRepository.countBySource(source);

        // Then
        assertEquals(5L, result);
        verify(vehModelRepository).countBySource(source);
    }
}
