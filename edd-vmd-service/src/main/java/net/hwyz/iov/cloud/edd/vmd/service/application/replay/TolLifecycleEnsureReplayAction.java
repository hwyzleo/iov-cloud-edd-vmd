package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * TOL 生命周期节点补齐动作
 * <p>
 * VMD-DSN-CR-057: 原批次无显式 TOL 时间，兜底使用当前时刻补齐 TOL 节点。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class TolLifecycleEnsureReplayAction extends AbstractLifecycleEnsureReplayAction {

    public TolLifecycleEnsureReplayAction(VehicleLifecycleAppService lifecycleAppService) {
        super(lifecycleAppService);
    }

    @Override
    public String actionType() {
        return VehicleImportReplayActionType.TOL_LIFECYCLE_ENSURE.getValue();
    }

    @Override
    protected VehicleLifecycleNodeEnum nodeEnum() {
        return VehicleLifecycleNodeEnum.TOL;
    }

    @Override
    protected String alreadyExistsSkipReason() {
        return "ALREADY_EXISTS: 车辆已存在TOL生命周期节点，不覆盖原时间";
    }

    @Override
    protected Instant fallbackTime() {
        return Instant.now();
    }
}
