package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 零件实例属性变更历史领域对象（CR-071）
 * <p>
 * 记录 tb_part_info 白名单属性的 before/after 与 changed_fields，随业务本体提交
 * 同事务写入（PartInfoMutationAppService）。rowVersion 为业务本体提交版本
 * （tb_part_info.row_version），非软件 inventoryVersion、非本表重试版本。
 * <p>
 * 职责边界（§7.1）：不替代 tb_part_software_installation 的多 Target/Slot 有效期历史。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PartInfoChangeLog implements DomainObj<PartInfoChangeLog> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件实例ID（关联tb_part_info.id）
     */
    private Long partId;

    /**
     * 变更类型：INBOUND/BIND/UNBIND/ATTRIBUTE_UPDATE/SOFTWARE_SNAPSHOT/STATE_CHANGE/BASELINE_CAPTURE
     */
    private String changeType;

    /**
     * 变更前白名单属性快照（创建为 null）
     */
    private String beforeJson;

    /**
     * 变更后白名单属性快照
     */
    private String afterJson;

    /**
     * 变更字段差异
     */
    private String changedFields;

    /**
     * 事实来源（MANUAL/MES/EOL/TOL/PRODUCE 等）
     */
    private String source;

    /**
     * 操作来源（MPT_REIMPORT/MPT_REPLAY/APP 等）
     */
    private String operationSource;

    /**
     * 操作人ID（认证上下文注入）
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 变更时刻（UTC）
     */
    private LocalDateTime changeTime;

    /**
     * 来源事件时刻（UTC，可证明时填充）
     */
    private LocalDateTime sourceEventTime;

    /**
     * 来源事件ID
     */
    private String sourceEventId;

    /**
     * 入站批次号
     */
    private String inboundBatchNo;

    /**
     * 操作号（普通导入为生成的操作号）
     */
    private String operationId;

    /**
     * 同事实变更唯一键：SHA-256(operationId,entityId,phase,变更序号)
     */
    private byte[] mutationKey;

    /**
     * 业务本体提交版本（tb_part_info.row_version）
     */
    private Integer rowVersion;

    /**
     * 扩展元数据（如软件投影 target/slot/inventoryVersion）
     */
    private String metadataJson;

}
