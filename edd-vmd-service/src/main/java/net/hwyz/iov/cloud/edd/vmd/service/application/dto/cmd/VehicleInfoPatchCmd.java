package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 车辆基础信息补丁命令（CR-071）
 * <p>
 * 白名单关键字段补丁（basic 命名空间）：工厂/品牌/平台/车系/车型/变体/配置/订单/整车基线/下线结果等。
 * FILL_MISSING 仅补空；CORRECT_EXPLICIT 显式覆盖（需独立权限与版本校验）。
 * detail/vehicle_option 快照由调用方提供命名空间数据（本次导入实际修改的部分）。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@Builder
public class VehicleInfoPatchCmd {

    /** 车辆基础信息ID（与 VIN 二选一） */
    private Long vehicleId;

    /** 车架号 */
    private String vin;

    /** 工厂代码 */
    private String plantCode;

    /** 品牌代码 */
    private String brandCode;

    /** 平台代码 */
    private String platformCode;

    /** 车系代码 */
    private String carLineCode;

    /** 车型代码 */
    private String modelCode;

    /** 变体代码 */
    private String variantCode;

    /** 生产配置代码 */
    private String configurationCode;

    /** 下线时间 */
    private Instant eolTime;

    /** 最后一次PDI时间 */
    private Instant pdiTime;

    /** 订单编码 */
    private String orderNum;

    /** 整车基线版本 */
    private String vehicleBaseVersion;

    /** 下线结果（PASS/NG/REWORK） */
    private String eolResult;

    /** 事实来源（MANUAL/MES/EOL/TOL/PRODUCE 等） */
    private String source;

    // ==================== 写上下文 ====================

    /** 回填策略：FILL_MISSING / CORRECT_EXPLICIT */
    private String repairPolicy;

    /** 期望当前业务版本（CORRECT_EXPLICIT 强校验） */
    private Integer expectedRowVersion;

    /** 变更类型 */
    private String changeType;

    /** 操作号 */
    private String operationId;

    /** 操作来源 */
    private String operationSource;

    /** 操作人ID */
    private String operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 来源事件时刻 */
    private Instant sourceEventTime;

    /** 来源事件ID */
    private String sourceEventId;

    /** 阶段 */
    private String phase;

    /** 该阶段内变更序号 */
    private Integer seq;

    /** 变更原因（CORRECT_EXPLICIT 必填） */
    private String reason;

}
