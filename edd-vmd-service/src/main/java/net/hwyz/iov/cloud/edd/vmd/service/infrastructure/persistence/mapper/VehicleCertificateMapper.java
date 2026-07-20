package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificatePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * <p>
 * 车辆设备证书表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-20
 */
@Mapper
public interface VehicleCertificateMapper extends BaseDao<VehicleCertificatePo, Long> {

    /**
     * 根据requestId查询证书
     *
     * @param requestId 业务请求ID
     * @return 证书
     */
    VehicleCertificatePo selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据certSn查询证书
     *
     * @param certSn 证书序列号
     * @return 证书
     */
    VehicleCertificatePo selectByCertSn(@Param("certSn") String certSn);

    /**
     * 根据pkiRequestId查询证书
     *
     * @param pkiRequestId PKI申请编号
     * @return 证书
     */
    VehicleCertificatePo selectByPkiRequestId(@Param("pkiRequestId") String pkiRequestId);

    /**
     * 根据VIN和设备类别查询活跃证书
     *
     * @param vin            车架号
     * @param deviceCategory 设备类别
     * @return 证书
     */
    VehicleCertificatePo selectActiveByVinAndDeviceCategory(@Param("vin") String vin, @Param("deviceCategory") String deviceCategory);

    /**
     * 根据设备SN和证书Profile查询活跃证书
     *
     * @param deviceSn          设备SN
     * @param certificateProfile 证书Profile
     * @return 证书
     */
    VehicleCertificatePo selectActiveByDeviceSnAndProfile(@Param("deviceSn") String deviceSn, @Param("certificateProfile") String certificateProfile);

    /**
     * 根据设备SN查询证书列表
     *
     * @param deviceSn 设备SN
     * @return 证书列表
     */
    List<VehicleCertificatePo> selectByDeviceSn(@Param("deviceSn") String deviceSn);

    /**
     * 根据VIN查询证书列表
     *
     * @param vin 车架号
     * @return 证书列表
     */
    List<VehicleCertificatePo> selectByVin(@Param("vin") String vin);

    /**
     * 查询更新时间大于指定时间的证书列表（用于对账）
     *
     * @param updatedAfter 更新时间
     * @param limit        限制数量
     * @return 证书列表
     */
    List<VehicleCertificatePo> selectUpdatedAfter(@Param("updatedAfter") Instant updatedAfter, @Param("limit") int limit);

    /**
     * 根据requestId修改证书状态
     *
     * @param requestId  业务请求ID
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @return 影响行数
     */
    int updateStatusByRequestId(@Param("requestId") String requestId, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

}
