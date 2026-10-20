package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrClientAuthMissingException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrEkuConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrKeyUsageInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrPublicKeyNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrUnsupportedCriticalExtensionException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestTboxEcdhCsr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeviceCertificateIdentityPolicy 单元测试（VMD-DSN-CR-063 §4.3）。
 * <p>
 * 覆盖逻辑 Profile v1 固定规则：EC P-256 公钥（拒 RSA/P-384）、KeyUsage 恰好
 * digitalSignature+keyAgreement、EKU 仅 clientAuth（缺 clientAuth / anyEKU / serverAuth / 过宽
 * 分别拒绝）、未知 critical 扩展拒绝、basicConstraints CA=true 拒绝。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
class DeviceCertificateIdentityPolicyTest {

    private static final String HSM_UID = "00000000000000000000000000000001";

    private DeviceCertificateIdentityPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DeviceCertificateIdentityPolicy();
    }

    private ParsedCsr parse(String csrDerBase64) {
        return new ParsedCsr(
                CsrUtils.parseCommonName(csrDerBase64),
                CsrUtils.calculatePublicKeySha256(csrDerBase64),
                CsrUtils.calculateFingerprint(csrDerBase64),
                CsrUtils.extractPublicKey(csrDerBase64),
                CsrUtils.extractKeyUsages(csrDerBase64),
                CsrUtils.extractExtendedKeyUsages(csrDerBase64),
                CsrUtils.extractRequestedExtensions(csrDerBase64),
                CsrUtils.verifySignature(csrDerBase64));
    }

    @Test
    void 合法双用途CSR_应通过() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.valid(HSM_UID));
        assertDoesNotThrow(() -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void RSA公钥_应拒绝806112() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.rsa(HSM_UID));
        CertificateCsrPublicKeyNotAllowedException ex = assertThrows(
                CertificateCsrPublicKeyNotAllowedException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806112", ex.getErrorCode().getCode());
    }

    @Test
    void P384曲线_应拒绝806112() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.p384(HSM_UID));
        assertThrows(CertificateCsrPublicKeyNotAllowedException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void 缺keyAgreement_应拒绝806113() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.kuMissingKeyAgreement(HSM_UID));
        CertificateCsrKeyUsageInvalidException ex = assertThrows(
                CertificateCsrKeyUsageInvalidException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806113", ex.getErrorCode().getCode());
    }

    @Test
    void 缺KU扩展_应拒绝806113() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.kuAbsent(HSM_UID));
        assertThrows(CertificateCsrKeyUsageInvalidException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void KU含keyCertSign_应拒绝806113() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.kuWithKeyCertSign(HSM_UID));
        assertThrows(CertificateCsrKeyUsageInvalidException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void 缺clientAuth_应拒绝806114() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.ekuAbsent(HSM_UID));
        CertificateCsrClientAuthMissingException ex = assertThrows(
                CertificateCsrClientAuthMissingException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806114", ex.getErrorCode().getCode());
    }

    @Test
    void EKU仅serverAuth_应拒绝806114() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.ekuServerAuthOnly(HSM_UID));
        assertThrows(CertificateCsrClientAuthMissingException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void EKU过宽_client加server_应拒绝806115() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.ekuClientAndServerAuth(HSM_UID));
        CertificateCsrEkuConflictException ex = assertThrows(
                CertificateCsrEkuConflictException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806115", ex.getErrorCode().getCode());
    }

    @Test
    void EKU含anyExtendedKeyUsage且无clientAuth_应拒绝806114() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.ekuAny(HSM_UID));
        // anyEKU 未含 clientAuth：命中 clientAuth 缺失（与“clientAuth 存在但过宽→806115”区分，RD 稳定 reason）
        CertificateCsrClientAuthMissingException ex = assertThrows(
                CertificateCsrClientAuthMissingException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806114", ex.getErrorCode().getCode());
    }

    @Test
    void 未知critical扩展_应拒绝806116() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.unknownCriticalExtension(HSM_UID));
        CertificateCsrUnsupportedCriticalExtensionException ex = assertThrows(
                CertificateCsrUnsupportedCriticalExtensionException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
        assertEquals("806116", ex.getErrorCode().getCode());
    }

    @Test
    void basicConstraintsCA意图_应拒绝806116() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.basicConstraintsCaTrue(HSM_UID));
        assertThrows(CertificateCsrUnsupportedCriticalExtensionException.class,
                () -> policy.validateTboxEcdhEsP256V1(csr));
    }

    @Test
    void ParsedCsr_toString_不得输出公钥或完整摘要() {
        ParsedCsr csr = parse(TestTboxEcdhCsr.valid(HSM_UID));
        String s = csr.toString();
        assertFalse(s.contains("SPKI") || s.contains("publicKey=") || s.contains(csr.spkiSha256()));
        assertTrue(s.contains("keyUsages="));
    }
}
