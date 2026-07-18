package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartImportDataConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportDataPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 零件导入数据仓储实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartImportDataRepositoryImpl implements PartImportDataRepository {

    private final PartImportDataMapper partImportDataMapper;

    @Override
    public PartImportData selectById(Long id) {
        PartImportDataPo po = partImportDataMapper.selectPoById(id);
        return PartImportDataConverter.INSTANCE.toEntity(po);
    }

    @Override
    public PartImportData selectByBatchNum(String batchNum) {
        PartImportDataPo po = partImportDataMapper.selectPoByBatchNum(batchNum);
        return PartImportDataConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(PartImportData partImportData) {
        PartImportDataPo po = PartImportDataConverter.INSTANCE.toPo(partImportData);
        return partImportDataMapper.insertPo(po);
    }

    @Override
    public int update(PartImportData partImportData) {
        PartImportDataPo po = PartImportDataConverter.INSTANCE.toPo(partImportData);
        return partImportDataMapper.updatePo(po);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return partImportDataMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<PartImportData> selectList(PartImportData partImportData, LocalDateTime beginTime, LocalDateTime endTime) {
        Map<String, Object> map = new HashMap<>();
        if (partImportData.getBatchNum() != null) {
            map.put("batchNum", partImportData.getBatchNum());
        }
        if (partImportData.getPartCode() != null) {
            map.put("partCode", partImportData.getPartCode());
        }
        if (partImportData.getHandle() != null) {
            map.put("handle", partImportData.getHandle());
        }
        if (beginTime != null) {
            map.put("beginTime", beginTime);
        }
        if (endTime != null) {
            map.put("endTime", endTime);
        }
        List<PartImportDataPo> poList = partImportDataMapper.selectPoByMap(map);
        return PageUtil.convert(poList, PartImportDataConverter.INSTANCE::toEntity);
    }

    @Override
    public boolean checkBatchNumUnique(Long id, String batchNum) {
        if (id != null) {
            return partImportDataMapper.countPoByBatchNumAndIdNot(batchNum, id) == 0;
        }
        return partImportDataMapper.countPoByBatchNum(batchNum) == 0;
    }
}
