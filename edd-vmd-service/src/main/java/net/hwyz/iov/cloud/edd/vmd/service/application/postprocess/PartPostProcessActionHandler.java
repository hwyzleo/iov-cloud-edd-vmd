package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

/**
 * 零件导入后置处理动作处理器 SPI
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 后置动作采用注册表扩展：实现类自描述动作类型，经
 * {@link PartPostProcessActionRegistry} 按稳定动作类型发现并执行，
 * 新增后置能力无需修改主编排（D34）。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public interface PartPostProcessActionHandler {

    /**
     * 动作类型（稳定标识，对应 PartPostProcessActionType.value）
     *
     * @return 动作类型
     */
    String actionType();

    /**
     * 判断当前上下文下该动作是否适用
     * <p>
     * 不适用时 AppService 记录 SKIPPED 并注明原因。
     *
     * @param context 执行上下文
     * @return 是否适用
     */
    boolean supports(PartPostProcessActionContext context);

    /**
     * 计算动作幂等键
     * <p>
     * 同步联动默认 replayId + partCode + sn + actionType；
     * 安全补偿按 (partCode, sn, constantType) 收敛。
     *
     * @param context 执行上下文
     * @return 幂等键
     */
    String idempotencyKey(PartPostProcessActionContext context);

    /**
     * 执行动作
     * <p>
     * 事件动作返回预构造的 Outbox 记录（不入库），由 AppService 与动作状态同事务提交；
     * 同步联动执行实际下游调用并返回结果。Handler 不得包含主体落库 / 入站解析。
     *
     * @param context 执行上下文
     * @return 动作结果
     */
    PartPostProcessActionResult execute(PartPostProcessActionContext context);
}
