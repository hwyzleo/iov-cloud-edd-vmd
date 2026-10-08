package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DeviceBusinessKeyPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备业务密钥目录表 DAO
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Mapper
public interface DeviceBusinessKeyMapper extends BaseDao<DeviceBusinessKeyPo, Long> {

    /**
     * 根据 keyId 查询
     *
     * @param keyId framework/KMS 不透明标识
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByKeyId(@Param("keyId") String keyId);

    /**
     * 根据 requestId 查询（幂等）
     *
     * @param requestId 调用幂等键
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByRequestId(@Param("requestId") String requestId);

    /**
     * 查询唯一 ACTIVE
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectActivePoByContext(@Param("deviceSn") String deviceSn,
                                                @Param("businessDomain") String businessDomain,
                                                @Param("purpose") String purpose);

    /**
     * 查询唯一 ACTIVE 并加行锁（轮换/吊销互斥，须在事务内调用）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectActivePoByContextForUpdate(@Param("deviceSn") String deviceSn,
                                                         @Param("businessDomain") String businessDomain,
                                                         @Param("purpose") String purpose);

    /**
     * 按上下文查询全部历史版本（按 business_key_version 升序）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return PO 列表
     */
    List<DeviceBusinessKeyPo> selectPoListByContext(@Param("deviceSn") String deviceSn,
                                                    @Param("businessDomain") String businessDomain,
                                                    @Param("purpose") String purpose);

    /**
     * 按上下文与业务版本查询
     *
     * @param deviceSn           设备实例序列号
     * @param businessDomain     业务域
     * @param purpose            用途
     * @param businessKeyVersion 业务版本
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByContextAndVersion(@Param("deviceSn") String deviceSn,
                                                    @Param("businessDomain") String businessDomain,
                                                    @Param("purpose") String purpose,
                                                    @Param("businessKeyVersion") Long businessKeyVersion);

    /**
     * 查询上下文的当前最大业务版本
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return 最大版本号，无记录返回 null
     */
    Long selectMaxBusinessKeyVersion(@Param("deviceSn") String deviceSn,
                                     @Param("businessDomain") String businessDomain,
                                     @Param("purpose") String purpose);

    /**
     * 统计上下文 ACTIVE 数量
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return ACTIVE 行数
     */
    int countActivePoByContext(@Param("deviceSn") String deviceSn,
                               @Param("businessDomain") String businessDomain,
                               @Param("purpose") String purpose);

    /**
     * 按设备查询全部业务密钥
     *
     * @param deviceSn 设备实例序列号
     * @return PO 列表
     */
    List<DeviceBusinessKeyPo> selectPoListByDeviceSn(@Param("deviceSn") String deviceSn);

    /**
     * 查询超过解密窗口的 DEPRECATED 行（EXPIRED 扫批）
     *
     * @param now   当前时间
     * @param limit 批量上限
     * @return PO 列表
     */
    List<DeviceBusinessKeyPo> selectDeprecatedExpiredPo(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
