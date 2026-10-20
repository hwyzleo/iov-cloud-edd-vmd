package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrExtension;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.KeyUsageFlag;

import java.security.PublicKey;
import java.util.List;
import java.util.Set;

/**
 * 解析后的 CSR 身份要素（CR-054 / VMD-DSN-CR-063 §4.2 扩展）
 * <p>
 * 由 CsrUtils 在签发编排中一次性解析，供身份校验 / 幂等 / 换钥授权 / 签发后复验复用，
 * 避免各处重复解析与口径漂移。bytes 防御性复制；toString 不输出 CSR DER、SPKI、公钥
 * 或完整摘要（安全边界，VMD-DSN-CR-063 §4.2）。
 *
 * @param subjectCn                 CSR Subject 单值 CN（规范化 hsm_uid 待比对）
 * @param spkiSha256                规范化 SubjectPublicKeyInfo SHA-256（hex，小写）
 * @param csrFingerprint            整份 CSR DER SHA-256 指纹（hex，小写，审计用）
 * @param publicKey                 CSR 内嵌公钥（复验 leaf SPKI 一致性用；不参与日志/序列化）
 * @param requestedKeyUsages        CSR 请求的 KeyUsage 位集合（CR-063 策略判定）
 * @param requestedExtendedKeyUsages CSR 请求的 ExtendedKeyUsage OID 集合（CR-063 策略判定）
 * @param requestedExtensions       CSR 请求的扩展列表（OID+criticality+value，CR-063 策略判定）
 * @param selfSignatureValid        CSR 自签名（PoP）是否有效
 * @author hwyz_leo
 * @since 2026-09-29
 */
public record ParsedCsr(
        String subjectCn,
        String spkiSha256,
        String csrFingerprint,
        PublicKey publicKey,
        Set<KeyUsageFlag> requestedKeyUsages,
        Set<String> requestedExtendedKeyUsages,
        List<CsrExtension> requestedExtensions,
        boolean selfSignatureValid
) {

    /**
     * 兼容既有 3 参构造（CR-054 旧调用/测试）：缺省公钥、用途与扩展为空、自签名视为有效。
     */
    public ParsedCsr(String subjectCn, String spkiSha256, String csrFingerprint) {
        this(subjectCn, spkiSha256, csrFingerprint, null, Set.of(), Set.of(), List.of(), true);
    }

    /**
     * canonical 构造：防御性复制集合/列表
     */
    public ParsedCsr {
        requestedKeyUsages = requestedKeyUsages == null ? Set.of() : Set.copyOf(requestedKeyUsages);
        requestedExtendedKeyUsages = requestedExtendedKeyUsages == null ? Set.of() : Set.copyOf(requestedExtendedKeyUsages);
        requestedExtensions = requestedExtensions == null ? List.of() : List.copyOf(requestedExtensions);
    }

    /**
     * 安全 toString：仅输出非敏感摘要（不含 CSR DER、SPKI、公钥、完整摘要值）
     */
    @Override
    public String toString() {
        return "ParsedCsr{subjectCn=" + subjectCn
                + ", keyUsages=" + requestedKeyUsages
                + ", ekuOidCount=" + requestedExtendedKeyUsages.size()
                + ", extensionCount=" + requestedExtensions.size()
                + ", selfSignatureValid=" + selfSignatureValid
                + ", hasPublicKey=" + (publicKey != null) + "}";
    }
}
