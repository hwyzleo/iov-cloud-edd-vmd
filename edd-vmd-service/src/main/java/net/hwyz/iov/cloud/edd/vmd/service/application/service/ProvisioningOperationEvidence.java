package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsClient;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsOperationResult;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsOperationState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 滞留开通批次 operation 安全判定证据（VMD-DSN-CR-065 §4.2）。
 * <p>
 * 逐 item 判定 Provider operation 是否可能已产生成功副作用：
 * <ul>
 *   <li>无 operationId（framework 调用前失败）→ NOT_STARTED，可终止；</li>
 *   <li>operation 查询明确 FAILED/REJECTED/NOT_COMMITTED → CONFIRMED_FAILED，可终止；</li>
 *   <li>operation 返回 SUCCEEDED → SUCCEEDED，不得终止（应恢复原结果并推进）；</li>
 *   <li>operation 返回 PROCESSING/UNKNOWN、查询不可用或结果存储不可判定 → IN_PROGRESS_OR_UNKNOWN，
 *       不得终止（fail-closed，先对账）。</li>
 * </ul>
 * 远程查询一律在事务外执行（不得在持有数据库锁时等待远程 Provider，VMD-DSN-CR-065 §5.1）；
 * 进入事务后仅重新校验批次 modifyTime 与 operation 结果引用，不重查远程。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisioningOperationEvidence {

    private final ObjectProvider<KmsClient> kmsClientProvider;

    /**
     * 事务外批量判定 item operation 状态（按 item id 返回）。
     *
     * @param items 批次 item 列表（未加锁读）
     * @return itemId → 判定结果
     */
    public Map<Long, OperationOutcome> query(List<BusinessKeyProvisioningItem> items) {
        Map<Long, OperationOutcome> outcomes = new LinkedHashMap<>();
        if (items == null) {
            return outcomes;
        }
        for (BusinessKeyProvisioningItem item : items) {
            outcomes.put(item.getId(), queryOne(item.getOperationId()));
        }
        return outcomes;
    }

    /**
     * 判定单个 operation 结果（远程查询在事务外调用；任何异常按不可判定 fail-closed）。
     */
    private OperationOutcome queryOne(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            // framework 调用前失败，无 operation 可回流成功结果
            return OperationOutcome.NOT_STARTED;
        }
        KmsClient kmsClient = kmsClientProvider.getIfAvailable();
        if (kmsClient == null) {
            log.warn("Provider operation 查询不可用（KmsClient 未装配），按不可判定处理: operationId 摘要={}",
                    digestRef(operationId));
            return OperationOutcome.IN_PROGRESS_OR_UNKNOWN;
        }
        try {
            KmsOperationResult result = kmsClient.getOperation(operationId);
            if (result == null) {
                log.warn("Provider operation 查询返回空（结果存储不可判定）: operationId 摘要={}", digestRef(operationId));
                return OperationOutcome.IN_PROGRESS_OR_UNKNOWN;
            }
            KmsOperationState state = result.state();
            if (state == KmsOperationState.FAILED) {
                return OperationOutcome.CONFIRMED_FAILED;
            }
            if (state == KmsOperationState.SUCCEEDED) {
                return OperationOutcome.SUCCEEDED;
            }
            // PENDING / UNKNOWN → 处理中或未知，禁止终止
            return OperationOutcome.IN_PROGRESS_OR_UNKNOWN;
        } catch (Exception e) {
            log.warn("Provider operation 查询失败，按不可判定处理（fail-closed）: operationId 摘要={}, cause={}",
                    digestRef(operationId), e.getClass().getSimpleName());
            return OperationOutcome.IN_PROGRESS_OR_UNKNOWN;
        }
    }

    /**
     * operation 判定结果（VMD-DSN-CR-065 §4.2）
     */
    public enum OperationOutcome {
        /** 无 operationId 或 framework 调用前失败 */
        NOT_STARTED,
        /** 明确 FAILED/REJECTED/NOT_COMMITTED */
        CONFIRMED_FAILED,
        /** SUCCEEDED：禁止终止 */
        SUCCEEDED,
        /** PROCESSING/UNKNOWN/查询不可用：禁止终止，先对账 */
        IN_PROGRESS_OR_UNKNOWN
    }

    /**
     * operationId 属敏感标识，日志仅输出不可逆摘要前缀
     */
    private String digestRef(String operationId) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(operationId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
