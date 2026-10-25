package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.model.EnvelopeAadContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite;
import net.hwyz.iov.cloud.framework.security.crypto.provider.EnvelopeAadEncoder;
import org.springframework.stereotype.Component;

/**
 * PAR-PROTO Keyprov AAD profile Encoder（VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009 §2.2）。
 * <p>
 * 实现 FW-SEC {@link EnvelopeAadEncoder} SPI：FW-SEC 在 AES-GCM 加密<strong>前</strong>调用
 * {@link #encode(EnvelopeAadContext)}，输出 PAR-PROTO canonical AAD（10 段 registry）作为
 * AES-GCM 认证输入。加密完成后 VMD 不得替换 canonical_aad 或重算 tag。
 * <ul>
 *   <li>profileId 固定 {@code PAR_PROTO_KEYPROV_AAD_V1}（与 {@code crypto.provisioning.derived-key.aad-profile-id} 一致）；</li>
 *   <li>仅实现 PAR-PROTO canonical helper，不依赖业务密钥、不解析 FW 私有 envelopeBytes；</li>
 *   <li>确定性输出（FW registry 构造期 probe 校验非空、≤1 KiB、两次编码一致）；</li>
 *   <li>未知/非白名单 suite、非法 formatVersion、负值一律 fail-closed 抛异常。</li>
 * </ul>
 * 注册方式：本类为 Spring {@code @Component}，FW {@code EnvelopeAadEncoderRegistry} 自动收集。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Component
public class ParProtoKeyprovAadEncoderV1 implements EnvelopeAadEncoder {

    /** PAR-PROTO proto 枚举值（与 key_provisioning.proto 一致；禁止 name()/ordinal()/十进制文本） */
    private static final int ENVELOPE_FORMAT_ECDH_ES_P256_V1 = 1;
    private static final int SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM = 2;
    private static final int PURPOSE_UPLINK_AEAD = 1;
    private static final int PURPOSE_DOWNLINK_AEAD = 2;

    /** 公开 AAD profile id（供配置/健康/测试引用） */
    public static final String PROFILE_ID = ParProtoWireCanonical.AAD_PROFILE;

    @Override
    public String profileId() {
        return PROFILE_ID;
    }

    @Override
    public byte[] encode(EnvelopeAadContext context) {
        if (context == null) {
            throw new IllegalStateException("PAR-PROTO AAD encode: EnvelopeAadContext 为空");
        }
        if (context.formatVersion() != net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1.FORMAT_VERSION_V1) {
            throw new IllegalStateException("PAR-PROTO AAD encode: 不受支持的 formatVersion=" + context.formatVersion());
        }
        int envelopeFormat = ENVELOPE_FORMAT_ECDH_ES_P256_V1;
        int suite = protoSuiteValue(context.wrappingSuite());
        int purpose = protoPurposeValue(context.purpose());
        byte[] aad = ParProtoWireCanonical.canonicalAad(
                envelopeFormat, suite, purpose,
                context.businessKeyVersion(), context.rootKeyVersion(),
                context.recipientCertificateDigest(),
                context.requestId(), context.businessDomain(), context.logicalKeyId(),
                context.derivationVersion());
        return aad;
    }

    /**
     * FW WrappingSuite → PAR-PROTO proto WrappingSuite 枚举值（首期仅允许 ECDH-ES，其余 fail-closed）。
     */
    private static int protoSuiteValue(WrappingSuite suite) {
        if (suite == WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM) {
            return SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM;
        }
        throw new IllegalStateException("PAR-PROTO AAD encode: 不受支持的 wrappingSuite=" + suite);
    }

    /**
     * FW KeyPurpose → PAR-PROTO proto KeyPurpose 枚举值（UPLINK=1 / DOWNLINK=2，禁止 ordinal）。
     */
    private static int protoPurposeValue(KeyPurpose purpose) {
        if (purpose == KeyPurpose.UPLINK_AEAD) {
            return PURPOSE_UPLINK_AEAD;
        }
        if (purpose == KeyPurpose.DOWNLINK_AEAD) {
            return PURPOSE_DOWNLINK_AEAD;
        }
        throw new IllegalStateException("PAR-PROTO AAD encode: 不受支持的 purpose=" + purpose);
    }
}
