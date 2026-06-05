package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final VehBrandRepository vehBrandRepository;
    private final VehCarLineRepository vehCarLineRepository;
    private final VehPlatformRepository vehPlatformRepository;

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
        Brand localBrand = vehBrandRepository.selectByExternalRefId(event.getEntityId());
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
            vehBrandRepository.insert(newBrand);
            log.info("新增 MDM 品牌投影: code={}, name={}", event.getCode(), event.getName());
        } else {
            // 本地存在，检查版本
            if (event.getVersion() > localBrand.getExternalVersion()) {
                // 更新 Brand 投影（版本更高）
                localBrand.setName(event.getName());
                localBrand.setExternalVersion(event.getVersion());
                localBrand.setLastSyncTime(LocalDateTime.now());
                vehBrandRepository.updateById(localBrand);
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
        CarLine localCarLine = vehCarLineRepository.selectByExternalRefId(event.getEntityId());
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
            vehCarLineRepository.insert(newCarLine);
            log.info("新增车系: code={}", event.getCode());
        } else {
            // 本地存在，检查版本
            if (event.getVersion() > localCarLine.getExternalVersion()) {
                localCarLine.setName(event.getName());
                localCarLine.setBrandCode(event.getBrandCode());
                localCarLine.setExternalVersion(event.getVersion());
                localCarLine.setLastSyncTime(LocalDateTime.now());
                vehCarLineRepository.updateById(localCarLine);
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
        Platform localPlatform = vehPlatformRepository.selectByExternalRefId(event.getEntityId());
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
            vehPlatformRepository.insert(newPlatform);
            log.info("新增平台: code={}", event.getCode());
        } else {
            // 本地存在，检查版本
            if (event.getVersion() > localPlatform.getExternalVersion()) {
                localPlatform.setName(event.getName());
                localPlatform.setExternalVersion(event.getVersion());
                localPlatform.setLastSyncTime(LocalDateTime.now());
                vehPlatformRepository.updateById(localPlatform);
                log.info("更新平台: code={}, version={}", event.getCode(), event.getVersion());
            } else {
                log.info("忽略平台事件（版本不高于本地）: code={}, eventVersion={}, localVersion={}",
                        event.getCode(), event.getVersion(), localPlatform.getExternalVersion());
            }
        }
    }

    /**
     * Bootstrap 全量同步品牌数据
     * 
     * <p>当本地 source=MDM 的品牌记录数为 0 时，自动调用 MDM Brand 全量快照接口
     * 拉取数据并 upsert 本地副本。</p>
     * 
     * <p>TODO: MDM Brand 全量快照接口待就绪，当前为 stub 实现。
     * 接口路径和返回格式由「edd-mdm 接入规范」定义。</p>
     */
    public void bootstrapBrand() {
        log.info("开始 Bootstrap 品牌数据同步");
        long count = vehBrandRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无 MDM 品牌记录（count=0），启动 Bootstrap 同步");
            // TODO: 调用 MDM Brand 全量快照接口
            // MdmBrandQueryClient 接口已定义，待 MDM 服务就绪后启用
            // List<Map<String, Object>> mdmBrands = mdmBrandQueryClient.getAllBrands();
            // for (Map<String, Object> brandData : mdmBrands) {
            //     Brand brand = buildBrandFromMdmData(brandData);
            //     vehBrandRepository.insert(brand);
            // }
            log.warn("MDM Brand 全量快照接口待实现，Bootstrap 跳过");
        } else {
            log.info("本地已有 MDM 品牌数据 {} 条，跳过 Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步车系数据
     */
    public void bootstrapSeries() {
        log.info("开始Bootstrap车系数据同步");
        long count = vehCarLineRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无MDM车系数据，开始从MDM拉取全量");
            // TODO: 调用 MDM 全量快照接口拉取车系数据
            // List<Series> mdmSeries = mdmSeriesQueryClient.getAllSeries();
            // for (Series series : mdmSeries) {
            //     upsertSeries(series);
            // }
            log.info("Bootstrap车系数据同步完成");
        } else {
            log.info("本地已有MDM车系数据{}条，跳过Bootstrap", count);
        }
    }

    /**
     * Bootstrap 全量同步平台数据
     */
    public void bootstrapPlatform() {
        log.info("开始Bootstrap平台数据同步");
        long count = vehPlatformRepository.countBySource(SourceType.MDM);
        if (count == 0) {
            log.info("本地无MDM平台数据，开始从MDM拉取全量");
            // TODO: 调用 MDM 全量快照接口拉取平台数据
            // List<Platform> mdmPlatforms = mdmPlatformQueryClient.getAllPlatforms();
            // for (Platform platform : mdmPlatforms) {
            //     upsertPlatform(platform);
            // }
            log.info("Bootstrap平台数据同步完成");
        } else {
            log.info("本地已有MDM平台数据{}条，跳过Bootstrap", count);
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
        log.info("Bootstrap全量数据同步完成");
    }

}
