package net.hwyz.iov.cloud.edd.vmd.service.domain.model;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.QrcodeHasExpiredException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.QrcodeHasUsedException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.QrcodeInvalidException;

import java.util.Date;

/**
 * 二维码领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
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
     * 创建时间
     */
    private Date createTime;

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
        createTime = new Date();
    }

    /**
     * 轮询
     */
    public void polling() {
        if (qrcodeState != QrcodeState.CONFIRMED && System.currentTimeMillis() - createTime.getTime() > type.timeout * 1000) {
            qrcodeState = QrcodeState.EXPIRED;
            stateChange();
        }
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
