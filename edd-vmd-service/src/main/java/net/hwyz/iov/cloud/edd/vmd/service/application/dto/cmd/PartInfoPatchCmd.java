package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * 零件实例补丁命令（CR-071）
 * <p>
 * 白名单属性补丁：FILL_MISSING 仅补空，CORRECT_EXPLICIT 显式覆盖（需独立权限与版本校验）。
 * 软件投影列（softwareVer/softwarePn/softwareSnapshot*）由 SSOT 派生路径写入，
 * 普通入站不得直接以 SOFTWARE[0] 填充。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@Builder
public class PartInfoPatchCmd {

    /** 零件实例ID（与 partCode+sn 二选一） */
    private Long partId;

    /** 零件编码 */
    private String partCode;

    /** 零件序列号 */
    private String sn;

    /** 车载节点代码 */
    private String vehicleNodeCode;

    /** 配置字 */
    private String configWord;

    /** 供应商编码 */
    private String supplierCode;

    /** 生产批次号（与入站批次 inboundBatchNo 独立） */
    private String batchNum;

    /** 硬件版本号 */
    private String hardwareVer;

    /** 软件版本号（兼容投影，仅 SSOT 派生路径写） */
    private String softwareVer;

    /** 硬件零件号 */
    private String hardwarePn;

    /** 软件零件号（兼容投影，仅 SSOT 派生路径写） */
    private String softwarePn;

    /** 附加信息（type-schema 合并） */
    private String extra;

    /** 实例状态 */
    private Integer instanceState;

    /** 入站来源系统 */
    private String source;

    /** 零件类型（仅 MDM ACTIVE 投影） */
    private String partType;

    /** 入站批次号 */
    private String inboundBatchNo;

    /** 源事件ID */
    private String sourceEventId;

    /** 最近入站时间（单调不倒退） */
    private Instant lastInboundTime;

    /** 软件兼容投影状态 */
    private String softwareSnapshotState;

    /** 主软件投影来源安装记录ID */
    private Long softwareSnapshotInstallationId;

    /** 主软件投影来源 Target */
    private String softwareSnapshotTarget;

    /** 主软件投影来源槽位 */
    private String softwareSnapshotSlot;

    /** 主软件投影来源清单版本 */
    private Long softwareSnapshotInventoryVersion;

    /** 需显式置 null 的字段名集合（软件投影列清空用） */
    private List<String> nullFields;

    // ==================== 写上下文 ====================

    /** 回填策略：FILL_MISSING / CORRECT_EXPLICIT */
    private String repairPolicy;

    /** 期望当前业务版本（CORRECT_EXPLICIT 强校验；null 表示由服务读取当前值） */
    private Integer expectedRowVersion;

    /** 变更类型（INBOUND/BIND/ATTRIBUTE_UPDATE/SOFTWARE_SNAPSHOT/STATE_CHANGE） */
    private String changeType;

    /** 操作号 */
    private String operationId;

    /** 操作来源（MPT_REIMPORT/MPT_REPLAY/APP 等） */
    private String operationSource;

    /** 操作人ID（认证上下文注入） */
    private String operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 来源事件时刻 */
    private Instant sourceEventTime;

    /** 阶段（BASIC/SOFTWARE_SNAPSHOT 等，参与 mutation_key 构造） */
    private String phase;

    /** 该阶段内变更序号（参与 mutation_key 构造） */
    private Integer seq;

    /** 变更原因（CORRECT_EXPLICIT 必填） */
    private String reason;

}
