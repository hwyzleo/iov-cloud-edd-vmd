package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificateOperationPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 证书人工补偿操作审计表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Mapper
public interface VehicleCertificateOperationMapper extends BaseDao<VehicleCertificateOperationPo, Long> {

    /**
     * 根据requestId查询操作审计列表（状态时间线，按发生时间升序）
     *
     * @param requestId 证书申请requestId
     * @return 操作审计列表
     */
    List<VehicleCertificateOperationPo> selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据操作记录ID查询
     *
     * @param operationId 操作记录ID
     * @return 操作审计
     */
    VehicleCertificateOperationPo selectByOperationId(@Param("operationId") String operationId);

}
