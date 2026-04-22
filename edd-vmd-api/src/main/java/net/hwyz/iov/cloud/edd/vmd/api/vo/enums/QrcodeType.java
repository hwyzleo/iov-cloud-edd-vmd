package net.hwyz.iov.cloud.edd.vmd.api.vo.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 二维码类型枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum QrcodeType {

    /** 车辆激活 **/
    VEHICLE_ACTIVE(1800);

    /**
     * 二维码过期时间
     * 单位：秒
     */
    public final Integer timeout;

    public static QrcodeType valOf(String val) {
        return Arrays.stream(QrcodeType.values())
                .filter(qrcodeType -> qrcodeType.name().equals(val))
                .findFirst()
                .orElse(null);
    }

}
