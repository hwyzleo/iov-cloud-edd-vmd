package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmBrandQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmCarLineQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmConfigurationQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmModelQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmOptionCodeQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmOptionFamilyQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPlantQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPlatformQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmVariantQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
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
    private MdmBrandRepository mdmBrandRepository;

    @Mock
    private MdmCarLineRepository mdmCarLineRepository;

    @Mock
    private MdmPlatformRepository mdmPlatformRepository;

    @Mock
    private MdmModelRepository mdmModelRepository;

    @Mock
    private MdmOptionFamilyRepository mdmOptionFamilyRepository;

    @Mock
    private MdmConfigurationRepository mdmConfigurationRepository;

    @Mock
    private MdmPlantRepository mdmPlantRepository;

    @Mock
    private MdmVariantRepository mdmVariantRepository;

    @Mock
    private MdmBrandQueryClient mdmBrandQueryClient;

    @Mock
    private MdmCarLineQueryClient mdmCarLineQueryClient;

    @Mock
    private MdmPlatformQueryClient mdmPlatformQueryClient;

    @Mock
    private MdmModelQueryClient mdmModelQueryClient;

    @Mock
    private MdmOptionFamilyQueryClient mdmOptionFamilyQueryClient;

    @Mock
    private MdmOptionCodeQueryClient mdmOptionCodeQueryClient;

    @Mock
    private MdmConfigurationQueryClient mdmConfigurationQueryClient;

    @Mock
    private MdmPlantQueryClient mdmPlantQueryClient;

    @Mock
    private MdmVariantQueryClient mdmVariantQueryClient;

    @InjectMocks
    private MdmSyncAppService mdmSyncAppService;

    @Test
    @DisplayName("handleBrandEvent应新增本地不存在的品牌投影")
    void handleBrandEvent_shouldInsertWhenLocalBrandNotExists() {
        // Given
        MdmBrandEvent event = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "新品牌", LocalDateTime.now());

        when(mdmBrandRepository.selectByExternalRefId("mdm-brand-001")).thenReturn(null);
        when(mdmBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByExternalRefId("mdm-brand-001");
        verify(mdmBrandRepository).insert(any(Brand.class));
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

        when(mdmBrandRepository.selectByExternalRefId("mdm-brand-002")).thenReturn(localBrand);
        when(mdmBrandRepository.updateById(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByExternalRefId("mdm-brand-002");
        verify(mdmBrandRepository).updateById(any(Brand.class));
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

        when(mdmBrandRepository.selectByExternalRefId("mdm-brand-003")).thenReturn(localBrand);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByExternalRefId("mdm-brand-003");
        verify(mdmBrandRepository, never()).updateById(any(Brand.class));
    }

    @Test
    @DisplayName("handleSeriesEvent应新增本地不存在的车系投影")
    void handleSeriesEvent_shouldInsertWhenLocalCarLineNotExists() {
        // Given
        MdmCarLineEvent event = new MdmCarLineEvent("CREATED", "mdm-carline-001", 1L, "CARLINE001", "新车系", "BRAND001", LocalDateTime.now());

        when(mdmCarLineRepository.selectByExternalRefId("mdm-carline-001")).thenReturn(null);
        when(mdmCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByExternalRefId("mdm-carline-001");
        verify(mdmCarLineRepository).insert(any(CarLine.class));
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

        when(mdmCarLineRepository.selectByExternalRefId("mdm-carline-002")).thenReturn(localCarLine);
        when(mdmCarLineRepository.updateById(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByExternalRefId("mdm-carline-002");
        verify(mdmCarLineRepository).updateById(any(CarLine.class));
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

        when(mdmCarLineRepository.selectByExternalRefId("mdm-carline-003")).thenReturn(localCarLine);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByExternalRefId("mdm-carline-003");
        verify(mdmCarLineRepository, never()).updateById(any(CarLine.class));
    }

    @Test
    @DisplayName("handlePlatformEvent应新增本地不存在的平台投影")
    void handlePlatformEvent_shouldInsertWhenLocalPlatformNotExists() {
        // Given
        MdmPlatformEvent event = new MdmPlatformEvent("CREATED", "mdm-platform-001", 1L, "PLATFORM001", "新平台", LocalDateTime.now());

        when(mdmPlatformRepository.selectByExternalRefId("mdm-platform-001")).thenReturn(null);
        when(mdmPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByExternalRefId("mdm-platform-001");
        verify(mdmPlatformRepository).insert(any(Platform.class));
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

        when(mdmPlatformRepository.selectByExternalRefId("mdm-platform-002")).thenReturn(localPlatform);
        when(mdmPlatformRepository.updateById(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByExternalRefId("mdm-platform-002");
        verify(mdmPlatformRepository).updateById(any(Platform.class));
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

        when(mdmPlatformRepository.selectByExternalRefId("mdm-platform-003")).thenReturn(localPlatform);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByExternalRefId("mdm-platform-003");
        verify(mdmPlatformRepository, never()).updateById(any(Platform.class));
    }

    @Test
    @DisplayName("bootstrapBrand应跳过当本地已有MDM品牌数据时")
    void bootstrapBrand_shouldSkipWhenLocalMdmBrandsExist() {
        // Given
        when(mdmBrandRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(mdmBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient, never()).getAllBrands();
    }

    @Test
    @DisplayName("bootstrapBrand应同步当本地无MDM品牌数据时")
    void bootstrapBrand_shouldSyncWhenNoLocalMdmBrands() {
        // Given
        when(mdmBrandRepository.countBySource(SourceType.MDM)).thenReturn(0L);

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
        when(mdmBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(mdmBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient).getAllBrands();
        verify(mdmBrandRepository, times(2)).insert(any(Brand.class));
    }

    @Test
    @DisplayName("bootstrapSeries应跳过当本地已有MDM车系数据时")
    void bootstrapSeries_shouldSkipWhenLocalMdmCarLinesExist() {
        // Given
        when(mdmCarLineRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(mdmCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient, never()).getAllSeries();
    }

    @Test
    @DisplayName("bootstrapSeries应同步当本地无MDM车系数据时")
    void bootstrapSeries_shouldSyncWhenNoLocalMdmCarLines() {
        // Given
        when(mdmCarLineRepository.countBySource(SourceType.MDM)).thenReturn(0L);

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
        when(mdmCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(mdmCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient).getAllSeries();
        verify(mdmCarLineRepository, times(2)).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("bootstrapPlatform应跳过当本地已有MDM平台数据时")
    void bootstrapPlatform_shouldSkipWhenLocalMdmPlatformsExist() {
        // Given
        when(mdmPlatformRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(mdmPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient, never()).getAllPlatforms();
    }

    @Test
    @DisplayName("bootstrapPlatform应同步当本地无MDM平台数据时")
    void bootstrapPlatform_shouldSyncWhenNoLocalMdmPlatforms() {
        // Given
        when(mdmPlatformRepository.countBySource(SourceType.MDM)).thenReturn(0L);

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
        when(mdmPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(mdmPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient).getAllPlatforms();
        verify(mdmPlatformRepository, times(2)).insert(any(Platform.class));
    }

    @Test
    @DisplayName("bootstrapAll应调用所有bootstrap方法")
    void bootstrapAll_shouldCallAllBootstrapMethods() {
        // Given
        when(mdmBrandRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmCarLineRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmPlatformRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmPlantRepository.countBySource(SourceType.MDM.name())).thenReturn(1);
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmVariantRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmConfigurationRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmOptionFamilyRepository.countBySource(SourceType.MDM.name())).thenReturn(1L);
        when(mdmOptionFamilyRepository.countOptionCodeBySource(SourceType.MDM.name())).thenReturn(1L);

        // When
        mdmSyncAppService.bootstrapAll();

        // Then
        verify(mdmBrandRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlantRepository).countBySource(SourceType.MDM.name());
        verify(mdmModelRepository).countBySource(SourceType.MDM);
        verify(mdmVariantRepository).countBySource(SourceType.MDM);
        verify(mdmConfigurationRepository).countBySource(SourceType.MDM);
        verify(mdmOptionFamilyRepository).countBySource(SourceType.MDM.name());
        verify(mdmOptionFamilyRepository).countOptionCodeBySource(SourceType.MDM.name());
    }

    @Test
    @DisplayName("bootstrapBrand应处理MDM接口异常并不清空本地数据")
    void bootstrapBrand_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(mdmBrandRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmBrandQueryClient.getAllBrands()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapBrand();

        // Then
        verify(mdmBrandRepository).countBySource(SourceType.MDM);
        verify(mdmBrandQueryClient).getAllBrands();
        verify(mdmBrandRepository, never()).insert(any(Brand.class));
    }

    @Test
    @DisplayName("bootstrapSeries应处理MDM接口异常并不清空本地数据")
    void bootstrapSeries_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(mdmCarLineRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmCarLineQueryClient.getAllSeries()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapSeries();

        // Then
        verify(mdmCarLineRepository).countBySource(SourceType.MDM);
        verify(mdmCarLineQueryClient).getAllSeries();
        verify(mdmCarLineRepository, never()).insert(any(CarLine.class));
    }

    @Test
    @DisplayName("bootstrapPlatform应处理MDM接口异常并不清空本地数据")
    void bootstrapPlatform_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(mdmPlatformRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmPlatformQueryClient.getAllPlatforms()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapPlatform();

        // Then
        verify(mdmPlatformRepository).countBySource(SourceType.MDM);
        verify(mdmPlatformQueryClient).getAllPlatforms();
        verify(mdmPlatformRepository, never()).insert(any(Platform.class));
    }

    @Test
    @DisplayName("handleModelEvent应新增本地不存在的车型投影")
    void handleModelEvent_shouldInsertWhenLocalModelNotExists() {
        // Given
        MdmModelEvent event = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001", "新车型", "PLATFORM001", "CARLINE001", LocalDateTime.now());

        when(mdmModelRepository.selectByExternalRefId("mdm-model-001")).thenReturn(null);
        when(mdmModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelRepository).selectByExternalRefId("mdm-model-001");
        verify(mdmModelRepository).insert(any(Model.class));
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

        when(mdmModelRepository.selectByExternalRefId("mdm-model-002")).thenReturn(localModel);
        when(mdmModelRepository.updateById(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelRepository).selectByExternalRefId("mdm-model-002");
        verify(mdmModelRepository).updateById(any(Model.class));
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

        when(mdmModelRepository.selectByExternalRefId("mdm-model-003")).thenReturn(localModel);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelRepository).selectByExternalRefId("mdm-model-003");
        verify(mdmModelRepository, never()).updateById(any(Model.class));
    }

    @Test
    @DisplayName("bootstrapModel应跳过当本地已有MDM车型数据时")
    void bootstrapModel_shouldSkipWhenLocalMdmModelsExist() {
        // Given
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(mdmModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient, never()).getAllModels();
    }

    @Test
    @DisplayName("bootstrapModel应同步当本地无MDM车型数据时")
    void bootstrapModel_shouldSyncWhenNoLocalMdmModels() {
        // Given
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);

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
        when(mdmModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(mdmModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(mdmModelRepository, times(2)).insert(any(Model.class));
    }

    @Test
    @DisplayName("bootstrapModel应处理MDM接口异常并不清空本地数据")
    void bootstrapModel_shouldHandleMdmClientExceptionAndNotClearLocalData() {
        // Given
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        when(mdmModelQueryClient.getAllModels()).thenThrow(new RuntimeException("MDM服务不可用"));

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(mdmModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(mdmModelRepository, never()).insert(any(Model.class));
    }

    @Test
    @DisplayName("handleModelEvent应正确处理平台和车系关联字段")
    void handleModelEvent_shouldCorrectlyHandlePlatformAndCarLineFields() {
        // Given
        MdmModelEvent event = new MdmModelEvent("CREATED", "mdm-model-004", 1L, "MODEL004", "新车型", "PLATFORM001", "CARLINE001", LocalDateTime.now());

        when(mdmModelRepository.selectByExternalRefId("mdm-model-004")).thenReturn(null);
        when(mdmModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelRepository).selectByExternalRefId("mdm-model-004");
        verify(mdmModelRepository).insert(argThat(model ->
                "PLATFORM001".equals(model.getPlatformCode()) &&
                "CARLINE001".equals(model.getCarLineCode()) &&
                SourceType.MDM.equals(model.getSource())
        ));
    }

    @Test
    @DisplayName("bootstrapModel应正确处理平台和车系关联字段")
    void bootstrapModel_shouldCorrectlyHandlePlatformAndCarLineFields() {
        // Given
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);

        Map<String, Object> modelData = new HashMap<>();
        modelData.put("id", "mdm-model-003");
        modelData.put("code", "MODEL003");
        modelData.put("name", "车型3");
        modelData.put("platformCode", "PLATFORM002");
        modelData.put("carLineCode", "CARLINE002");
        modelData.put("version", 1);

        List<Map<String, Object>> mdmModels = Arrays.asList(modelData);
        when(mdmModelQueryClient.getAllModels()).thenReturn(mdmModels);
        when(mdmModelRepository.insert(any(Model.class))).thenReturn(1);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(mdmModelRepository).countBySource(SourceType.MDM);
        verify(mdmModelQueryClient).getAllModels();
        verify(mdmModelRepository).insert(argThat(model ->
                "PLATFORM002".equals(model.getPlatformCode()) &&
                "CARLINE002".equals(model.getCarLineCode()) &&
                SourceType.MDM.equals(model.getSource())
        ));
    }

    // ==================== OptionFamily Event Tests ====================

    @Test
    @DisplayName("handleOptionFamilyEvent应新增本地不存在的选项族投影")
    void handleOptionFamilyEvent_shouldInsertWhenLocalOptionFamilyNotExists() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("CREATED", "mdm-of-001", 1L, "OF001",
                "选装族1", "Option Family 1", "EXTERIOR", true, true, 1, LocalDateTime.now());

        when(mdmOptionFamilyRepository.selectByExternalRefId("mdm-of-001")).thenReturn(null);
        when(mdmOptionFamilyRepository.insert(any(OptionFamily.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByExternalRefId("mdm-of-001");
        verify(mdmOptionFamilyRepository).insert(any(OptionFamily.class));
    }

    @Test
    @DisplayName("handleOptionFamilyEvent应更新本地已存在且版本更高的选项族投影")
    void handleOptionFamilyEvent_shouldUpdateWhenLocalOptionFamilyExistsAndVersionHigher() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("UPDATED", "mdm-of-002", 2L, "OF002",
                "更新后的选装族", "Updated Option Family", "INTERIOR", false, true, 2, LocalDateTime.now());

        OptionFamily localOptionFamily = OptionFamily.builder()
                .id(1L)
                .code("OF002")
                .name("原始选装族")
                .nameEn("Original Option Family")
                .type("EXTERIOR")
                .mandatory(true)
                .enable(false)
                .sort(1)
                .source(SourceType.MDM.name())
                .externalRefId("mdm-of-002")
                .externalVersion(1L)
                .build();

        when(mdmOptionFamilyRepository.selectByExternalRefId("mdm-of-002")).thenReturn(localOptionFamily);
        when(mdmOptionFamilyRepository.updateById(any(OptionFamily.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByExternalRefId("mdm-of-002");
        verify(mdmOptionFamilyRepository).updateById(any(OptionFamily.class));
    }

    @Test
    @DisplayName("handleOptionFamilyEvent应忽略版本不高于本地的选项族事件")
    void handleOptionFamilyEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("UPDATED", "mdm-of-003", 1L, "OF003",
                "旧版本选装族", "Old Option Family", "EXTERIOR", true, true, 1, LocalDateTime.now());

        OptionFamily localOptionFamily = OptionFamily.builder()
                .id(1L)
                .code("OF003")
                .name("本地选装族")
                .nameEn("Local Option Family")
                .type("EXTERIOR")
                .mandatory(true)
                .enable(true)
                .sort(1)
                .source(SourceType.MDM.name())
                .externalRefId("mdm-of-003")
                .externalVersion(2L)
                .build();

        when(mdmOptionFamilyRepository.selectByExternalRefId("mdm-of-003")).thenReturn(localOptionFamily);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByExternalRefId("mdm-of-003");
        verify(mdmOptionFamilyRepository, never()).updateById(any(OptionFamily.class));
    }

    // ==================== OptionCode Event Tests ====================

    @Test
    @DisplayName("handleOptionCodeEvent应新增本地不存在的选项值投影")
    void handleOptionCodeEvent_shouldInsertWhenLocalOptionCodeNotExists() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("CREATED", "mdm-oc-001", 1L, "OC001",
                "OF001", "选装值1", "Option Code 1", "V001", true, 1, LocalDateTime.now());

        when(mdmOptionFamilyRepository.selectOptionCodeByExternalRefId("mdm-oc-001")).thenReturn(null);
        when(mdmOptionFamilyRepository.insertOptionCode(any(OptionCode.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByExternalRefId("mdm-oc-001");
        verify(mdmOptionFamilyRepository).insertOptionCode(any(OptionCode.class));
    }

    @Test
    @DisplayName("handleOptionCodeEvent应更新本地已存在且版本更高的选项值投影")
    void handleOptionCodeEvent_shouldUpdateWhenLocalOptionCodeExistsAndVersionHigher() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("UPDATED", "mdm-oc-002", 2L, "OC002",
                "OF002", "更新后的选装值", "Updated Option Code", "V002", true, 2, LocalDateTime.now());

        OptionCode localOptionCode = OptionCode.builder()
                .id(1L)
                .code("OC002")
                .optionFamilyCode("OF001")
                .name("原始选装值")
                .nameEn("Original Option Code")
                .val("V001")
                .enable(false)
                .sort(1)
                .source(SourceType.MDM.name())
                .externalRefId("mdm-oc-002")
                .externalVersion(1L)
                .build();

        when(mdmOptionFamilyRepository.selectOptionCodeByExternalRefId("mdm-oc-002")).thenReturn(localOptionCode);
        when(mdmOptionFamilyRepository.updateOptionCodeById(any(OptionCode.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByExternalRefId("mdm-oc-002");
        verify(mdmOptionFamilyRepository).updateOptionCodeById(any(OptionCode.class));
    }

    @Test
    @DisplayName("handleOptionCodeEvent应忽略版本不高于本地的选项值事件")
    void handleOptionCodeEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("UPDATED", "mdm-oc-003", 1L, "OC003",
                "OF003", "旧版本选装值", "Old Option Code", "V003", true, 1, LocalDateTime.now());

        OptionCode localOptionCode = OptionCode.builder()
                .id(1L)
                .code("OC003")
                .optionFamilyCode("OF003")
                .name("本地选装值")
                .nameEn("Local Option Code")
                .val("V003")
                .enable(true)
                .sort(1)
                .source(SourceType.MDM.name())
                .externalRefId("mdm-oc-003")
                .externalVersion(2L)
                .build();

        when(mdmOptionFamilyRepository.selectOptionCodeByExternalRefId("mdm-oc-003")).thenReturn(localOptionCode);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByExternalRefId("mdm-oc-003");
        verify(mdmOptionFamilyRepository, never()).updateOptionCodeById(any(OptionCode.class));
    }
}
