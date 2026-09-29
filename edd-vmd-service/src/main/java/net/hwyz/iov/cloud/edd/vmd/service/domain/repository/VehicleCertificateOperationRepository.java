package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificateOperation;

import java.util.List;

/**
 * 证书人工补偿操作审计数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleCertificateOperationRepository {

    /**
     * 根据requestId查询操作审计列表（状态时间线，按发生时间升序）
     *
     * @param requestId 证书申请requestId
     * @return 操作审计列表
     */
    List<VehicleCertificateOperation> selectByRequestId(String requestId);

    /**
     * 根据操作记录ID查询
     *
     * @param operationId 操作记录ID
     * @return 操作审计
     */
    VehicleCertificateOperation selectByOperationId(String operationId);

    /**
     * 新增操作审计
     *
     * @param operation 操作审计
     * @return 影响行数
     */
    int insert(VehicleCertificateOperation operation);

}
