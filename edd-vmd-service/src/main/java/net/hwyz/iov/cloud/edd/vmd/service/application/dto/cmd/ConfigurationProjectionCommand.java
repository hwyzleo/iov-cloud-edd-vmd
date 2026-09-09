package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 配置投影命令（CR-047 / RD-047-3）
 * <p>
 * Bootstrap 快照与 Kafka 事件统一映射的中立载荷：仅承载 Configuration 最小投影字段
 * （配置身份、名称、Variant 引用、投影治理字段），不含旧层级冗余字段。
 * </p>
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationProjectionCommand {

    /**
     * 配置编码（必填）
     */
    private String code;

    /**
     * 配置名称（必填）
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 版本代码（必填，Configuration 唯一直接产品树父引用）
     */
    private String variantCode;

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
