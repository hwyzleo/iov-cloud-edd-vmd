package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

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
     * 规范化 HSM UID（CR-054 §7）
     * <p>
     * 仅允许：去除两端空白、统一十六进制大写、处理可选 0x 前缀。
     * 不得截断、补零、contains 或忽略内部字符。
     *
     * @param uid 原始 UID
     * @return 规范化 UID；入参 null 返回 null
     */
    public static String normalizeUid(String uid) {
        if (uid == null) {
            return null;
        }
        String v = uid.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.startsWith("0x") || v.startsWith("0X")) {
            v = v.substring(2);
        }
        return v.toUpperCase(Locale.ROOT);
    }

    /**
     * 检查 CSR Subject / SAN 是否包含任一禁止值（CR-054 §7：不得含 VIN / device_sn）
     * <p>
     * 对 Subject 全部 RDN 属性值与 SAN 全部 GeneralName 做不区分大小写的包含扫描。
     * SAN 解析失败按无 SAN 处理（不 fail）；Subject 缺失视为不含禁止值。
     *
     * @param csrDerBase64  CSR DER Base64
     * @param forbiddenValues 禁止值列表（如 VIN、device_sn）
     * @return true 表示命中禁止值
     */
    public static boolean subjectOrSanContains(String csrDerBase64, List<String> forbiddenValues) {
        if (forbiddenValues == null || forbiddenValues.isEmpty()) {
            return false;
        }
        try {
            PKCS10CertificationRequest req = parseCsr(csrDerBase64);
            X500Name subject = req.getSubject();
            if (subject != null) {
                for (RDN rdn : subject.getRDNs()) {
                    for (AttributeTypeAndValue atv : rdn.getTypesAndValues()) {
                        if (containsAnyIgnoreCase(atv.getValue().toString(), forbiddenValues)) {
                            return true;
                        }
                    }
                }
            }
            Extensions exts = extractRequestedExtensions(req);
            if (exts != null) {
                Extension sanExt = exts.getExtension(Extension.subjectAlternativeName);
                if (sanExt != null) {
                    GeneralNames gns = GeneralNames.getInstance(sanExt.getParsedValue());
                    for (GeneralName gn : gns.getNames()) {
                        if (containsAnyIgnoreCase(gn.getName().toString(), forbiddenValues)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("CSR Subject/SAN 禁止项扫描失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 CSR 的 extensionRequest 属性提取 Extensions（兼容 BC 1.69，无 getRequestedExtensions()）
     */
    private static Extensions extractRequestedExtensions(PKCS10CertificationRequest req) {
        try {
            org.bouncycastle.asn1.pkcs.Attribute[] extReqAttrs =
                    req.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
            if (extReqAttrs != null && extReqAttrs.length > 0
                    && extReqAttrs[0].getAttrValues() != null
                    && extReqAttrs[0].getAttrValues().size() > 0) {
                return Extensions.getInstance(extReqAttrs[0].getAttrValues().getObjectAt(0));
            }
        } catch (Exception e) {
            log.warn("解析CSR extensionRequest失败: {}", e.getMessage());
        }
        return null;
    }

    private static boolean containsAnyIgnoreCase(String value, List<String> forbiddenValues) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String f : forbiddenValues) {
            if (f != null && lower.contains(f.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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
     * <p>
     * BUG 修复（CSR PEM 头尾兼容）：解码前先剥离 PEM 头尾标记（BEGIN/END 行）
     * 与全部空白字符，兼容 openssl 默认输出的完整 PEM 块（每 64 字符换行）。
     * 仍为 fail-closed：剥离后依旧非法 Base64 时抛同样异常。
     *
     * @param base64 CSR DER 的 Base64 字符串（URL安全或标准编码均可，可带 PEM 头尾/换行）
     * @return DER 字节数组
     */
    public static byte[] decodeBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("CSR Base64为空");
        }
        String normalized = normalizePem(base64);
        try {
            return Base64.getUrlDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            try {
                return Base64.getDecoder().decode(normalized);
            } catch (IllegalArgumentException e2) {
                throw new IllegalArgumentException("CSR Base64解码失败", e2);
            }
        }
    }

    /**
     * 剥离 PEM 头尾标记（-----BEGIN xxx----- / -----END xxx----- 行）与全部空白字符。
     * 仅做字符清洗，不改变内容语义；清洗后内容仍需满足 Base64 字母表（非法输入继续抛异常）。
     */
    private static String normalizePem(String input) {
        return input.replaceAll("-----BEGIN[^-]*-----|-----END[^-]*-----", "")
                .replaceAll("\\s", "");
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
