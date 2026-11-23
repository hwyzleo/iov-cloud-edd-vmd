package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

/**
 * 激活事实消费台账状态（VMD-DSN-CR-077 §6.2，R-10 定稿）
 * <p>
 * 终态集合固定为四值，<strong>不含 CONFLICT</strong>：同业务键不同摘要属合法重投，
 * 一律 {@link #DUPLICATE}（保留首次）+ {@code DIGEST_MISMATCH} 备注；
 * 真正的冲突（同 {@code eventId} 但 {@code vin}/{@code bindingOperationId} 不同）
 * 收敛为 {@link #QUARANTINED}。
 *
 * @author hwyz_leo
 */
public enum ActivationFactConsumeStatus {

    /**
     * 已记录：合法事实，已作用于投影（首次插入或向前纠正）
     */
    RECORDED,

    /**
     * 重复：同 eventId 或同业务键（含摘要不一致的合法重投），投影不被覆盖
     */
    DUPLICATE,

    /**
     * 隔离：契约错误或真冲突，不重试、不阻塞分区
     */
    QUARANTINED,

    /**
     * 失败：技术错误退避重试耗尽，保留位点待重放
     */
    FAILED;

    /**
     * 是否为未生效终态（查询 PENDING 的判定来源）
     *
     * @return true=未作用于投影的终态
     */
    public boolean isIneffective() {
        return this == QUARANTINED || this == FAILED;
    }

}
