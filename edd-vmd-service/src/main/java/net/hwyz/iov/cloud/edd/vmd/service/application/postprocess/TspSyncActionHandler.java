package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import org.springframework.stereotype.Component;

/**
 * TSP 下游联动动作处理器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Component
public class TspSyncActionHandler extends AbstractDownstreamSyncActionHandler {

    private static final String SYSTEM = "TSP";

    public TspSyncActionHandler(DownstreamProcessorRegistry downstreamProcessorRegistry) {
        super(downstreamProcessorRegistry);
    }

    @Override
    public String actionType() {
        return PartPostProcessActionType.TSP_SYNC.getValue();
    }

    @Override
    protected String targetSystem() {
        return SYSTEM;
    }
}
