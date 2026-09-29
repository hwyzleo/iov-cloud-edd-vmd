package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificateOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleCertificateOperationConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleCertificateOperationMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificateOperationPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 证书人工补偿操作审计数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleCertificateOperationRepositoryImpl implements VehicleCertificateOperationRepository {

    private final VehicleCertificateOperationMapper vehicleCertificateOperationMapper;

    @Override
    public List<VehicleCertificateOperation> selectByRequestId(String requestId) {
        List<VehicleCertificateOperationPo> poList = vehicleCertificateOperationMapper.selectByRequestId(requestId);
        return PageUtil.convert(poList, VehicleCertificateOperationConverter.INSTANCE::toDomain);
    }

    @Override
    public VehicleCertificateOperation selectByOperationId(String operationId) {
        return VehicleCertificateOperationConverter.INSTANCE.toDomain(
                vehicleCertificateOperationMapper.selectByOperationId(operationId));
    }

    @Override
    public int insert(VehicleCertificateOperation operation) {
        VehicleCertificateOperationPo po = VehicleCertificateOperationConverter.INSTANCE.fromDomain(operation);
        int rows = vehicleCertificateOperationMapper.insertPo(po);
        operation.setId(po.getId());
        return rows;
    }

}
