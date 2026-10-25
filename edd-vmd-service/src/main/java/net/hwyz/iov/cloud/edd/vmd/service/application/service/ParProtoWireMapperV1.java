package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKeyEnvelopeView;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite;
import net.hwyz.iov.security.keyprov.v1.EcdhEsP256EnvelopeParametersV1;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

/**
 * PAR-PROTO V2C Keyprov Wire Mapper（VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009 §2.1）。
 * <p>
 * 在最终 ciphertext/tag 与 deterministic parameters 确定后计算 9 字段 wireEnvelopeDigest，
 * 并组装 canonical {@link WrappedKeyItem}。职责（RD-068-2）：
 * <ul>
 *   <li>校验 FW 结构化 view、长度、suite/format 与 coreEnvelopeDigest 内部完整性；</li>
 *   <li>使用官方生成类构建 {@link EcdhEsP256EnvelopeParametersV1} 并 deterministic serialize；</li>
 *   <li>断言 {@code parameters.canonical_aad == view.canonicalAad} 且
 *       {@code aad_digest == SHA-256(canonical_aad)}；</li>
 *   <li>按 PAR-PROTO 9 字段 framing 计算 wireEnvelopeDigest 并写入 Response item
 *       （{@code envelope_digest} 为 wire 摘要，<strong>不是</strong> FW core digest）；</li>
 *   <li>core digest 仅作内部完整性/审计，不得进入 wire DTO。</li>
 * </ul>
 * 禁止解析 FW 私有 {@code envelopeBytes}、禁止手写 Protobuf 编码、禁止加密后替换 AAD 或重算 tag。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Slf4j
@Component
public class ParProtoWireMapperV1 {

    /** PAR-PROTO proto 枚举值（与 key_provisioning.proto 一致） */
    private static final int ENVELOPE_FORMAT_ECDH_ES_P256_V1 = 1;
    private static final int SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM = 2;
    private static final int PURPOSE_UPLINK_AEAD = 1;
    private static final int PURPOSE_DOWNLINK_AEAD = 2;

    private final ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider;

    public ParProtoWireMapperV1(ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider) {
        this.envelopeDigestVerifierProvider = envelopeDigestVerifierProvider;
    }

    /**
     * 将派生封装结果映射为 canonical {@link WrappedKeyItem} 并计算 wire digest（固定流水线）。
     *
     * @param source    framework 根派生封装结果（含结构化 view）
     * @param ctx        wire 上下文（requestId / businessDomain）
     * @param directory 目标业务密钥目录行（keyAlgorithm/有效期；可为 null，仅影响非核心字段）
     * @return 映射结果：canonical item + wire/core digest + parameters/wrappedKey digest + profile
     * @throws KeyprovEnvelopeIntegrityException view 为空、版本不支持、摘要不一致或字段不对应（不可重试）
     */
    public WireMappedItem map(DerivedWrappedKey source, KeyProvisioningWireContext ctx, DeviceBusinessKey directory) {
        Objects.requireNonNull(source, "DerivedWrappedKey must not be null");
        Objects.requireNonNull(ctx, "KeyProvisioningWireContext must not be null");
        DerivedWrappedKeyEnvelopeView view = source.envelope();
        if (view == null) {
            // CR-062 RD-062-9：兼容构造的 envelope=null 仅允许旧结果查询/对账，禁止新 envelope v1 下发
            throw new KeyprovEnvelopeIntegrityException(
                    "DerivedWrappedKey.envelope 为空（旧格式结果），禁止下发 envelope v1 且不得回退解析 envelopeBytes");
        }
        if (!(view instanceof EcdhEsP256EnvelopeViewV1 v1)) {
            throw new KeyprovEnvelopeIntegrityException("不受支持的信封视图实现: " + view.getClass().getName());
        }
        if (!EcdhEsP256EnvelopeViewV1.ENVELOPE_FORMAT.equals(view.envelopeFormat())
                || view.formatVersion() != EcdhEsP256EnvelopeViewV1.FORMAT_VERSION_V1) {
            throw new KeyprovEnvelopeIntegrityException("不受支持的信封 format/version: "
                    + view.envelopeFormat() + "/v" + view.formatVersion());
        }

        // 顶层元数据与结构化视图一致性（构造期已强制；此处防御性复核）
        if (source.wrappingSuite() != view.wrappingSuite()) {
            throw new KeyprovEnvelopeIntegrityException("顶层 wrappingSuite 与 envelope 视图不一致");
        }
        if (!Arrays.equals(source.envelopeDigest(), view.envelopeDigest())) {
            throw new KeyprovEnvelopeIntegrityException("顶层 envelopeDigest 与 envelope 视图不一致");
        }
        if (!Arrays.equals(source.kcv(), view.keyCheckValue())) {
            throw new KeyprovEnvelopeIntegrityException("顶层 KCV 与 envelope 视图不一致");
        }
        if (source.operationId() != null && !source.operationId().equals(v1.operationId())) {
            throw new KeyprovEnvelopeIntegrityException("顶层 operationId 与 envelope 视图不一致");
        }

        // 公开 canonical（core）校验：唯一位于 Proto 映射之前
        DerivedEnvelopeDigestVerifier verifier = envelopeDigestVerifierProvider.getIfAvailable();
        if (verifier == null) {
            throw new KeyprovEnvelopeIntegrityException(
                    "DerivedEnvelopeDigestVerifier 未装配（crypto.provisioning.enabled=false）");
        }
        verifier.verify(view);

        // 官方生成类构建 deterministic parameters（map 排序稳定、字段号顺序固定）
        EcdhEsP256EnvelopeParametersV1 parameters = EcdhEsP256EnvelopeParametersV1.newBuilder()
                .setFormatVersion(v1.formatVersion())
                .setServerEphemeralPublicKey(ByteString.copyFrom(v1.serverEphemeralPublicKey()))
                .setHkdfSalt(ByteString.copyFrom(v1.hkdfSalt()))
                .setHkdfContextVersion(v1.hkdfContextVersion())
                .setNonce(ByteString.copyFrom(v1.nonce()))
                .setAuthenticationTag(ByteString.copyFrom(v1.authenticationTag()))
                .setCanonicalAad(ByteString.copyFrom(v1.canonicalAad()))
                .setAadDigest(ByteString.copyFrom(v1.aadDigest()))
                .setRootKeyVersion(v1.rootKeyVersion())
                .setDerivationVersion(v1.derivationVersion())
                .setRecipientCertificateDigest(ByteString.copyFrom(v1.recipientCertificateDigest()))
                .setOperationId(v1.operationId())
                .build();
        byte[] parametersBytes = deterministicSerialize(parameters);
        assertViewWireParity(v1, parametersBytes);

        // 断言 canonical AAD 与 view 一致、aad_digest == SHA-256(canonical_aad)（RD-068-2 / CR-009 §3）
        assertAadConsistency(v1);

        // 9 字段 wire digest（wrappedKey = ciphertext；parameters = 原始 deterministic bytes）
        byte[] wireDigest = ParProtoWireCanonical.wireEnvelopeDigest(
                ENVELOPE_FORMAT_ECDH_ES_P256_V1,
                ctx.requestId(),
                ctx.businessDomain(),
                protoPurposeValue(v1.purpose()),
                v1.logicalKeyId(),
                v1.businessKeyVersion(),
                protoSuiteValue(v1.wrappingSuite()),
                v1.ciphertext(),
                parametersBytes);

        WrappedKeyItem.Builder builder = WrappedKeyItem.newBuilder()
                .setPurpose(KeyprovConstants.purposeProto(v1.purpose().name()))
                .setKeyId(v1.logicalKeyId())
                .setBusinessKeyVersion(v1.businessKeyVersion())
                .setKeyAlgorithm(canonicalKeyAlgorithm(directory))
                .setWrappingSuite(KeyprovConstants.suiteProto(v1.wrappingSuite().name()))
                .setWrappedKey(ByteString.copyFrom(v1.ciphertext()))
                .setParameters(ByteString.copyFrom(parametersBytes))
                // wire 权威：envelope_digest = wireEnvelopeDigest（非 FW core digest）
                .setEnvelopeDigest(ByteString.copyFrom(wireDigest))
                .setKeyCheckValue(ByteString.copyFrom(v1.keyCheckValue()))
                .setEnvelopeFormat(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1);
        if (directory != null) {
            // fail-closed：目录行缺有效期不得静默不下发（valid_from_ms=0 会被车端 SEC 以
            // WRAPPED_KEY_INVALID 拒绝导入）；缺有效期属内部数据缺陷，显式抛信封完整性异常
            if (directory.getValidFrom() == null || directory.getValidTo() == null) {
                throw new KeyprovEnvelopeIntegrityException(
                        "目录行缺少有效期（validFrom/validTo 缺失），拒绝下发 envelope: keyId="
                                + directory.getKeyId());
            }
            builder.setValidFromMs(toEpochMs(directory.getValidFrom()));
            builder.setValidToMs(toEpochMs(directory.getValidTo()));
        }
        return new WireMappedItem(
                builder.build(),
                wireDigest,
                v1.coreEnvelopeDigest(),
                ParProtoWireCanonical.sha256(parametersBytes),
                ParProtoWireCanonical.sha256(v1.ciphertext()),
                v1.aadProfileId() != null ? v1.aadProfileId() : ParProtoWireCanonical.AAD_PROFILE);
    }

    /**
     * view → proto KeyPurpose 枚举值（UPLINK=1 / DOWNLINK=2，禁止 ordinal()/name() 文本映射）。
     */
    private static int protoPurposeValue(KeyPurpose purpose) {
        if (purpose == KeyPurpose.UPLINK_AEAD) {
            return PURPOSE_UPLINK_AEAD;
        }
        if (purpose == KeyPurpose.DOWNLINK_AEAD) {
            return PURPOSE_DOWNLINK_AEAD;
        }
        throw new KeyprovEnvelopeIntegrityException("PAR-PROTO wire digest: 不受支持的 purpose=" + purpose);
    }

    /**
     * view → proto WrappingSuite 枚举值（首期仅允许 ECDH-ES，其余 fail-closed）。
     */
    private static int protoSuiteValue(WrappingSuite suite) {
        if (suite == WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM) {
            return SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM;
        }
        throw new KeyprovEnvelopeIntegrityException("PAR-PROTO wire digest: 不受支持的 wrappingSuite=" + suite);
    }

    private static KeyPurpose viewPurposeToProto(KeyPurpose purpose) {
        return purpose; // framework KeyPurpose 与 proto 名称一一对应（UPLINK_AEAD/DOWNLINK_AEAD）
    }

    /**
     * 使用官方生成类 deterministic serialization 编码 parameters（map 排序稳定、字段号顺序固定）。
     */
    private static byte[] deterministicSerialize(MessageLite message) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(message.getSerializedSize());
            CodedOutputStream output = CodedOutputStream.newInstance(buffer);
            output.useDeterministicSerialization();
            message.writeTo(output);
            output.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new KeyprovEnvelopeIntegrityException("parameters deterministic serialization 失败", e);
        }
    }

    /**
     * view→wire 字段一一对应校验：重新解析 deterministic bytes 并逐字段比对视图。
     */
    private static void assertViewWireParity(EcdhEsP256EnvelopeViewV1 view, byte[] parametersBytes) {
        EcdhEsP256EnvelopeParametersV1 parsed;
        try {
            parsed = EcdhEsP256EnvelopeParametersV1.parseFrom(parametersBytes);
        } catch (IOException e) {
            throw new KeyprovEnvelopeIntegrityException("parameters 反序列化失败", e);
        }
        boolean consistent = parsed.getFormatVersion() == view.formatVersion()
                && Arrays.equals(parsed.getServerEphemeralPublicKey().toByteArray(), view.serverEphemeralPublicKey())
                && Arrays.equals(parsed.getHkdfSalt().toByteArray(), view.hkdfSalt())
                && parsed.getHkdfContextVersion().equals(view.hkdfContextVersion())
                && Arrays.equals(parsed.getNonce().toByteArray(), view.nonce())
                && Arrays.equals(parsed.getAuthenticationTag().toByteArray(), view.authenticationTag())
                && Arrays.equals(parsed.getCanonicalAad().toByteArray(), view.canonicalAad())
                && Arrays.equals(parsed.getAadDigest().toByteArray(), view.aadDigest())
                && parsed.getRootKeyVersion() == view.rootKeyVersion()
                && parsed.getDerivationVersion().equals(view.derivationVersion())
                && Arrays.equals(parsed.getRecipientCertificateDigest().toByteArray(),
                        view.recipientCertificateDigest())
                && parsed.getOperationId().equals(view.operationId());
        if (!consistent) {
            throw new KeyprovEnvelopeIntegrityException("view→wire 字段不一一对应，拒绝返回部分 wire material");
        }
    }

    /**
     * 断言 parameters.canonical_aad 与 view.canonicalAad 逐字节一致，且 aad_digest == SHA-256(canonical_aad)
     * （RD-068-2；加密完成后不得替换 AAD）。
     */
    private static void assertAadConsistency(EcdhEsP256EnvelopeViewV1 view) {
        if (view.canonicalAad() == null || view.canonicalAad().length == 0) {
            throw new KeyprovEnvelopeIntegrityException("view.canonicalAad 为空，拒绝计算 wire digest");
        }
        if (view.aadDigest() == null || view.aadDigest().length != 32) {
            throw new KeyprovEnvelopeIntegrityException("view.aadDigest 缺失或长度非法，拒绝计算 wire digest");
        }
        byte[] recomputed = ParProtoWireCanonical.aadDigest(view.canonicalAad());
        if (!Arrays.equals(recomputed, view.aadDigest())) {
            throw new KeyprovEnvelopeIntegrityException("aad_digest != SHA-256(canonical_aad)，拒绝计算 wire digest");
        }
    }

    private static long toEpochMs(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 输出 ECDH-ES v1 信封下 key_algorithm 的协议 canonical 值。
     * <p>
     * 目录/KMS 策略可能存通用标签（如 {@code AES}，key-spec AES-256-GCM）；
     * PAR-PROTO §7.2 规定 v1 固定 {@code AES-256}，SEC 侧 allowlist 与
     * validateEnvelope 均硬编码 AES-256。故 wire 必须输出 canonical 值，
     * 不在目录行上改写（避免 claim-or-reuse algorithm_mismatch）。
     */
    private static String canonicalKeyAlgorithm(DeviceBusinessKey directory) {
        if (directory == null || directory.getAlgorithm() == null
                || directory.getAlgorithm().isBlank()) {
            return "";
        }
        String algorithm = directory.getAlgorithm();
        if ("AES".equals(algorithm)) {
            return KeyprovConstants.KEY_ALGORITHM_ECDH_ES_V1;
        }
        return algorithm;
    }

    /**
     * 确定性自检（readiness / 测试）：固定输入两次计算 wire digest 必须一致且长度 32。
     * 仅证明计算确定性，不构成与 PAR-PROTO 权威向量的一致性证据（后者由向量包逐字节比对）。
     */
    public static boolean deterministicSelfCheck() {
        byte[] a = ParProtoWireCanonical.wireEnvelopeDigest(
                ENVELOPE_FORMAT_ECDH_ES_P256_V1, "probe-request", "V2C_COMM",
                PURPOSE_UPLINK_AEAD, "logical-probe", 7L, SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                new byte[]{1, 2, 3}, new byte[]{4, 5, 6, 7});
        byte[] b = ParProtoWireCanonical.wireEnvelopeDigest(
                ENVELOPE_FORMAT_ECDH_ES_P256_V1, "probe-request", "V2C_COMM",
                PURPOSE_UPLINK_AEAD, "logical-probe", 7L, SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                new byte[]{1, 2, 3}, new byte[]{4, 5, 6, 7});
        return a != null && a.length == 32 && Arrays.equals(a, b);
    }

    /**
     * 映射结果：canonical item + 逐项摘要证据（wire/core digest、parameters/wrappedKey digest、profile）。
     */
    public record WireMappedItem(WrappedKeyItem item, byte[] wireEnvelopeDigest, byte[] coreEnvelopeDigest,
                                 byte[] parametersDigest, byte[] wrappedKeyDigest, String aadProfileId) {
    }
}
