package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 车辆主数据服务错误码
 * <p>
 * D23 错误码治理：业务错误码从历史 202xxx 统一迁移到模块命名空间 806001～806999，
 * 806000 保留为模块基准码。原 202xxx 全部下线，不保留兼容。
 * </p>
 *
 * @author hwyz_leo
 * @see ErrorCodeRegistry
 */
@Getter
@AllArgsConstructor
public enum VmdErrorCode implements ErrorCode {

    VEHICLE_NOT_EXIST("806001", "车辆不存在"),
    VEHICLE_HAS_BIND_ORDER("806009", "车辆已绑定订单"),
    PART_NOT_EXIST("806011", "零件不存在"),
    PART_NOT_ALLOW_BIND("806012", "零件不允许绑定"),
    PARSER_NOT_FOUND("806013", "导入数据解析器不存在"),
    PRODUCT_DATA_READ_ONLY("806014", "产品数据只读，不允许通过VMD后台修改"),
    SUPPLIER_MAINTENANCE_RETIRED("806015", "供应商本地维护已下线"),
    PART_INSTANCE_ALREADY_EXISTS("806016", "物理零件实例已存在"),
    PART_BINDING_CONFLICT("806017", "零件绑定冲突"),
    PART_INSTANCE_NOT_EXIST("806018", "物理零件实例不存在"),
    PART_INBOUND_VALIDATE_FAILED("806019", "零件实例入站校验失败"),
    SECURITY_CONSTANT_PRESET_FAILED("806021", "安全常量预置失败"),
    KMS_HSM_UNAVAILABLE("806022", "KMS/HSM服务不可用"),
    IMMO_ROOT_NOT_PRESET("806023", "该车辆防盗根未就绪"),
    PROV_FACILITY_NOT_REGISTERED("806024", "安全灌注机未注册或未就绪"),
    OTA_ROOT_NOT_PRESET("806025", "该车辆OTA根未就绪"),
    PART_NOT_FOUND_IN_MDM("806026", "零件编码在MDM主数据中不存在"),
    PART_NOT_ACTIVE_IN_MDM("806027", "零件在MDM主数据中非ACTIVE状态"),
    PART_IMPORT_DATA_EXCEPTION("806028", "零件导入数据异常"),
    VEHICLE_IMPORT_EVENT_REPLAY_NOT_ALLOWED("806029", "该车辆导入记录不允许补发消息"),
    VEHICLE_IMPORT_EVENT_REPLAY_IN_PROGRESS("806030", "该车辆导入记录正在补发消息，请勿重复操作");

    private final String code;
    private final String message;

}
