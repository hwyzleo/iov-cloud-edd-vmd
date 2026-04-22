package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.QrcodeConfirmEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.QrcodeValidateEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 二维码事件发布类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QrcodePublish {

    final ApplicationContext ctx;

    /**
     * 二维码扫描验证
     *
     * @param type      二维码类型
     * @param vin       车架号
     * @param accountId 账号ID
     */
    public void validate(QrcodeType type, String vin, String accountId) {
        log.debug("发布用户[{}]扫描验证车辆[{}]二维码[{}]事件", accountId, vin, type);
        ctx.publishEvent(new QrcodeValidateEvent(type, vin, accountId));
    }

    /**
     * 二维码确认
     *
     * @param type      二维码类型
     * @param vin       车架号
     * @param accountId 账号ID
     */
    public void confirm(QrcodeType type, String vin, String accountId) {
        log.debug("发布用户[{}]确认车辆[{}]二维码[{}]事件", accountId, vin, type);
        ctx.publishEvent(new QrcodeConfirmEvent(type, vin, accountId));
    }

}
