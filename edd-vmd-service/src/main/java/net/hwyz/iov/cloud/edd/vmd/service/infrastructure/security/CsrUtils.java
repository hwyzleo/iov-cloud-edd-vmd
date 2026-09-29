package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Base64;

/**
 * CSR工具类
 * <p>
 * 用于解析和校验PKCS#10 CSR。
 *
 * <p>TBOX-SEC-DSN-CR-015 §9.1：落地真实 CSR 解析与签名验证（PoP），
 * 删除所有 MOCK/TODO 默认成功路径。签发授权不再依赖 CSR 是否携带 VIN
 * （证书/CSR 设计上不含 VIN，见 TBOX-SEC Identity Contract），
 * 改由绑定服务核对 VIN ↔ 设备绑定。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CsrUtils {

    static {
        // BouncyCastle 提供 PKCS#10/ECDSA 解析与验签（JCA provider）
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CsrUtils() {
        // 工具类不实例化
    }

    /**
     * 解析CSR获取Common Name (CN)
     * <p>
     * 解析PKCS#10 DER，从Subject中提取单值CN。畸形ASN.1/无CN/多值CN均视为非法
     * （fail-closed，抛IllegalArgumentException）。
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串（URL安全或标准Base64均可）
     * @return Common Name
     */
    public static String parseCommonName(String csrDerBase64) {
        PKCS10CertificationRequest req = parseCsr(csrDerBase64);
        X500Name subject = req.getSubject();
        if (subject == null) {
            throw new IllegalArgumentException("CSR Subject缺失");
        }
        RDN[] cnRdns = subject.getRDNs(BCStyle.CN);
        if (cnRdns.length != 1) {
            throw new IllegalArgumentException("CSR Subject CN必须为单值");
        }
        String cn = cnRdns[0].getFirst().getValue().toString();
        if (cn == null || cn.isEmpty()) {
            throw new IllegalArgumentException("CSR Subject CN为空");
        }
        return cn;
    }

    /**
     * 验证CSR签名（持有性证明 PoP）
     * <p>
     * 使用CSR内嵌公钥验证PKCS#10自签名，证明CSR创建方持有对应私钥。
     * 解析失败视为无效（返回false），不允许静默通过。
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return 签名是否有效
     */
    public static boolean verifySignature(String csrDerBase64) {
        try {
            PKCS10CertificationRequest req = parseCsr(csrDerBase64);
            ContentVerifierProvider verifierProvider =
                    new JcaContentVerifierProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(req.getSubjectPublicKeyInfo());
            return req.isSignatureValid(verifierProvider);
        } catch (Exception e) {
            log.warn("CSR签名验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算CSR中 SubjectPublicKeyInfo 的 SHA-256 摘要（hex）
     * <p>
     * 用于签发幂等/换钥授权（TBOX-SEC-DSN-CR-015 §5：vin + ecu_uid + public_key_sha256 + profile）。
     * 服务端从规范化 SPKI 计算，不信任客户端传值。
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return SPKI SHA-256 hex（小写）
     */
    public static String calculatePublicKeySha256(String csrDerBase64) {
        try {
            PKCS10CertificationRequest req = parseCsr(csrDerBase64);
            SubjectPublicKeyInfo spki = req.getSubjectPublicKeyInfo();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(spki.getEncoded()));
        } catch (Exception e) {
            log.warn("计算CSR SPKI摘要失败: {}", e.getMessage());
            throw new IllegalArgumentException("计算CSR SPKI摘要失败", e);
        }
    }

    /**
     * 提取CSR内嵌公钥的算法名（如 EC / RSA）
     * <p>
     * 用于构造 framework CertificateProfile 时不硬编码算法，避免与设备实际
     * 密钥算法（TBOX 为 ECDSA P-256，见 TBOX-SEC-DSN-CR-015 §4）错配。
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return 公钥算法名，如 "EC" / "RSA"
     */
    public static String extractPublicKeyAlgorithm(String csrDerBase64) {
        try {
            PKCS10CertificationRequest req = parseCsr(csrDerBase64);
            java.security.PublicKey publicKey =
                    new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getPublicKey(req.getSubjectPublicKeyInfo());
            String algorithm = publicKey.getAlgorithm();
            // BC 转换器对椭圆曲线密钥返回 "ECDSA"，归一化为 JCA 标准名 "EC"
            return "ECDSA".equals(algorithm) ? "EC" : algorithm;
        } catch (Exception e) {
            throw new IllegalArgumentException("提取CSR公钥算法失败", e);
        }
    }

    /**
     * 计算CSR指纹（SHA-256，整份DER）
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return SHA-256指纹（hex）
     */
    public static String calculateFingerprint(String csrDerBase64) {
        try {
            byte[] csrDer = decodeBase64(csrDerBase64);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(csrDer));
        } catch (NoSuchAlgorithmException e) {
            log.error("计算CSR指纹失败", e);
            throw new RuntimeException("计算CSR指纹失败", e);
        }
    }

    /**
     * 解析PKCS#10 DER（fail-closed：畸形输入抛IllegalArgumentException）
     */
    private static PKCS10CertificationRequest parseCsr(String csrDerBase64) {
        try {
            byte[] der = decodeBase64(csrDerBase64);
            return new PKCS10CertificationRequest(der);
        } catch (Exception e) {
            throw new IllegalArgumentException("CSR解析失败（畸形ASN.1/非法编码）: " + e.getMessage(), e);
        }
    }

    /**
     * Base64解码，兼容 URL 安全 与 标准 两种编码（TBOX 侧可能用标准 Base64）
     * <p>
     * 全链路唯一解码入口：签发编排（framework 请求构造）必须复用本方法，
     * 避免校验处与签发处解码口径不一致导致标准 Base64 在校验通过后解码抛异常。
     *
     * @param base64 CSR DER 的 Base64 字符串（URL安全或标准编码均可）
     * @return DER 字节数组
     */
    public static byte[] decodeBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("CSR Base64为空");
        }
        try {
            return Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            try {
                return Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("CSR Base64解码失败", e2);
            }
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
