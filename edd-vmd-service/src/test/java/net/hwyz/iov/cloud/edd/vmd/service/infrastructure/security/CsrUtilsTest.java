package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CsrUtils 单元测试（TBOX-SEC-DSN-CR-015 §9.1：真实解析/验签，删除 MOCK 默认成功路径）
 */
class CsrUtilsTest {

    private static String buildCsr(String cn) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            X500Name subject = new X500Name("CN=" + cn + ",OU=TBOX-TSP,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            extGen.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildCsrWithMismatchedSignature(String cn) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pubPair = kpg.generateKeyPair();
            KeyPair signPair = kpg.generateKeyPair();
            X500Name subject = new X500Name("CN=" + cn);
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(pubPair.getPublic().getEncoded()));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signPair.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parseCommonName_合法CSR_应返回CN() {
        assertEquals("TBOX-UID-000001", CsrUtils.parseCommonName(buildCsr("TBOX-UID-000001")));
    }

    @Test
    void parseCommonName_畸形输入_应抛异常() {
        // 纯文本（非 ASN.1）
        String plain = Base64.getEncoder().encodeToString("NOT_A_CSR".getBytes());
        assertThrows(IllegalArgumentException.class, () -> CsrUtils.parseCommonName(plain));

        // 非法 Base64
        assertThrows(IllegalArgumentException.class, () -> CsrUtils.parseCommonName("!!!not-base64!!!"));
    }

    @Test
    void verifySignature_合法CSR_应通过() {
        assertTrue(CsrUtils.verifySignature(buildCsr("TBOX-UID-000001")));
    }

    @Test
    void verifySignature_签名不匹配_应拒绝() {
        assertFalse(CsrUtils.verifySignature(buildCsrWithMismatchedSignature("TBOX-UID-000001")));
    }

    @Test
    void verifySignature_畸形输入_应拒绝() {
        String plain = Base64.getEncoder().encodeToString("NOT_A_CSR".getBytes());
        assertFalse(CsrUtils.verifySignature(plain));
    }

    @Test
    void calculatePublicKeySha256_应为64位hex且稳定() {
        String csr = buildCsr("TBOX-UID-000001");
        String h1 = CsrUtils.calculatePublicKeySha256(csr);
        String h2 = CsrUtils.calculatePublicKeySha256(csr);
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
    }

    @Test
    void calculateFingerprint_应稳定() {
        String csr = buildCsr("TBOX-UID-000001");
        assertEquals(CsrUtils.calculateFingerprint(csr), CsrUtils.calculateFingerprint(csr));
        assertEquals(64, CsrUtils.calculateFingerprint(csr).length());
    }

    @Test
    void 标准Base64编码的CSR_也应可解析() {
        // 使用标准 Base64（无 padding 的 URL-safe 转成标准形式：仅替换字符不改变内容语义）
        String urlSafe = buildCsr("TBOX-UID-000001");
        String standard = urlSafe.replace('-', '+').replace('_', '/');
        // 补回 padding 不影响标准解码器（长度按 4 对齐自动处理）
        assertEquals("TBOX-UID-000001", CsrUtils.parseCommonName(standard));
        assertTrue(CsrUtils.verifySignature(standard));
    }

    @Test
    void extractPublicKeyAlgorithm_ECDSA_应返回EC() {
        assertEquals("EC", CsrUtils.extractPublicKeyAlgorithm(buildCsr("TBOX-UID-000001")));
    }

    @Test
    void extractPublicKeyAlgorithm_畸形输入_应抛异常() {
        String plain = Base64.getEncoder().encodeToString("NOT_A_CSR".getBytes());
        assertThrows(IllegalArgumentException.class, () -> CsrUtils.extractPublicKeyAlgorithm(plain));
    }

    // =====================================================================
    // CR-054：normalizeUid + Subject/SAN 禁止项扫描
    // =====================================================================

    @Test
    void normalizeUid_去除空白与前缀_应统一大写() {
        assertEquals("00000000000000000000000000000001",
                CsrUtils.normalizeUid("00000000000000000000000000000001"));
        assertEquals("00000000000000000000000000000001",
                CsrUtils.normalizeUid(" 0x00000000000000000000000000000001 "));
        assertEquals("ABCDEF0123456789", CsrUtils.normalizeUid("0Xabcdef0123456789"));
        assertEquals("ABCDEF0123456789", CsrUtils.normalizeUid("abcdef0123456789"));
    }

    @Test
    void normalizeUid_空值_应返回null() {
        assertEquals(null, CsrUtils.normalizeUid(null));
        assertEquals(null, CsrUtils.normalizeUid("   "));
    }

    @Test
    void subjectOrSanContains_Subject含禁止值_应命中() {
        // CN 正确但 OU 属性携带 device_sn（CR-054 §7 禁止项扫描）
        String csr = buildCsrWithCustomSubject("CN=HSM-UID-001,OU=DEV-SN-999,O=OpenIOV,C=CN");
        assertTrue(CsrUtils.subjectOrSanContains(csr, java.util.List.of("DEV-SN-999")));
        assertTrue(CsrUtils.subjectOrSanContains(csr, java.util.List.of("HSM-UID-001")));
        assertFalse(CsrUtils.subjectOrSanContains(csr, java.util.List.of("NOT-PRESENT")));
    }

    @Test
    void subjectOrSanContains_SAN含VIN_应命中() {
        String csr = buildCsrWithSan("HWYZTEST900000001.invalid");
        assertTrue(CsrUtils.subjectOrSanContains(csr, java.util.List.of("HWYZTEST900000001")));
        assertFalse(CsrUtils.subjectOrSanContains(csr, java.util.List.of("OTHER-VIN")));
    }

    @Test
    void subjectOrSanContains_合法CSR无禁止值_应不命中() {
        String csr = buildCsr("TBOX-UID-000001");
        assertFalse(CsrUtils.subjectOrSanContains(csr, java.util.List.of("HWYZTEST900000001", "DEV-SN-999")));
        // 空禁止列表/畸形输入不抛异常
        assertFalse(CsrUtils.subjectOrSanContains(csr, java.util.List.of()));
        assertFalse(CsrUtils.subjectOrSanContains(
                Base64.getEncoder().encodeToString("NOT_A_CSR".getBytes()),
                java.util.List.of("X")));
    }

    private static String buildCsrWithCustomSubject(String subject) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    new X500Name(subject), SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildCsrWithSan(String sanDns) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    new X500Name("CN=TBOX-UID-000001,OU=TBOX-TSP,O=OpenIOV,C=CN"),
                    SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            extGen.addExtension(Extension.subjectAlternativeName, false,
                    new org.bouncycastle.asn1.x509.GeneralNames(
                            new org.bouncycastle.asn1.x509.GeneralName(
                                    org.bouncycastle.asn1.x509.GeneralName.dNSName, sanDns)));
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
