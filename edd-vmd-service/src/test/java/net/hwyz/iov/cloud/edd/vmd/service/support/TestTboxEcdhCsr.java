package net.hwyz.iov.cloud.edd.vmd.service.support;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;

/**
 * VMD-DSN-CR-063 测试用逻辑 v1 CSR 夹具。
 * <p>
 * 构造 EC P-256 双用途（digitalSignature+keyAgreement / clientAuth）CSR 及其非法变体
 * （RSA/P-384、KU 缺失/冲突、EKU 过宽/缺失、未知 critical 扩展、CA 意图）。
 * 仅用于测试，不进入生产。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
public final class TestTboxEcdhCsr {

    /** 未知 critical 扩展测试 OID（非受治理扩展） */
    public static final String UNKNOWN_EXTENSION_OID = "1.3.6.1.4.1.99999.1.1";

    private TestTboxEcdhCsr() {
    }

    /** 合法 CSR 及其密钥对（供签发后复验测试构造同 SPKI 的叶子证书） */
    public record CsrWithKey(String csrDerBase64, KeyPair keyPair) {
    }

    /** 合法：EC P-256、KU=DS+KA、EKU=clientAuth、仅受治理扩展，并返回密钥对 */
    public static CsrWithKey validWithKey(String cn) {
        try {
            KeyPair kp = ecKeyPair("secp256r1");
            String csr = buildWithKey(cn, kp, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
            return new CsrWithKey(csr, kp);
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }

    /** 合法：EC P-256、KU=DS+KA、EKU=clientAuth、仅受治理扩展 */
    public static String valid(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** RSA 公钥 */
    public static String rsa(String cn) {
        return build(cn, "RSA", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** EC P-384 曲线 */
    public static String p384(String cn) {
        return build(cn, "secp384r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** KU 仅 digitalSignature（缺 keyAgreement） */
    public static String kuMissingKeyAgreement(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** KU 含 keyCertSign（CA 意图） */
    public static String kuWithKeyCertSign(String cn) {
        return build(cn, "secp256r1",
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement | KeyUsage.keyCertSign),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** 无 KU 扩展 */
    public static String kuAbsent(String cn) {
        return build(cn, "secp256r1", null,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), null);
    }

    /** EKU 缺失 clientAuth（仅 serverAuth） */
    public static String ekuServerAuthOnly(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth), null);
    }

    /** EKU 过宽（clientAuth + serverAuth） */
    public static String ekuClientAndServerAuth(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}), null);
    }

    /** EKU anyExtendedKeyUsage */
    public static String ekuAny(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.anyExtendedKeyUsage), null);
    }

    /** EKU 扩展缺失 */
    public static String ekuAbsent(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                null, null);
    }

    /** basicConstraints CA=true（CA 意图） */
    public static String basicConstraintsCaTrue(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth), caTrueExtension());
    }

    private static Extension caTrueExtension() {
        try {
            return new Extension(Extension.basicConstraints, true,
                    new org.bouncycastle.asn1.DEROctetString(new BasicConstraints(true)));
        } catch (Exception e) {
            throw new RuntimeException("生成 basicConstraints 扩展失败", e);
        }
    }

    /** 未知 critical 扩展 */
    public static String unknownCriticalExtension(String cn) {
        return build(cn, "secp256r1", new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement),
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth),
                new Extension(new org.bouncycastle.asn1.ASN1ObjectIdentifier(UNKNOWN_EXTENSION_OID), true,
                        new byte[]{0x05, 0x00}));
    }

    /**
     * 通用构造（使用外部密钥对）
     */
    private static String buildWithKey(String cn, KeyPair kp, KeyUsage keyUsage, ExtendedKeyUsage eku, Extension extra) {
        try {
            X500Name subject = new X500Name("CN=" + cn + ",OU=TBOX-ECDH,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            if (keyUsage != null) {
                extGen.addExtension(Extension.keyUsage, true, keyUsage);
            }
            if (eku != null) {
                extGen.addExtension(Extension.extendedKeyUsage, false, eku);
            }
            if (extra != null) {
                extGen.addExtension(extra);
            }
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder(
                    kp.getPrivate().getAlgorithm().startsWith("RSA") ? "SHA256withRSA" : "SHA256withECDSA")
                    .build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }

    /**
     * 通用构造
     *
     * @param cn         CN（Subject 单值）
     * @param algorithm  "secp256r1" | "secp384r1" | "RSA"
     * @param keyUsage   KeyUsage 扩展（可为 null）
     * @param eku        ExtendedKeyUsage 扩展（可为 null）
     * @param extra      额外扩展（可为 null）
     */
    private static String build(String cn, String algorithm, KeyUsage keyUsage, ExtendedKeyUsage eku, Extension extra) {
        try {
            KeyPair kp;
            if ("RSA".equals(algorithm)) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
                kp = kpg.generateKeyPair();
            } else {
                kp = ecKeyPair(algorithm);
            }
            return buildWithKey(cn, kp, keyUsage, eku, extra);
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }

    private static KeyPair ecKeyPair(String algorithm) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec(algorithm));
        return kpg.generateKeyPair();
    }
}
