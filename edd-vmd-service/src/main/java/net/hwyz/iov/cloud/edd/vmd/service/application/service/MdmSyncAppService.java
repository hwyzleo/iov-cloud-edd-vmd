package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmBrandQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmCarLineQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmConfigurationQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmModelQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPlantQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmPlatformQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmOptionFamilyQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmOptionCodeQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.gateway.http.MdmVariantQueryClient;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionFamilyEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmOptionCodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final MdmBrandQueryClient mdmBrandQueryClient;
    private final MdmCarLineQueryClient mdmCarLineQueryClient;
    private final MdmConfigurationQueryClient mdmConfigurationQueryClient;
    private final MdmModelQueryClient mdmModelQueryClient;
    private final MdmPlantQueryClient mdmPlantQueryClient;
    private final MdmPlatformQueryClient mdmPlatformQueryClient;
    private final MdmVariantQueryClient mdmVariantQueryClient;
    private final MdmOptionFamilyQueryClient mdmOptionFamilyQueryClient;
    private final MdmOptionCodeQueryClient mdmOptionCodeQueryClient;

    /**
     * 处理 MDM 品牌事件
     * 
     * <p>CR-012：Brand 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     *
     * @param event 品牌事件
     */
    public void handleBrandEvent(MdmBrandEvent event) {
        log.debug("处理 MDM 品牌事件: eventType={}, entityId={}, code={}", 
                event.getEventType(), event.getEntityId(), event.getCode());
        // 根据 externalRefId 查找本地记录
        Brand localBrand = mdmBrandRepository.selectByExternalRefId(event.getEntityId());
        if (localBrand == null) {
            // 本地不存在，新增 Brand 投影
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
            // 本地存在，检查版本
            if (event.getVersion() > localBrand.getExternalVersion()) {
                // 更新 Brand 投影（版本更高）
                localBrand.setName(event.getName());
                localBrand.setExternalVersion(event.getVersion());
                localBrand.setLastSyncTime(LocalDateTime.now());
                mdmBrandRepository.updateById(localBrand);
                log.info("更新 MDM 品牌投影: code={}, oldVersion={}, newVersion={}", 
                        event.getCode(), localBrand.getExternalVersion(), event.getVersion());
            } else {
                // 忽略乱序事件
                log.debug("忽略 MDM 品牌事件（版本不满足）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localBrand.getExternalVersion());
            }
        }
    }

    /**
     * 处理 MDM 车系事件
     *
     * @param event 车系事件
     */
    public void handleSeriesEvent(MdmCarLineEvent event) {
        log.info("处理MDM车系事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        // 根据 externalRefId 查找本地记录
        CarLine localCarLine = mdmCarLineRepository.selectByExternalRefId(event.getEntityId());
        if (localCarLine == null) {
            // 本地不存在，新增
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
            // 本地存在，检查版本
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
     *
     * @param event 平台事件
     */
    public void handlePlatformEvent(MdmPlatformEvent event) {
        log.info("处理MDM平台事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        // 根据 externalRefId 查找本地记录
        Platform localPlatform = mdmPlatformRepository.selectByExternalRefId(event.getEntityId());
        if (localPlatform == null) {
            // 本地不存在，新增
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
            // 本地存在，检查版本
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
     *
     * <p>CR-015：Model 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code 关联字段）。</p>
     *
     * @param event 车型事件
     */
    public void handleModelEvent(MdmModelEvent event) {
        log.info("处理MDM车型事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        // 根据 externalRefId 查找本地记录
        Model localModel = mdmModelRepository.selectByExternalRefId(event.getEntityId());
        if (localModel == null) {
            // 本地不存在，新增
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
            // 本地存在，检查版本
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
     *
     * <p>CR-016：Variant 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code / model_code 关联字段）。</p>
     *
     * @param event 版本事件
     */
    public void handleVariantEvent(MdmVariantEvent event) {
        log.info("处理MDM版本事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        // 根据 externalRefId 查找本地记录
        Variant localVariant = mdmVariantRepository.selectByExternalRefId(event.getEntityId());
        if (localVariant == null) {
            // 本地不存在，新增
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
            // 本地存在，检查版本
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
     *
     * <p>CR-017：Configuration 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code / model_code / variant_code 关联字段）。</p>
     *
     * @param event 配置事件
     */
    public void handleConfigurationEvent(MdmConfigurationEvent event) {
        log.info("处理MDM配置事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        // 根据 externalRefId 查找本地记录
        Configuration localConfiguration = mdmConfigurationRepository.selectByExternalRefId(event.getEntityId());
        if (localConfiguration == null) {
            // 本地不存在，新增
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
            // 本地存在，检查版本
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
     * Bootstrap 全量同步品牌数据
     * 
     * <p>当本地 source=MDM 的品牌记录数为 0 时，自动调用 MDM Brand 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     * 
     * <p>CR-012：Brand 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     */
    public void bootstrapBrand() {
        log.info("开始 Bootstrap 品牌数据同步");
        long count = mdmBrandRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 品牌记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmBrands = mdmBrandQueryClient.getAllBrands();
                for (Map<String, Object> brandData : mdmBrands) {
                    String code = (String) brandData.get("code");
                    String name = (String) brandData.get("name");
                    String entityId = (String) brandData.get("id");
                    Long version = Long.valueOf(brandData.get("version").toString());
                    
                    Brand brand = Brand.builder()
                            .code(code)
                            .name(name)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmBrandRepository.insert(brand);
                    log.info("Bootstrap 新增 MDM 品牌投影: code={}", code);
                }
                log.info("Bootstrap 品牌数据同步完成，共同步 {} 条", mdmBrands.size());
            } catch (Exception e) {
                log.error("Bootstrap 品牌数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 品牌数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车系数据
     * 
     * <p>当本地 source=MDM 的车系记录数为 0 时，自动调用 MDM CarLine 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     * 
     * <p>CR-014：CarLine 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段（含 brand_code 冗余字段）。</p>
     */
    public void bootstrapSeries() {
        log.info("开始 Bootstrap 车系数据同步");
        long count = mdmCarLineRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 车系记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmCarLines = mdmCarLineQueryClient.getAllSeries();
                for (Map<String, Object> carLineData : mdmCarLines) {
                    String code = (String) carLineData.get("code");
                    String name = (String) carLineData.get("name");
                    String brandCode = (String) carLineData.get("brandCode");
                    String entityId = (String) carLineData.get("id");
                    Long version = Long.valueOf(carLineData.get("version").toString());
                    
                    CarLine carLine = CarLine.builder()
                            .code(code)
                            .name(name)
                            .brandCode(brandCode)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmCarLineRepository.insert(carLine);
                    log.info("Bootstrap 新增 MDM 车系投影: code={}, brandCode={}", code, brandCode);
                }
                log.info("Bootstrap 车系数据同步完成，共同步 {} 条", mdmCarLines.size());
            } catch (Exception e) {
                log.error("Bootstrap 车系数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 车系数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步平台数据
     * 
     * <p>当本地 source=MDM 的平台记录数为 0 时，自动调用 MDM Platform 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     * 
     * <p>CR-013：Platform 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     */
    public void bootstrapPlatform() {
        log.info("开始 Bootstrap 平台数据同步");
        long count = mdmPlatformRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 平台记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmPlatforms = mdmPlatformQueryClient.getAllPlatforms();
                for (Map<String, Object> platformData : mdmPlatforms) {
                    String code = (String) platformData.get("code");
                    String name = (String) platformData.get("name");
                    String entityId = (String) platformData.get("id");
                    Long version = Long.valueOf(platformData.get("version").toString());
                    
                    Platform platform = Platform.builder()
                            .code(code)
                            .name(name)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmPlatformRepository.insert(platform);
                    log.info("Bootstrap 新增 MDM 平台投影: code={}", code);
                }
                log.info("Bootstrap 平台数据同步完成，共同步 {} 条", mdmPlatforms.size());
            } catch (Exception e) {
                log.error("Bootstrap 平台数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 平台数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车型数据
     *
     * <p>当本地 source=MDM 的车型记录数为 0 时，自动调用 MDM Model 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-015：Model 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code 关联字段）。</p>
     */
    public void bootstrapModel() {
        log.info("开始 Bootstrap 车型数据同步");
        long count = mdmModelRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 车型记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmModels = mdmModelQueryClient.getAllModels();
                for (Map<String, Object> modelData : mdmModels) {
                    String code = (String) modelData.get("code");
                    String name = (String) modelData.get("name");
                    String platformCode = (String) modelData.get("platformCode");
                    String carLineCode = (String) modelData.get("carLineCode");
                    String entityId = (String) modelData.get("id");
                    Long version = Long.valueOf(modelData.get("version").toString());

                    Model model = Model.builder()
                            .code(code)
                            .name(name)
                            .platformCode(platformCode)
                            .carLineCode(carLineCode)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmModelRepository.insert(model);
                    log.info("Bootstrap 新增 MDM 车型投影: code={}, platformCode={}, carLineCode={}", code, platformCode, carLineCode);
                }
                log.info("Bootstrap 车型数据同步完成，共同步 {} 条", mdmModels.size());
            } catch (Exception e) {
                log.error("Bootstrap 车型数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 车型数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步版本数据
     *
     * <p>当本地 source=MDM 的版本记录数为 0 时，自动调用 MDM Variant 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-016：Variant 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code / model_code 关联字段）。</p>
     */
    public void bootstrapVariant() {
        log.info("开始 Bootstrap 版本数据同步");
        long count = mdmVariantRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 版本记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmVariants = mdmVariantQueryClient.getAllVariants();
                for (Map<String, Object> variantData : mdmVariants) {
                    String code = (String) variantData.get("code");
                    String name = (String) variantData.get("name");
                    String platformCode = (String) variantData.get("platformCode");
                    String carLineCode = (String) variantData.get("carLineCode");
                    String modelCode = (String) variantData.get("modelCode");
                    String entityId = (String) variantData.get("id");
                    Long version = Long.valueOf(variantData.get("version").toString());

                    Variant variant = Variant.builder()
                            .code(code)
                            .name(name)
                            .platformCode(platformCode)
                            .carLineCode(carLineCode)
                            .modelCode(modelCode)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmVariantRepository.insert(variant);
                    log.info("Bootstrap 新增 MDM 版本投影: code={}, platformCode={}, carLineCode={}, modelCode={}", code, platformCode, carLineCode, modelCode);
                }
                log.info("Bootstrap 版本数据同步完成，共同步 {} 条", mdmVariants.size());
            } catch (Exception e) {
                log.error("Bootstrap 版本数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 版本数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步配置数据
     *
     * <p>当本地 source=MDM 的配置记录数为 0 时，自动调用 MDM Configuration 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-017：Configuration 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段
     * （含 platform_code / carLine_code / model_code / variant_code 关联字段）。</p>
     */
    public void bootstrapConfiguration() {
        log.info("开始 Bootstrap 配置数据同步");
        long count = mdmConfigurationRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 配置记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmConfigurations = mdmConfigurationQueryClient.getAllConfigurations();
                for (Map<String, Object> configurationData : mdmConfigurations) {
                    String code = (String) configurationData.get("code");
                    String name = (String) configurationData.get("name");
                    String nameEn = (String) configurationData.get("nameEn");
                    String platformCode = (String) configurationData.get("platformCode");
                    String carLineCode = (String) configurationData.get("carLineCode");
                    String modelCode = (String) configurationData.get("modelCode");
                    String variantCode = (String) configurationData.get("variantCode");
                    String vehicleStageCode = (String) configurationData.get("vehicleStageCode");
                    Boolean enable = (Boolean) configurationData.get("enable");
                    Integer sort = (Integer) configurationData.get("sort");
                    String entityId = (String) configurationData.get("id");
                    Long version = Long.valueOf(configurationData.get("version").toString());

                    Configuration configuration = Configuration.builder()
                            .code(code)
                            .name(name)
                            .nameEn(nameEn)
                            .platformCode(platformCode)
                            .carLineCode(carLineCode)
                            .modelCode(modelCode)
                            .variantCode(variantCode)
                            .vehicleStageCode(vehicleStageCode)
                            .enable(enable)
                            .sort(sort)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmConfigurationRepository.insert(configuration);
                    log.info("Bootstrap 新增 MDM 配置投影: code={}, variantCode={}", code, variantCode);
                }
                log.info("Bootstrap 配置数据同步完成，共同步 {} 条", mdmConfigurations.size());
            } catch (Exception e) {
                log.error("Bootstrap 配置数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 配置数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * 处理 MDM 选项族事件
     *
     * <p>CR-018：OptionFamily 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     *
     * @param event 选项族事件
     */
    public void handleOptionFamilyEvent(MdmOptionFamilyEvent event) {
        log.info("处理MDM选项族事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        OptionFamily localOptionFamily = mdmOptionFamilyRepository.selectByExternalRefId(event.getEntityId());
        if (localOptionFamily == null) {
            OptionFamily newOptionFamily = OptionFamily.builder()
                    .code(event.getCode())
                    .name(event.getName())
                    .nameEn(event.getNameEn())
                    .type(event.getType())
                    .mandatory(event.getMandatory())
                    .enable(event.getEnable())
                    .sort(event.getSort())
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
                localOptionFamily.setNameEn(event.getNameEn());
                localOptionFamily.setType(event.getType());
                localOptionFamily.setMandatory(event.getMandatory());
                localOptionFamily.setEnable(event.getEnable());
                localOptionFamily.setSort(event.getSort());
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
     *
     * <p>CR-018：OptionCode 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     *
     * @param event 选项值事件
     */
    public void handleOptionCodeEvent(MdmOptionCodeEvent event) {
        log.info("处理MDM选项值事件: entityId={}, version={}", event.getEntityId(), event.getVersion());
        OptionCode localOptionCode = mdmOptionFamilyRepository.selectOptionCodeByExternalRefId(event.getEntityId());
        if (localOptionCode == null) {
            OptionCode newOptionCode = OptionCode.builder()
                    .code(event.getCode())
                    .optionFamilyCode(event.getOptionFamilyCode())
                    .name(event.getName())
                    .nameEn(event.getNameEn())
                    .val(event.getVal())
                    .enable(event.getEnable())
                    .sort(event.getSort())
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
                localOptionCode.setNameEn(event.getNameEn());
                localOptionCode.setVal(event.getVal());
                localOptionCode.setEnable(event.getEnable());
                localOptionCode.setSort(event.getSort());
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
     * Bootstrap 全量同步选项族数据
     *
     * <p>当本地 source=MDM 的选项族记录数为 0 时，自动调用 MDM OptionFamily 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-018：OptionFamily 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     */
    public void bootstrapOptionFamily() {
        log.info("开始 Bootstrap 选项族数据同步");
        long count = mdmOptionFamilyRepository.countBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 选项族记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmOptionFamilies = mdmOptionFamilyQueryClient.getAllOptionFamilies();
                for (Map<String, Object> optionFamilyData : mdmOptionFamilies) {
                    String code = (String) optionFamilyData.get("code");
                    String name = (String) optionFamilyData.get("name");
                    String nameEn = (String) optionFamilyData.get("nameEn");
                    String type = (String) optionFamilyData.get("type");
                    Boolean mandatory = (Boolean) optionFamilyData.get("mandatory");
                    Boolean enable = (Boolean) optionFamilyData.get("enable");
                    Integer sort = (Integer) optionFamilyData.get("sort");
                    String entityId = (String) optionFamilyData.get("id");
                    Long version = Long.valueOf(optionFamilyData.get("version").toString());

                    OptionFamily optionFamily = OptionFamily.builder()
                            .code(code)
                            .name(name)
                            .nameEn(nameEn)
                            .type(type)
                            .mandatory(mandatory)
                            .enable(enable)
                            .sort(sort)
                            .source(SourceType.MDM.name())
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmOptionFamilyRepository.insert(optionFamily);
                    log.info("Bootstrap 新增 MDM 选项族投影: code={}", code);
                }
                log.info("Bootstrap 选项族数据同步完成，共同步 {} 条", mdmOptionFamilies.size());
            } catch (Exception e) {
                log.error("Bootstrap 选项族数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 选项族数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步选项值数据
     *
     * <p>当本地 source=MDM 的选项值记录数为 0 时，自动调用 MDM OptionCode 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-018：OptionCode 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     */
    public void bootstrapOptionCode() {
        log.info("开始 Bootstrap 选项值数据同步");
        long count = mdmOptionFamilyRepository.countOptionCodeBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 选项值记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmOptionCodes = mdmOptionCodeQueryClient.getAllOptionCodes();
                for (Map<String, Object> optionCodeData : mdmOptionCodes) {
                    String code = (String) optionCodeData.get("code");
                    String optionFamilyCode = (String) optionCodeData.get("optionFamilyCode");
                    String name = (String) optionCodeData.get("name");
                    String nameEn = (String) optionCodeData.get("nameEn");
                    String val = (String) optionCodeData.get("val");
                    Boolean enable = (Boolean) optionCodeData.get("enable");
                    Integer sort = (Integer) optionCodeData.get("sort");
                    String entityId = (String) optionCodeData.get("id");
                    Long version = Long.valueOf(optionCodeData.get("version").toString());

                    OptionCode optionCode = OptionCode.builder()
                            .code(code)
                            .optionFamilyCode(optionFamilyCode)
                            .name(name)
                            .nameEn(nameEn)
                            .val(val)
                            .enable(enable)
                            .sort(sort)
                            .source(SourceType.MDM.name())
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmOptionFamilyRepository.insertOptionCode(optionCode);
                    log.info("Bootstrap 新增 MDM 选项值投影: code={}", code);
                }
                log.info("Bootstrap 选项值数据同步完成，共同步 {} 条", mdmOptionCodes.size());
            } catch (Exception e) {
                log.error("Bootstrap 选项值数据同步失败", e);
            }
        } else {
            log.info("本地已有 MDM 选项值数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * 处理 MDM 工厂事件
     *
     * <p>CR-011：Plant 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     *
     * @param event 工厂事件
     */
    public void handlePlantEvent(MdmPlantEvent event) {
        log.debug("处理 MDM 工厂事件: eventType={}, entityId={}, code={}",
                event.getEventType(), event.getEntityId(), event.getCode());
        // 根据 externalRefId 查找本地记录
        Plant localPlant = mdmPlantRepository.selectByExternalRefId(event.getEntityId());
        if (localPlant == null) {
            // 本地不存在，新增 Plant 投影
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
            // 本地存在，检查版本
            if (event.getVersion() > localPlant.getExternalVersion()) {
                // 更新 Plant 投影（版本更高）
                localPlant.setName(event.getName());
                localPlant.setExternalVersion(event.getVersion());
                localPlant.setLastSyncTime(LocalDateTime.now());
                mdmPlantRepository.update(localPlant);
                log.info("更新 MDM 工厂投影: code={}, oldVersion={}, newVersion={}",
                        event.getCode(), localPlant.getExternalVersion(), event.getVersion());
            } else {
                // 忽略乱序事件
                log.debug("忽略 MDM 工厂事件（版本不满足）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localPlant.getExternalVersion());
            }
        }
    }

    /**
     * Bootstrap 全量同步工厂数据
     *
     * <p>当本地 source=MDM 的工厂记录数为 0 时，自动调用 MDM Plant 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     *
     * <p>CR-011：Plant 投影采用按需最小化只读投影，仅同步 VMD 业务所需字段。</p>
     */
    public void bootstrapPlant() {
        log.info("开始 Bootstrap 工厂数据同步");
        long count = mdmPlantRepository.countBySource(SourceType.MDM.name());
        if (count == 0) {
            log.info("本地无 MDM 工厂记录（count=0），启动 Bootstrap 同步");
            try {
                List<Map<String, Object>> mdmPlants = mdmPlantQueryClient.getAllPlants();
                for (Map<String, Object> plantData : mdmPlants) {
                    String code = (String) plantData.get("code");
                    String name = (String) plantData.get("name");
                    String entityId = (String) plantData.get("id");
                    Long version = Long.valueOf(plantData.get("version").toString());

                    Plant plant = Plant.builder()
                            .code(code)
                            .name(name)
                            .source(SourceType.MDM)
                            .externalRefId(entityId)
                            .externalVersion(version)
                            .lastSyncTime(LocalDateTime.now())
                            .build();
                    mdmPlantRepository.insert(plant);
                    log.info("Bootstrap 新增 MDM 工厂投影: code={}", code);
                }
                log.info("Bootstrap 工厂数据同步完成，共同步 {} 条", mdmPlants.size());
            } catch (Exception e) {
                log.error("Bootstrap 工厂数据同步失败", e);
                // 不清空本地已有数据
            }
        } else {
            log.info("本地已有 MDM 工厂数据 {} 条，跳过 Bootstrap", count);
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
        log.info("Bootstrap全量数据同步完成");
    }

}
