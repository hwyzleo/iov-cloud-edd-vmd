package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;

/**
 * OTA 车辆软件观测消费审计仓储接口
 * <p>
 * VMD-DSN-CR-046: 事件级幂等（eventId + observationKey）与隔离/DLQ 审计
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
public interface SoftwareInventoryConsumeAuditRepository {

    /**
     * 根据主键ID查询消费审计记录
     *
     * @param id 主键ID
     * @return 消费审计记录（可能为null）
     */
    SoftwareInventoryConsumeAudit selectById(Long id);

    /**
     * 根据事件ID查询消费审计记录（幂等键）
     *
     * @param eventId 事件ID
     * @return 消费审计记录（可能为null）
     */
    SoftwareInventoryConsumeAudit selectByEventId(String eventId);

    /**
     * 根据观测身份查询消费审计记录（幂等键）
     *
     * @param observationKey 观测身份
     * @return 消费审计记录（可能为null）
     */
    SoftwareInventoryConsumeAudit selectByObservationKey(String observationKey);

    /**
     * 新增消费审计记录
     *
     * @param audit 消费审计记录
     * @return 影响行数
     */
    int insert(SoftwareInventoryConsumeAudit audit);

    /**
     * 更新消费审计记录
     *
     * @param audit 消费审计记录
     * @return 影响行数
     */
    int update(SoftwareInventoryConsumeAudit audit);
}
