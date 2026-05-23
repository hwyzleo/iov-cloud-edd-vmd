package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 车辆主数据服务错误码
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum VmdErrorCode implements ErrorCode {

    VEHICLE_NOT_EXIST("202001", "车辆不存在"),
    VEHICLE_HAS_BIND_ORDER("202009", "车辆已绑定订单"),
    VEHICLE_IMPORT_DATA_EXCEPTION("202010", "车辆导入数据异常"),
    PART_NOT_EXIST("202011", "零件不存在"),
    PART_NOT_ALLOW_BIND("202012", "零件不允许绑定");

    private final String code;
    private final String message;

}
