package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 统一导入操作主实体（CR-071 §7.2）
 * <p>
 * REPLAY / REIMPORT / 普通 IMPORT / 旧后置补偿的统一操作载体：
 * 服务端持久化预览计划、摘要幂等、租约与 fence 并发控制、逐项审计引用。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ImportOperation implements DomainObj<ImportOperation> {

    /** 主键 */
    private Long id;

    /** UUID 操作号（区分大小写） */
    private String operationId;

    /** 操作域：VEHICLE/PART */
    private String domain;

    /** 操作类型：IMPORT/REIMPORT/REPLAY/POST_PROCESS */
    private String operationType;

    /** 来源导入数据ID（veh_import_data/part_import_data） */
    private Long importDataId;

    /** 来源批次号 */
    private String batchNum;

    /** 纠错批次引用的原始导入数据ID */
    private Long originalImportDataId;

    /** 原始载荷版本化规范化摘要 */
    private byte[] payloadDigest;

    /** 执行请求摘要（幂等复用/冲突判定） */
    private byte[] requestDigest;

    /** 预览计划摘要（token 校验） */
    private byte[] planDigest;

    /** 服务端持久化预览计划 */
    private String planJson;

    /** 操作人ID */
    private String operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作来源（MPT_REIMPORT/MPT_REPLAY 等） */
    private String operationSource;

    /** 操作原因 */
    private String reason;

    /** 工单号 */
    private String ticketNo;

    /** 状态：PENDING/RUNNING/SUCCEEDED/PARTIAL_FAILED/FAILED/RECOVERING */
    private String status;

    /** 阶段计数：changed/unchanged/skipped/failed */
    private String countersJson;

    /** 关联旧补发 replayId */
    private String legacyReplayId;

    /** 执行器租约持有者 */
    private String leaseOwner;

    /** 租约到期时间（UTC） */
    private LocalDateTime leaseUntil;

    /** 接管 fence 令牌（单调递增） */
    private Long fenceToken;

    /** 创建时刻（UTC） */
    private LocalDateTime createdAt;

    /** 开始时刻 */
    private LocalDateTime startedAt;

    /** 结束时刻 */
    private LocalDateTime finishedAt;

}
