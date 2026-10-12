package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningIntent;
import net.hwyz.iov.security.keyprov.v1.KeyImportItemResult;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningAck;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningRequest;
import net.hwyz.iov.security.keyprov.v1.WrappingSuite;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 业务密钥开通批次领域服务（CR-058 §4 D35）。
 * <p>
 * 负责状态机不变量、用途集合完整性、幂等摘要规范化、服务端目标版本模式推导与
 * ACK 幂等冲突判定；不直接持有交易编排与 framework 网络 I/O。
 * <p>
 * 目标版本模式由 VMD 依据 canonical {@code current_versions} + 当前 ACTIVE 目录 +
 * 服务端轮换策略推导（canonical proto 无 intent 字段，Q4a=A）：
 * <ul>
 *   <li>无 ACTIVE → INITIAL</li>
 *   <li>设备声明版本 == ACTIVE 版本 → REIMPORT（复用 ACTIVE，重新 Wrap）</li>
 *   <li>版本缺失/落后 → ROTATE（新建 PENDING）</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Service
public class BusinessKeyProvisioningDomainService {

    /**
     * 规范化请求 purposes（保留出现顺序，去重后判定完整性）
     */
    public Set<String> normalizePurposes(KeyProvisioningRequest request) {
        Set<String> codes = new LinkedHashSet<>();
        request.getPurposesList().forEach(purpose -> {
            String code = KeyprovConstants.purposeCode(purpose);
            if (code != null) {
                codes.add(code);
            }
        });
        return codes;
    }

    /**
     * 是否为首期 V2C 场景（业务域 + 完整用途集合）
     */
    public boolean isV2c(KeyProvisioningRequest request) {
        return KeyprovConstants.BUSINESS_DOMAIN_V2C.equalsIgnoreCase(request.getBusinessDomain());
    }

    /**
     * 服务端推导目标版本模式。
     *
     * @param active          当前 ACTIVE 目录行（可为空）
     * @param declaredVersion 设备在 current_versions 中声明的版本（0 表示无版本）
     * @return 目标版本模式
     */
    public BusinessKeyProvisioningIntent deriveIntent(DeviceBusinessKey active, long declaredVersion) {
        if (active == null) {
            return BusinessKeyProvisioningIntent.INITIAL;
        }
        if (declaredVersion > 0 && active.getBusinessKeyVersion() != null
                && declaredVersion == active.getBusinessKeyVersion()) {
            return BusinessKeyProvisioningIntent.REIMPORT;
        }
        return BusinessKeyProvisioningIntent.ROTATE;
    }

    /**
     * 解析 request.current_versions 为 purpose→version。
     */
    public Map<String, Long> extractCurrentVersions(KeyProvisioningRequest request) {
        Map<String, Long> versions = new TreeMap<>();
        request.getCurrentVersionsList().forEach(current -> {
            String code = KeyprovConstants.purposeCode(current.getPurpose());
            if (code != null) {
                versions.put(code, current.getBusinessKeyVersion());
            }
        });
        return versions;
    }

    /**
     * 选择设备支持的封装套件：request.supported_wrapping_suites ∩ allowlist，保持请求顺序。
     *
     * @return 选定套件代码；无交集返回 null（fail-closed）
     */
    public String negotiateWrappingSuite(KeyProvisioningRequest request, List<String> allowedSuites) {
        for (WrappingSuite suite : request.getSupportedWrappingSuitesList()) {
            String code = KeyprovConstants.suiteCode(suite);
            if (code != null && allowedSuites.contains(code)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 规范化请求摘要（同 requestId 参数冲突识别）。
     * <p>不包含任何敏感材料；仅设备身份摘要 + 业务域 + 用途集合 + 声明版本 + 幂等键。
     */
    public String buildRequestDigest(KeyProvisioningRequest request, String hsmUidDigest) {
        Set<String> purposes = normalizePurposes(request);
        List<String> sortedPurposes = new ArrayList<>(purposes);
        sortedPurposes.sort(String::compareTo);
        Map<String, Long> currentVersions = extractCurrentVersions(request);
        StringBuilder sb = new StringBuilder();
        sb.append(hsmUidDigest == null ? "" : hsmUidDigest).append('|')
                .append(upper(request.getBusinessDomain())).append('|')
                .append(String.join(",", sortedPurposes)).append('|');
        currentVersions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append('=').append(e.getValue() == null ? 0 : e.getValue()).append(','));
        sb.append('|').append(request.getIdempotencyKey() == null ? "" : request.getIdempotencyKey());
        return DigestUtil.sha256Hex(sb.toString());
    }

    /**
     * 规范化 ACK 摘要（相同 requestId 相同摘要幂等、不同摘要冲突）。
     * <p>只含 purpose/keyId/version/result，不含 import_proof 原文。
     */
    public String buildAckDigest(KeyProvisioningAck ack) {
        List<String> parts = new ArrayList<>();
        for (KeyImportItemResult item : ack.getItemsList()) {
            parts.add(item.getPurposeValue() + "|" + item.getKeyId() + "|"
                    + item.getBusinessKeyVersion() + "|" + item.getResultValue());
        }
        parts.sort(String::compareTo);
        return DigestUtil.sha256Hex(String.join(";", parts));
    }

    /**
     * HSM UID 摘要（不落明文身份）
     */
    public String digestHsmUid(String hsmUid) {
        return hsmUid == null ? null : DigestUtil.sha256Hex(hsmUid.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 非敏感证明摘要（import_proof 原文不落库）
     */
    public String digestImportProof(byte[] importProof) {
        if (importProof == null || importProof.length == 0) {
            return null;
        }
        return DigestUtil.sha256Hex(importProof);
    }

    /**
     * 字节内容 SHA-256 摘要
     */
    public String sha256Hex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return DigestUtil.sha256Hex(bytes);
    }

    /**
     * 用途代码集合是否与批次 item 完整一致（无缺项、无重复、无多余）
     */
    public boolean itemsComplete(Set<String> ackPurposes, Set<String> batchPurposes) {
        return ackPurposes != null && batchPurposes != null && ackPurposes.equals(batchPurposes);
    }

    /**
     * 空/空白字符串归一
     */
    public String upper(String value) {
        return StrUtil.isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 字符串 UTF-8 字节（供参数编码）
     */
    public byte[] utf8(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }
}
