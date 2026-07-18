package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSoftwareInstallationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.PartSoftwareInstallationConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartSoftwareInstallationMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSoftwareInstallationPo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 软件实装时态记录数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PartSoftwareInstallationRepositoryImpl implements PartSoftwareInstallationRepository {

    private final PartSoftwareInstallationMapper partSoftwareInstallationMapper;

    @Override
    public PartSoftwareInstallation selectById(Long id) {
        return PartSoftwareInstallationConverter.INSTANCE.toDomain(partSoftwareInstallationMapper.selectById(id));
    }

    @Override
    public List<PartSoftwareInstallation> selectByPartId(Long partId) {
        LambdaQueryWrapper<PartSoftwareInstallationPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartSoftwareInstallationPo::getPartId, partId)
               .eq(PartSoftwareInstallationPo::getInstallState, "ACTIVE")
               .eq(PartSoftwareInstallationPo::getRowValid, 1);
        return PartSoftwareInstallationConverter.INSTANCE.toDomainList(partSoftwareInstallationMapper.selectList(wrapper));
    }

    @Override
    public PartSoftwareInstallation selectActiveByPartIdAndTargetCode(Long partId, String softwareTargetCode) {
        LambdaQueryWrapper<PartSoftwareInstallationPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartSoftwareInstallationPo::getPartId, partId)
               .eq(PartSoftwareInstallationPo::getSoftwareTargetCode, softwareTargetCode)
               .eq(PartSoftwareInstallationPo::getInstallState, "ACTIVE")
               .eq(PartSoftwareInstallationPo::getRowValid, 1);
        return PartSoftwareInstallationConverter.INSTANCE.toDomain(partSoftwareInstallationMapper.selectOne(wrapper));
    }

    @Override
    public int insert(PartSoftwareInstallation partSoftwareInstallation) {
        PartSoftwareInstallationPo po = PartSoftwareInstallationConverter.INSTANCE.fromDomain(partSoftwareInstallation);
        int result = partSoftwareInstallationMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            partSoftwareInstallation.setId(po.getId());
        }
        return result;
    }

    @Override
    public int update(PartSoftwareInstallation partSoftwareInstallation) {
        return partSoftwareInstallationMapper.updateById(PartSoftwareInstallationConverter.INSTANCE.fromDomain(partSoftwareInstallation));
    }

    @Override
    public int deactivateByPartIdAndTargetCode(Long partId, String softwareTargetCode) {
        return partSoftwareInstallationMapper.deactivateByPartIdAndTargetCode(partId, softwareTargetCode);
    }
}
