package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportPostProcessReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartImportPostProcessReplayConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartImportPostProcessReplayMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportPostProcessReplayPo;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 零件导入后置处理重放主任务仓储实现
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartImportPostProcessReplayRepositoryImpl implements PartImportPostProcessReplayRepository {

    private final PartImportPostProcessReplayMapper partImportPostProcessReplayMapper;

    @Override
    public PartImportPostProcessReplay selectById(Long id) {
        PartImportPostProcessReplayPo po = partImportPostProcessReplayMapper.selectPoById(id);
        return PartImportPostProcessReplayConverter.INSTANCE.toEntity(po);
    }

    @Override
    public PartImportPostProcessReplay selectByReplayId(String replayId) {
        PartImportPostProcessReplayPo po = partImportPostProcessReplayMapper.selectPoByReplayId(replayId);
        return PartImportPostProcessReplayConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(PartImportPostProcessReplay replay) {
        PartImportPostProcessReplayPo po = PartImportPostProcessReplayConverter.INSTANCE.toPo(replay);
        int rows = partImportPostProcessReplayMapper.insertPo(po);
        if (rows > 0 && po.getId() != null) {
            replay.setId(po.getId());
        }
        return rows;
    }

    @Override
    public int update(PartImportPostProcessReplay replay) {
        PartImportPostProcessReplayPo po = PartImportPostProcessReplayConverter.INSTANCE.toPo(replay);
        return partImportPostProcessReplayMapper.updatePo(po);
    }

    @Override
    public List<PartImportPostProcessReplay> selectList(PartImportPostProcessReplay replay) {
        Map<String, Object> map = new HashMap<>();
        if (replay.getReplayId() != null) {
            map.put("replayId", replay.getReplayId());
        }
        if (replay.getPartImportDataId() != null) {
            map.put("partImportDataId", replay.getPartImportDataId());
        }
        if (replay.getBatchNum() != null) {
            map.put("batchNum", replay.getBatchNum());
        }
        if (replay.getStatus() != null) {
            map.put("status", replay.getStatus());
        }
        List<PartImportPostProcessReplayPo> poList = partImportPostProcessReplayMapper.selectPoByMap(map);
        return poList.stream()
                .map(PartImportPostProcessReplayConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countRunningByPartImportDataId(Long partImportDataId) {
        return partImportPostProcessReplayMapper.countRunningByPartImportDataId(partImportDataId);
    }

    @Override
    public PartImportPostProcessReplay selectLatestTerminalByPartImportDataId(Long partImportDataId) {
        PartImportPostProcessReplayPo po = partImportPostProcessReplayMapper.selectLatestTerminalByPartImportDataId(partImportDataId);
        return PartImportPostProcessReplayConverter.INSTANCE.toEntity(po);
    }

    @Override
    public List<PartImportPostProcessReplay> selectTimeoutRunningRecords(int timeoutMinutes) {
        List<PartImportPostProcessReplayPo> poList = partImportPostProcessReplayMapper.selectTimeoutRunningRecords(timeoutMinutes);
        return poList.stream()
                .map(PartImportPostProcessReplayConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int updateTimeoutRunningToFailed(int timeoutMinutes) {
        return partImportPostProcessReplayMapper.updateTimeoutRunningToFailed(timeoutMinutes);
    }
}
