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
    PART_NOT_EXIST("202011", "零件不存在"),
    PART_NOT_ALLOW_BIND("202012", "零件不允许绑定"),
    PARSER_NOT_FOUND("202013", "导入数据解析器不存在"),
    PRODUCT_DATA_READ_ONLY("202014", "产品数据只读，不允许通过VMD后台修改"),
    SUPPLIER_MAINTENANCE_RETIRED("202015", "供应商本地维护已下线"),
    PART_INSTANCE_ALREADY_EXISTS("202016", "物理零件实例已存在"),
    PART_BINDING_CONFLICT("202017", "零件绑定冲突"),
    PART_INSTANCE_NOT_EXIST("202018", "物理零件实例不存在"),
    PART_INBOUND_VALIDATE_FAILED("202019", "零件实例入站校验失败"),
    SECURITY_CONSTANT_PRESET_FAILED("202021", "安全常量预置失败"),
    KMS_HSM_UNAVAILABLE("202022", "KMS/HSM服务不可用"),
    IMMO_ROOT_NOT_PRESET("202023", "该车辆防盗根未就绪"),
    PROV_FACILITY_NOT_REGISTERED("202024", "安全灌注机未注册或未就绪"),
    OTA_ROOT_NOT_PRESET("202025", "该车辆OTA根未就绪"),
    PART_NOT_FOUND_IN_MDM("202026", "零件编码在MDM主数据中不存在"),
    PART_NOT_ACTIVE_IN_MDM("202027", "零件在MDM主数据中非ACTIVE状态"),
    PART_IMPORT_DATA_EXCEPTION("202028", "零件导入数据异常");

    private final String code;
    private final String message;

}
