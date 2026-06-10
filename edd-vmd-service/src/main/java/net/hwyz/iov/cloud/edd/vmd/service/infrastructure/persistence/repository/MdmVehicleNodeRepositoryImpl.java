package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleNodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmVehicleNodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVehicleNodePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车载节点数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MdmVehicleNodeRepositoryImpl implements MdmVehicleNodeRepository {

    private final MdmVehicleNodeMapper mdmVehicleNodeMapper;

    @Override
    public List<VehicleNode> selectByMap(Map<String, Object> map) {
        List<MdmVehicleNodePo> poList = mdmVehicleNodeMapper.selectPoByMap(map);
        return PageUtil.convert(poList, VehicleNodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmVehicleNodeMapper.countPoByMap(map);
    }

    @Override
    public VehicleNode selectById(Long id) {
        return VehicleNodeConverter.INSTANCE.toDomain(mdmVehicleNodeMapper.selectPoById(id));
    }

    @Override
    public VehicleNode selectByCode(String code) {
        return VehicleNodeConverter.INSTANCE.toDomain(mdmVehicleNodeMapper.selectPoByCode(code));
    }

    @Override
    public VehicleNode selectByExternalRefId(String externalRefId) {
        return VehicleNodeConverter.INSTANCE.toDomain(mdmVehicleNodeMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmVehicleNodeMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(VehicleNode vehicleNode) {
        return mdmVehicleNodeMapper.insertPo(VehicleNodeConverter.INSTANCE.fromDomain(vehicleNode));
    }

    @Override
    public int update(VehicleNode vehicleNode) {
        return mdmVehicleNodeMapper.updatePo(VehicleNodeConverter.INSTANCE.fromDomain(vehicleNode));
    }

    @Override
    public int updateById(VehicleNode vehicleNode) {
        return mdmVehicleNodeMapper.updatePo(VehicleNodeConverter.INSTANCE.fromDomain(vehicleNode));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return mdmVehicleNodeMapper.batchPhysicalDeletePo(ids);
    }

}
