package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 版本投影命令（CR-048 / RD-048-3）
 * <p>
 * Bootstrap 快照与 Kafka 事件统一映射的中立载荷：仅承载 Variant 最小投影字段
 * （身份、名称、唯一直接父引用 modelCode、投影治理字段），
 * 不含旧 platform/carLine 冗余列与 enable/sort/name_en（RD-048-1/2）。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantProjectionCommand {

    /**
     * 版本编码（必填）
     */
    private String code;

    /**
     * 版本名称（必填）
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 车型代码（必填，Variant 唯一直接产品树父引用）
     */
    private String modelCode;

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
