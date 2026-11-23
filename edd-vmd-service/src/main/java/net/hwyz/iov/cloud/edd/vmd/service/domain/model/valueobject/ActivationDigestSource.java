package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

/**
 * 载荷摘要来源（VMD-DSN-CR-077 / D-VMD-077-05，终审修复 B-4 残留定稿）
 * <p>
 * 仅用于观测，不改变任何判定：
 * <ul>
 *   <li>{@link #EVENT}：事件载荷<strong>已提供</strong> {@code payloadDigest} → <strong>一律以事件值为权威落库</strong>
 *       （本地复算仅作完整性校验；校验不一致记 {@code DIGEST_VERIFY_MISMATCH} + 指标 {@code digest_mismatch}，
 *       但 <strong>不</strong>降级为 {@code LOCAL}、<strong>不</strong>改变 status/幂等）</li>
 *   <li>{@link #LOCAL}：事件<strong>未提供</strong> {@code payloadDigest} → 本地按定稿口径复算
 *       （{@code occurredAt} 取事件字符串原文参与哈希）</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public enum ActivationDigestSource {

    /**
     * 事件提供（权威值落库，本地复算仅校验）
     */
    EVENT,

    /**
     * 本地回算（事件未提供）
     */
    LOCAL

}
