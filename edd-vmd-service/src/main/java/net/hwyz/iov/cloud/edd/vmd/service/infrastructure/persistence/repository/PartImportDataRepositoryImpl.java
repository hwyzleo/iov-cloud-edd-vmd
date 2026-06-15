package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartImportDataConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportDataPo;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
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
    public PartImportData selectByBatchNum(String batchNum) {
        LambdaQueryWrapper<PartImportDataPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartImportDataPo::getBatchNum, batchNum);
        PartImportDataPo po = partImportDataMapper.selectOne(wrapper);
        return PartImportDataConverter.INSTANCE.toEntity(po);
    }

    @Override
    public int insert(PartImportData partImportData) {
        PartImportDataPo po = PartImportDataConverter.INSTANCE.toPo(partImportData);
        return partImportDataMapper.insert(po);
    }

    @Override
    public int update(PartImportData partImportData) {
        PartImportDataPo po = PartImportDataConverter.INSTANCE.toPo(partImportData);
        return partImportDataMapper.updateById(po);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return partImportDataMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public List<PartImportData> selectList(PartImportData partImportData) {
        LambdaQueryWrapper<PartImportDataPo> wrapper = new LambdaQueryWrapper<>();
        if (partImportData.getBatchNum() != null) {
            wrapper.like(PartImportDataPo::getBatchNum, partImportData.getBatchNum());
        }
        if (partImportData.getPartCode() != null) {
            wrapper.like(PartImportDataPo::getPartCode, partImportData.getPartCode());
        }
        if (partImportData.getHandle() != null) {
            wrapper.eq(PartImportDataPo::getHandle, partImportData.getHandle());
        }
        wrapper.orderByDesc(PartImportDataPo::getCreateTime);
        List<PartImportDataPo> poList = partImportDataMapper.selectList(wrapper);
        return poList.stream()
                .map(PartImportDataConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkBatchNumUnique(Long id, String batchNum) {
        LambdaQueryWrapper<PartImportDataPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartImportDataPo::getBatchNum, batchNum);
        if (id != null) {
            wrapper.ne(PartImportDataPo::getId, id);
        }
        return partImportDataMapper.selectCount(wrapper) == 0;
    }
}
