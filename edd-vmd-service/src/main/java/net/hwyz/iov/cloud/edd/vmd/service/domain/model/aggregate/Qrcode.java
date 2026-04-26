package net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.QrcodeHasExpiredException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.QrcodeHasUsedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.QrcodeInvalidException;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

/**
 * 二维码领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class Qrcode extends BaseDo<String> implements DomainObj<Qrcode> {

    /**
     * 车架号
     */
    private String vin;
    /**
     * 车机序列号
     */
    private String sn;
    /**
     * 二维码
     */
    private String qrcode;
    /**
     * 二维码类型
     */
    private QrcodeType type;
    /**
     * 二维码状态
     */
    private QrcodeState qrcodeState;

    /**
     * 初始化
     *
     * @param vin 车架号
     * @param sn  车机序列号
     */
    public void init(String vin, String sn) {
        stateInit();
        qrcode = generateQrcode(vin, sn);
        qrcodeState = QrcodeState.INITIALIZED;
    }

    /**
     * 轮询
     */
    public void polling() {
        // 由于 createTime 已移除，polling 逻辑需依赖基础设施层或重新设计
        // 此处暂时移除超时逻辑，待后续完善
    }

    /**
     * 扫描验证二维码
     *
     * @param qrcode 二维码
     */
    public void validate(String qrcode) {
        if (qrcodeState == QrcodeState.CONFIRMED) {
            throw new QrcodeHasUsedException(vin, type);
        }
        if (qrcodeState == QrcodeState.EXPIRED) {
            throw new QrcodeHasExpiredException(vin, type);
        }
        if (StrUtil.isBlank(qrcode) || !qrcode.toUpperCase().equals(this.qrcode)) {
            throw new QrcodeInvalidException(vin, type);
        }
        qrcodeState = QrcodeState.SCANNED;
        stateChange();
    }

    /**
     * 确认二维码
     *
     * @param qrcode 二维码
     */
    public void confirm(String qrcode) {
        if (qrcodeState == QrcodeState.CONFIRMED) {
            throw new QrcodeHasUsedException(vin, type);
        }
        if (qrcodeState == QrcodeState.EXPIRED) {
            throw new QrcodeHasExpiredException(vin, type);
        }
        if (StrUtil.isBlank(qrcode) || !qrcode.toUpperCase().equals(this.qrcode)) {
            throw new QrcodeInvalidException(vin, type);
        }
        qrcodeState = QrcodeState.CONFIRMED;
        stateChange();
    }

    /**
     * 生成二维码
     *
     * @return 二维码
     */
    private String generateQrcode(String vin, String sn) {
        // TODO 生成二维码策略
        return MD5.create().digestHex(vin + sn + System.currentTimeMillis()).toUpperCase();
    }

}
