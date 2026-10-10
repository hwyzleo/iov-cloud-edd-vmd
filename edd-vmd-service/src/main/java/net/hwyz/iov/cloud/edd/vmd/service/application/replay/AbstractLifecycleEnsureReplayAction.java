package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayAggregateType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;

import java.time.Instant;
import java.util.List;

/**
 * 生命周期节点补齐动作基类
 * <p>
 * VMD-DSN-CR-057: TOL/EOL 基于原批次时间与当前生命周期，经
 * VehicleLifecycleAppService.ensureNode() 幂等补齐节点；已有节点不覆盖原时间，
 * 返回 SKIPPED_ALREADY_EXISTS。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
public abstract class AbstractLifecycleEnsureReplayAction implements VehicleImportReplayAction {

    protected final VehicleLifecycleAppService lifecycleAppService;

    protected AbstractLifecycleEnsureReplayAction(VehicleLifecycleAppService lifecycleAppService) {
        this.lifecycleAppService = lifecycleAppService;
    }

    /**
     * 生命周期节点枚举
     */
    protected abstract VehicleLifecycleNodeEnum nodeEnum();

    /**
     * 已有节点时的跳过原因
     */
    protected abstract String alreadyExistsSkipReason();

    /**
     * 原批次时间缺失时的兜底时间（可返回 null 表示不指定）
     */
    protected abstract Instant fallbackTime();

    @Override
    public List<VehicleImportReplayActionTarget> plan(VehicleImportReplayActionContext context) {
        return List.of(new VehicleImportReplayActionTarget(
                VehicleImportReplayAggregateType.VEHICLE_LIFECYCLE.getValue(),
                context.getVin(),
                0L,
                null));
    }

    @Override
    public VehicleImportReplayActionResult execute(VehicleImportReplayActionContext context,
                                                   VehicleImportReplayActionTarget target) {
        Instant reachTime = context.getBatchTime() != null ? context.getBatchTime() : fallbackTime();
        boolean ensured = lifecycleAppService.ensureNode(context.getVin(), nodeEnum(), reachTime);
        if (ensured) {
            return VehicleImportReplayActionResult.success();
        }
        return VehicleImportReplayActionResult.skipped(alreadyExistsSkipReason());
    }
}
