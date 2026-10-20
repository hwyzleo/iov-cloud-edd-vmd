package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.DefaultCertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrClientAuthMissingException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrContainsForbiddenValueException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrEkuConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrKeyUsageInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrPublicKeyNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrSubjectMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrUnsupportedCriticalExtensionException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateDeviceCategoryMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.KeyUsageFlag;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestTboxEcdhCsr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CertificateIdentityValidator 单元测试（CR-054）
 * <p>
 * 覆盖 CSR 密码学（解析/验签 PoP）、CN==hsm_uid、声明 ecu_uid 一致性、Subject/SAN 禁止项、Profile 白名单。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
class CertificateIdentityValidatorTest {

    private static final String VIN = "HWYZTEST900000001";
    private static final String DEVICE_SN = "00000005AA00000001";
    private static final String HSM_UID = "00000000000000000000000000000001";
    private static final String PROFILE = "TBOX_TSP_CLIENT";
    private static final String ECDH_V1_PROFILE = "TBOX_DEVICE_ECDH_ES_P256_V1";

    private CertificateIdentityValidator validator;
    private BoundDeviceIdentity identity;

    @BeforeEach
    void setUp() {
        // CR-063：注册表启用逻辑 v1 Profile，校验内核按 Profile 分支（legacy TBOX_TSP_CLIENT 保持原口径）
        validator = new CertificateIdentityValidator(
                new DefaultCertificateProfileRegistry(true, "TBOX_DEVICE_ECDH_ES_P256_V1"),
                new DeviceCertificateIdentityPolicy());
        identity = new BoundDeviceIdentity(VIN, 1L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
    }

    @Test
    void validate_CN等于绑定hsmUid_应返回解析要素() {
        // Given：CN=hsm_uid（合法 TBOX CSR），device_sn 是另一个业务序列号
        String csr = buildCsr(HSM_UID, null, null);

        // When
        ParsedCsr parsed = validator.validate(csr, identity, null, PROFILE);

        // Then
        assertNotNull(parsed);
        assertEquals(HSM_UID, parsed.subjectCn());
        assertNotNull(parsed.spkiSha256());
        assertNotNull(parsed.csrFingerprint());
        assertEquals(64, parsed.spkiSha256().length());
        assertEquals(64, parsed.csrFingerprint().length());
    }

    @Test
    void validate_CN为deviceSn但不等于绑定hsmUid_应拒绝() {
        // Given：CN=device_sn（旧口径），与绑定 hsm_uid 不一致
        String csr = buildCsr(DEVICE_SN, null, null);

        // When & Then：806044，PKI 不应被调用
        CertificateCsrSubjectMismatchException ex = assertThrows(CertificateCsrSubjectMismatchException.class,
                () -> validator.validate(csr, identity, null, PROFILE));
        assertEquals("806044", ex.getErrorCode().getCode());
    }

    @Test
    void validate_CN大小写与前缀规范化等价_应通过() {
        // Given：绑定 hsm_uid 全大写，CSR CN 小写且带 0x 前缀——规范化后相等
        BoundDeviceIdentity lowerIdentity = new BoundDeviceIdentity(
                VIN, 1L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        String csr = buildCsr("0x" + HSM_UID.toLowerCase(), null, null);

        // When & Then
        ParsedCsr parsed = validator.validate(csr, lowerIdentity, null, PROFILE);
        assertNotNull(parsed);
    }

    @Test
    void validate_声明ecuUid与绑定不一致_应拒绝() {
        // Given：CSR CN 正确，但调用方声明 ecu_uid 与绑定 hsm_uid 不一致
        String csr = buildCsr(HSM_UID, null, null);

        // When & Then：806044
        assertThrows(CertificateCsrSubjectMismatchException.class,
                () -> validator.validate(csr, identity, "00000000000000000000000000000009", PROFILE));
    }

    @Test
    void validate_SAN包含VIN_应拒绝() {
        // Given：CN 正确，但 SAN 携带 VIN（第二真相源，CR-015 §4 禁止）
        String csr = buildCsr(HSM_UID, "dns:" + VIN + ".internal", null);

        // When & Then：806045
        CertificateCsrContainsForbiddenValueException ex =
                assertThrows(CertificateCsrContainsForbiddenValueException.class,
                        () -> validator.validate(csr, identity, null, PROFILE));
        assertEquals("806045", ex.getErrorCode().getCode());
    }

    @Test
    void validate_Subject其他RDN包含deviceSn_应拒绝() {
        // Given：CN 正确，但 Subject 其他属性携带 device_sn
        String csr = buildCsr(HSM_UID, null, "CN=" + HSM_UID + ",OU=" + DEVICE_SN + ",O=OpenIOV,C=CN");

        // When & Then：806045
        assertThrows(CertificateCsrContainsForbiddenValueException.class,
                () -> validator.validate(csr, identity, null, PROFILE));
    }

    @Test
    void validate_CSR签名无效_应拒绝() {
        // Given：公钥与签名不匹配（用另一私钥签名）
        String csr = buildTamperedSignatureCsr(HSM_UID);

        // When & Then：806043
        CertificateCsrInvalidException ex = assertThrows(CertificateCsrInvalidException.class,
                () -> validator.validate(csr, identity, null, PROFILE));
        assertEquals("806043", ex.getErrorCode().getCode());
    }

    @Test
    void validate_CSR畸形_应拒绝() {
        // Given：非 CSR 的畸形输入
        String malformed = Base64.getEncoder().encodeToString("NOT_A_REAL_CSR".getBytes());

        // When & Then：806043（fail-closed）
        CertificateCsrInvalidException ex = assertThrows(CertificateCsrInvalidException.class,
                () -> validator.validate(malformed, identity, null, PROFILE));
        assertEquals("806043", ex.getErrorCode().getCode());
    }

    @Test
    void validate_Profile不在白名单_应拒绝() {
        // Given：CN 正确但 Profile 非法
        String csr = buildCsr(HSM_UID, null, null);

        // When & Then：806046
        assertThrows(CertificateProfileNotAllowedException.class,
                () -> validator.validate(csr, identity, null, "UNKNOWN_PROFILE"));
    }

    // ================= CR-063：逻辑 v1 Profile 双用途门禁（TBOX_DEVICE_ECDH_ES_P256_V1） =================

    @Test
    void validate_合法v1双用途CSR_应通过并返回完整要素() {
        // Given：EC P-256 + KU(DS+KA) + EKU(clientAuth) + CN=hsm_uid
        String csr = TestTboxEcdhCsr.valid(HSM_UID);

        // When
        ParsedCsr parsed = validator.validate(csr, identity, null, ECDH_V1_PROFILE);

        // Then：完整 ParsedCsr（含公钥/用途/扩展）
        assertNotNull(parsed);
        assertEquals(HSM_UID, parsed.subjectCn());
        assertTrue(parsed.selfSignatureValid());
        assertNotNull(parsed.publicKey());
        assertTrue(parsed.requestedKeyUsages().contains(KeyUsageFlag.DIGITAL_SIGNATURE));
        assertTrue(parsed.requestedKeyUsages().contains(KeyUsageFlag.KEY_AGREEMENT));
        assertTrue(parsed.requestedExtendedKeyUsages()
                .contains(DeviceCertificateIdentityPolicy.OID_EKU_CLIENT_AUTH));
    }

    @Test
    void validate_v1RSA公钥_应拒绝806112() {
        String csr = TestTboxEcdhCsr.rsa(HSM_UID);
        CertificateCsrPublicKeyNotAllowedException ex = assertThrows(
                CertificateCsrPublicKeyNotAllowedException.class,
                () -> validator.validate(csr, identity, null, ECDH_V1_PROFILE));
        assertEquals("806112", ex.getErrorCode().getCode());
    }

    @Test
    void validate_v1缺keyAgreement_应拒绝806113() {
        String csr = TestTboxEcdhCsr.kuMissingKeyAgreement(HSM_UID);
        CertificateCsrKeyUsageInvalidException ex = assertThrows(
                CertificateCsrKeyUsageInvalidException.class,
                () -> validator.validate(csr, identity, null, ECDH_V1_PROFILE));
        assertEquals("806113", ex.getErrorCode().getCode());
    }

    @Test
    void validate_v1EKU过宽_应拒绝806115() {
        String csr = TestTboxEcdhCsr.ekuClientAndServerAuth(HSM_UID);
        CertificateCsrEkuConflictException ex = assertThrows(
                CertificateCsrEkuConflictException.class,
                () -> validator.validate(csr, identity, null, ECDH_V1_PROFILE));
        assertEquals("806115", ex.getErrorCode().getCode());
    }

    @Test
    void validate_v1缺clientAuth_应拒绝806114() {
        String csr = TestTboxEcdhCsr.ekuAbsent(HSM_UID);
        CertificateCsrClientAuthMissingException ex = assertThrows(
                CertificateCsrClientAuthMissingException.class,
                () -> validator.validate(csr, identity, null, ECDH_V1_PROFILE));
        assertEquals("806114", ex.getErrorCode().getCode());
    }

    @Test
    void validate_v1未知critical扩展_应拒绝806116() {
        String csr = TestTboxEcdhCsr.unknownCriticalExtension(HSM_UID);
        CertificateCsrUnsupportedCriticalExtensionException ex = assertThrows(
                CertificateCsrUnsupportedCriticalExtensionException.class,
                () -> validator.validate(csr, identity, null, ECDH_V1_PROFILE));
        assertEquals("806116", ex.getErrorCode().getCode());
    }

    @Test
    void validate_v1设备类别不匹配_应拒绝806042() {
        String csr = TestTboxEcdhCsr.valid(HSM_UID);
        BoundDeviceIdentity nonTbox = new BoundDeviceIdentity(VIN, 1L, 1L, DEVICE_SN, "ECU", HSM_UID, "BOTH");
        CertificateDeviceCategoryMismatchException ex = assertThrows(
                CertificateDeviceCategoryMismatchException.class,
                () -> validator.validate(csr, nonTbox, null, ECDH_V1_PROFILE));
        assertEquals("806042", ex.getErrorCode().getCode());
    }

    @Test
    void validate_legacyProfile不受双用途门禁影响() {
        // Given：legacy TBOX_TSP_CLIENT 的 KU 仅 digitalSignature（旧口径），不应触发 v1 KU 门禁
        String csr = buildCsr(HSM_UID, null, null);

        // When & Then：仍通过（保持存量行为）
        ParsedCsr parsed = validator.validate(csr, identity, null, PROFILE);
        assertNotNull(parsed);
        assertFalse(parsed.requestedKeyUsages().contains(KeyUsageFlag.KEY_AGREEMENT));
    }

    /**
     * 生成真实 PKCS#10 自签名 CSR（ECDSA P-256）
     *
     * @param cn       CN
     * @param sanDns   SAN DNS（可为空）
     * @param subject  自定义 Subject（为空时用 CN=cn,OU=TBOX-TSP,O=OpenIOV,C=CN）
     */
    private static String buildCsr(String cn, String sanDns, String subject) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            X500Name x500Subject = subject != null
                    ? new X500Name(subject)
                    : new X500Name("CN=" + cn + ",OU=TBOX-TSP,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    x500Subject, SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            extGen.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
            if (sanDns != null) {
                extGen.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(new GeneralName(GeneralName.dNSName, sanDns)));
            }
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }

    /**
     * 生成签名无效的 CSR：公钥与签名不匹配
     */
    private static String buildTamperedSignatureCsr(String cn) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pubKeyPair = kpg.generateKeyPair();
            KeyPair signKeyPair = kpg.generateKeyPair();
            X500Name subject = new X500Name("CN=" + cn + ",OU=TBOX-TSP,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(pubKeyPair.getPublic().getEncoded()));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signKeyPair.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成篡改CSR失败", e);
        }
    }

}
