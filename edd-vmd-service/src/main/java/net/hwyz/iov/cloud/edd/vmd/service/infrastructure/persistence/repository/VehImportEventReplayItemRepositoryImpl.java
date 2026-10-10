package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehImportEventReplayItemConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportEventReplayItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayItemPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆导入事件补发逐项动作审计仓储实现
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehImportEventReplayItemRepositoryImpl implements VehImportEventReplayItemRepository {

    private final VehImportEventReplayItemMapper vehImportEventReplayItemMapper;

    @Override
    public VehImportEventReplayItem selectById(Long id) {
        VehImportEventReplayItemPo po = vehImportEventReplayItemMapper.selectPoById(id);
        return VehImportEventReplayItemConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(VehImportEventReplayItem item) {
        VehImportEventReplayItemPo po = VehImportEventReplayItemConverter.INSTANCE.toPo(item);
        int rows = vehImportEventReplayItemMapper.insertPo(po);
        if (rows > 0 && po.getId() != null) {
            item.setId(po.getId());
        }
        return rows;
    }

    @Override
    public int update(VehImportEventReplayItem item) {
        VehImportEventReplayItemPo po = VehImportEventReplayItemConverter.INSTANCE.toPo(item);
        return vehImportEventReplayItemMapper.updatePo(po);
    }

    @Override
    public VehImportEventReplayItem selectByUniqueKey(String replayId, String actionType, String aggregateType,
                                                      String aggregateId, Long aggregateVersion) {
        VehImportEventReplayItemPo po = vehImportEventReplayItemMapper.selectPoByUniqueKey(
                replayId, actionType, aggregateType, aggregateId, aggregateVersion);
        return VehImportEventReplayItemConverter.INSTANCE.toEntity(po);
    }

    @Override
    public List<VehImportEventReplayItem> selectListByReplayId(String replayId) {
        List<VehImportEventReplayItemPo> poList = vehImportEventReplayItemMapper.selectPoListByReplayId(replayId);
        return poList.stream()
                .map(VehImportEventReplayItemConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }
}
