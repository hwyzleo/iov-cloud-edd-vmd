package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务密钥多用途开通批次表 DAO（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Mapper
public interface BusinessKeyProvisioningMapper extends BaseDao<BusinessKeyProvisioningPo, Long> {

    /**
     * 根据 requestId 查询
     */
    BusinessKeyProvisioningPo selectPoByRequestId(@Param("requestId") String requestId);

    /**
     * 根据 requestId 查询并加行锁（完成事务串行化）
     */
    BusinessKeyProvisioningPo selectPoByRequestIdForUpdate(@Param("requestId") String requestId);

    /**
     * 查询同设备+业务域的未终结批次（open_flag=1）
     */
    BusinessKeyProvisioningPo selectOpenPoByDeviceAndDomain(@Param("deviceSn") String deviceSn,
                                                            @Param("businessDomain") String businessDomain);

    /**
     * 查询同设备+业务域全部未终结批次（证书维度寻址，VMD-DSN-CR-065 §6.2，不 LIMIT）
     */
    List<BusinessKeyProvisioningPo> selectOpenPoListByDeviceAndDomain(@Param("deviceSn") String deviceSn,
                                                                      @Param("businessDomain") String businessDomain);

    /**
     * 查询同设备+业务域历史终结批次（VMD-DSN-CR-065 §6.2 includeTerminal=true）
     */
    List<BusinessKeyProvisioningPo> selectTerminalPoListByDeviceAndDomain(@Param("deviceSn") String deviceSn,
                                                                          @Param("businessDomain") String businessDomain);

    /**
     * 扫描指定状态的过期批次
     */
    List<BusinessKeyProvisioningPo> selectPoListByStatusBefore(@Param("status") String status,
                                                               @Param("before") LocalDateTime before,
                                                               @Param("limit") int limit);
}
