package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * OTA 车辆软件观测消费审计 领域实体
 * <p>
 * VMD-DSN-CR-046: 事件级幂等（eventId + observationKey）、item 隔离与 DLQ 监控
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Getter
@Setter
@SuperBuilder
public class SoftwareInventoryConsumeAudit extends BaseDo<Long> implements DomainObj<SoftwareInventoryConsumeAudit> {

    /**
     * 主键
     */
    private Long id;

    /**
     * OTA 观测事件 ID（幂等键，UK）
     */
    private String eventId;

    /**
     * OTA FULL 观测身份（幂等键，UK）
     */
    private String observationKey;

    /**
     * VIN 哈希（SHA-256，脱敏审计）
     */
    private String vinHash;

    /**
     * 状态：PROCESSING/SUCCESS/PARTIAL/QUARANTINED/FAILED
     */
    private String status;

    /**
     * 事件 item 总数
     */
    private Integer itemTotal;

    /**
     * 成功写入数
     */
    private Integer itemApplied;

    /**
     * 幂等/版本gate忽略数
     */
    private Integer itemIgnored;

    /**
     * 隔离数（无绑定/多绑定/非法item）
     */
    private Integer itemQuarantined;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最近错误码
     */
    private String lastErrorCode;

    /**
     * 隔离明细 JSON（ecuId/target/slot/脱敏VIN/原因）
     */
    private String quarantineDetail;

    /**
     * 接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public void init() {
        stateInit();
    }
}
