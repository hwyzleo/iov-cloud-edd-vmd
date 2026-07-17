package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;

import java.util.List;

/**
 * 通用 Outbox 仓储接口
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
public interface VmdOutboxRepository {

    VmdOutbox selectById(Long id);

    VmdOutbox selectByEventId(String eventId);

    int insert(VmdOutbox vmdOutbox);

    int update(VmdOutbox vmdOutbox);

    List<VmdOutbox> selectList(VmdOutbox vmdOutbox);

    /**
     * 查询待发布的消息（PENDING 或 FAILED_RETRYABLE 且到达重试时间）
     *
     * @param limit 限制数量
     * @return 待发布消息列表
     */
    List<VmdOutbox> selectPendingMessages(int limit);

    /**
     * 统计指定来源关联ID下各状态的消息数量
     *
     * @param sourceRefId 来源关联ID（如replayId）
     * @param publishState 发布状态
     * @return 消息数量
     */
    long countBySourceRefIdAndPublishState(String sourceRefId, String publishState);
}
