package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VmdOutboxPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通用 Outbox 表 DAO
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VmdOutboxMapper extends BaseDao<VmdOutboxPo, Long> {

    /**
     * 根据事件ID查询
     *
     * @param eventId 事件ID
     * @return Outbox记录
     */
    @Select("SELECT * FROM tb_vmd_outbox WHERE event_id = #{eventId} AND row_valid = 1")
    VmdOutboxPo selectPoByEventId(@Param("eventId") String eventId);

    /**
     * 查询待发布的消息
     * <p>
     * 条件：publish_state = 'PENDING' 或 (publish_state = 'FAILED_RETRYABLE' 且 next_retry_time <= now())
     *
     * @param limit 限制数量
     * @return 待发布消息列表
     */
    @Select("SELECT * FROM tb_vmd_outbox WHERE row_valid = 1 AND (" +
            "publish_state = 'PENDING' OR " +
            "(publish_state = 'FAILED_RETRYABLE' AND next_retry_time <= NOW())" +
            ") ORDER BY id ASC LIMIT #{limit}")
    List<VmdOutboxPo> selectPendingPoList(@Param("limit") int limit);

    /**
     * 统计指定来源关联ID下指定状态的消息数量
     *
     * @param sourceRefId 来源关联ID
     * @param publishState 发布状态
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_ref_id = #{sourceRefId} AND publish_state = #{publishState} AND row_valid = 1")
    long countBySourceRefIdAndPublishState(@Param("sourceRefId") String sourceRefId, @Param("publishState") String publishState);
}
