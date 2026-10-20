package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.RegisteredCertificateProfile;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateIssuedProfileMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateMaterialUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificatePublicKeyBindingMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateUidBindingMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.framework.security.crypto.TboxDeviceCertificateValidator;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceMismatchException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateEvidenceRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateIdempotencyConflictException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateNotReadyException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileVerificationUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusConfigurationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusDependencyUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusProtocolException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateStatusTrustValidationException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.InvalidCertificateRequestException;
import net.hwyz.iov.cloud.framework.security.crypto.model.ValidatedTboxDeviceCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 签发结果复验器（VMD-DSN-CR-063 §7，FW-SEC CR-017 适配）。
 * <p>
 * 固定流程（仅 keyprovEligible 逻辑 Profile 启用；legacy sign-only Profile 跳过以保持兼容）：
 * <pre>
 * IssuedCertificate leaf + chain
 * → leaf SPKI SHA-256 == 登记 publicKeySha256（CSR SPKI 绑定）
 * → leaf Subject CN（规范化）== 权威 hsmUid（UID 绑定）
 * → DeviceCertificateRef.ofCertificate(leafDer)
 * → TboxDeviceCertificateValidator.validate(TBOX_DEVICE_ECDH_ES_P256_V1)
 * → 校验 p256/digitalSignature/keyAgreement/clientAuth 全真
 * → leafCertificateDigest = SHA-256(原始 leaf DER)
 * → profileValidatedAt = now
 * </pre>
 * 任何一方失败抛稳定异常且不得把证书状态置为 ACTIVE；材料/依赖暂不可用映射为可重试
 * {@link CertificateMaterialUnavailableException}，确定性拒绝映射为
 * {@link CertificateIssuedProfileMismatchException} / {@link CertificatePublicKeyBindingMismatchException} /
 * {@link CertificateUidBindingMismatchException}。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
@Component
public class IssuedCertificateVerifier {

    private final CertificateProfileRegistry certificateProfileRegistry;
    private final ObjectProvider<TboxDeviceCertificateValidator> tboxValidatorProvider;

    public IssuedCertificateVerifier(CertificateProfileRegistry certificateProfileRegistry,
                                     ObjectProvider<TboxDeviceCertificateValidator> tboxValidatorProvider) {
        this.certificateProfileRegistry = certificateProfileRegistry;
        this.tboxValidatorProvider = tboxValidatorProvider;
    }

    /**
     * 签发后复验并登记 leaf digest / 复验时间。
     *
     * @param certificate 证书记录（须含 publicKeySha256 / hsmUid / certificateProfile 快照）
     * @param issuedCert  框架签发的叶子证书（原始 leaf DER）
     * @throws CertificatePublicKeyBindingMismatchException  leaf SPKI 与 CSR SPKI 不一致
     * @throws CertificateUidBindingMismatchException        leaf Subject CN 与权威 UID 不一致
     * @throws CertificateIssuedProfileMismatchException     FW-SEC 复验拒绝或语义不满足
     * @throws CertificateMaterialUnavailableException       材料/依赖暂不可用（可重试）
     */
    public void verifyPostIssuance(VehicleCertificate certificate, net.hwyz.iov.cloud.framework.security.crypto.model.IssuedCertificate issuedCert) {
        if (certificate == null || issuedCert == null || issuedCert.leafCertificate() == null
                || issuedCert.leafCertificate().length == 0) {
            throw new CertificateIssuedProfileMismatchException("签发结果缺少叶子证书材料");
        }
        RegisteredCertificateProfile registered;
        try {
            registered = certificateProfileRegistry.requireEnabled(certificate.getCertificateProfile());
        } catch (RuntimeException e) {
            throw new CertificateIssuedProfileMismatchException(
                    "证书 Profile 未注册/停用，无法复验: " + certificate.getCertificateProfile());
        }
        if (!registered.keyprovEligible()) {
            log.debug("legacy Profile 跳过签发后复验: profile={}", certificate.getCertificateProfile());
            return;
        }

        byte[] leafDer = issuedCert.leafCertificate();
        X509Certificate x509 = parseLeafCertificate(leafDer);

        // 1. leaf SPKI == CSR SPKI（以登记 publicKeySha256 为 CSR 公钥事实源）
        String leafSpkiSha256 = sha256Hex(x509.getPublicKey().getEncoded());
        if (certificate.getPublicKeySha256() == null
                || !Objects.equals(certificate.getPublicKeySha256().toLowerCase(java.util.Locale.ROOT),
                leafSpkiSha256.toLowerCase(java.util.Locale.ROOT))) {
            throw new CertificatePublicKeyBindingMismatchException(
                    "leaf SPKI 与 CSR SPKI 不一致（post-issuance 复验拒绝）");
        }

        // 2. leaf Subject CN（规范化）== 权威 hsmUid
        String leafCn = parseLeafSingleCn(x509);
        if (leafCn == null || !Objects.equals(CsrUtils.normalizeUid(leafCn), CsrUtils.normalizeUid(certificate.getHsmUid()))) {
            throw new CertificateUidBindingMismatchException(
                    "leaf Subject CN=" + leafCn + " 与权威 hsmUid=" + certificate.getHsmUid() + " 不一致");
        }

        // 3. FW-SEC CR-017 复验（链/时间/吊销/CA=false/P-256/双 KU/clientAuth/critical 扩展）
        TboxDeviceCertificateValidator validator = tboxValidatorProvider.getIfAvailable();
        if (validator == null) {
            throw new CertificateMaterialUnavailableException(
                    "FW-SEC TboxDeviceCertificateValidator 未装配，无法完成签发后复验");
        }
        ValidatedTboxDeviceCertificate validated;
        try {
            validated = validator.validate(DeviceCertificateRef.ofCertificate(leafDer), registered.frameworkProfile());
        } catch (CertificateProfileRejectedException | CertificateEvidenceRejectedException
                 | CertificateEvidenceMismatchException | InvalidCertificateRequestException
                 | CertificateProfileNotAllowedException | CertificateIdempotencyConflictException e) {
            throw new CertificateIssuedProfileMismatchException("FW-SEC 复验拒绝: " + e.getMessage());
        } catch (net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateMaterialUnavailableException
                 | CertificateNotReadyException
                 | CertificateProfileVerificationUnavailableException | CertificateReferenceAmbiguousException
                 | CertificateStatusOutcomeUnknownException | CertificateStatusDependencyUnavailableException
                 | CertificateStatusConfigurationException | CertificateStatusTrustValidationException
                 | CertificateStatusProtocolException e) {
            throw new CertificateMaterialUnavailableException("FW-SEC 复验依赖暂不可用: " + e.getMessage());
        }
        if (validated == null || !validated.p256() || !validated.digitalSignature()
                || !validated.keyAgreement() || !validated.clientAuth()) {
            throw new CertificateIssuedProfileMismatchException(
                    "FW-SEC 复验语义不满足: p256=" + (validated != null && validated.p256())
                            + ", digitalSignature=" + (validated != null && validated.digitalSignature())
                            + ", keyAgreement=" + (validated != null && validated.keyAgreement())
                            + ", clientAuth=" + (validated != null && validated.clientAuth()));
        }

        // 4. leaf digest = SHA-256(原始 leaf DER)；复验时间（审计，非认证事实）
        certificate.setLeafCertificateDigest(sha256(leafDer));
        certificate.setProfileValidatedAt(LocalDateTime.now());
        log.info("签发后复验通过: requestId={}, profile={}", certificate.getRequestId(), certificate.getCertificateProfile());
    }

    /**
     * 解析叶子证书 X.509；失败抛 {@link CertificateIssuedProfileMismatchException}（材料不可解析，fail-closed）
     */
    private X509Certificate parseLeafCertificate(byte[] leafDer) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(leafDer));
        } catch (Exception e) {
            throw new CertificateIssuedProfileMismatchException("叶子证书 DER 解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析叶子证书单值 CN；缺失/多值返回 null（fail-closed）
     */
    private String parseLeafSingleCn(X509Certificate x509) {
        try {
            X500Name name = new X500Name(x509.getSubjectX500Principal().getName());
            RDN[] cnRdns = name.getRDNs(BCStyle.CN);
            if (cnRdns.length != 1) {
                return null;
            }
            String cn = cnRdns[0].getFirst().getValue().toString();
            return (cn == null || cn.isEmpty()) ? null : cn;
        } catch (Exception e) {
            log.warn("叶子证书 Subject CN 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new CertificateMaterialUnavailableException("SHA-256 摘要计算失败: " + e.getMessage());
        }
    }

    private String sha256Hex(byte[] data) {
        byte[] digest = sha256(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
