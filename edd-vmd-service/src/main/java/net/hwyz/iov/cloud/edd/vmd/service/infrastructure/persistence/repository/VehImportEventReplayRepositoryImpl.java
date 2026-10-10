package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehImportEventReplayConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportEventReplayMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayPo;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车辆导入事件补发主任务审计仓储实现
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 * VMD-DSN-CR-057: 扩展为按 ImportType 路由的动作补偿主任务
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehImportEventReplayRepositoryImpl implements VehImportEventReplayRepository {

    private final VehImportEventReplayMapper vehImportEventReplayMapper;

    @Override
    public VehImportEventReplay selectById(Long id) {
        VehImportEventReplayPo po = vehImportEventReplayMapper.selectPoById(id);
        return VehImportEventReplayConverter.INSTANCE.toEntity(po);
    }

    @Override
    public VehImportEventReplay selectByReplayId(String replayId) {
        VehImportEventReplayPo po = vehImportEventReplayMapper.selectPoByReplayId(replayId);
        return VehImportEventReplayConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(VehImportEventReplay vehImportEventReplay) {
        VehImportEventReplayPo po = VehImportEventReplayConverter.INSTANCE.toPo(vehImportEventReplay);
        int rows = vehImportEventReplayMapper.insertPo(po);
        // 回填生成的主键id到Entity
        if (rows > 0 && po.getId() != null) {
            vehImportEventReplay.setId(po.getId());
        }
        return rows;
    }

    @Override
    public int update(VehImportEventReplay vehImportEventReplay) {
        VehImportEventReplayPo po = VehImportEventReplayConverter.INSTANCE.toPo(vehImportEventReplay);
        return vehImportEventReplayMapper.updatePo(po);
    }

    @Override
    public List<VehImportEventReplay> selectList(VehImportEventReplay vehImportEventReplay) {
        Map<String, Object> map = new HashMap<>();
        if (vehImportEventReplay.getReplayId() != null) {
            map.put("replayId", vehImportEventReplay.getReplayId());
        }
        if (vehImportEventReplay.getVehImportDataId() != null) {
            map.put("vehImportDataId", vehImportEventReplay.getVehImportDataId());
        }
        if (vehImportEventReplay.getBatchNum() != null) {
            map.put("batchNum", vehImportEventReplay.getBatchNum());
        }
        if (vehImportEventReplay.getImportType() != null) {
            map.put("importType", vehImportEventReplay.getImportType());
        }
        if (vehImportEventReplay.getStatus() != null) {
            map.put("status", vehImportEventReplay.getStatus());
        }
        List<VehImportEventReplayPo> poList = vehImportEventReplayMapper.selectPoByMap(map);
        return poList.stream()
                .map(VehImportEventReplayConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countRunningByVehImportDataId(Long vehImportDataId) {
        return vehImportEventReplayMapper.countRunningByVehImportDataId(vehImportDataId);
    }

    @Override
    public List<VehImportEventReplay> selectTimeoutRunningRecords(int timeoutMinutes) {
        List<VehImportEventReplayPo> poList = vehImportEventReplayMapper.selectTimeoutRunningRecords(timeoutMinutes);
        return poList.stream()
                .map(VehImportEventReplayConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int updateTimeoutRunningToFailed(int timeoutMinutes) {
        return vehImportEventReplayMapper.updateTimeoutRunningToFailed(timeoutMinutes);
    }
}
