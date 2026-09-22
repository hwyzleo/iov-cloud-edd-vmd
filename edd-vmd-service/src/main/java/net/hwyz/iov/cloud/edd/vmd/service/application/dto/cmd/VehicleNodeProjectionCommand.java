package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车载节点投影命令（CR-049）
 * <p>
 * Bootstrap 快照与 Kafka 事件统一映射的中立载荷：仅承载 VehicleNode 最小投影字段
 * （身份、分类、能力、投影治理字段），Bootstrap 与增量事件共用同一内核（RD-049 / CR-047 范式）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNodeProjectionCommand {

    /**
     * 车载节点代码（必填）
     */
    private String code;

    /**
     * 车载节点名称
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 设备分类（BizType 路由主键，CR-049）
     */
    private String deviceCategory;

    /**
     * HSM 能力（NONE/SHE/HSM_LIGHT/HSM_FULL，预置资格权威来源）
     */
    private String hsmCapability;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * OTA 支持类型
     */
    private String otaSupport;

    /**
     * 是否核心设备
     */
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * MDM 侧实体主键 ID
     */
    private String externalRefId;

    /**
     * MDM 侧实体版本号（版本门禁）
     */
    private Long externalVersion;

    /**
     * 同步发生 / 事件发生时刻
     */
    private LocalDateTime occurredAt;

}
