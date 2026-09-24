package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.mdm.api.service.BrandService;
import net.hwyz.iov.cloud.edd.mdm.api.service.CarLineService;
import net.hwyz.iov.cloud.edd.mdm.api.service.ConfigurationService;
import net.hwyz.iov.cloud.edd.mdm.api.service.ModelService;
import net.hwyz.iov.cloud.edd.mdm.api.service.OptionCodeService;
import net.hwyz.iov.cloud.edd.mdm.api.service.OptionFamilyService;
import net.hwyz.iov.cloud.edd.mdm.api.service.MdmPartService;
import net.hwyz.iov.cloud.edd.mdm.api.service.PlantService;
import net.hwyz.iov.cloud.edd.mdm.api.service.PlatformService;
import net.hwyz.iov.cloud.edd.mdm.api.service.VariantService;
import net.hwyz.iov.cloud.edd.mdm.api.service.VehicleNodeService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodePageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPartEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmConfigurationProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmVehicleNodeProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmModelProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmVariantProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNodeProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MdmSyncAppService单元测试
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
    private MdmVehicleNodeRepository mdmVehicleNodeRepository;

    @Mock
    private MdmPartRepository mdmPartRepository;

    @Mock
    private MdmConfigurationProjectionMapper mdmConfigurationProjectionMapper;

    @Mock
    private MdmVehicleNodeProjectionMapper mdmVehicleNodeProjectionMapper;

    @Mock
    private MdmModelProjectionMapper mdmModelProjectionMapper;

    @Mock
    private MdmVariantProjectionMapper mdmVariantProjectionMapper;

    @Mock
    private ProjectionIntegrityChecker projectionIntegrityChecker;

    @Mock
    private ConfigurationSyncMetrics configurationSyncMetrics;

    @Mock
    private MdmConsumerMetrics mdmConsumerMetrics;

    @Mock
    private BrandService brandService;

    @Mock
    private CarLineService carLineService;

    @Mock
    private PlatformService platformService;

    @Mock
    private PlantService plantService;

    @Mock
    private ModelService modelService;

    @Mock
    private VariantService variantService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private OptionFamilyService optionFamilyService;

    @Mock
    private OptionCodeService optionCodeService;

    @Mock
    private VehicleNodeService vehicleNodeService;

    @Mock
    private MdmPartService mdmPartService;

    @InjectMocks
    private MdmSyncAppService mdmSyncAppService;

    @Test
    @DisplayName("handleBrandEvent应新增本地不存在的品牌投影")
    void handleBrandEvent_shouldInsertWhenLocalBrandNotExists() {
        // Given
        MdmBrandEvent event = new MdmBrandEvent("CREATED", "mdm-brand-001", 1L, "BRAND001", "新品牌", LocalDateTime.now());

        when(mdmBrandRepository.selectByCode("BRAND001")).thenReturn(null);
        when(mdmBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByCode("BRAND001");
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

        when(mdmBrandRepository.selectByCode("BRAND002")).thenReturn(localBrand);
        when(mdmBrandRepository.updateById(any(Brand.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByCode("BRAND002");
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

        when(mdmBrandRepository.selectByCode("BRAND003")).thenReturn(localBrand);

        // When
        mdmSyncAppService.handleBrandEvent(event);

        // Then
        verify(mdmBrandRepository).selectByCode("BRAND003");
        verify(mdmBrandRepository, never()).updateById(any(Brand.class));
    }

    @Test
    @DisplayName("handleSeriesEvent应新增本地不存在的车系投影")
    void handleSeriesEvent_shouldInsertWhenLocalCarLineNotExists() {
        // Given
        MdmCarLineEvent event = new MdmCarLineEvent("CREATED", "mdm-carline-001", 1L, "CARLINE001", "新车系", "BRAND001", LocalDateTime.now());

        when(mdmCarLineRepository.selectByCode("CARLINE001")).thenReturn(null);
        when(mdmCarLineRepository.insert(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByCode("CARLINE001");
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

        when(mdmCarLineRepository.selectByCode("CARLINE002")).thenReturn(localCarLine);
        when(mdmCarLineRepository.updateById(any(CarLine.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByCode("CARLINE002");
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

        when(mdmCarLineRepository.selectByCode("CARLINE003")).thenReturn(localCarLine);

        // When
        mdmSyncAppService.handleSeriesEvent(event);

        // Then
        verify(mdmCarLineRepository).selectByCode("CARLINE003");
        verify(mdmCarLineRepository, never()).updateById(any(CarLine.class));
    }

    @Test
    @DisplayName("handlePlatformEvent应新增本地不存在的平台投影")
    void handlePlatformEvent_shouldInsertWhenLocalPlatformNotExists() {
        // Given
        MdmPlatformEvent event = new MdmPlatformEvent("CREATED", "mdm-platform-001", 1L, "PLATFORM001", "新平台", LocalDateTime.now());

        when(mdmPlatformRepository.selectByCode("PLATFORM001")).thenReturn(null);
        when(mdmPlatformRepository.insert(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByCode("PLATFORM001");
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

        when(mdmPlatformRepository.selectByCode("PLATFORM002")).thenReturn(localPlatform);
        when(mdmPlatformRepository.updateById(any(Platform.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByCode("PLATFORM002");
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

        when(mdmPlatformRepository.selectByCode("PLATFORM003")).thenReturn(localPlatform);

        // When
        mdmSyncAppService.handlePlatformEvent(event);

        // Then
        verify(mdmPlatformRepository).selectByCode("PLATFORM003");
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
        verify(brandService, never()).listAll(anyInt(), anyInt(), any());
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
        verify(carLineService, never()).listAll(anyInt(), anyInt(), any(), any());
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
        verify(platformService, never()).listAll(anyInt(), anyInt(), any());
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
        when(mdmVehicleNodeRepository.countBySource(SourceType.MDM)).thenReturn(1L);
        when(mdmPartRepository.countBySource(SourceType.MDM)).thenReturn(1L);

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
        verify(mdmVehicleNodeRepository).countBySource(SourceType.MDM);
        verify(mdmPartRepository).countBySource(SourceType.MDM);
    }

    @Test
    @DisplayName("handleConfigurationEvent创建事件应经统一Projection Mapper apply")
    void handleConfigurationEvent_shouldApplyProjectionForCreatedEvent() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("CREATED", "mdm-cfg-001", 1L, "CFG001",
                "配置1", "配置1本地化", "VAR001", "desc", LocalDateTime.now());
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CFG001").name("配置1").nameLocal("配置1本地化").variantCode("VAR001")
                .externalRefId("mdm-cfg-001").externalVersion(1L).build();
        when(mdmConfigurationProjectionMapper.fromEvent(event)).thenReturn(command);

        // When
        mdmSyncAppService.handleConfigurationEvent(event);

        // Then
        verify(mdmConfigurationProjectionMapper).fromEvent(event);
        verify(mdmConfigurationProjectionMapper).apply(command);
        verify(mdmConfigurationProjectionMapper, never()).handleDeletion(any());
    }

    @Test
    @DisplayName("handleConfigurationEvent删除事件应经统一Projection Mapper逻辑删除")
    void handleConfigurationEvent_shouldHandleDeletionForDeletedEvent() {
        // Given
        MdmConfigurationEvent event = new MdmConfigurationEvent("DELETED", "mdm-cfg-001", 2L, "CFG001",
                null, null, null, null, LocalDateTime.now());

        // When
        mdmSyncAppService.handleConfigurationEvent(event);

        // Then
        verify(mdmConfigurationProjectionMapper).handleDeletion(event);
        verify(mdmConfigurationProjectionMapper, never()).apply(any());
    }

    @Test
    @DisplayName("bootstrapConfiguration应经统一Projection Mapper写入并执行完整性检查")
    void bootstrapConfiguration_shouldUseProjectionMapperAndRunIntegrityCheck() {
        // Given
        when(mdmConfigurationRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        ConfigurationResponse snapshot = ConfigurationResponse.builder()
                .id(1001L).code("CFG001").name("配置1").nameLocal("配置1本地化")
                .variantCode("VAR001").description("desc").sourceId("mdm-cfg-001").version(1)
                .build();
        ConfigurationPageResponse pageResponse = ConfigurationPageResponse.builder()
                .total(1L)
                .rows(java.util.Collections.singletonList(snapshot))
                .build();
        when(configurationService.listAll(anyInt(), anyInt(), any(), any())).thenReturn(pageResponse);
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CFG001").name("配置1").nameLocal("配置1本地化").variantCode("VAR001")
                .externalRefId("mdm-cfg-001").externalVersion(1L).build();
        when(mdmConfigurationProjectionMapper.fromSnapshot(snapshot)).thenReturn(command);

        // When
        mdmSyncAppService.bootstrapConfiguration();

        // Then
        verify(configurationService).listAll(anyInt(), anyInt(), any(), any());
        verify(mdmConfigurationProjectionMapper).fromSnapshot(snapshot);
        verify(mdmConfigurationProjectionMapper).apply(any(ConfigurationProjectionCommand.class));
        verify(projectionIntegrityChecker).check();
    }

    @Test
    @DisplayName("handleModelEvent创建事件应经统一Projection Mapper apply")
    void handleModelEvent_shouldApplyProjectionForCreatedEvent() {
        // Given
        MdmModelEvent event = new MdmModelEvent("CREATED", "mdm-model-001", 1L, "MODEL001", "新车型", "PLATFORM001", "CARLINE001", LocalDateTime.now());
        ModelProjectionCommand command = ModelProjectionCommand.builder()
                .code("MODEL001").name("新车型")
                .platformCode("PLATFORM001").carLineCode("CARLINE001")
                .externalRefId("mdm-model-001").externalVersion(1L).build();
        when(mdmModelProjectionMapper.fromEvent(event)).thenReturn(command);

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelProjectionMapper).fromEvent(event);
        verify(mdmModelProjectionMapper).apply(command);
        verify(mdmModelProjectionMapper, never()).handleDeletion(any());
    }

    @Test
    @DisplayName("handleModelEvent删除/失效事件应经统一Projection Mapper逻辑删除")
    void handleModelEvent_shouldHandleDeletionForDeletedEvent() {
        // Given
        MdmModelEvent event = new MdmModelEvent("DELETED", "mdm-model-002", 2L, "MODEL002", null, null, null, LocalDateTime.now());

        // When
        mdmSyncAppService.handleModelEvent(event);

        // Then
        verify(mdmModelProjectionMapper).handleDeletion(event);
        verify(mdmModelProjectionMapper, never()).apply(any());
    }

    @Test
    @DisplayName("bootstrapModel应经统一Projection Mapper写入")
    void bootstrapModel_shouldUseProjectionMapper() {
        // Given
        when(mdmModelRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        ModelResponse snapshot = ModelResponse.builder()
                .id(1001L).code("MODEL001").name("新车型").nameLocal("新车型本地化")
                .platformCode("PLATFORM001").carLineCode("CARLINE001").sourceId("mdm-model-001").version(1)
                .build();
        ModelPageResponse pageResponse = ModelPageResponse.builder()
                .total(1L)
                .rows(java.util.Collections.singletonList(snapshot))
                .build();
        when(modelService.listAll(anyInt(), anyInt(), any(), any(), any())).thenReturn(pageResponse);
        ModelProjectionCommand command = ModelProjectionCommand.builder()
                .code("MODEL001").name("新车型")
                .platformCode("PLATFORM001").carLineCode("CARLINE001")
                .externalRefId("mdm-model-001").externalVersion(1L).build();
        when(mdmModelProjectionMapper.fromSnapshot(snapshot)).thenReturn(command);

        // When
        mdmSyncAppService.bootstrapModel();

        // Then
        verify(modelService).listAll(anyInt(), anyInt(), any(), any(), any());
        verify(mdmModelProjectionMapper).fromSnapshot(snapshot);
        verify(mdmModelProjectionMapper).apply(any(ModelProjectionCommand.class));
    }

    @Test
    @DisplayName("handleVariantEvent创建事件应经统一Projection Mapper apply")
    void handleVariantEvent_shouldApplyProjectionForCreatedEvent() {
        // Given
        MdmVariantEvent event = new MdmVariantEvent("CREATED", "mdm-var-001", 1L, "VAR001", "新版本", "MODEL001", LocalDateTime.now());
        VariantProjectionCommand command = VariantProjectionCommand.builder()
                .code("VAR001").name("新版本").modelCode("MODEL001")
                .externalRefId("mdm-var-001").externalVersion(1L).build();
        when(mdmVariantProjectionMapper.fromEvent(event)).thenReturn(command);

        // When
        mdmSyncAppService.handleVariantEvent(event);

        // Then
        verify(mdmVariantProjectionMapper).fromEvent(event);
        verify(mdmVariantProjectionMapper).apply(command);
        verify(mdmVariantProjectionMapper, never()).handleDeletion(any());
    }

    @Test
    @DisplayName("handleVariantEvent删除/失效事件应经统一Projection Mapper逻辑删除")
    void handleVariantEvent_shouldHandleDeletionForDeletedEvent() {
        // Given
        MdmVariantEvent event = new MdmVariantEvent("DELETED", "mdm-var-002", 2L, "VAR002", null, "MODEL001", LocalDateTime.now());

        // When
        mdmSyncAppService.handleVariantEvent(event);

        // Then
        verify(mdmVariantProjectionMapper).handleDeletion(event);
        verify(mdmVariantProjectionMapper, never()).apply(any());
    }

    @Test
    @DisplayName("handleOptionFamilyEvent应新增本地不存在的选项族投影")
    void handleOptionFamilyEvent_shouldInsertWhenLocalOptionFamilyNotExists() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("CREATED", "mdm-of-001", 1L, "OF001",
                "选装族1", "Option Family 1", "EXTERIOR", LocalDateTime.now());

        when(mdmOptionFamilyRepository.selectByCode("OF001")).thenReturn(null);
        when(mdmOptionFamilyRepository.insert(any(OptionFamily.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByCode("OF001");
        verify(mdmOptionFamilyRepository).insert(any(OptionFamily.class));
    }

    @Test
    @DisplayName("handleOptionFamilyEvent应更新本地已存在且版本更高的选项族投影")
    void handleOptionFamilyEvent_shouldUpdateWhenLocalOptionFamilyExistsAndVersionHigher() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("UPDATED", "mdm-of-002", 2L, "OF002",
                "更新后的选装族", "Updated Option Family", "INTERIOR", LocalDateTime.now());

        OptionFamily localOptionFamily = OptionFamily.builder()
                .id(1L)
                .code("OF002")
                .name("原始选装族")
                .nameLocal("Original Option Family")
                .type("EXTERIOR")
                .source(SourceType.MDM.name())
                .externalRefId("mdm-of-002")
                .externalVersion(1L)
                .build();

        when(mdmOptionFamilyRepository.selectByCode("OF002")).thenReturn(localOptionFamily);
        when(mdmOptionFamilyRepository.updateById(any(OptionFamily.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByCode("OF002");
        verify(mdmOptionFamilyRepository).updateById(any(OptionFamily.class));
    }

    @Test
    @DisplayName("handleOptionFamilyEvent应忽略版本不高于本地的选项族事件")
    void handleOptionFamilyEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmOptionFamilyEvent event = new MdmOptionFamilyEvent("UPDATED", "mdm-of-003", 1L, "OF003",
                "旧版本选装族", "Old Option Family", "EXTERIOR", LocalDateTime.now());

        OptionFamily localOptionFamily = OptionFamily.builder()
                .id(1L)
                .code("OF003")
                .name("本地选装族")
                .nameLocal("Local Option Family")
                .type("EXTERIOR")
                .source(SourceType.MDM.name())
                .externalRefId("mdm-of-003")
                .externalVersion(2L)
                .build();

        when(mdmOptionFamilyRepository.selectByCode("OF003")).thenReturn(localOptionFamily);

        // When
        mdmSyncAppService.handleOptionFamilyEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectByCode("OF003");
        verify(mdmOptionFamilyRepository, never()).updateById(any(OptionFamily.class));
    }

    @Test
    @DisplayName("handleOptionCodeEvent应新增本地不存在的选项值投影")
    void handleOptionCodeEvent_shouldInsertWhenLocalOptionCodeNotExists() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("CREATED", "mdm-oc-001", 1L, "OC001",
                "OF001", "选装值1", "Option Code 1", LocalDateTime.now());

        when(mdmOptionFamilyRepository.selectOptionCodeByCode("OC001")).thenReturn(null);
        when(mdmOptionFamilyRepository.insertOptionCode(any(OptionCode.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode("OC001");
        verify(mdmOptionFamilyRepository).insertOptionCode(any(OptionCode.class));
    }

    @Test
    @DisplayName("handleOptionCodeEvent应更新本地已存在且版本更高的选项值投影")
    void handleOptionCodeEvent_shouldUpdateWhenLocalOptionCodeExistsAndVersionHigher() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("UPDATED", "mdm-oc-002", 2L, "OC002",
                "OF002", "更新后的选装值", "Updated Option Code", LocalDateTime.now());

        OptionCode localOptionCode = OptionCode.builder()
                .id(1L)
                .code("OC002")
                .optionFamilyCode("OF001")
                .name("原始选装值")
                .nameLocal("Original Option Code")
                .source(SourceType.MDM.name())
                .externalRefId("mdm-oc-002")
                .externalVersion(1L)
                .build();

        when(mdmOptionFamilyRepository.selectOptionCodeByCode("OC002")).thenReturn(localOptionCode);
        when(mdmOptionFamilyRepository.updateOptionCodeById(any(OptionCode.class))).thenReturn(1);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode("OC002");
        verify(mdmOptionFamilyRepository).updateOptionCodeById(any(OptionCode.class));
    }

    @Test
    @DisplayName("handleOptionCodeEvent应忽略版本不高于本地的选项值事件")
    void handleOptionCodeEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmOptionCodeEvent event = new MdmOptionCodeEvent("UPDATED", "mdm-oc-003", 1L, "OC003",
                "OF003", "旧版本选装值", "Old Option Code", LocalDateTime.now());

        OptionCode localOptionCode = OptionCode.builder()
                .id(1L)
                .code("OC003")
                .optionFamilyCode("OF003")
                .name("本地选装值")
                .nameLocal("Local Option Code")
                .source(SourceType.MDM.name())
                .externalRefId("mdm-oc-003")
                .externalVersion(2L)
                .build();

        when(mdmOptionFamilyRepository.selectOptionCodeByCode("OC003")).thenReturn(localOptionCode);

        // When
        mdmSyncAppService.handleOptionCodeEvent(event);

        // Then
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode("OC003");
        verify(mdmOptionFamilyRepository, never()).updateOptionCodeById(any(OptionCode.class));
    }

    @Test
    @DisplayName("handlePartEvent应新增本地不存在的零件投影")
    void handlePartEvent_shouldInsertWhenLocalPartNotExists() {
        // Given
        MdmPartEvent event = new MdmPartEvent("CREATED", "mdm-part-001", 1L, "PART001",
                "零件1", "NORMAL", "NODE001", "SUPPLIER001", true, true, true, "PRODUCTION", LocalDateTime.now());

        when(mdmPartRepository.selectByCode("PART001")).thenReturn(null);
        when(mdmPartRepository.insert(any(Part.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePartEvent(event);

        // Then
        verify(mdmPartRepository).selectByCode("PART001");
        verify(mdmPartRepository).insert(any(Part.class));
    }

    @Test
    @DisplayName("handlePartEvent应更新本地已存在且版本更高的零件投影")
    void handlePartEvent_shouldUpdateWhenLocalPartExistsAndVersionHigher() {
        // Given
        MdmPartEvent event = new MdmPartEvent("UPDATED", "mdm-part-002", 2L, "PART002",
                "更新后的零件", "NORMAL", "NODE002", "SUPPLIER002", true, true, true, "PRODUCTION", LocalDateTime.now());

        Part localPart = Part.builder()
                .id(1L)
                .code("PART002")
                .name("原始零件")
                .source(SourceType.MDM)
                .externalRefId("mdm-part-002")
                .externalVersion(1L)
                .build();

        when(mdmPartRepository.selectByCode("PART002")).thenReturn(localPart);
        when(mdmPartRepository.updateById(any(Part.class))).thenReturn(1);

        // When
        mdmSyncAppService.handlePartEvent(event);

        // Then
        verify(mdmPartRepository).selectByCode("PART002");
        verify(mdmPartRepository).updateById(any(Part.class));
    }

    @Test
    @DisplayName("handlePartEvent应忽略版本不高于本地的零件事件")
    void handlePartEvent_shouldIgnoreWhenVersionNotHigher() {
        // Given
        MdmPartEvent event = new MdmPartEvent("UPDATED", "mdm-part-003", 1L, "PART003",
                "旧版本零件", "NORMAL", "NODE003", "SUPPLIER003", true, true, true, "PRODUCTION", LocalDateTime.now());

        Part localPart = Part.builder()
                .id(1L)
                .code("PART003")
                .name("本地零件")
                .source(SourceType.MDM)
                .externalRefId("mdm-part-003")
                .externalVersion(2L)
                .build();

        when(mdmPartRepository.selectByCode("PART003")).thenReturn(localPart);

        // When
        mdmSyncAppService.handlePartEvent(event);

        // Then
        verify(mdmPartRepository).selectByCode("PART003");
        verify(mdmPartRepository, never()).updateById(any(Part.class));
    }

    @Test
    @DisplayName("bootstrapPart应跳过当本地已有MDM零件数据时")
    void bootstrapPart_shouldSkipWhenLocalMdmPartsExist() {
        // Given
        when(mdmPartRepository.countBySource(SourceType.MDM)).thenReturn(5L);

        // When
        mdmSyncAppService.bootstrapPart();

        // Then
        verify(mdmPartRepository).countBySource(SourceType.MDM);
        verify(mdmPartService, never()).snapshot(anyBoolean(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("handleVehicleNodeEvent应经统一投影Mapper新增本地不存在的车载节点投影")
    void handleVehicleNodeEvent_shouldInsertWhenLocalVehicleNodeNotExists() {
        // Given
        MdmVehicleNodeEvent event = new MdmVehicleNodeEvent("CREATED", "mdm-vn-001", 1L, "VN001",
                "车载节点1", "Vehicle Node 1", "CATEGORY001", "FUNC001",
                "TYPE001", "OTA001", true, 10, LocalDateTime.now());

        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("VN001").name("车载节点1").externalRefId("mdm-vn-001").externalVersion(1L).build();
        when(mdmVehicleNodeProjectionMapper.fromEvent(event)).thenReturn(command);

        // When
        mdmSyncAppService.handleVehicleNodeEvent(event);

        // Then
        verify(mdmVehicleNodeProjectionMapper).fromEvent(event);
        verify(mdmVehicleNodeProjectionMapper).apply(command);
        verify(mdmVehicleNodeRepository, never()).insert(any(VehicleNode.class));
    }

    @Test
    @DisplayName("handleVehicleNodeEvent应透传hsmCapability/deviceCategory到投影命令（CR-049）")
    void handleVehicleNodeEvent_shouldMapHsmCapabilityAndDeviceCategory() {
        // Given
        MdmVehicleNodeEvent event = new MdmVehicleNodeEvent("CREATED", "mdm-vn-006", 1L, "VN006",
                "车载节点6", "Vehicle Node 6", "CCU", "FUNC001",
                "TYPE001", "OTA001", true, 10, "HSM_FULL", LocalDateTime.now());

        when(mdmVehicleNodeProjectionMapper.fromEvent(event)).thenReturn(
                VehicleNodeProjectionCommand.builder()
                        .code("VN006").name("车载节点6").deviceCategory("CCU").hsmCapability("HSM_FULL")
                        .externalRefId("mdm-vn-006").externalVersion(1L).build());

        // When
        mdmSyncAppService.handleVehicleNodeEvent(event);

        // Then
        verify(mdmVehicleNodeProjectionMapper).fromEvent(event);
        verify(mdmVehicleNodeProjectionMapper).apply(argThat(command ->
                "HSM_FULL".equals(command.getHsmCapability()) && "CCU".equals(command.getDeviceCategory())));
    }

    @Test
    @DisplayName("handleVehicleNodeEvent应更新本地已存在且版本更高的车载节点投影（委托统一Mapper）")
    void handleVehicleNodeEvent_shouldUpdateWhenLocalVehicleNodeExistsAndVersionHigher() {
        // Given
        MdmVehicleNodeEvent event = new MdmVehicleNodeEvent("UPDATED", "mdm-vn-002", 2L, "VN002",
                "更新后的车载节点", "Updated Vehicle Node", "CATEGORY002", "FUNC002",
                "TYPE002", "OTA002", false, 20, LocalDateTime.now());

        VehicleNodeProjectionCommand command = VehicleNodeProjectionCommand.builder()
                .code("VN002").name("更新后的车载节点").externalRefId("mdm-vn-002").externalVersion(2L).build();
        when(mdmVehicleNodeProjectionMapper.fromEvent(event)).thenReturn(command);

        // When
        mdmSyncAppService.handleVehicleNodeEvent(event);

        // Then
        verify(mdmVehicleNodeProjectionMapper).fromEvent(event);
        verify(mdmVehicleNodeProjectionMapper).apply(command);
        verify(mdmVehicleNodeRepository, never()).updateById(any(VehicleNode.class));
    }

    @Test
    @DisplayName("handleVehicleNodeEvent缺失契约字段时由投影Mapper校验抛异常（不写半条投影）")
    void handleVehicleNodeEvent_shouldThrowProjectionExceptionOnInvalidPayload() {
        // Given
        MdmVehicleNodeEvent event = new MdmVehicleNodeEvent("CREATED", null, 1L, "VN007",
                "车载节点7", "Vehicle Node 7", "CATEGORY001", "FUNC001",
                "TYPE001", "OTA001", true, 10, LocalDateTime.now());

        when(mdmVehicleNodeProjectionMapper.fromEvent(event))
                .thenThrow(new VehicleNodeProjectionException("VehicleNode 投影 payload 缺少 externalRefId"));

        // When & Then
        assertThrows(VehicleNodeProjectionException.class, () -> mdmSyncAppService.handleVehicleNodeEvent(event));
        verify(mdmVehicleNodeProjectionMapper, never()).apply(any());
    }

    @Test
    @DisplayName("bootstrapVehicleNode应经统一投影Mapper同步快照（CR-049）")
    void bootstrapVehicleNode_shouldUseProjectionMapper() {
        // Given
        when(mdmVehicleNodeRepository.countBySource(SourceType.MDM)).thenReturn(0L);
        VehicleNodeResponse snapshot = VehicleNodeResponse.builder()
                .nodeCode("CCU_GEN2")
                .name("中央计算单元GEN2")
                .deviceCategory("CCU")
                .hsmCapability("HSM_FULL")
                .externalRefId("mdm-vn-ccu2")
                .externalVersion(12L)
                .build();
        VehicleNodePageResponse pageResponse = new VehicleNodePageResponse();
        pageResponse.setRows(List.of(snapshot));
        when(vehicleNodeService.snapshot(anyInt(), anyInt(), any())).thenReturn(pageResponse);
        when(mdmVehicleNodeProjectionMapper.fromSnapshot(snapshot))
                .thenReturn(VehicleNodeProjectionCommand.builder()
                        .code("CCU_GEN2").deviceCategory("CCU").hsmCapability("HSM_FULL")
                        .externalRefId("mdm-vn-ccu2").externalVersion(12L).build());

        // When
        mdmSyncAppService.bootstrapVehicleNode();

        // Then
        verify(mdmVehicleNodeProjectionMapper).fromSnapshot(snapshot);
        verify(mdmVehicleNodeProjectionMapper).apply(argThat(command ->
                "CCU_GEN2".equals(command.getCode())
                        && "HSM_FULL".equals(command.getHsmCapability())
                        && "CCU".equals(command.getDeviceCategory())));
    }
}
