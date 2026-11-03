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
 * <h2>退役编号保留登记（VMD-DSN-CR-074 迁出删除）</h2>
 * <p>
 * 设备证书 / 运行期业务密钥 / Keyprov 职责迁出至 SEC-VIAM 后，以下编号退役，
 * <strong>禁止重新分配</strong>，仅保留在本登记中防止复用（退役编号不保留可抛出异常）：
 * </p>
 * <ul>
 *   <li>806040～806052：证书域（CERTIFICATE_*，CR-044/053/054）</li>
 *   <li>806060～806065：证书补偿域（CERTIFICATE_COMPENSATION_NOT_ALLOWED/REQUEST_IDEMPOTENCY/ISSUANCE_CONFLICT/KEY_CONFLICT/DEVICE_UID_UNAVAILABLE）</li>
 *   <li>806066～806077：业务密钥域（BUSINESS_KEY_*，CR-055）</li>
 *   <li>806083～806089：业务密钥开通批次/ACK 域（BUSINESS_KEY_PROVISIONING_*，CR-058）</li>
 *   <li>806090～806100：官方 Keyprov Service 契约域（KEYPROV_*，CR-059）</li>
 *   <li>806101～806105：证书材料解析与 Provider 就绪门禁域（KEYPROV_*，CR-060）</li>
 *   <li>806107～806108：V2C 根派生域（KEYPROV_DERIVED_PROVIDER_NOT_READY/DERIVATION_UNSUPPORTED，CR-061）
 *       —— 其中 806106 KEYPROV_VIN_BINDING_INVALID 因 VMD 保留业务 VIN 规范化（VinCanonicalizer）
 *       继续复用而保持活动注册，语义为通用 VIN 非法校验，不再承担 Keyprov 绑定裁决语义</li>
 *   <li>806109～806111：ECDH-ES envelope v1 / Import Attestation 域（KEYPROV_*，CR-062）</li>
 *   <li>806112～806121：逻辑 TBOX ECDH-ES v1 证书 Profile 域（CERTIFICATE_*，CR-063）</li>
 *   <li>806122～806129：滞留开通批次运维终止域（BUSINESS_KEY_PROVISIONING_TERMINATE_*，CR-065）</li>
 *   <li>806130～806131：逻辑材料身份与版本代际冲突域（KEYPROV_LOGICAL_IDENTITY_CONFLICT/VERSION_GENERATION_MISMATCH，CR-066）</li>
 * </ul>
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

    // ================= 软件实装域（806053～806057，保留） =================
    SOFTWARE_MANIFEST_INVALID("806053", "软件实装清单校验失败"),
    SOFTWARE_MANIFEST_ITEM_INVALID("806054", "软件实装清单条目非法"),
    SOFTWARE_SOURCE_UNSUPPORTED("806055", "不支持的软件实装来源"),
    SOFTWARE_SOURCE_VERSION_MISSING("806056", "缺少软件实装来源版本或时刻"),
    SOFTWARE_IDEMPOTENCY_CONFLICT("806057", "软件实装幂等键冲突"),

    // ================= 产线安全常量预置域（806058～806059，保留） =================
    SECURITY_PRESET_INVALID_CAPABILITY("806058", "器件HSM能力值非法或不受支持"),
    SECURITY_PRESET_BIZ_TYPE_UNRESOLVED("806059", "安全常量预置业务类型不可解析"),

    // ================= 零件导入后置处理重放域（806078～806080，保留） =================
    PART_IMPORT_POST_PROCESS_REPLAY_NOT_ALLOWED("806078", "当前零件导入记录不允许重放后置处理"),
    PART_IMPORT_POST_PROCESS_REPLAY_IN_PROGRESS("806079", "该零件导入记录正在重放后置处理"),
    PART_IMPORT_POST_PROCESS_REPLAY_ACTION_NOT_FOUND("806080", "不支持的零件导入后置处理动作"),

    // ================= 车辆导入补发动作注册表域（806081～806082，保留） =================
    VEHICLE_IMPORT_REPLAY_ACTION_NOT_REGISTERED("806081", "车辆导入补发动作未登记，不允许执行"),
    VEHICLE_IMPORT_REPLAY_TOPIC_NOT_REGISTERED("806082", "车辆导入补发目标Topic未登记，不允许发布"),

    // ================= 通用 VIN 校验（806106，VMD-DSN-CR-074 迁出后保留业务复用） =================
    // 原为 CR-061 Keyprov VIN 绑定域（KEYPROV_VIN_BINDING_INVALID）；迁出后由 VMD 保留业务
    // VIN 规范化（VinCanonicalizer）继续复用，语义收敛为通用 VIN 非法校验。
    KEYPROV_VIN_BINDING_INVALID("806106", "VIN非法"),

    // ================= CR-072 实例 device_category 最小快照域（806132～806136，保留） =================
    // 落地 VMD-DSN-CR-072 第一阶段：节点-类别成对一致性、解析失败（可重试/数据）、预览陈旧、显式纠错拒绝、功能开关未启用。
    DEVICE_CATEGORY_NODE_CONFLICT("806132", "零件实例车载节点与输入节点均非空且不同，拒绝本次变更（节点-类别成对一致性）"),
    DEVICE_CATEGORY_RESOLVE_FAILED("806133", "车载节点设备类别解析失败（节点缺失/投影未就绪/类别空或超长）"),
    DEVICE_CATEGORY_PREVIEW_STALE("806134", "重导预览计划已过期或与当前状态不符，需重新预览"),
    DEVICE_CATEGORY_CORRECTION_NOT_SUPPORTED("806135", "deviceCategory 显式纠错第一阶段暂不支持，仅服务器派生补缺"),
    DEVICE_CATEGORY_DISABLED("806136", "设备类别补缺功能未启用（vmd.part.device-category.enabled=false）");

    private final String code;
    private final String message;

}
