package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;

import java.util.Set;

/**
 * 零件导入后置处理动作执行上下文
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 承载单个候选实例 (partCode, sn) 的当前权威状态快照：
 * 当前 part_info / 当前 active vehicle_part / 当前 Part、VehicleNode 投影。
 * Handler 依据本上下文判断动作适用性与构造执行载荷。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@Builder
public class PartPostProcessActionContext {

    /** 重放请求ID */
    private final String replayId;

    /** 关联的零件导入数据ID */
    private final Long partImportDataId;

    /** 原导入批次号 */
    private final String batchNum;

    /** 零件编码 */
    private final String partCode;

    /** 零件序列号 */
    private final String sn;

    /** 当前 part_info（候选实例不存在时为 null） */
    private final PartInfo partInfo;

    /** 当前唯一 active 绑定（无 active 绑定时为 null） */
    private final VehiclePart activeBinding;

    /** 当前 MDM Part 投影（不存在时为 null） */
    private final Part mdmPart;

    /** 当前 MDM VehicleNode 投影（不存在时为 null） */
    private final VehicleNode vehicleNode;

    /** 操作人ID */
    private final String operatorId;

    /** 操作人姓名 */
    private final String operatorName;

    /** 重放原因 */
    private final String reason;

    /** 本次重放的动作范围（子集） */
    private final Set<PartPostProcessActionType> scope;
}
