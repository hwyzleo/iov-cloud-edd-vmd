package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleImportReplayActionType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * EOL 生命周期节点补齐动作
 * <p>
 * VMD-DSN-CR-057: 优先使用原批次 EOL 时间（报文 EOL_TIME/EOL_DATE），缺失时兜底当前时刻。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Component
public class EolLifecycleEnsureReplayAction extends AbstractLifecycleEnsureReplayAction {

    public EolLifecycleEnsureReplayAction(VehicleLifecycleAppService lifecycleAppService) {
        super(lifecycleAppService);
    }

    @Override
    public String actionType() {
        return VehicleImportReplayActionType.EOL_LIFECYCLE_ENSURE.getValue();
    }

    @Override
    protected VehicleLifecycleNodeEnum nodeEnum() {
        return VehicleLifecycleNodeEnum.EOL;
    }

    @Override
    protected String alreadyExistsSkipReason() {
        return "ALREADY_EXISTS: 车辆已存在EOL生命周期节点，不覆盖原时间";
    }

    @Override
    protected Instant fallbackTime() {
        return Instant.now();
    }
}
