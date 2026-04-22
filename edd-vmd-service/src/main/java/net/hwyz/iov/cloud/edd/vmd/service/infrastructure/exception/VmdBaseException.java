package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 车辆主数据服务基础异常
 *
 * @author hwyz_leo
 */
public class VmdBaseException extends BaseException {

    private static final int ERROR_CODE = 202000;
    protected static final int ERROR_CODE_VEHICLE_NOT_EXIST = 202001;
    protected static final int ERROR_CODE_VEHICLE_HAS_ACTIVATED = 202002;
    protected static final int ERROR_CODE_QRCODE_NOT_EXIST = 202003;
    protected static final int ERROR_CODE_QRCODE_HAS_USED = 202004;
    protected static final int ERROR_CODE_QRCODE_INVALID = 202005;
    protected static final int ERROR_CODE_QRCODE_HAS_EXPIRED = 202006;
    protected static final int ERROR_CODE_VEHICLE_PRESET_OWNER_NOT_MATCH = 202007;
    protected static final int ERROR_CODE_VEHICLE_WITHOUT_PRESET_OWNER = 202008;
    protected static final int ERROR_CODE_VEHICLE_HAS_BIND_ORDER = 202009;
    protected static final int ERROR_CODE_VEHICLE_IMPORT_DATA_EXCEPTION = 202010;
    protected static final int ERROR_CODE_PART_NOT_EXIST = 202011;
    protected static final int ERROR_CODE_PART_NOT_ALLOW_BIND = 202012;

    public VmdBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public VmdBaseException(int errorCode) {
        super(errorCode);
    }

    public VmdBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
