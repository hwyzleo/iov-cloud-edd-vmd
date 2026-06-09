package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlatformEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmPlantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MDM 事件订阅类
 * 监听 MDM 系统推送的品牌/车系/平台变更事件
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmEventSubscribe {

    private final MdmSyncAppService mdmSyncAppService;

    /**
     * 订阅 MDM 品牌事件
     *
     * @param event 品牌事件
     */
    @EventListener
    public void onMdmBrandEvent(MdmBrandEvent event) {
        log.info("收到MDM品牌事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handleBrandEvent(event);
    }

    /**
     * 订阅 MDM 车系事件
     *
     * @param event 车系事件
     */
    @EventListener
    public void onMdmSeriesEvent(MdmCarLineEvent event) {
        log.info("收到MDM车系事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handleSeriesEvent(event);
    }

    /**
     * 订阅 MDM 平台事件
     *
     * @param event 平台事件
     */
    @EventListener
    public void onMdmPlatformEvent(MdmPlatformEvent event) {
        log.info("收到MDM平台事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handlePlatformEvent(event);
    }

    /**
     * 订阅 MDM 工厂事件
     *
     * @param event 工厂事件
     */
    @EventListener
    public void onMdmPlantEvent(MdmPlantEvent event) {
        log.info("收到MDM工厂事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handlePlantEvent(event);
    }

    /**
     * 订阅 MDM 车型事件
     *
     * @param event 车型事件
     */
    @EventListener
    public void onMdmModelEvent(MdmModelEvent event) {
        log.info("收到MDM车型事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handleModelEvent(event);
    }

    /**
     * 订阅 MDM 版本事件
     *
     * @param event 版本事件
     */
    @EventListener
    public void onMdmVariantEvent(MdmVariantEvent event) {
        log.info("收到MDM版本事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handleVariantEvent(event);
    }

    /**
     * 订阅 MDM 配置事件
     *
     * @param event 配置事件
     */
    @EventListener
    public void onMdmConfigurationEvent(MdmConfigurationEvent event) {
        log.info("收到MDM配置事件: type={}, entityId={}, version={}, code={}",
                event.getEventType(), event.getEntityId(), event.getVersion(), event.getCode());
        mdmSyncAppService.handleConfigurationEvent(event);
    }

}
