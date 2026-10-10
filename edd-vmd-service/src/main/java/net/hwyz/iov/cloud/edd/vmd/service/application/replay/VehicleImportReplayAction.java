package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import java.util.List;

/**
 * 车辆导入补发动作处理器 SPI
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作注册表
 * <p>
 * 仅注册显式审核过的动作：不允许按类名反射调用任意 Publisher / Subscriber，
 * 不重跑解析器、不泛化重放 Spring 事件、不触发证书/密钥/安全预置副作用。
 * 实现类自描述动作类型，经 {@link VehicleImportReplayActionRegistry} 发现；
 * 事件动作直接构造既有 Kafka 契约 payload 写 Outbox。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public interface VehicleImportReplayAction {

    /**
     * 动作类型（稳定标识，对应 VehicleImportReplayActionType.value）
     *
     * @return 动作类型
     */
    String actionType();

    /**
     * 规划该动作在当前 VIN 下的执行目标（聚合列表）
     * <p>
     * 只读计算，不落库、不产生副作用；preview 与执行规划共用。
     * 当前事实缺失 / 已失效 / 版本变化的目标不返回（不发布历史脏快照）。
     *
     * @param context 执行上下文
     * @return 规划目标列表
     */
    List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context);

    /**
     * 对单个规划目标执行动作
     * <p>
     * 事件动作返回预构造的 Outbox 记录（不入库），由 AppService 与动作状态同事务提交；
     * Handler 不得包含主体落库 / 入站解析。
     *
     * @param context 执行上下文
     * @param target  规划目标
     * @return 动作结果
     */
    VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                            VehicleImportReplayActionTarget target);
}
