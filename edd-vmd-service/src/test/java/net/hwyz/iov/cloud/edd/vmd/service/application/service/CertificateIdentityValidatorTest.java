package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrContainsForbiddenValueException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrSubjectMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateProfileNotAllowedException;
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

    private CertificateIdentityValidator validator;
    private BoundDeviceIdentity identity;

    @BeforeEach
    void setUp() {
        validator = new CertificateIdentityValidator();
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
