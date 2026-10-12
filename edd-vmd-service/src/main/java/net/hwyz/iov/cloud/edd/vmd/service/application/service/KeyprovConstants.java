package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.security.keyprov.v1.KeyImportResult;
import net.hwyz.iov.security.keyprov.v1.KeyPurpose;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * keyprov canonical 常量与映射（CR-058 / PAR-PROTO-DSN-CR-006）。
 * <p>
 * 业务域与用途代码冻结：V2C_COMM / UPLINK_AEAD / DOWNLINK_AEAD（RD-058-3）。
 * V2C_COMM_ROOT 仅为 framework/KMS 材料 BizType，绝不透传到车云协议。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public final class KeyprovConstants {

    private KeyprovConstants() {
    }

    /** 首期 V2C 业务域（车云协议业务域，固定不可按环境改名） */
    public static final String BUSINESS_DOMAIN_V2C = "V2C_COMM";

    /** 上行 AEAD 用途代码 */
    public static final String PURPOSE_UPLINK_AEAD = "UPLINK_AEAD";

    /** 下行 AEAD 用途代码 */
    public static final String PURPOSE_DOWNLINK_AEAD = "DOWNLINK_AEAD";

    /** V2C 完整用途集合（顺序稳定：上行在前） */
    public static final List<String> V2C_PURPOSES = List.of(PURPOSE_UPLINK_AEAD, PURPOSE_DOWNLINK_AEAD);

    /** V2C 完整用途集合（无序判定用） */
    public static final Set<String> V2C_PURPOSE_SET = Set.copyOf(V2C_PURPOSES);

    /** 支持的封装套件代码（allowlist，未知/UNSPECIFIED fail-closed） */
    public static final String SUITE_HPKE_BASE_X25519_SHA256_AES128GCM =
            "HPKE_BASE_X25519_SHA256_AES128GCM";
    public static final String SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM =
            "ECDH_ES_P256_HKDF_SHA256_AES256GCM";

    /** 默认支持的封装套件 allowlist（环境批准后可扩展） */
    public static final List<String> DEFAULT_SUPPORTED_SUITES = List.of(
            SUITE_HPKE_BASE_X25519_SHA256_AES128GCM,
            SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM);

    /** canonical payload type 完整名 */
    public static final String PAYLOAD_TYPE_REQUEST = "security.keyprov.v1.KeyProvisioningRequest";
    public static final String PAYLOAD_TYPE_RESPONSE = "security.keyprov.v1.KeyProvisioningResponse";
    public static final String PAYLOAD_TYPE_ACK = "security.keyprov.v1.KeyProvisioningAck";
    public static final String PAYLOAD_TYPE_ACK_RESULT = "security.keyprov.v1.KeyProvisioningAckResult";
    public static final String PAYLOAD_TYPE_STATUS_REQUEST = "security.keyprov.v1.KeyProvisioningStatusRequest";
    public static final String PAYLOAD_TYPE_STATUS_RESPONSE = "security.keyprov.v1.KeyProvisioningStatusResponse";

    /** Envelope 固定 service 与 protocolVersion */
    public static final String ENVELOPE_SERVICE = "security.keyprov";
    public static final String PROTOCOL_VERSION = "keyprov-v1";

    /** 稳定协议错误码（ProtocolError.code） */
    public static final String ERR_NOT_FOUND = "KEYPROV_PROVISIONING_NOT_FOUND";
    public static final String ERR_PURPOSE_INCOMPLETE = "KEYPROV_PURPOSE_INCOMPLETE";
    public static final String ERR_ACK_CONFLICT = "KEYPROV_ACK_DIGEST_CONFLICT";
    public static final String ERR_STATE_NOT_ALLOWED = "KEYPROV_STATE_NOT_ALLOWED";
    public static final String ERR_ITEM_MISMATCH = "KEYPROV_ACK_ITEM_MISMATCH";
    public static final String ERR_DEVICE_NOT_ELIGIBLE = "KEYPROV_DEVICE_NOT_ELIGIBLE";
    public static final String ERR_EXPIRED = "KEYPROV_EXPIRED";
    public static final String ERR_PROTOCOL_INVALID = "KEYPROV_PROTOCOL_INVALID";
    public static final String ERR_UNAVAILABLE = "KEYPROV_VMD_UNAVAILABLE";
    public static final String ERR_IDEMPOTENCY_CONFLICT = "KEYPROV_IDEMPOTENCY_CONFLICT";
    public static final String ERR_IMPORT_FAILED = "KEYPROV_IMPORT_FAILED";
    public static final String ERR_RECONCILE_REQUIRED = "KEYPROV_RECONCILE_REQUIRED";

    private static final Map<KeyPurpose, String> PURPOSE_TO_CODE = new LinkedHashMap<>();
    private static final Map<WrappingSuite, String> SUITE_TO_CODE = new LinkedHashMap<>();

    static {
        PURPOSE_TO_CODE.put(KeyPurpose.KEY_PURPOSE_UPLINK_AEAD, PURPOSE_UPLINK_AEAD);
        PURPOSE_TO_CODE.put(KeyPurpose.KEY_PURPOSE_DOWNLINK_AEAD, PURPOSE_DOWNLINK_AEAD);

        SUITE_TO_CODE.put(WrappingSuite.WRAPPING_SUITE_HPKE_BASE_X25519_SHA256_AES128GCM,
                SUITE_HPKE_BASE_X25519_SHA256_AES128GCM);
        SUITE_TO_CODE.put(WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM);
    }

    /**
     * proto KeyPurpose → VMD 用途代码（未知/UNSPECIFIED 返回 null）
     */
    public static String purposeCode(KeyPurpose purpose) {
        return PURPOSE_TO_CODE.get(purpose);
    }

    /**
     * VMD 用途代码 → proto KeyPurpose（未知返回 UNSPECIFIED）
     */
    public static KeyPurpose purposeProto(String code) {
        for (Map.Entry<KeyPurpose, String> e : PURPOSE_TO_CODE.entrySet()) {
            if (e.getValue().equals(code)) {
                return e.getKey();
            }
        }
        return KeyPurpose.KEY_PURPOSE_UNSPECIFIED;
    }

    /**
     * proto WrappingSuite → 套件代码（未知/UNSPECIFIED 返回 null）
     */
    public static String suiteCode(WrappingSuite suite) {
        return SUITE_TO_CODE.get(suite);
    }

    /**
     * 套件代码 → proto WrappingSuite（未知返回 UNSPECIFIED）
     */
    public static WrappingSuite suiteProto(String code) {
        for (Map.Entry<WrappingSuite, String> e : SUITE_TO_CODE.entrySet()) {
            if (e.getValue().equals(code)) {
                return e.getKey();
            }
        }
        return WrappingSuite.WRAPPING_SUITE_UNSPECIFIED;
    }

    /**
     * 是否 V2C 完整用途集合（恰为 {UPLINK_AEAD, DOWNLINK_AEAD}，无重复、无 UNSPECIFIED）
     */
    public static boolean isCompleteV2cPurposeSet(Set<String> purposes) {
        return purposes != null && purposes.equals(V2C_PURPOSE_SET);
    }

    /**
     * 是否成功结果
     */
    public static boolean isSucceeded(KeyProvisioningResult result) {
        return result == KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED;
    }

    /**
     * proto KeyImportResult → 批次明细设备结果代码
     */
    public static String importResultCode(KeyImportResult result) {
        return switch (result) {
            case KEY_IMPORT_RESULT_IMPORTED -> "SUCCEEDED";
            case KEY_IMPORT_RESULT_FAILED -> "FAILED";
            case KEY_IMPORT_RESULT_UNKNOWN -> "UNKNOWN";
            default -> null;
        };
    }
}
