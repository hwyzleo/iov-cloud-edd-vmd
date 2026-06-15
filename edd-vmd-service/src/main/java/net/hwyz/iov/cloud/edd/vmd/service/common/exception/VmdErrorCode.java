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
    PART_NOT_ALLOW_BIND("202012", "零件不允许绑定"),
    PARSER_NOT_FOUND("202013", "导入数据解析器不存在"),
    PRODUCT_DATA_READ_ONLY("202014", "产品数据只读，不允许通过VMD后台修改"),
    SUPPLIER_MAINTENANCE_RETIRED("202015", "供应商本地维护已下线"),
    PART_INSTANCE_ALREADY_EXISTS("202016", "物理零件实例已存在"),
    PART_BINDING_CONFLICT("202017", "零件绑定冲突"),
    PART_INSTANCE_NOT_EXIST("202018", "物理零件实例不存在"),
    PART_INBOUND_VALIDATE_FAILED("202019", "零件实例入站校验失败"),
    PART_TYPE_SCHEMA_NOT_FOUND("202020", "未登记的零件类型字段契约"),
    PART_NOT_FOUND_IN_MDM("202021", "零件编码在MDM主数据中不存在"),
    PART_NOT_ACTIVE_IN_MDM("202022", "零件在MDM主数据中非ACTIVE状态"),
    PART_IMPORT_DATA_EXCEPTION("202023", "零件导入数据异常");

    private final String code;
    private final String message;

}
