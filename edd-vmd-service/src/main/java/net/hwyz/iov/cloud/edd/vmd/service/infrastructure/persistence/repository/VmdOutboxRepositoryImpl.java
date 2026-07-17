package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VmdOutboxConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VmdOutboxMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VmdOutboxPo;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用 Outbox 仓储实现
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VmdOutboxRepositoryImpl implements VmdOutboxRepository {

    private final VmdOutboxMapper vmdOutboxMapper;

    @Override
    public VmdOutbox selectById(Long id) {
        VmdOutboxPo po = vmdOutboxMapper.selectPoById(id);
        return VmdOutboxConverter.INSTANCE.toEntity(po);
    }

    @Override
    public VmdOutbox selectByEventId(String eventId) {
        VmdOutboxPo po = vmdOutboxMapper.selectPoByEventId(eventId);
        return VmdOutboxConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(VmdOutbox vmdOutbox) {
        VmdOutboxPo po = VmdOutboxConverter.INSTANCE.toPo(vmdOutbox);
        int rows = vmdOutboxMapper.insertPo(po);
        // 回填生成的主键id到Entity
        if (rows > 0 && po.getId() != null) {
            vmdOutbox.setId(po.getId());
        }
        return rows;
    }

    @Override
    public int update(VmdOutbox vmdOutbox) {
        VmdOutboxPo po = VmdOutboxConverter.INSTANCE.toPo(vmdOutbox);
        return vmdOutboxMapper.updatePo(po);
    }

    @Override
    public List<VmdOutbox> selectList(VmdOutbox vmdOutbox) {
        Map<String, Object> map = new HashMap<>();
        if (vmdOutbox.getEventId() != null) {
            map.put("eventId", vmdOutbox.getEventId());
        }
        if (vmdOutbox.getEventType() != null) {
            map.put("eventType", vmdOutbox.getEventType());
        }
        if (vmdOutbox.getAggregateType() != null) {
            map.put("aggregateType", vmdOutbox.getAggregateType());
        }
        if (vmdOutbox.getAggregateId() != null) {
            map.put("aggregateId", vmdOutbox.getAggregateId());
        }
        if (vmdOutbox.getPublishState() != null) {
            map.put("publishState", vmdOutbox.getPublishState());
        }
        if (vmdOutbox.getSourceType() != null) {
            map.put("sourceType", vmdOutbox.getSourceType());
        }
        if (vmdOutbox.getSourceRefId() != null) {
            map.put("sourceRefId", vmdOutbox.getSourceRefId());
        }
        List<VmdOutboxPo> poList = vmdOutboxMapper.selectPoByMap(map);
        return poList.stream()
                .map(VmdOutboxConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<VmdOutbox> selectPendingMessages(int limit) {
        List<VmdOutboxPo> poList = vmdOutboxMapper.selectPendingPoList(limit);
        return poList.stream()
                .map(VmdOutboxConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySourceRefIdAndPublishState(String sourceRefId, String publishState) {
        return vmdOutboxMapper.countBySourceRefIdAndPublishState(sourceRefId, publishState);
    }
}
