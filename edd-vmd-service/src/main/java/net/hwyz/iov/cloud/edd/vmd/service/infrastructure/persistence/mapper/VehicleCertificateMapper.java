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
     * 根据设备SN、证书Profile与CSR指纹查询证书（幂等复用，F15）
     *
     * @param deviceSn           设备SN
     * @param certificateProfile 证书Profile
     * @param csrFingerprint     CSR SHA-256指纹
     * @return 证书
     */
    VehicleCertificatePo selectByDeviceSnAndProfileAndCsrFingerprint(@Param("deviceSn") String deviceSn, @Param("certificateProfile") String certificateProfile, @Param("csrFingerprint") String csrFingerprint);

    /**
     * 业务幂等复用：同 vin + hsm_uid + public_key_sha256 + certificate_profile（CR-054）
     */
    VehicleCertificatePo selectByVinAndUidAndSpkiAndProfile(@Param("vin") String vin, @Param("hsmUid") String hsmUid, @Param("publicKeySha256") String publicKeySha256, @Param("certificateProfile") String certificateProfile);

    /**
     * 换钥冲突检测：同 vin + hsm_uid + certificate_profile 但 SPKI 不同且非终态（CR-054）
     */
    VehicleCertificatePo selectKeyConflictByVinAndUidAndProfile(@Param("vin") String vin, @Param("hsmUid") String hsmUid, @Param("publicKeySha256") String publicKeySha256, @Param("certificateProfile") String certificateProfile);

    /**
     * 根据主键ID查询证书并加行锁（reconcile 并发互斥，FOR UPDATE）
     *
     * @param id 主键ID
     * @return 证书，不存在返回 null
     */
    VehicleCertificatePo selectByIdForUpdate(@Param("id") Long id);

    /**
     * 根据MES原请求号查询证书（人工补偿关联，CR-053）
     *
     * @param originalRequestId MES原请求号
     * @return 证书，不存在返回 null
     */
    VehicleCertificatePo selectByOriginalRequestId(@Param("originalRequestId") String originalRequestId);

    /**
     * 根据设备SN和Profile查询有效/处理中申请（防重复签发，CR-053）
     * <p>有效或处理中：REQUESTED / ISSUING / PENDING_RECONCILE / ISSUED_NOT_CONFIRMED / ACTIVE，
     * 排除 INSTALL_FAILED / SUPERSEDED / REVOKED / EXPIRED / FAILED</p>
     *
     * @param deviceSn           设备SN
     * @param certificateProfile 证书Profile
     * @return 证书，不存在返回 null
     */
    VehicleCertificatePo selectActiveOrInProgressByDeviceSnAndProfile(@Param("deviceSn") String deviceSn, @Param("certificateProfile") String certificateProfile);

    /**
     * 将同设备同Profile的其它 ACTIVE 证书置为 SUPERSEDED（落实“最多一条 ACTIVE”，§3.1）
     *
     * @param deviceSn           设备SN
     * @param certificateProfile 证书Profile
     * @param excludeRequestId   排除的当前请求ID（避免误伤自身）
     * @return 影响行数
     */
    int supersedeActiveByDeviceSnAndProfile(@Param("deviceSn") String deviceSn, @Param("certificateProfile") String certificateProfile, @Param("excludeRequestId") String excludeRequestId);

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
