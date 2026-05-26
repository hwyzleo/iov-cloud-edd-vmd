package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * VMD 启动监听器
 * 在应用启动时自动触发 MDM 数据 Bootstrap 同步
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapListener {

    private final MdmSyncAppService mdmSyncAppService;

    /**
     * 应用启动完成后自动触发 Bootstrap 同步
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("VMD 应用启动完成，开始检查 MDM 数据 Bootstrap 同步");
        try {
            mdmSyncAppService.bootstrapAll();
        } catch (Exception e) {
            log.error("MDM 数据 Bootstrap 同步失败", e);
        }
    }

}
