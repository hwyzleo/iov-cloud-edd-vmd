package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车型投影命令（CR-048 / RD-048-3）
 * <p>
 * Bootstrap 快照与 Kafka 事件统一映射的中立载荷：仅承载 Model 最小投影字段
 * （身份、名称、直接产品树父引用 carLineCode/platformCode、投影治理字段），
 * 不含旧 enable/sort/name_en 字段（RD-048-2）。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProjectionCommand {

    /**
     * 车型编码（必填）
     */
    private String code;

    /**
     * 车型名称（必填）
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 车系代码（必填，Model 直接产品树父引用）
     */
    private String carLineCode;

    /**
     * 平台代码（必填，Model 直接产品树父引用）
     */
    private String platformCode;

    /**
     * 备注
     */
    private String description;

    /**
     * MDM 侧实体主键 ID（必填，upsert 优先键）
     */
    private String externalRefId;

    /**
     * MDM 侧实体版本号（必填，版本门禁）
     */
    private Long externalVersion;

    /**
     * 同步发生 / 事件发生时刻
     */
    private LocalDateTime occurredAt;

}
