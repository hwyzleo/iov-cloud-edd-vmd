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
    VEHICLE_IMPORT_EVENT_REPLAY_IN_PROGRESS("806030", "该车辆导入记录正在补发消息，请勿重复操作"),
    CERTIFICATE_VIN_NOT_EXIST("806040", "VIN不存在或状态不允许申请证书"),
    CERTIFICATE_DEVICE_NOT_BOUND("806041", "设备与车辆未建立active绑定"),
    CERTIFICATE_DEVICE_CATEGORY_MISMATCH("806042", "设备类别不匹配"),
    CERTIFICATE_CSR_INVALID("806043", "CSR格式或签名无效"),
    CERTIFICATE_CSR_SUBJECT_MISMATCH("806044", "CSR Subject CN缺失、重复或与hsm_uid/ecu_uid不一致"),
    CERTIFICATE_CSR_CONTAINS_VIN("806045", "CSR不应包含VIN"),
    CERTIFICATE_PROFILE_NOT_ALLOWED("806046", "证书Profile不允许"),
    CERTIFICATE_PKI_UNAVAILABLE("806047", "PKI服务不可用"),
    CERTIFICATE_ISSUANCE_UNKNOWN("806048", "签发结果未知，待对账"),
    CERTIFICATE_PKI_REJECTED("806049", "PKI明确拒签"),
    CERTIFICATE_INSTALL_CONFIRM_MISMATCH("806050", "证书安装确认对象不匹配"),
    CERTIFICATE_STATUS_NOT_ALLOWED("806051", "证书状态不允许此操作"),
    CERTIFICATE_REQUEST_NOT_EXIST("806052", "证书申请不存在"),
    SOFTWARE_MANIFEST_INVALID("806053", "软件实装清单校验失败"),
    SOFTWARE_MANIFEST_ITEM_INVALID("806054", "软件实装清单条目非法"),
    SOFTWARE_SOURCE_UNSUPPORTED("806055", "不支持的软件实装来源"),
    SOFTWARE_SOURCE_VERSION_MISSING("806056", "缺少软件实装来源版本或时刻"),
    SOFTWARE_IDEMPOTENCY_CONFLICT("806057", "软件实装幂等键冲突"),
    SECURITY_PRESET_INVALID_CAPABILITY("806058", "器件HSM能力值非法或不受支持"),
    SECURITY_PRESET_BIZ_TYPE_UNRESOLVED("806059", "安全常量预置业务类型不可解析"),
    CERTIFICATE_COMPENSATION_NOT_ALLOWED("806060", "当前证书申请状态不允许补偿"),
    CERTIFICATE_REQUEST_IDEMPOTENCY_CONFLICT("806061", "证书申请幂等键冲突"),
    CERTIFICATE_ISSUANCE_CONFLICT("806062", "设备已存在有效或处理中证书申请，不能重复补偿"),
    CERTIFICATE_COMPENSATION_REASON_REQUIRED("806063", "请填写证书补偿原因和工单信息"),
    CERTIFICATE_KEY_CONFLICT("806064", "同设备身份已存在不同公钥的有效/处理中证书，需授权换钥（rekey/reissue）"),
    CERTIFICATE_DEVICE_UID_UNAVAILABLE("806065", "设备HSM UID缺失或来源冲突，无法完成证书身份校验"),

    // ================= CR-055 业务密钥域（806066～806077） =================
    BUSINESS_KEY_DEVICE_SESSION_MISMATCH("806066", "设备会话身份与请求deviceSn不一致"),
    BUSINESS_KEY_DOMAIN_NOT_AUTHORIZED("806067", "业务域或用途未授权"),
    BUSINESS_KEY_NOT_EXIST("806068", "业务密钥不存在或当前无ACTIVE"),
    BUSINESS_KEY_MULTIPLE_ACTIVE("806069", "业务密钥目录存在多个ACTIVE，数据异常需对账"),
    BUSINESS_KEY_STATE_NOT_ALLOWED("806070", "业务密钥状态不允许此操作"),
    BUSINESS_KEY_IDEMPOTENCY_CONFLICT("806071", "业务密钥申请幂等键冲突"),
    BUSINESS_KEY_ROTATION_CONFLICT("806072", "业务密钥轮换并发冲突"),
    BUSINESS_KEY_DEVICE_CERT_NOT_FOUND("806073", "有效设备证书不存在"),
    BUSINESS_KEY_KMS_UNAVAILABLE("806074", "KMS/HSM或framework安全服务不可用"),
    BUSINESS_KEY_WRAP_FAILED("806075", "业务密钥设备封装失败"),
    BUSINESS_KEY_REVOCATION_FAILED("806076", "业务密钥吊销失败"),
    BUSINESS_KEY_OUTCOME_UNKNOWN("806077", "密钥操作结果未知，需以原幂等键对账"),

    // ================= CR-056 零件导入后置处理重放域（806078～806080） =================
    PART_IMPORT_POST_PROCESS_REPLAY_NOT_ALLOWED("806078", "当前零件导入记录不允许重放后置处理"),
    PART_IMPORT_POST_PROCESS_REPLAY_IN_PROGRESS("806079", "该零件导入记录正在重放后置处理"),
    PART_IMPORT_POST_PROCESS_REPLAY_ACTION_NOT_FOUND("806080", "不支持的零件导入后置处理动作");

    private final String code;
    private final String message;

}
