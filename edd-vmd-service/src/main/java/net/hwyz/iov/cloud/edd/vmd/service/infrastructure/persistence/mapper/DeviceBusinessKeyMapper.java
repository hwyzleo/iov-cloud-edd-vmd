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
     * 根据 keyId 查询并加行锁（CR-066 claim-or-reuse，须在事务内调用）
     *
     * @param keyId framework/KMS 不透明标识
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByKeyIdForUpdate(@Param("keyId") String keyId);

    /**
     * 根据 requestId 查询（幂等）
     *
     * @param requestId 调用幂等键
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByRequestId(@Param("requestId") String requestId);

    /**
     * 根据 requestId + purpose 查询（CR-058 多用途批次目录行定位）
     *
     * @param requestId 调用幂等键
     * @param purpose   用途
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByRequestIdAndPurpose(@Param("requestId") String requestId,
                                                      @Param("purpose") String purpose);

    /**
     * 根据主键查询并加行锁（CR-058 完成事务原子切换，须在事务内调用）
     *
     * @param id 主键
     * @return PO，不存在返回 null
     */
    DeviceBusinessKeyPo selectPoByIdForUpdate(@Param("id") Long id);

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
     * 条件批量失败化本批次 PENDING 目录（VMD-DSN-CR-065）
     *
     * @param requestId 批次幂等键
     * @param keyIds    本批次关联的目录 key_id 集合
     * @param reason    非敏感失败原因
     * @return 受影响行数
     */
    int markPendingKeysFailed(@Param("requestId") String requestId,
                              @Param("keyIds") List<String> keyIds,
                              @Param("reason") String reason);

    /**
     * 查询超过解密窗口的 DEPRECATED 行（EXPIRED 扫批）
     *
     * @param now   当前时间
     * @param limit 批量上限
     * @return PO 列表
     */
    List<DeviceBusinessKeyPo> selectDeprecatedExpiredPo(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 回填根派生非敏感材料（CR-066 §6.3）。
     * <p>仅更新 PENDING 且 key_id IS NULL 的占位行（乐观锁保护）；撞 uk_key_id 时抛 DuplicateKeyException。
     *
     * @param po 携带 id/rowVersion 与 keyId/kmsKeyRef/rootKeyVersion/derivationVersion/operationId/algorithm/keySpec/logicalIdentityDigest
     * @return 影响行数
     */
    int attachDerivedMaterial(DeviceBusinessKeyPo po);

    /**
     * 占位行标记 SUPERSEDED（CR-066 §6.4，仅 key_id IS NULL 的 PENDING 占位行）。
     *
     * @param id            占位行主键
     * @param supersededById canonical 目录行主键
     * @param reason        非敏感取代原因
     * @return 影响行数
     */
    int markPlaceholderSuperseded(@Param("id") Long id, @Param("supersededById") Long supersededById,
                                  @Param("reason") String reason);

    /**
     * FAILED→PENDING_RECOVERY（恢复入口，仅 FAILED 且已有 keyId 证据；CR-066 §6.4）。
     *
     * @param id     目录行主键
     * @param reason 非敏感恢复原因
     * @return 影响行数
     */
    int markPendingRecovery(@Param("id") Long id, @Param("reason") String reason);

    /**
     * PENDING_RECOVERY→PENDING（恢复确认，recovery_count+1，记录最近恢复 attempt 审计引用；CR-066 §6.4）。
     *
     * @param id                  目录行主键
     * @param recoveryRequestId   本次恢复 attempt 的 requestId
     * @return 影响行数
     */
    int confirmRecovery(@Param("id") Long id, @Param("recoveryRequestId") String recoveryRequestId);
}
