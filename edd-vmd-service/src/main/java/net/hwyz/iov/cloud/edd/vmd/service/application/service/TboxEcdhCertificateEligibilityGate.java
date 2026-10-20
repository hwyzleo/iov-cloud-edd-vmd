package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.VmdCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovCertificateProfileCapability;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovReadiness;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateLocator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 新 ECDH-ES Keyprov Request 前置门禁（VMD-DSN-CR-063 §10，CR-062 衔接）。
 * <p>
 * 进入新 Request 前按序校验：
 * <ol>
 *   <li>逻辑 Profile {@code TBOX_DEVICE_ECDH_ES_P256_V1} 已启用——enabled=false 时整个新
 *       ECDH-ES 请求路径关闭（可重试 NOT_READY，不回退到旧 Profile 继续请求）；</li>
 *   <li>active VehCertificate Profile == v1（sign-only 存量证书 fail-closed，RD-063-5）；</li>
 *   <li>leafCertificateDigest 存在（签发后复验已完成，RD-063-4）；</li>
 *   <li>{@link CertificateReferenceDirectory} 可解析该证书材料（pkiRequestId locator）；</li>
 *   <li>FW-SEC profile capability READY（{@link KeyprovReadiness#keyprovEcdhEsV1Ready()}）；</li>
 *   <li>CR-062 envelope/import-proof readiness READY（DerivedEnvelopeDigestVerifier +
 *       ImportProofVerifier + 根派生 Provider）。</li>
 * </ol>
 * 任一步不就绪即拒绝；Status/原 operation 对账保持安全只读能力（RD-063-6）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
@Component
public class TboxEcdhCertificateEligibilityGate {

    private final CertificateProfileRegistry certificateProfileRegistry;
    private final ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    private final ObjectProvider<KeyprovReadiness> keyprovReadinessProvider;
    private final DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate;
    private final ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider;
    private final ObjectProvider<ImportProofVerifier> importProofVerifierProvider;

    public TboxEcdhCertificateEligibilityGate(CertificateProfileRegistry certificateProfileRegistry,
                                              ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider,
                                              ObjectProvider<KeyprovReadiness> keyprovReadinessProvider,
                                              DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate,
                                              ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider,
                                              ObjectProvider<ImportProofVerifier> importProofVerifierProvider) {
        this.certificateProfileRegistry = certificateProfileRegistry;
        this.certificateDirectoryProvider = certificateDirectoryProvider;
        this.keyprovReadinessProvider = keyprovReadinessProvider;
        this.derivedKeyProviderReadinessGate = derivedKeyProviderReadinessGate;
        this.envelopeDigestVerifierProvider = envelopeDigestVerifierProvider;
        this.importProofVerifierProvider = importProofVerifierProvider;
    }

    /**
     * 门禁结果
     *
     * @param eligible   是否允许进入新 ECDH-ES Request
     * @param retryable  true=能力/依赖暂不可用（同 requestId 重试）；false=确定性拒绝（不得重试）
     * @param reason     拒绝原因（不含敏感材料）
     */
    public record GateResult(boolean eligible, boolean retryable, String reason) {

        public static GateResult notReady(String reason) {
            return new GateResult(false, true, reason);
        }

        public static GateResult rejected(String reason) {
            return new GateResult(false, false, reason);
        }
    }

    /**
     * 评估新 ECDH-ES Request 的证书前置门禁。
     *
     * @param activeCert 当前 active 设备证书（含 Profile / leaf digest / 材料 locator）
     * @return 门禁结果
     */
    public GateResult evaluate(ActiveDeviceCertificate activeCert) {
        // 1. 逻辑 Profile 未启用 → 整个新 ECDH-ES 请求路径关闭（不回退旧 Profile）
        if (!certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)) {
            return GateResult.notReady(
                    "逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1 未启用，新 ECDH-ES Request 路径关闭");
        }

        // 2. active 证书必须为逻辑 v1 Profile（sign-only 存量证书 fail-closed）
        if (activeCert == null
                || !VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1.name().equals(activeCert.certificateProfile())) {
            return GateResult.rejected(
                    "当前ACTIVE证书非逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1（sign-only 存量证书不适用新 ECDH-ES Keyprov）");
        }

        // 3. leafCertificateDigest 必须存在（签发后复验已完成）
        if (activeCert.leafCertificateDigest() == null || activeCert.leafCertificateDigest().length != 32) {
            return GateResult.rejected("ACTIVE证书缺少 leafCertificateDigest（未完成签发后复验），拒绝新 ECDH-ES Keyprov");
        }

        // 4. 证书目录可解析材料（pkiRequestId locator）
        if (!canResolveCertificateMaterial(activeCert)) {
            return GateResult.notReady("证书目录无法解析 ACTIVE 证书材料（locator 缺失），需以原 requestId 重试");
        }

        // 5. FW-SEC profile capability READY
        KeyprovReadiness readiness = keyprovReadinessProvider.getIfAvailable();
        boolean fwCapabilityReady = readiness != null && readiness.keyprovEcdhEsV1Ready();
        if (!fwCapabilityReady
                || !KeyprovCertificateProfileCapability.supports(
                        KeyprovCertificateProfileCapability.TBOX_DEVICE_CERT_PROFILE_ECDH_ES_P256_V1, true)) {
            return GateResult.notReady("FW-SEC TBOX ECDH-ES v1 证书 Profile capability 未就绪");
        }

        // 6. CR-062 envelope/import-proof readiness
        if (envelopeDigestVerifierProvider.getIfAvailable() == null
                || importProofVerifierProvider.getIfAvailable() == null
                || !derivedKeyProviderReadinessGate.isReady()) {
            return GateResult.notReady("CR-062 envelope/import-proof/根派生能力未就绪，拒绝新 ECDH-ES Request");
        }

        return new GateResult(true, false, "ok");
    }

    /**
     * 经 VMD 证书目录（cert_sn → pki_request_id）判定材料可解析；目录缺失视为不可解析（fail-closed）
     */
    private boolean canResolveCertificateMaterial(ActiveDeviceCertificate activeCert) {
        if (activeCert == null || activeCert.certSn() == null || activeCert.certSn().isBlank()) {
            return false;
        }
        CertificateReferenceDirectory directory = certificateDirectoryProvider.getIfAvailable();
        if (directory == null) {
            log.warn("证书目录未装配，Keyprov 门禁 fail-closed: certSn={}", activeCert.certSn());
            return false;
        }
        try {
            Optional<CertificateLocator> locator = directory.locate(DeviceCertificateRef.ofSerial(activeCert.certSn()));
            return locator != null && locator.isPresent();
        } catch (Exception e) {
            log.warn("证书目录解析失败，Keyprov 门禁 fail-closed: certSn={}, cause={}",
                    activeCert.certSn(), e.getClass().getSimpleName());
            return false;
        }
    }
}
