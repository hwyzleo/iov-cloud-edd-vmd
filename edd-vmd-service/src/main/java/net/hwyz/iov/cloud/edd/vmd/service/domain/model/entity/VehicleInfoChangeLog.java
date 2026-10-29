package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 车辆基础信息属性变更历史领域对象（CR-071）
 * <p>
 * 记录 tb_veh_basic_info 关键字段及本次导入实际修改的 detail/vehicle_option
 * 快照的 before/after（basic/detail/options 命名空间）。随业务本体提交同事务写入
 * （VehicleInfoMutationAppService）。rowVersion 为业务本体提交版本
 * （tb_veh_basic_info.row_version）。
 * <p>
 * 职责边界（§7.1）：不替代生命周期首次节点、绑定历史或软件时态历史。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VehicleInfoChangeLog implements DomainObj<VehicleInfoChangeLog> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车辆基础信息ID（关联tb_veh_basic_info.id）
     */
    private Long vehicleId;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 变更类型：INBOUND/ATTRIBUTE_UPDATE/STATE_CHANGE/BASELINE_CAPTURE
     */
    private String changeType;

    /**
     * 变更前快照（basic/detail/options 命名空间）
     */
    private String beforeJson;

    /**
     * 变更后快照
     */
    private String afterJson;

    /**
     * 变更字段差异
     */
    private String changedFields;

    /**
     * 事实来源
     */
    private String source;

    /**
     * 操作来源（MPT_REIMPORT/MPT_REPLAY/APP 等）
     */
    private String operationSource;

    /**
     * 操作人ID
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
     * 来源事件时刻
     */
    private LocalDateTime sourceEventTime;

    /**
     * 来源事件ID
     */
    private String sourceEventId;

    /**
     * 操作号
     */
    private String operationId;

    /**
     * 同事实变更唯一键
     */
    private byte[] mutationKey;

    /**
     * 业务本体提交版本（tb_veh_basic_info.row_version）
     */
    private Integer rowVersion;

    /**
     * 扩展元数据
     */
    private String metadataJson;

}
