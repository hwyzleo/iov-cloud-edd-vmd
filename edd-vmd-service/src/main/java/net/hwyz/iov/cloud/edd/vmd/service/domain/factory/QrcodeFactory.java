package net.hwyz.iov.cloud.edd.vmd.service.domain.factory;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Qrcode;
import org.springframework.stereotype.Component;

/**
 * 二维码领域工厂类
 *
 * @author hwyz_leo
 */
@Component
public class QrcodeFactory {

    /**
     * 创建二维码领域对象
     *
     * @param type 二维码类型
     * @param vin  车架号
     * @param sn   车机序列号
     * @return 二维码领域对象
     */
    public Qrcode buildQrcode(QrcodeType type, String vin, String sn) {
        Qrcode qrcodePo = Qrcode.builder()
                .vin(vin)
                .sn(sn)
                .type(type)
                .build();
        qrcodePo.init(vin, sn);
        return qrcodePo;
    }

}
