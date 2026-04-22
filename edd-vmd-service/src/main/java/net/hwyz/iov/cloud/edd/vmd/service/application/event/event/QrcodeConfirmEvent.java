package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;

/**
 * 二维码确认事件
 *
 * @author hwyz_leo
 */
@Getter
public class QrcodeConfirmEvent extends BaseEvent {

    /**
     * 二维码类型
     */
    private final QrcodeType type;
    /**
     * 车架号
     */
    private final String vin;
    /**
     * 账号ID
     */
    private final String accountId;

    public QrcodeConfirmEvent(QrcodeType type, String vin, String accountId) {
        super(vin);
        this.type = type;
        this.vin = vin;
        this.accountId = accountId;
    }

}
