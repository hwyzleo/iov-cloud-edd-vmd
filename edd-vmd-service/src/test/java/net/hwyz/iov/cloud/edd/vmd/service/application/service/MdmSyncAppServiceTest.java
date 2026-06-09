package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.api.service.MdmBrandQueryClient;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmCarLineQueryClient;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmModelQueryClient;
import net.hwyz.iov.cloud.edd.vmd.api.service.MdmPlatformQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlatformRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MdmSyncAppService单元测试
 * 
 * <p>CR-014：CarLine 投影采用按需最小化只读投影，保留 brand_code 冗余字段。</p>
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class MdmSyncAppServiceTest {

    @Mock
    private VehBrandRepository vehBrandRepository;

    @Mock
    private VehCarLineRepository vehCarLineRepository;

    @Mock
    private VehPlatformRepository vehPlatformRepository;

    @Mock
    private VehModelRepository vehModelRepository;

    @Mock
    private MdmBrandQueryClient mdmBrandQueryClient;

    @Mock
    private MdmCarLineQueryClient mdmCarLineQueryClient;

    @Mock
    private MdmPlatformQueryClient mdmPlatformQueryClient;

    @Mock
    private MdmModelQueryClient mdmModelQueryClient;

    @InjectMocks
    private MdmSyncAppService mdmSyncAppService;

    @Test
    @DisplayName("handleBrandEvent应新增本地不存在的品牌投影")
    void handleBrandEvent_shouldInsertWhenLocalBrandNotExists() {
        // Given
        MdmBrandEvent event = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "新品牌", LocalDateTime.now());

        when(vehBrandRepository.selectByExternalRefId("mdm-brand-001")).thenReturn(null);
        when(vehBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(vehBrandRepository).selectByExternalRefId("mdm-brand-001");
        verify(vehBrandRepository).insert(any(Brand.class));
    }

    @Test
    @DisplayName("handleBrandEvent应更新本地已存在且版本更高的品牌投影")
    void handleBrandEvent_shouldUpdateWhenLocalBrandExistsAndVersionHigher() {
        // Given
        MdmBrandEvent event = new MdmBrandEvent("UPDATED", "mdm-brand-002", 2L, "BRAND002", "更新后的品牌", LocalDateTime.now());

        Brand localBrand = Brand.builder()
                .id(1L)
                .code("BRAND002")
                .name("原始品牌")
                .source(SourceType.MDM)
                .externalRefId("mdm-brand-002")
                .externalVersion(1L)
                .build();

        when(vehBrandRepository.selectByExternalRefId("mdm-brand-002")).thenReturn(localBrand);
        when(vehBrandRepository.updateById(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(vehBrandRepository).selectByExternalRefId("mdm-brand-002");
        verify(vehBrandRepository).updateById(any(Brand.class));
    }

    @Test
    @DisplayName("handleBrandEvent应忽略版本不高于本地的品牌事件")
    void handleBrandEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmBrandEvent event = new MdmBrandEvent("UPDATED", "mdm-brand-003", 1L, "BRAND003", "旧版本品牌", LocalDateTime.now());

        Brand localBrand = Brand.builder()
                .id(1L)
                .code("BRAND003")
                .name("本地品牌")
                .source(SourceType.MDM)
                .externalRefId("mdm-brand-003")
                .externalVersion(2L)
                .build();

        when(vehBrandRepository.selectByExternalRefId("mdm-brand-003")).thenReturn(localBrand);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(vehBrandRepository).selectByExternalRefId("mdm-brand-003");
        verify(vehBrandRepository, never()).updateById(any(Brand.class));
    }

    @Test
    @DisplayName("handleSeriesEvent应新增本地不存在的车系投影")
    void handleSeriesEvent_shouldInsertWhenLocalCarLineNotExists() {
        // Given
        MdmCarLineEvent event = new MdmCarLineEvent("CREATED", "mdm-carline-001", 1L, "CARLINE001", "新车系", "BRAND001", LocalDateTime.now());

        when(vehCarLineRepository.selectByExternalRefId("mdm-carline-001")).thenReturn(null);
        when(vehCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(vehCarLineRepository).selectByExternalRefId("mdm-carline-001");
        verify(vehCarLineRepository).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("handleSeriesEvent应更新本地已存在且版本更高的车系投影")
    void handleSeriesEvent_shouldUpdateWhenLocalCarLineExistsAndVersionHigher() {
        // Given
        MdmCarLineEvent event = new MdmCarLineEvent("UPDATED", "mdm-carline-002", 2L, "CARLINE002", "更新后的车系", "BRAND002", LocalDateTime.now());

        CarLine localCarLine = CarLine.builder()
                .id(1L)
                .code("CARLINE002")
                .name("原始车系")
                .brandCode("BRAND002")
                .source(SourceType.MDM)
                .externalRefId("mdm-carline-002")
                .externalVersion(1L)
                .build();

        when(vehCarLineRepository.selectByExternalRefId("mdm-carline-002")).thenReturn(localCarLine);
        when(vehCarLineRepository.updateById(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(vehCarLineRepository).selectByExternalRefId("mdm-carline-002");
        verify(vehCarLineRepository).updateById(any(CarLine.class));
    }

    @Test
    @DisplayName("handleSeriesEvent应忽略版本不高于本地的车系事件")
    void handleSeriesEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmCarLineEvent event = new MdmCarLineEvent("UPDATED", "mdm-carline-003", 1L, "CARLINE003", "旧版本车系", "BRAND003", LocalDateTime.now());

        CarLine localCarLine = CarLine.builder()
                .id(1L)
                .code("CARLINE003")
                .name("本地车系")
                .brandCode("BRAND003")
                .source(SourceType.MDM)
                .externalRefId("mdm-carline-003")
                .externalVersion(2L)
                .build();

        when(vehCarLineRepository.selectByExternalRefId("mdm-carline-003")).thenReturn(localCarLine);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(vehCarLineRepository).selectByExternalRefId("mdm-carline-003");
        verify(vehCarLineRepository, never()).updateById(any(CarLine.class));
    }

    @Test
    @DisplayName("handlePlatformEvent应新增本地不存在的平台投影")
    void handlePlatformEvent_shouldInsertWhenLocalPlatformNotExists() {
        // Given
        MdmPlatformEvent event = new MdmPlatformEvent("CREATED", "mdm-platform-001", 1L, "PLATFORM001", "新平台", LocalDateTime.now());

        when(vehPlatformRepository.selectByExternalRefId("mdm-platform-001")).thenReturn(null);
        when(vehPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(vehPlatformRepository).selectByExternalRefId("mdm-platform-001");
        verify(vehPlatformRepository).insert(any(Platform.class));
    }

    @Test
    @DisplayName("handlePlatformEvent应更新本地已存在且版本更高的平台投影")
    void handlePlatformEvent_shouldUpdateWhenLocalPlatformExistsAndVersionHigher() {
        // Given
        MdmPlatformEvent event = new MdmPlatformEvent("UPDATED", "mdm-platform-002", 2L, "PLATFORM002", "更新后的平台", LocalDateTime.now());

        Platform localPlatform = Platform.builder()
                .id(1L)
                .code("PLATFORM002")
                .name("原始平台")
                .source(SourceType.MDM)
                .externalRefId("mdm-platform-002")
                .externalVersion(1L)
                .build();

        when(vehPlatformRepository.selectByExternalRefId("mdm-platform-002")).thenReturn(localPlatform);
        when(vehPlatformRepository.updateById(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(vehPlatformRepository).selectByExternalRefId("mdm-platform-002");
        verify(vehPlatformRepository).updateById(any(Platform.class));
    }

    @Test
    @DisplayName("handlePlatformEvent应忽略版本不高于本地的平台事件")
    void handlePlatformEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmPlatformEvent event = new MdmPlatformEvent("UPDATED", "mdm-platform-003", 1L, "PLATFORM003", "旧版本平台", LocalDateTime.now());

        Platform localPlatform = Platform.builder()
                .id(1L)
                .code("PLATFORM003")
                .name("本地平台")
                .source(SourceType.MDM)
                .externalRefId("mdm-platform-003")
                .externalVersion(2L)
                .build();

        when(vehPlatformRepository.selectByExternalRefId("mdm-platform-003")).thenReturn(localPlatform);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(vehPlatformRepository).selectByExternalRefId("mdm-platform-003");
        verify(vehPlatformRepository, never()).updateById(any(Platform.class));
    }

    @Test
    @DisplayName("bootstrapBrand应跳过当本地已有MDM品牌数据时")
    void bootstrapBrand_shouldSkipWhenLocalMdmBrandsExist() {
        // Given
        when(vehBrandRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(vehBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient, never()).getAllBrands();
    }

    @Test
    @DisplayName("bootstrapBrand应同步当本地无MDM品牌数据时")
    void bootstrapBrand_shouldSyncWhenNoLocalMdmBrands() {
        // Given
        when(vehBrandRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> brandData1 = new HashMap<>();
        brandData1.put("id", "mdm-brand-001");
        brandData1.put("code", "BRAND001");
        brandData1.put("name", "品牌1");
        brandData1.put("version", 1);

        Map<String, Object> brandData2 = new HashMap<>();
        brandData2.put("id", "mdm-brand-002");
        brandData2.put("code", "BRAND002");
        brandData2.put("name", "品牌2");
        brandData2.put("version", 1);

        List<Map<String, Object>> mdmBrands = Arrays.asList(brandData1, brandData2);
        when(mdmBrandQueryClient.getAllBrands()).thenReturn(mdmBrands);
        when(vehBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(vehBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient).getAllBrands();
        verify(vehBrandRepository, times(2)).insert(any(Brand.class));
    }

    @Test
    @DisplayName("bootstrapSeries应跳过当本地已有MDM车系数据时")
    void bootstrapSeries_shouldSkipWhenLocalMdmCarLinesExist() {
        // Given
        when(vehCarLineRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(vehCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient, never()).getAllSeries();
    }

    @Test
    @DisplayName("bootstrapSeries应同步当本地无MDM车系数据时")
    void bootstrapSeries_shouldSyncWhenNoLocalMdmCarLines() {
        // Given
        when(vehCarLineRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> carLineData1 = new HashMap<>();
        carLineData1.put("id", "mdm-carline-001");
        carLineData1.put("code", "CARLINE001");
        carLineData1.put("name", "车系1");
        carLineData1.put("brandCode", "BRAND001");
        carLineData1.put("version", 1);

        Map<String, Object> carLineData2 = new HashMap<>();
        carLineData2.put("id", "mdm-carline-002");
        carLineData2.put("code", "CARLINE002");
        carLineData2.put("name", "车系2");
        carLineData2.put("brandCode", "BRAND001");
        carLineData2.put("version", 1);

        List<Map<String, Object>> mdmCarLines = Arrays.asList(carLineData1, carLineData2);
        when(mdmCarLineQueryClient.getAllSeries()).thenReturn(mdmCarLines);
        when(vehCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(vehCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient).getAllSeries();
        verify(vehCarLineRepository, times(2)).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("bootstrapPlatform应跳过当本地已有MDM平台数据时")
    void bootstrapPlatform_shouldSkipWhenLocalMdmPlatformsExist() {
        // Given
        when(vehPlatformRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(vehPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient, never()).getAllPlatforms();
    }

    @Test
    @DisplayName("bootstrapPlatform应同步当本地无MDM平台数据时")
    void bootstrapPlatform_shouldSyncWhenNoLocalMdmPlatforms() {
        // Given
        when(vehPlatformRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> platformData1 = new HashMap<>();
        platformData1.put("id", "mdm-platform-001");
        platformData1.put("code", "PLATFORM001");
        platformData1.put("name", "平台1");
        platformData1.put("version", 1);

        Map<String, Object> platformData2 = new HashMap<>();
        platformData2.put("id", "mdm-platform-002");
        platformData2.put("code", "PLATFORM002");
        platformData2.put("name", "平台2");
        platformData2.put("version", 1);

        List<Map<String, Object>> mdmPlatforms = Arrays.asList(platformData1, platformData2);
        when(mdmPlatformQueryClient.getAllPlatforms()).thenReturn(mdmPlatforms);
        when(vehPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(vehPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient).getAllPlatforms();
        verify(vehPlatformRepository, times(2)).insert(any(Platform.class));
    }

    @Test
    @DisplayName("bootstrapAll应调用所有bootstrap方法")
    void bootstrapAll_shouldCallAllBootstrapMethods() {
        // Given
        when(vehBrandRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(vehCarLineRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(vehPlatformRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(vehModelRepository.countBySource(SourceType.MDM)).thenReturn(1L);

        // When
        mdmSyncAppService.bootstrapAll();

        // Then
        verify(vehBrandRepository).countBySource(SourceType.MDM);
        verify(vehCarLineRepository).countBySource(SourceType.MDM);
        verify(vehPlatformRepository).countBySource(SourceType.MDM);
        verify(vehModelRepository).countBySource(SourceType.MDM);
    }

    @Test
    @DisplayName("bootstrapBrand应处理MDM接口异常并不清空本地数据")
    void bootstrapBrand_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(vehBrandRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmBrandQueryClient.getAllBrands()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(vehBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient).getAllBrands();
        verify(vehBrandRepository, never()).insert(any(Brand.class));
    }

    @Test
    @DisplayName("bootstrapSeries应处理MDM接口异常并不清空本地数据")
    void bootstrapSeries_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(vehCarLineRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmCarLineQueryClient.getAllSeries()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(vehCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient).getAllSeries();
        verify(vehCarLineRepository, never()).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("bootstrapPlatform应处理MDM接口异常并不清空本地数据")
    void bootstrapPlatform_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(vehPlatformRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmPlatformQueryClient.getAllPlatforms()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(vehPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient).getAllPlatforms();
        verify(vehPlatformRepository, never()).insert(any(Platform.class));
    }

    @Test
    @DisplayName("handleModelEvent应新增本地不存在的车型投影")
    void handleModelEvent_shouldInsertWhenLocalModelNotExists() {
        // Given
        MdmModelEvent event = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001", "新车型", "PLATFORM001", "CARLINE001", LocalDateTime.now());

        when(vehModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(null);
        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(vehModelRepository).selectByExternalRefId("mdm-model-001");
        verify(vehModelRepository).insert(any(Model.class));
    }

    @Test
    @DisplayName("handleModelEvent应更新本地已存在且版本更高的车型投影")
    void handleModelEvent_shouldUpdateWhenLocalModelExistsAndVersionHigher() {
        // Given
        MdmModelEvent event = new MdmModelEvent("UPDATED", "mdm-model-002", 2L, "MODEL002", "更新后的车型", "PLATFORM002", "CARLINE002", LocalDateTime.now());

        Model localModel = Model.builder()
                .id(1L)
                .code("MODEL002")
                .name("原始车型")
                .platformCode("PLATFORM002")
                .carLineCode("CARLINE002")
                .source(SourceType.MDM)
                .externalRefId("mdm-model-002")
                .externalVersion(1L)
                .build();

        when(vehModelRepository.selectByExternalRefId("mdm-model-002")).thenReturn(localModel);
        when(vehModelRepository.updateById(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(vehModelRepository).selectByExternalRefId("mdm-model-002");
        verify(vehModelRepository).updateById(any(Model.class));
    }

    @Test
    @DisplayName("handleModelEvent应忽略版本不高于本地的车型事件")
    void handleModelEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmModelEvent event = new MdmModelEvent("UPDATED", "mdm-model-003", 1L, "MODEL003", "旧版本车型", "PLATFORM003", "CARLINE003", LocalDateTime.now());

        Model localModel = Model.builder()
                .id(1L)
                .code("MODEL003")
                .name("本地车型")
                .platformCode("PLATFORM003")
                .carLineCode("CARLINE003")
                .source(SourceType.MDM)
                .externalRefId("mdm-model-003")
                .externalVersion(2L)
                .build();

        when(vehModelRepository.selectByExternalRefId("mdm-model-003")).thenReturn(localModel);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(vehModelRepository).selectByExternalRefId("mdm-model-003");
        verify(vehModelRepository, never()).updateById(any(Model.class));
    }

    @Test
    @DisplayName("bootstrapModel应跳过当本地已有MDM车型数据时")
    void bootstrapModel_shouldSkipWhenLocalMdmModelsExist() {
        // Given
        when(vehModelRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(vehModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient, never()).getAllModels();
    }

    @Test
    @DisplayName("bootstrapModel应同步当本地无MDM车型数据时")
    void bootstrapModel_shouldSyncWhenNoLocalMdmModels() {
        // Given
        when(vehModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> modelData1 = new HashMap<>();
        modelData1.put("id", "mdm-model-001");
        modelData1.put("code", "MODEL001");
        modelData1.put("name", "车型1");
        modelData1.put("platformCode", "PLATFORM001");
        modelData1.put("carLineCode", "CARLINE001");
        modelData1.put("version", 1);

        Map<String, Object> modelData2 = new HashMap<>();
        modelData2.put("id", "mdm-model-002");
        modelData2.put("code", "MODEL002");
        modelData2.put("name", "车型2");
        modelData2.put("platformCode", "PLATFORM001");
        modelData2.put("carLineCode", "CARLINE001");
        modelData2.put("version", 1);

        List<Map<String, Object>> mdmModels = Arrays.asList(modelData1, modelData2);
        when(mdmModelQueryClient.getAllModels()).thenReturn(mdmModels);
        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(vehModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(vehModelRepository, times(2)).insert(any(Model.class));
    }

    @Test
    @DisplayName("bootstrapModel应处理MDM接口异常并不清空本地数据")
    void bootstrapModel_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(vehModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmModelQueryClient.getAllModels()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(vehModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(vehModelRepository, never()).insert(any(Model.class));
    }

    @Test
    @DisplayName("handleModelEvent应正确处理平台和车系关联字段")
    void handleModelEvent_shouldCorrectlyHandlePlatformAndCarLineFields() {
        // Given
        MdmModelEvent event = new MdmModelEvent("CREATED", "mdm-model-004", 1L, "MODEL004", "新车型", "PLATFORM001", "CARLINE001", LocalDateTime.now());

        when(vehModelRepository.selectByExternalRefId("mdm-model-004")).thenReturn(null);
        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(vehModelRepository).selectByExternalRefId("mdm-model-004");
        verify(vehModelRepository).insert(argThat(model ->
                "PLATFORM001".equals(model.getPlatformCode()) &&
                "CARLINE001".equals(model.getCarLineCode()) &&
                SourceType.MDM.equals(model.getSource())
        ));
    }

    @Test
    @DisplayName("bootstrapModel应正确处理平台和车系关联字段")
    void bootstrapModel_shouldCorrectlyHandlePlatformAndCarLineFields() {
        // Given
        when(vehModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> modelData = new HashMap<>();
        modelData.put("id", "mdm-model-003");
        modelData.put("code", "MODEL003");
        modelData.put("name", "车型3");
        modelData.put("platformCode", "PLATFORM002");
        modelData.put("carLineCode", "CARLINE002");
        modelData.put("version", 1);

        List<Map<String, Object>> mdmModels = Arrays.asList(modelData);
        when(mdmModelQueryClient.getAllModels()).thenReturn(mdmModels);
        when(vehModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(vehModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(vehModelRepository).insert(argThat(model ->
                "PLATFORM002".equals(model.getPlatformCode()) &&
                "CARLINE002".equals(model.getCarLineCode()) &&
                SourceType.MDM.equals(model.getSource())
        ));
    }
}
