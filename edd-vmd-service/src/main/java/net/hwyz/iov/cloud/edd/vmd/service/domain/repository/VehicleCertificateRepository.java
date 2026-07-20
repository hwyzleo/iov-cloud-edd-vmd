package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 车辆设备证书数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleCertificateRepository {

    /**
     * 根据条件查询证书列表
     *
     * @param map 查询条件
     * @return 证书列表
     */
    List<VehicleCertificate> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询证书
     *
     * @param id 主键ID
     * @return 证书
     */
    VehicleCertificate selectById(Long id);

    /**
     * 根据requestId查询证书
     *
     * @param requestId 业务请求ID
     * @return 证书
     */
    VehicleCertificate selectByRequestId(String requestId);

    /**
     * 根据certSn查询证书
     *
     * @param certSn 证书序列号
     * @return 证书
     */
    VehicleCertificate selectByCertSn(String certSn);

    /**
     * 根据pkiRequestId查询证书
     *
     * @param pkiRequestId PKI申请编号
     * @return 证书
     */
    VehicleCertificate selectByPkiRequestId(String pkiRequestId);

    /**
     * 根据VIN和设备类别查询活跃证书
     *
     * @param vin            车架号
     * @param deviceCategory 设备类别
     * @return 证书
     */
    VehicleCertificate selectActiveByVinAndDeviceCategory(String vin, String deviceCategory);

    /**
     * 根据设备SN和证书Profile查询活跃证书
     *
     * @param deviceSn          设备SN
     * @param certificateProfile 证书Profile
     * @return 证书
     */
    VehicleCertificate selectActiveByDeviceSnAndProfile(String deviceSn, String certificateProfile);

    /**
     * 根据设备SN查询证书列表
     *
     * @param deviceSn 设备SN
     * @return 证书列表
     */
    List<VehicleCertificate> selectByDeviceSn(String deviceSn);

    /**
     * 根据VIN查询证书列表
     *
     * @param vin 车架号
     * @return 证书列表
     */
    List<VehicleCertificate> selectByVin(String vin);

    /**
     * 查询更新时间大于指定时间的证书列表（用于对账）
     *
     * @param updatedAfter 更新时间
     * @param limit        限制数量
     * @return 证书列表
     */
    List<VehicleCertificate> selectUpdatedAfter(Instant updatedAfter, int limit);

    /**
     * 新增证书
     *
     * @param vehicleCertificate 证书
     * @return 影响行数
     */
    int insert(VehicleCertificate vehicleCertificate);

    /**
     * 修改证书
     *
     * @param vehicleCertificate 证书
     * @return 影响行数
     */
    int update(VehicleCertificate vehicleCertificate);

    /**
     * 根据requestId修改证书状态
     *
     * @param requestId  业务请求ID
     * @param fromStatus 原状态
     * @param toStatus   目标状态
     * @return 影响行数
     */
    int updateStatusByRequestId(String requestId, CertificateStatus fromStatus, CertificateStatus toStatus);

    /**
     * 物理删除证书
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int physicalDeleteById(Long id);

}
