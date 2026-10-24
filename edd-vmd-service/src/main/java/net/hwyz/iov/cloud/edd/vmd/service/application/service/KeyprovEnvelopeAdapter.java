package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KeyprovEnvelopeIntegrityException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKeyEnvelopeView;
import net.hwyz.iov.cloud.framework.security.crypto.model.EcdhEsP256EnvelopeViewV1;
import net.hwyz.iov.security.keyprov.v1.EcdhEsP256EnvelopeParametersV1;
import net.hwyz.iov.security.keyprov.v1.KeyEnvelopeFormat;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * VMD keyprov 协议 Adapter：FW-SEC 结构化信封视图 → PAR-PROTO canonical wire（VMD-DSN-CR-062 §4）。
 * <p>
 * 职责（RD-062-1/2/8/9）：
 * <ul>
 *   <li>Request：对 {@code supported_wrapping_suites}/{@code supported_envelope_formats} 去重校验，
 *       仅当显式声明并命中 ECDH-ES + {@code ECDH_ES_P256_V1} allowlist 时才允许 envelope v1，否则
 *       不下发、不降级（缺失/UNSPECIFIED/未知关键值 fail-closed）；</li>
 *   <li>Response：只读取公开 {@link DerivedWrappedKeyEnvelopeView}，先调用
 *       {@link DerivedEnvelopeDigestVerifier#verify(DerivedWrappedKeyEnvelopeView)} 复核 canonical
 *       digest，再用锁定的 PAR-PROTO 官方生成类一次性构建并 deterministic serialize {@code parameters}，
 *       最后校验 view→wire 字段一一对应后返回 {@link WrappedKeyItem}；</li>
 *   <li>不解析私有 {@code envelopeBytes}，不手写 Protobuf codec，不按 wire framing 计算第二套摘要。</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyprovEnvelopeAdapter {

    /** 首期唯一允许的信封格式标识（framework 视图常量）。 */
    private static final String ENVELOPE_FORMAT_V1 = EcdhEsP256EnvelopeViewV1.ENVELOPE_FORMAT;

    private final ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider;

    /**
     * Request envelope v1 能力协商（fail-closed）。
     *
     * @param request canonical KeyProvisioningRequest
     * @return 命中 allowlist 返回 {@link KeyEnvelopeFormat#KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1}，否则 {@code null}
     */
    public KeyEnvelopeFormat negotiateEnvelopeFormat(KeyProvisioningRequest request) {
        if (request == null) {
            return null;
        }
        Set<KeyEnvelopeFormat> formats = new LinkedHashSet<>();
        for (KeyEnvelopeFormat format : request.getSupportedEnvelopeFormatsList()) {
            if (format == null || format == KeyEnvelopeFormat.UNRECOGNIZED
                    || format == KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_UNSPECIFIED) {
                // 未知/UNSPECIFIED 关键值 fail-closed，不做前缀或别名推断
                return null;
            }
            formats.add(format);
        }
        if (!formats.contains(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1)) {
            return null;
        }
        boolean ecdhDeclared = request.getSupportedWrappingSuitesList().stream()
                .anyMatch(suite -> suite == WrappingSuite.WRAPPING_SUITE_ECDH_ES_P256_HKDF_SHA256_AES256GCM);
        if (!ecdhDeclared) {
            return null;
        }
        return KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1;
    }

    /**
     * 将派生封装结果映射为 canonical {@link WrappedKeyItem}（固定流水线）。
     *
     * @param purposeCode VMD 用途代码（UPLINK_AEAD/DOWNLINK_AEAD）
     * @param directory   目标业务密钥目录行（用于 keyAlgorithm/有效期）
     * @param source      framework 根派生封装结果
     * @return canonical WrappedKeyItem
     * @throws KeyprovEnvelopeIntegrityException view 为空、版本不支持、摘要不一致或字段不对应（不可重试）
     */
    public WrappedKeyItem toWrappedKeyItem(String purposeCode, DeviceBusinessKey directory,
                                           DerivedWrappedKey source) {
        Objects.requireNonNull(source, "DerivedWrappedKey must not be null");
        DerivedWrappedKeyEnvelopeView view = source.envelope();
        if (view == null) {
            // CR-062 RD-062-9：兼容构造的 envelope=null 仅允许旧结果查询/对账，禁止新 envelope v1 下发
            throw new KeyprovEnvelopeIntegrityException(
                    "DerivedWrappedKey.envelope 为空（旧格式结果），禁止下发 envelope v1 且不得回退解析 envelopeBytes");
        }
        if (!(view instanceof EcdhEsP256EnvelopeViewV1 v1)) {
            throw new KeyprovEnvelopeIntegrityException("不受支持的信封视图实现: " + view.getClass().getName());
        }
        if (!ENVELOPE_FORMAT_V1.equals(view.envelopeFormat())
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

        // 公开 canonical 校验：唯一位于 Proto 映射之前
        DerivedEnvelopeDigestVerifier verifier = envelopeDigestVerifierProvider.getIfAvailable();
        if (verifier == null) {
            throw new KeyprovEnvelopeIntegrityException(
                    "DerivedEnvelopeDigestVerifier 未装配（crypto.provisioning.enabled=false）");
        }
        verifier.verify(view);

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

        WrappedKeyItem.Builder builder = WrappedKeyItem.newBuilder()
                .setPurpose(KeyprovConstants.purposeProto(purposeCode))
                .setKeyId(v1.logicalKeyId())
                .setBusinessKeyVersion(v1.businessKeyVersion())
                .setKeyAlgorithm(canonicalKeyAlgorithm(directory))
                .setWrappingSuite(KeyprovConstants.suiteProto(v1.wrappingSuite().name()))
                .setWrappedKey(ByteString.copyFrom(v1.ciphertext()))
                .setParameters(ByteString.copyFrom(parametersBytes))
                .setEnvelopeDigest(ByteString.copyFrom(v1.envelopeDigest()))
                .setKeyCheckValue(ByteString.copyFrom(v1.keyCheckValue()))
                .setEnvelopeFormat(KeyEnvelopeFormat.KEY_ENVELOPE_FORMAT_ECDH_ES_P256_V1);
        if (directory != null && directory.getValidFrom() != null) {
            builder.setValidFromMs(toEpochMs(directory.getValidFrom()));
        }
        if (directory != null && directory.getValidTo() != null) {
            builder.setValidToMs(toEpochMs(directory.getValidTo()));
        }
        return builder.build();
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
}
