package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务密钥多用途开通批次仓储接口（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public interface BusinessKeyProvisioningRepository {

    /**
     * 根据主键查询批次
     */
    BusinessKeyProvisioning selectById(Long id);

    /**
     * 根据 requestId 查询批次
     */
    BusinessKeyProvisioning selectByRequestId(String requestId);

    /**
     * 根据 requestId 查询批次并加行锁（完成事务串行化，须在事务内调用）
     */
    BusinessKeyProvisioning selectByRequestIdForUpdate(String requestId);

    /**
     * 查询同设备+业务域未终结批次
     */
    BusinessKeyProvisioning selectOpenByDeviceAndDomain(String deviceSn, String businessDomain);

    /**
     * 按状态扫描修改时间早于 before 的批次（超时/对账扫批）
     */
    List<BusinessKeyProvisioning> selectListByStatusBefore(BusinessKeyProvisioningState status, LocalDateTime before, int limit);

    /**
     * 插入批次
     */
    int insert(BusinessKeyProvisioning entity);

    /**
     * 更新批次（乐观锁由 SQL row_version 控制）
     */
    int update(BusinessKeyProvisioning entity);
}
