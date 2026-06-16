package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.service.BrandService;
import net.hwyz.iov.cloud.edd.mdm.api.service.CarLineService;
import net.hwyz.iov.cloud.edd.mdm.api.service.ConfigurationService;
import net.hwyz.iov.cloud.edd.mdm.api.service.ModelService;
import net.hwyz.iov.cloud.edd.mdm.api.service.OptionCodeService;
import net.hwyz.iov.cloud.edd.mdm.api.service.OptionFamilyService;
import net.hwyz.iov.cloud.edd.mdm.api.service.PartService;
import net.hwyz.iov.cloud.edd.mdm.api.service.PlantService;
import net.hwyz.iov.cloud.edd.mdm.api.service.PlatformService;
import net.hwyz.iov.cloud.edd.mdm.api.service.VariantService;
import net.hwyz.iov.cloud.edd.mdm.api.service.VehicleNodeService;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.BrandPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.BrandResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.CarLinePageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.CarLineResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.OptionCodePageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.OptionCodeResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.OptionFamilyPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.OptionFamilyResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PartPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PartResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PlantPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PlantResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PlatformPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.PlatformResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VariantPageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VariantResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodePageResponse;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPartEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPartRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * MDM 同步应用服务类
 * 处理 MDM 事件订阅和 Bootstrap 全量同步
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmSyncAppService {

    private final MdmBrandRepository mdmBrandRepository;
    private final MdmCarLineRepository mdmCarLineRepository;
    private final MdmConfigurationRepository mdmConfigurationRepository;
    private final MdmPlatformRepository mdmPlatformRepository;
    private final MdmModelRepository mdmModelRepository;
    private final MdmPlantRepository mdmPlantRepository;
    private final MdmVariantRepository mdmVariantRepository;
    private final MdmOptionFamilyRepository mdmOptionFamilyRepository;
    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private final MdmPartRepository mdmPartRepository;

    // 使用 MDM 标准 API 接口
    private final BrandService brandService;
    private final CarLineService carLineService;
    private final ConfigurationService configurationService;
    private final ModelService modelService;
    private final PlantService plantService;
    private final PlatformService platformService;
    private final VariantService variantService;
    private final OptionFamilyService optionFamilyService;
    private final OptionCodeService optionCodeService;
    private final VehicleNodeService vehicleNodeService;
    private final PartService partService;

    /**
     * 处理 MDM 品牌事件
     *
     * @param event 品牌事件
     */
    public void handleBrandEvent(MdmBrandEvent event) {
        log.debug("处理 MDM 品牌事件: eventType={}, entityId={}, code={}",
                event.getEventType(), event.getEntityId(), event.getCode());
        Brand localBrand = mdmBrandRepository.selectByExternalRefId(event.getEntityId());
        if (localBrand == null) {
            Brand newBrand = Brand.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmBrandRepository.insert(newBrand);
            log.info("新增 MDM 品牌投影: code={}, name={}", event.getCode(), event.getName());
        } else {
            if (event.getVersion() > localBrand.getExternalVersion()) {
                localBrand.setName(event.getName());
                localBrand.setExternalVersion(event.getVersion());
                localBrand.setLastSyncTime(LocalDateTime.now());
                mdmBrandRepository.updateById(localBrand);
                log.info("更新 MDM 品牌投影: code={}, oldVersion={}, newVersion={}",
                        event.getCode(), localBrand.getExternalVersion(), event.getVersion());
            } else {
                log.debug("忽略 MDM 品牌事件（版本不满足）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localBrand.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 车系事件
     */
    public void handleSeriesEvent(MdmCarLineEvent event) {
        log.info("处理MDM车系事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        CarLine localCarLine = mdmCarLineRepository.selectByExternalRefId(event.getEntityId());
        if (localCarLine == null) {
            CarLine newCarLine = CarLine.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .brandCode(event.getBrandCode())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmCarLineRepository.insert(newCarLine);
            log.info("新增车系: code={}", event.getCode());
        } else {
            if (event.getVersion() > localCarLine.getExternalVersion()) {
                localCarLine.setName(event.getName());
                localCarLine.setBrandCode(event.getBrandCode());
                localCarLine.setExternalVersion(event.getVersion());
                localCarLine.setLastSyncTime(LocalDateTime.now());
                mdmCarLineRepository.updateById(localCarLine);
                log.info("更新车系: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略车系事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localCarLine.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 平台事件
     */
    public void handlePlatformEvent(MdmPlatformEvent event) {
        log.info("处理MDM平台事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        Platform localPlatform = mdmPlatformRepository.selectByExternalRefId(event.getEntityId());
        if (localPlatform == null) {
            Platform newPlatform = Platform.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmPlatformRepository.insert(newPlatform);
            log.info("新增平台: code={}", event.getCode());
        } else {
            if (event.getVersion() > localPlatform.getExternalVersion()) {
                localPlatform.setName(event.getName());
                localPlatform.setExternalVersion(event.getVersion());
                localPlatform.setLastSyncTime(LocalDateTime.now());
                mdmPlatformRepository.updateById(localPlatform);
                log.info("更新平台: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略平台事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localPlatform.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 车型事件
     */
    public void handleModelEvent(MdmModelEvent event) {
        log.info("处理MDM车型事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        Model localModel = mdmModelRepository.selectByExternalRefId(event.getEntityId());
        if (localModel == null) {
            Model newModel = Model.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .platformCode(event.getPlatformCode())
                    .carLineCode(event.getCarLineCode())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmModelRepository.insert(newModel);
            log.info("新增车型: code={}", event.getCode());
        } else {
            if (event.getVersion() > localModel.getExternalVersion()) {
                localModel.setName(event.getName());
                localModel.setPlatformCode(event.getPlatformCode());
                localModel.setCarLineCode(event.getCarLineCode());
                localModel.setExternalVersion(event.getVersion());
                localModel.setLastSyncTime(LocalDateTime.now());
                mdmModelRepository.updateById(localModel);
                log.info("更新车型: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略车型事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localModel.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 版本事件
     */
    public void handleVariantEvent(MdmVariantEvent event) {
        log.info("处理MDM版本事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        Variant localVariant = mdmVariantRepository.selectByExternalRefId(event.getEntityId());
        if (localVariant == null) {
            Variant newVariant = Variant.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .platformCode(event.getPlatformCode())
                    .carLineCode(event.getCarLineCode())
                    .modelCode(event.getModelCode())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmVariantRepository.insert(newVariant);
            log.info("新增版本: code={}", event.getCode());
        } else {
            if (event.getVersion() > localVariant.getExternalVersion()) {
                localVariant.setName(event.getName());
                localVariant.setPlatformCode(event.getPlatformCode());
                localVariant.setCarLineCode(event.getCarLineCode());
                localVariant.setModelCode(event.getModelCode());
                localVariant.setExternalVersion(event.getVersion());
                localVariant.setLastSyncTime(LocalDateTime.now());
                mdmVariantRepository.updateById(localVariant);
                log.info("更新版本: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略版本事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localVariant.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 配置事件
     */
    public void handleConfigurationEvent(MdmConfigurationEvent event) {
        log.info("处理MDM配置事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        Configuration localConfiguration = mdmConfigurationRepository.selectByExternalRefId(event.getEntityId());
        if (localConfiguration == null) {
            Configuration newConfiguration = Configuration.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .nameEn(event.getNameEn())
                    .platformCode(event.getPlatformCode())
                    .carLineCode(event.getCarLineCode())
                    .modelCode(event.getModelCode())
                    .variantCode(event.getVariantCode())
                    .vehicleStageCode(event.getVehicleStageCode())
                    .enable(event.getEnable())
                    .sort(event.getSort())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmConfigurationRepository.insert(newConfiguration);
            log.info("新增配置: code={}", event.getCode());
        } else {
            if (event.getVersion() > localConfiguration.getExternalVersion()) {
                localConfiguration.setName(event.getName());
                localConfiguration.setNameEn(event.getNameEn());
                localConfiguration.setPlatformCode(event.getPlatformCode());
                localConfiguration.setCarLineCode(event.getCarLineCode());
                localConfiguration.setModelCode(event.getModelCode());
                localConfiguration.setVariantCode(event.getVariantCode());
                localConfiguration.setVehicleStageCode(event.getVehicleStageCode());
                localConfiguration.setEnable(event.getEnable());
                localConfiguration.setSort(event.getSort());
                localConfiguration.setExternalVersion(event.getVersion());
                localConfiguration.setLastSyncTime(LocalDateTime.now());
                mdmConfigurationRepository.updateById(localConfiguration);
                log.info("更新配置: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略配置事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localConfiguration.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 选项族事件
     */
    public void handleOptionFamilyEvent(MdmOptionFamilyEvent event) {
        log.info("处理MDM选项族事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        OptionFamily localOptionFamily = mdmOptionFamilyRepository.selectByExternalRefId(event.getEntityId());
        if (localOptionFamily == null) {
            OptionFamily newOptionFamily = OptionFamily.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .nameLocal(event.getNameLocal())
                    .type(event.getType())
                    .source(SourceType.MDM.name())
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmOptionFamilyRepository.insert(newOptionFamily);
            log.info("新增选项族: code={}", event.getCode());
        } else {
            if (event.getVersion() > localOptionFamily.getExternalVersion()) {
                localOptionFamily.setName(event.getName());
                localOptionFamily.setNameLocal(event.getNameLocal());
                localOptionFamily.setType(event.getType());
                localOptionFamily.setExternalVersion(event.getVersion());
                localOptionFamily.setLastSyncTime(LocalDateTime.now());
                mdmOptionFamilyRepository.updateById(localOptionFamily);
                log.info("更新选项族: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略选项族事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localOptionFamily.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 选项值事件
     */
    public void handleOptionCodeEvent(MdmOptionCodeEvent event) {
        log.info("处理MDM选项值事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        OptionCode localOptionCode = mdmOptionFamilyRepository.selectOptionCodeByExternalRefId(event.getEntityId());
        if (localOptionCode == null) {
            OptionCode newOptionCode = OptionCode.builder()
                    .code(event.getCode())
                    .optionFamilyCode(event.getOptionFamilyCode())
                    .name(event.getName())
                    .nameLocal(event.getNameLocal())
                    .source(SourceType.MDM.name())
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmOptionFamilyRepository.insertOptionCode(newOptionCode);
            log.info("新增选项值: code={}", event.getCode());
        } else {
            if (event.getVersion() > localOptionCode.getExternalVersion()) {
                localOptionCode.setOptionFamilyCode(event.getOptionFamilyCode());
                localOptionCode.setName(event.getName());
                localOptionCode.setNameLocal(event.getNameLocal());
                localOptionCode.setExternalVersion(event.getVersion());
                localOptionCode.setLastSyncTime(LocalDateTime.now());
                mdmOptionFamilyRepository.updateOptionCodeById(localOptionCode);
                log.info("更新选项值: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略选项值事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localOptionCode.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 工厂事件
     */
    public void handlePlantEvent(MdmPlantEvent event) {
        log.debug("处理 MDM 工厂事件: eventType={}, entityId={}, code={}",
                event.getEventType(), event.getEntityId(), event.getCode());
        Plant localPlant = mdmPlantRepository.selectByExternalRefId(event.getEntityId());
        if (localPlant == null) {
            Plant newPlant = Plant.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmPlantRepository.insert(newPlant);
            log.info("新增 MDM 工厂投影: code={}, name={}", event.getCode(), event.getName());
        } else {
            if (event.getVersion() > localPlant.getExternalVersion()) {
                localPlant.setName(event.getName());
                localPlant.setExternalVersion(event.getVersion());
                localPlant.setLastSyncTime(LocalDateTime.now());
                mdmPlantRepository.update(localPlant);
                log.info("更新 MDM 工厂投影: code={}, oldVersion={}, newVersion={}",
                        event.getCode(), localPlant.getExternalVersion(), event.getVersion());
            } else {
                log.debug("忽略 MDM 工厂事件（版本不满足）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localPlant.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 车载节点事件
     */
    public void handleVehicleNodeEvent(MdmVehicleNodeEvent event) {
        log.info("处理MDM车载节点事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        VehicleNode localVehicleNode = mdmVehicleNodeRepository.selectByExternalRefId(event.getEntityId());
        if (localVehicleNode == null) {
            VehicleNode newVehicleNode = VehicleNode.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .nameEn(event.getNameEn())
                    .type(event.getType())
                    .deviceItem(event.getDeviceItem())
                    .funcDomain(event.getFuncDomain())
                    .nodeType(event.getNodeType())
                    .otaSupport(event.getOtaSupport())
                    .core(event.getCore())
                    .sort(event.getSort())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmVehicleNodeRepository.insert(newVehicleNode);
            log.info("新增车载节点: code={}", event.getCode());
        } else {
            if (event.getVersion() > localVehicleNode.getExternalVersion()) {
                localVehicleNode.setName(event.getName());
                localVehicleNode.setNameEn(event.getNameEn());
                localVehicleNode.setType(event.getType());
                localVehicleNode.setDeviceItem(event.getDeviceItem());
                localVehicleNode.setFuncDomain(event.getFuncDomain());
                localVehicleNode.setNodeType(event.getNodeType());
                localVehicleNode.setOtaSupport(event.getOtaSupport());
                localVehicleNode.setCore(event.getCore());
                localVehicleNode.setSort(event.getSort());
                localVehicleNode.setExternalVersion(event.getVersion());
                localVehicleNode.setLastSyncTime(LocalDateTime.now());
                mdmVehicleNodeRepository.updateById(localVehicleNode);
                log.info("更新车载节点: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略车载节点事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localVehicleNode.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 零件事件
     * <p>
     * 幂等策略：按零件编号(code)查找本地记录，按externalVersion判断是否需要更新。
     * 使用code而非MDM主键作为查找条件，确保Bootstrap全量同步和Kafka增量同步的幂等一致性。
     * </p>
     */
    public void handlePartEvent(MdmPartEvent event) {
        log.info("处理MDM零件事件: code={}, version={}", event.getCode(), event.getVersion());
        Part localPart = mdmPartRepository.selectByCode(event.getCode());
        if (localPart == null) {
            Part newPart = Part.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .partType(event.getPartType())
                    .vehicleNodeCode(event.getVehicleNodeCode())
                    .supplierCode(event.getSupplierCode())
                    .isSoftware(event.getIsSoftware())
                    .fotaUpgradeable(event.getFotaUpgradeable())
                    .isAccuratelyTraced(event.getIsAccuratelyTraced())
                    .status(event.getStatus())
                    .source(SourceType.MDM)
                    .externalRefId(event.getEntityId())
                    .externalVersion(event.getVersion())
                    .lastSyncTime(LocalDateTime.now())
                    .build();
            mdmPartRepository.insert(newPart);
            log.info("新增零件: code={}", event.getCode());
        } else {
            if (event.getVersion() > localPart.getExternalVersion()) {
                localPart.setName(event.getName());
                localPart.setPartType(event.getPartType());
                localPart.setVehicleNodeCode(event.getVehicleNodeCode());
                localPart.setSupplierCode(event.getSupplierCode());
                localPart.setIsSoftware(event.getIsSoftware());
                localPart.setFotaUpgradeable(event.getFotaUpgradeable());
                localPart.setIsAccuratelyTraced(event.getIsAccuratelyTraced());
                localPart.setStatus(event.getStatus());
                localPart.setExternalRefId(event.getEntityId());
                localPart.setExternalVersion(event.getVersion());
                localPart.setLastSyncTime(LocalDateTime.now());
                mdmPartRepository.updateById(localPart);
                log.info("更新零件: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略零件事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localPart.getExternalVersion());
            }
        }
    }

    /**
     * Bootstrap 全量同步品牌数据
     */
    public void bootstrapBrand() {
        log.info("开始 Bootstrap 品牌数据同步");
        long count = mdmBrandRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 品牌记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    BrandPageResponse pageResponse = brandService.listAll(page, pageSize, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (BrandResponse brandData : pageResponse.getRows()) {
                        // 先按 code 查询是否已存在
                        Brand existingBrand = mdmBrandRepository.selectByCode(brandData.getCode());
                        if (existingBrand == null) {
                            Brand brand = Brand.builder()
                                    .code(brandData.getCode())
                                    .name(brandData.getName())
                                    .enable(true)
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(brandData.getSourceId())
                                    .externalVersion(brandData.getVersion() != null ? brandData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(brandData.getModifyTime()))
                                    .build();
                            mdmBrandRepository.insert(brand);
                            log.info("Bootstrap 新增 MDM 品牌投影: code={}", brandData.getCode());
                        } else {
                            existingBrand.setName(brandData.getName());
                            existingBrand.setSource(SourceType.MDM);
                            existingBrand.setExternalRefId(brandData.getSourceId());
                            existingBrand.setExternalVersion(brandData.getVersion() != null ? brandData.getVersion().longValue() : 0L);
                            existingBrand.setLastSyncTime(convertToLocalDateTime(brandData.getModifyTime()));
                            mdmBrandRepository.updateById(existingBrand);
                            log.info("Bootstrap 更新 MDM 品牌投影: code={}", brandData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 品牌数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 品牌数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 品牌数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车系数据
     */
    public void bootstrapSeries() {
        log.info("开始 Bootstrap 车系数据同步");
        long count = mdmCarLineRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 车系记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    CarLinePageResponse pageResponse = carLineService.listAll(page, pageSize, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (CarLineResponse carLineData : pageResponse.getRows()) {
                        CarLine existingCarLine = mdmCarLineRepository.selectByCode(carLineData.getCode());
                        if (existingCarLine == null) {
                            CarLine carLine = CarLine.builder()
                                    .code(carLineData.getCode())
                                    .name(carLineData.getName())
                                    .brandCode(carLineData.getBrandCode())
                                    .enable(true)
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(carLineData.getSourceId())
                                    .externalVersion(carLineData.getVersion() != null ? carLineData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(carLineData.getModifyTime()))
                                    .build();
                            mdmCarLineRepository.insert(carLine);
                            log.info("Bootstrap 新增 MDM 车系投影: code={}", carLineData.getCode());
                        } else {
                            existingCarLine.setName(carLineData.getName());
                            existingCarLine.setBrandCode(carLineData.getBrandCode());
                            existingCarLine.setSource(SourceType.MDM);
                            existingCarLine.setExternalRefId(carLineData.getSourceId());
                            existingCarLine.setExternalVersion(carLineData.getVersion() != null ? carLineData.getVersion().longValue() : 0L);
                            existingCarLine.setLastSyncTime(convertToLocalDateTime(carLineData.getModifyTime()));
                            mdmCarLineRepository.updateById(existingCarLine);
                            log.info("Bootstrap 更新 MDM 车系投影: code={}", carLineData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 车系数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 车系数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 车系数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步平台数据
     */
    public void bootstrapPlatform() {
        log.info("开始 Bootstrap 平台数据同步");
        long count = mdmPlatformRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 平台记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    PlatformPageResponse pageResponse = platformService.listAll(page, pageSize, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (PlatformResponse platformData : pageResponse.getRows()) {
                        Platform existingPlatform = mdmPlatformRepository.selectByCode(platformData.getCode());
                        if (existingPlatform == null) {
                            Platform platform = Platform.builder()
                                    .code(platformData.getCode())
                                    .name(platformData.getName())
                                    .enable(true)
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(platformData.getSourceId())
                                    .externalVersion(platformData.getVersion() != null ? platformData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(platformData.getModifyTime()))
                                    .build();
                            mdmPlatformRepository.insert(platform);
                            log.info("Bootstrap 新增 MDM 平台投影: code={}", platformData.getCode());
                        } else {
                            existingPlatform.setName(platformData.getName());
                            existingPlatform.setSource(SourceType.MDM);
                            existingPlatform.setExternalRefId(platformData.getSourceId());
                            existingPlatform.setExternalVersion(platformData.getVersion() != null ? platformData.getVersion().longValue() : 0L);
                            existingPlatform.setLastSyncTime(convertToLocalDateTime(platformData.getModifyTime()));
                            mdmPlatformRepository.updateById(existingPlatform);
                            log.info("Bootstrap 更新 MDM 平台投影: code={}", platformData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 平台数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 平台数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 平台数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步工厂数据
     */
    public void bootstrapPlant() {
        log.info("开始 Bootstrap 工厂数据同步");
        long count = mdmPlantRepository.countBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 工厂记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 10;
                boolean hasMore = true;
                while (hasMore) {
                    PlantPageResponse pageResponse = plantService.snapshot(false, page, pageSize);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (PlantResponse plantData : pageResponse.getRows()) {
                        Plant existingPlant = mdmPlantRepository.selectByCode(plantData.getCode());
                        if (existingPlant == null) {
                            Plant plant = Plant.builder()
                                    .code(plantData.getCode())
                                    .name(plantData.getName())
                                    .enable(true)
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(plantData.getSourceId())
                                    .externalVersion(plantData.getVersion() != null ? plantData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(plantData.getModifyTime()))
                                    .build();
                            mdmPlantRepository.insert(plant);
                            log.info("Bootstrap 新增 MDM 工厂投影: code={}", plantData.getCode());
                        } else {
                            existingPlant.setName(plantData.getName());
                            existingPlant.setSource(SourceType.MDM);
                            existingPlant.setExternalRefId(plantData.getSourceId());
                            existingPlant.setExternalVersion(plantData.getVersion() != null ? plantData.getVersion().longValue() : 0L);
                            existingPlant.setLastSyncTime(convertToLocalDateTime(plantData.getModifyTime()));
                            mdmPlantRepository.update(existingPlant);
                            log.info("Bootstrap 更新 MDM 工厂投影: code={}", plantData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 工厂数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 工厂数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 工厂数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车型数据
     */
    public void bootstrapModel() {
        log.info("开始 Bootstrap 车型数据同步");
        long count = mdmModelRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 车型记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    ModelPageResponse pageResponse = modelService.listAll(page, pageSize, null, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (ModelResponse modelData : pageResponse.getRows()) {
                        Model model = Model.builder()
                                .code(modelData.getCode())
                                .name(modelData.getName())
                                .platformCode(modelData.getPlatformCode())
                                .carLineCode(modelData.getCarLineCode())
                                .source(SourceType.MDM)
                                .externalRefId(modelData.getSourceId())
                                .externalVersion(modelData.getVersion() != null ? modelData.getVersion().longValue() : 0L)
                                .lastSyncTime(convertToLocalDateTime(modelData.getModifyTime()))
                                .build();
                        mdmModelRepository.insert(model);
                        log.info("Bootstrap 新增 MDM 车型投影: code={}", modelData.getCode());
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 车型数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 车型数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 车型数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步版本数据
     */
    public void bootstrapVariant() {
        log.info("开始 Bootstrap 版本数据同步");
        long count = mdmVariantRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 版本记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    VariantPageResponse pageResponse = variantService.listAll(page, pageSize, null, null, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (VariantResponse variantData : pageResponse.getRows()) {
                        Variant existingVariant = mdmVariantRepository.selectByCode(variantData.getCode());
                        if (existingVariant == null) {
                            Variant variant = Variant.builder()
                                    .code(variantData.getCode())
                                    .name(variantData.getName())
                                    .platformCode("DEFAULT")
                                    .carLineCode("DEFAULT")
                                    .modelCode(variantData.getModelCode())
                                    .enable(true)
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(variantData.getSourceId())
                                    .externalVersion(variantData.getVersion() != null ? variantData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(variantData.getModifyTime()))
                                    .build();
                            mdmVariantRepository.insert(variant);
                            log.info("Bootstrap 新增 MDM 版本投影: code={}", variantData.getCode());
                        } else {
                            existingVariant.setName(variantData.getName());
                            existingVariant.setModelCode(variantData.getModelCode());
                            existingVariant.setSource(SourceType.MDM);
                            existingVariant.setExternalRefId(variantData.getSourceId());
                            existingVariant.setExternalVersion(variantData.getVersion() != null ? variantData.getVersion().longValue() : 0L);
                            existingVariant.setLastSyncTime(convertToLocalDateTime(variantData.getModifyTime()));
                            mdmVariantRepository.updateById(existingVariant);
                            log.info("Bootstrap 更新 MDM 版本投影: code={}", variantData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 版本数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 版本数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 版本数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步配置数据
     */
    public void bootstrapConfiguration() {
        log.info("开始 Bootstrap 配置数据同步");
        long count = mdmConfigurationRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 配置记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    ConfigurationPageResponse pageResponse = configurationService.listAll(page, pageSize, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (ConfigurationResponse configurationData : pageResponse.getRows()) {
                        // ConfigurationResponse 只含 variantCode，不含 platformCode/carLineCode/modelCode
                        Configuration configuration = Configuration.builder()
                                .code(configurationData.getCode())
                                .name(configurationData.getName())
                                .variantCode(configurationData.getVariantCode())
                                .source(SourceType.MDM)
                                .externalRefId(configurationData.getSourceId())
                                .externalVersion(configurationData.getVersion() != null ? configurationData.getVersion().longValue() : 0L)
                                .lastSyncTime(convertToLocalDateTime(configurationData.getModifyTime()))
                                .build();
                        mdmConfigurationRepository.insert(configuration);
                        log.info("Bootstrap 新增 MDM 配置投影: code={}", configurationData.getCode());
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 配置数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 配置数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 配置数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步选项族数据
     */
    public void bootstrapOptionFamily() {
        log.info("开始 Bootstrap 选项族数据同步");
        long count = mdmOptionFamilyRepository.countBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 选项族记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    OptionFamilyPageResponse pageResponse = optionFamilyService.listAll(page, pageSize, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (OptionFamilyResponse optionFamilyData : pageResponse.getRows()) {
                        OptionFamily existingOptionFamily = mdmOptionFamilyRepository.selectByCode(optionFamilyData.getCode());
                        if (existingOptionFamily == null) {
                            OptionFamily optionFamily = OptionFamily.builder()
                                    .code(optionFamilyData.getCode())
                                    .name(optionFamilyData.getName())
                                    .source(SourceType.MDM.name())
                                    .externalRefId(optionFamilyData.getSourceId())
                                    .externalVersion(optionFamilyData.getVersion() != null ? optionFamilyData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(optionFamilyData.getModifyTime()))
                                    .build();
                            mdmOptionFamilyRepository.insert(optionFamily);
                            log.info("Bootstrap 新增 MDM 选项族投影: code={}", optionFamilyData.getCode());
                        } else {
                            existingOptionFamily.setName(optionFamilyData.getName());
                            existingOptionFamily.setSource(SourceType.MDM.name());
                            existingOptionFamily.setExternalRefId(optionFamilyData.getSourceId());
                            existingOptionFamily.setExternalVersion(optionFamilyData.getVersion() != null ? optionFamilyData.getVersion().longValue() : 0L);
                            existingOptionFamily.setLastSyncTime(convertToLocalDateTime(optionFamilyData.getModifyTime()));
                            mdmOptionFamilyRepository.updateById(existingOptionFamily);
                            log.info("Bootstrap 更新 MDM 选项族投影: code={}", optionFamilyData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 选项族数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 选项族数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 选项族数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步选项值数据
     */
    public void bootstrapOptionCode() {
        log.info("开始 Bootstrap 选项值数据同步");
        long count = mdmOptionFamilyRepository.countOptionCodeBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 选项值记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    OptionCodePageResponse pageResponse = optionCodeService.listAll(page, pageSize, null, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (OptionCodeResponse optionCodeData : pageResponse.getRows()) {
                        OptionCode existingOptionCode = mdmOptionFamilyRepository.selectOptionCodeByCode(optionCodeData.getCode());
                        if (existingOptionCode == null) {
                            OptionCode optionCode = OptionCode.builder()
                                    .code(optionCodeData.getCode())
                                    .optionFamilyCode(optionCodeData.getOptionFamilyCode())
                                    .name(optionCodeData.getName())
                                    .source(SourceType.MDM.name())
                                    .externalRefId(optionCodeData.getSourceId())
                                    .externalVersion(optionCodeData.getVersion() != null ? optionCodeData.getVersion().longValue() : 0L)
                                    .lastSyncTime(convertToLocalDateTime(optionCodeData.getModifyTime()))
                                    .build();
                            mdmOptionFamilyRepository.insertOptionCode(optionCode);
                            log.info("Bootstrap 新增 MDM 选项值投影: code={}", optionCodeData.getCode());
                        } else {
                            existingOptionCode.setOptionFamilyCode(optionCodeData.getOptionFamilyCode());
                            existingOptionCode.setName(optionCodeData.getName());
                            existingOptionCode.setSource(SourceType.MDM.name());
                            existingOptionCode.setExternalRefId(optionCodeData.getSourceId());
                            existingOptionCode.setExternalVersion(optionCodeData.getVersion() != null ? optionCodeData.getVersion().longValue() : 0L);
                            existingOptionCode.setLastSyncTime(convertToLocalDateTime(optionCodeData.getModifyTime()));
                            mdmOptionFamilyRepository.updateOptionCodeById(existingOptionCode);
                            log.info("Bootstrap 更新 MDM 选项值投影: code={}", optionCodeData.getCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 选项值数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 选项值数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 选项值数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车载节点数据
     */
    public void bootstrapVehicleNode() {
        log.info("开始 Bootstrap 车载节点数据同步");
        long count = mdmVehicleNodeRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 车载节点记录（count=0），启动 Bootstrap 同步");
            try {
                int page = 1;
                int pageSize = 100;
                boolean hasMore = true;
                while (hasMore) {
                    VehicleNodePageResponse pageResponse = vehicleNodeService.snapshot(page, pageSize, null);
                    if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    for (VehicleNodeResponse vehicleNodeData : pageResponse.getRows()) {
                        VehicleNode existingVehicleNode = mdmVehicleNodeRepository.selectByCode(vehicleNodeData.getNodeCode());
                        if (existingVehicleNode == null) {
                            VehicleNode vehicleNode = VehicleNode.builder()
                                    .code(vehicleNodeData.getNodeCode())
                                    .name(vehicleNodeData.getName())
                                    .nameEn(vehicleNodeData.getNameLocal())
                                    .type(vehicleNodeData.getNodeType() != null ? vehicleNodeData.getNodeType() : "ECU")
                                    .deviceItem(vehicleNodeData.getDeviceCategory())
                                    .funcDomain(vehicleNodeData.getFunctionalDomain() != null ? vehicleNodeData.getFunctionalDomain() : "GENERAL")
                                    .nodeType(vehicleNodeData.getNodeType() != null ? vehicleNodeData.getNodeType() : "ECU")
                                    .otaSupport(vehicleNodeData.getOtaSupportType() != null ? vehicleNodeData.getOtaSupportType() : "NONE")
                                    .core(vehicleNodeData.getIsCoreNode())
                                    .sort(0)
                                    .source(SourceType.MDM)
                                    .externalRefId(vehicleNodeData.getExternalRefId())
                                    .externalVersion(vehicleNodeData.getExternalVersion())
                                    .lastSyncTime(convertToLocalDateTime(vehicleNodeData.getLastSyncTime()))
                                    .build();
                            mdmVehicleNodeRepository.insert(vehicleNode);
                            log.info("Bootstrap 新增 MDM 车载节点投影: code={}", vehicleNodeData.getNodeCode());
                        } else {
                            existingVehicleNode.setName(vehicleNodeData.getName());
                            existingVehicleNode.setNameEn(vehicleNodeData.getNameLocal());
                            existingVehicleNode.setType(vehicleNodeData.getNodeType() != null ? vehicleNodeData.getNodeType() : "ECU");
                            existingVehicleNode.setDeviceItem(vehicleNodeData.getDeviceCategory());
                            existingVehicleNode.setFuncDomain(vehicleNodeData.getFunctionalDomain() != null ? vehicleNodeData.getFunctionalDomain() : "GENERAL");
                            existingVehicleNode.setNodeType(vehicleNodeData.getNodeType() != null ? vehicleNodeData.getNodeType() : "ECU");
                            existingVehicleNode.setOtaSupport(vehicleNodeData.getOtaSupportType() != null ? vehicleNodeData.getOtaSupportType() : "NONE");
                            existingVehicleNode.setCore(vehicleNodeData.getIsCoreNode());
                            existingVehicleNode.setSource(SourceType.MDM);
                            existingVehicleNode.setExternalRefId(vehicleNodeData.getExternalRefId());
                            existingVehicleNode.setExternalVersion(vehicleNodeData.getExternalVersion());
                            existingVehicleNode.setLastSyncTime(convertToLocalDateTime(vehicleNodeData.getLastSyncTime()));
                            mdmVehicleNodeRepository.updateById(existingVehicleNode);
                            log.info("Bootstrap 更新 MDM 车载节点投影: code={}", vehicleNodeData.getNodeCode());
                        }
                    }
                    if (pageResponse.getRows().size() < pageSize) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }
                log.info("Bootstrap 车载节点数据同步完成");
            } catch (Exception e) {
                log.error("Bootstrap 车载节点数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 车载节点数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步零件数据
     */
    public void bootstrapPart() {
        log.info("开始 Bootstrap 零件数据同步");
        try {
            long existingCount = mdmPartRepository.countBySource(SourceType.MDM);
            if (existingCount > 0) {
                log.info("本地已存在 {} 条 MDM 零件数据，跳过 Bootstrap 同步", existingCount);
                return;
            }

            int page = 1;
            int pageSize = 10;
            boolean hasMore = true;
            int insertCount = 0;
            int updateCount = 0;
            int skipCount = 0;

            while (hasMore) {
                PartPageResponse pageResponse = partService.snapshot(false, page, pageSize);
                if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                    hasMore = false;
                    break;
                }
                for (PartResponse partData : pageResponse.getRows()) {
                    Part localPart = mdmPartRepository.selectByExternalRefId(partData.getSourceId());
                    if (localPart == null) {
                        Part newPart = Part.builder()
                                .code(partData.getCode())
                                .name(partData.getName())
                                .partType(partData.getPartType())
                                .vehicleNodeCode(partData.getVehicleNodeCode())
                                .supplierCode(partData.getSupplierCode())
                                .isSoftware(partData.getIsSoftware())
                                .fotaUpgradeable(partData.getFotaUpgradeable())
                                .isAccuratelyTraced(partData.getIsAccuratelyTraced())
                                .status(partData.getStatus())
                                .source(SourceType.MDM)
                                .externalRefId(partData.getSourceId())
                                .externalVersion(partData.getVersion() != null ? partData.getVersion().longValue() : 0L)
                                .lastSyncTime(convertToLocalDateTime(partData.getModifyTime()))
                                .build();
                        mdmPartRepository.insert(newPart);
                        log.info("Bootstrap 新增 MDM 零件投影: code={}", partData.getCode());
                        insertCount++;
                    } else {
                        Long remoteVersion = partData.getVersion() != null ? partData.getVersion().longValue() : 0L;
                        if (remoteVersion > localPart.getExternalVersion()) {
                            localPart.setCode(partData.getCode());
                            localPart.setName(partData.getName());
                            localPart.setPartType(partData.getPartType());
                            localPart.setVehicleNodeCode(partData.getVehicleNodeCode());
                            localPart.setSupplierCode(partData.getSupplierCode());
                            localPart.setIsSoftware(partData.getIsSoftware());
                            localPart.setFotaUpgradeable(partData.getFotaUpgradeable());
                            localPart.setIsAccuratelyTraced(partData.getIsAccuratelyTraced());
                            localPart.setStatus(partData.getStatus());
                            localPart.setExternalVersion(remoteVersion);
                            localPart.setLastSyncTime(convertToLocalDateTime(partData.getModifyTime()));
                            mdmPartRepository.updateById(localPart);
                            log.info("Bootstrap 更新 MDM 零件投影: code={}, version={}", partData.getCode(), remoteVersion);
                            updateCount++;
                        } else {
                            skipCount++;
                        }
                    }
                }
                if (pageResponse.getRows().size() < pageSize) {
                    hasMore = false;
                } else {
                    page++;
                }
            }
            log.info("Bootstrap 零件数据同步完成: 新增={}, 更新={}, 跳过={}", insertCount, updateCount, skipCount);
        } catch (Exception e) {
            log.error("Bootstrap 零件数据同步失败", e);
        }
    }

    /**
     * 强制全量同步零件数据
     */
    public void forceBootstrapPart() {
        log.info("开始强制 Bootstrap 零件数据同步");
        try {
            int page = 1;
            int pageSize = 10;
            boolean hasMore = true;
            int insertCount = 0;
            int updateCount = 0;
            int skipCount = 0;

            while (hasMore) {
                PartPageResponse pageResponse = partService.snapshot(false, page, pageSize);
                if (pageResponse == null || pageResponse.getRows() == null || pageResponse.getRows().isEmpty()) {
                    hasMore = false;
                    break;
                }
                for (PartResponse partData : pageResponse.getRows()) {
                    Part localPart = mdmPartRepository.selectByExternalRefId(partData.getSourceId());
                    if (localPart == null) {
                        Part newPart = Part.builder()
                                .code(partData.getCode())
                                .name(partData.getName())
                                .partType(partData.getPartType())
                                .vehicleNodeCode(partData.getVehicleNodeCode())
                                .supplierCode(partData.getSupplierCode())
                                .isSoftware(partData.getIsSoftware())
                                .fotaUpgradeable(partData.getFotaUpgradeable())
                                .isAccuratelyTraced(partData.getIsAccuratelyTraced())
                                .status(partData.getStatus())
                                .source(SourceType.MDM)
                                .externalRefId(partData.getSourceId())
                                .externalVersion(partData.getVersion() != null ? partData.getVersion().longValue() : 0L)
                                .lastSyncTime(convertToLocalDateTime(partData.getModifyTime()))
                                .build();
                        mdmPartRepository.insert(newPart);
                        log.info("强制 Bootstrap 新增 MDM 零件投影: code={}", partData.getCode());
                        insertCount++;
                    } else {
                        Long remoteVersion = partData.getVersion() != null ? partData.getVersion().longValue() : 0L;
                        if (remoteVersion > localPart.getExternalVersion()) {
                            localPart.setCode(partData.getCode());
                            localPart.setName(partData.getName());
                            localPart.setPartType(partData.getPartType());
                            localPart.setVehicleNodeCode(partData.getVehicleNodeCode());
                            localPart.setSupplierCode(partData.getSupplierCode());
                            localPart.setIsSoftware(partData.getIsSoftware());
                            localPart.setFotaUpgradeable(partData.getFotaUpgradeable());
                            localPart.setIsAccuratelyTraced(partData.getIsAccuratelyTraced());
                            localPart.setStatus(partData.getStatus());
                            localPart.setExternalVersion(remoteVersion);
                            localPart.setLastSyncTime(convertToLocalDateTime(partData.getModifyTime()));
                            mdmPartRepository.updateById(localPart);
                            log.info("强制 Bootstrap 更新 MDM 零件投影: code={}, version={}", partData.getCode(), remoteVersion);
                            updateCount++;
                        } else {
                            skipCount++;
                        }
                    }
                }
                if (pageResponse.getRows().size() < pageSize) {
                    hasMore = false;
                } else {
                    page++;
                }
            }
            log.info("强制 Bootstrap 零件数据同步完成: 新增={}, 更新={}, 跳过={}", insertCount, updateCount, skipCount);
        } catch (Exception e) {
            log.error("强制 Bootstrap 零件数据同步失败", e);
        }
    }

    /**
     * Bootstrap 全量同步所有数据
     */
    public void bootstrapAll() {
        log.info("开始Bootstrap全量数据同步");
        bootstrapBrand();
        bootstrapSeries();
        bootstrapPlatform();
        bootstrapPlant();
        bootstrapModel();
        bootstrapVariant();
        bootstrapConfiguration();
        bootstrapOptionFamily();
        bootstrapOptionCode();
        bootstrapVehicleNode();
        bootstrapPart();
        log.info("Bootstrap全量数据同步完成");
    }

    /**
     * 将 Date 转换为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
