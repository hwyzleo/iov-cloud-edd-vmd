package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.VehImportReplayExtractor;

import java.time.Instant;
import java.util.List;

/**
 * 车辆导入补发动作执行上下文
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发动作注册表
 * <p>
 * 承载单个候选车辆（VIN）的原批次候选零件与当前权威事实装载所需信息；
 * Handler 依据上下文执行动作，payload 一律从当前事实构造，禁止重跑解析器或泛化重放 Spring 事件。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@Builder
public class VehicleImportReplayActionContext {

    /** 补发请求ID */
    private final String replayId;

    /** 关联的车辆导入数据ID */
    private final Long vehImportDataId;

    /** 原导入批次号 */
    private final String batchNum;

    /** 导入类型（PRODUCE/TOL/EOL） */
    private final String importType;

    /** 车架号 */
    private final String vin;

    /** 原批次候选零件（PRODUCE 为空） */
    private final List<VehImportReplayExtractor.PartCandidate> candidates;

    /** 原批次时间（TOL/EOL 生命周期补齐用，EOL 取报文 EOL_TIME/EOL_DATE，TOL 无则空） */
    private final Instant batchTime;

    /** 操作人ID */
    private final String operatorId;

    /** 操作人姓名 */
    private final String operatorName;

    /** 补发原因 */
    private final String reason;
}
