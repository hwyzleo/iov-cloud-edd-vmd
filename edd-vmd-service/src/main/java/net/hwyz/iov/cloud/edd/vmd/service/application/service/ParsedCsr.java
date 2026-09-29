package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * 解析后的 CSR 身份要素（CR-054）
 * <p>
 * 由 CsrUtils 在签发编排中一次性解析，供身份校验 / 幂等 / 换钥授权复用，
 * 避免各处重复解析与口径漂移。
 *
 * @param subjectCn     CSR Subject 单值 CN（规范化 hsm_uid 待比对）
 * @param spkiSha256    规范化 SubjectPublicKeyInfo SHA-256（hex，小写）
 * @param csrFingerprint 整份 CSR DER SHA-256 指纹（hex，小写，审计用）
 */
public record ParsedCsr(
        String subjectCn,
        String spkiSha256,
        String csrFingerprint
) {
}
