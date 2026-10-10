package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartImportPostProcessReplayItemConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartImportPostProcessReplayItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportPostProcessReplayItemPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 零件导入后置处理重放动作明细仓储实现
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartImportPostProcessReplayItemRepositoryImpl implements PartImportPostProcessReplayItemRepository {

    private final PartImportPostProcessReplayItemMapper partImportPostProcessReplayItemMapper;

    @Override
    public PartImportPostProcessReplayItem selectById(Long id) {
        PartImportPostProcessReplayItemPo po = partImportPostProcessReplayItemMapper.selectPoById(id);
        return PartImportPostProcessReplayItemConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(PartImportPostProcessReplayItem item) {
        PartImportPostProcessReplayItemPo po = PartImportPostProcessReplayItemConverter.INSTANCE.toPo(item);
        int rows = partImportPostProcessReplayItemMapper.insertPo(po);
        if (rows > 0 && po.getId() != null) {
            item.setId(po.getId());
        }
        return rows;
    }

    @Override
    public int update(PartImportPostProcessReplayItem item) {
        PartImportPostProcessReplayItemPo po = PartImportPostProcessReplayItemConverter.INSTANCE.toPo(item);
        return partImportPostProcessReplayItemMapper.updatePo(po);
    }

    @Override
    public PartImportPostProcessReplayItem selectByUniqueKey(String replayId, String partCode, String sn, String actionType) {
        PartImportPostProcessReplayItemPo po = partImportPostProcessReplayItemMapper.selectPoByUniqueKey(replayId, partCode, sn, actionType);
        return PartImportPostProcessReplayItemConverter.INSTANCE.toEntity(po);
    }

    @Override
    public List<PartImportPostProcessReplayItem> selectListByReplayId(String replayId) {
        List<PartImportPostProcessReplayItemPo> poList = partImportPostProcessReplayItemMapper.selectPoListByReplayId(replayId);
        return poList.stream()
                .map(PartImportPostProcessReplayItemConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartImportPostProcessReplayItem> selectRetryableByReplayId(String replayId) {
        List<PartImportPostProcessReplayItemPo> poList = partImportPostProcessReplayItemMapper.selectPoListRetryableByReplayId(replayId);
        return poList.stream()
                .map(PartImportPostProcessReplayItemConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }
}
