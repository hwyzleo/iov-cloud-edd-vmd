package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备业务密钥目录仓储接口
 * <p>
 * CR-055：按业务上下文（device_sn + business_domain + purpose）、keyId、requestId 查询与条件更新。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public interface DeviceBusinessKeyRepository {

    /**
     * 根据主键ID查询
     *
     * @param id 主键ID
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectById(Long id);

    /**
     * 根据 keyId 查询
     *
     * @param keyId framework/KMS 不透明标识
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByKeyId(String keyId);

    /**
     * 根据 keyId 查询并加行锁（CR-066 claim-or-reuse，须在事务内调用）
     *
     * @param keyId framework/KMS 不透明标识
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByKeyIdForUpdate(String keyId);

    /**
     * 根据 requestId 查询（幂等）
     *
     * @param requestId 调用幂等键
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByRequestId(String requestId);

    /**
     * 根据 requestId + purpose 查询（CR-058 多用途批次目录行定位）
     *
     * @param requestId 调用幂等键
     * @param purpose   用途
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByRequestIdAndPurpose(String requestId, String purpose);

    /**
     * 根据主键查询并加行锁（CR-058 完成事务原子切换，须在事务内调用）
     *
     * @param id 主键ID
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByIdForUpdate(Long id);

    /**
     * 查询唯一 ACTIVE（正向目录解析；无/多 ACTIVE 由调用方判定）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return ACTIVE 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectActiveByContext(String deviceSn, String businessDomain, String purpose);

    /**
     * 查询唯一 ACTIVE 并加行锁（轮换/吊销互斥，须在事务内调用）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return ACTIVE 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectActiveByContextForUpdate(String deviceSn, String businessDomain, String purpose);

    /**
     * 按上下文查询全部历史版本（按 business_key_version 升序）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return 全部版本列表
     */
    List<DeviceBusinessKey> selectByContext(String deviceSn, String businessDomain, String purpose);

    /**
     * 按上下文与业务版本查询
     *
     * @param deviceSn           设备实例序列号
     * @param businessDomain     业务域
     * @param purpose            用途
     * @param businessKeyVersion 业务版本
     * @return 业务密钥，不存在返回 null
     */
    DeviceBusinessKey selectByContextAndVersion(String deviceSn, String businessDomain, String purpose, Long businessKeyVersion);

    /**
     * 查询同逻辑材料、已带 keyId 证据的可恢复行（CR-066 §6.4 复用；CR-068 修复）。
     * <p>
     * 无 ACTIVE 时优先恢复并复用既有行（沿用其 businessKeyVersion），避免新建重复版本行后
     * 派生撞 uk_key_id 走 claim-or-reuse 重挂，导致 operationId 与版本分离、重试触发
     * FW 结果存储摘要冲突（同 operation 不同业务版本，FW-SEC-DSN-CR-020 §4）。
     * 仅命中同代际（derivation_version 一致）的 DERIVED_ROOT 证据行。
     *
     * @param deviceSn          设备实例序列号
     * @param businessDomain    业务域
     * @param purpose           用途
     * @param derivationVersion 当前派生格式版本（仅复用同代际材料）
     * @return 可恢复目录行（FAILED/PENDING_RECOVERY/RECONCILE_REQUIRED 且 key_id 非空），不存在返回 null
     */
    DeviceBusinessKey selectRecoverableByContext(String deviceSn, String businessDomain, String purpose,
                                                 String derivationVersion);

    /**
     * 查询上下文的当前最大业务版本（无记录返回 0）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return 最大版本号
     */
    Long maxBusinessKeyVersion(String deviceSn, String businessDomain, String purpose);

    /**
     * 统计上下文 ACTIVE 数量（多 ACTIVE 冲突判定）
     *
     * @param deviceSn       设备实例序列号
     * @param businessDomain 业务域
     * @param purpose        用途
     * @return ACTIVE 行数
     */
    int countActiveByContext(String deviceSn, String businessDomain, String purpose);

    /**
     * 按设备查询全部业务密钥（设备解绑/吊销策略）
     *
     * @param deviceSn 设备实例序列号
     * @return 全部版本列表
     */
    List<DeviceBusinessKey> selectByDeviceSn(String deviceSn);

    /**
     * 查询超过解密窗口的 DEPRECATED 行（EXPIRED 扫批）
     *
     * @param now   当前时间
     * @param limit 批量上限
     * @return 过期待转 EXPIRED 的行
     */
    List<DeviceBusinessKey> selectDeprecatedExpired(LocalDateTime now, int limit);

    /**
     * 回填根派生非敏感材料（CR-066 §6.3，仅 PENDING 且 key_id IS NULL 的占位行，乐观锁保护）。
     * <p>撞 uk_key_id 时抛 {@link org.springframework.dao.DuplicateKeyException}，由调用方执行 claim-or-reuse。
     *
     * @param entity 携带 id/rowVersion 与回填字段的目录行
     * @return 影响行数
     */
    int attachDerivedMaterial(DeviceBusinessKey entity);

    /**
     * 占位行标记 SUPERSEDED（CR-066 §6.4，仅 key_id IS NULL 的 PENDING 占位行；保留全部证据）。
     *
     * @param id            占位行主键
     * @param supersededById canonical 目录行主键
     * @param reason        非敏感取代原因
     * @return 影响行数
     */
    int markPlaceholderSuperseded(Long id, Long supersededById, String reason);

    /**
     * FAILED→PENDING_RECOVERY（恢复入口，仅 FAILED 且已有 keyId 证据；CR-066 §6.4）。
     *
     * @param id     目录行主键
     * @param reason 非敏感恢复原因
     * @return 影响行数
     */
    int markPendingRecovery(Long id, String reason);

    /**
     * PENDING_RECOVERY→PENDING（恢复确认，recovery_count+1，记录最近恢复 attempt 审计引用；CR-066 §6.4）。
     *
     * @param id                 目录行主键
     * @param recoveryRequestId  本次恢复 attempt 的 requestId
     * @return 影响行数
     */
    int confirmRecovery(Long id, String recoveryRequestId);

    /**
     * 条件批量失败化本批次 PENDING 目录（VMD-DSN-CR-065 RD-065-3）
     * <p>
     * 仅允许 PENDING → FAILED：{@code WHERE request_id = :requestId AND key_state = 'PENDING'
     * AND key_id IN (:keyIds)}，返回受影响行数；与预期 item 数不一致时由调用方整体回滚。
     * 不删除数据、不清空 key_id/operation_id/request_digest 等证据元数据。
     *
     * @param requestId 批次幂等键
     * @param keyIds    本批次关联的目录 key_id 集合（空集合时不做任何更新并返回 0）
     * @param reason    非敏感失败原因（紧凑终止码 + 审计关联号）
     * @return 受影响行数
     */
    int markPendingKeysFailed(String requestId, java.util.List<String> keyIds, String reason);

    /**
     * 插入新行
     *
     * @param entity 业务密钥
     * @return 影响行数
     */
    int insert(DeviceBusinessKey entity);

    /**
     * 更新行（条件更新，乐观锁由 SQL 层 row_version 控制）
     *
     * @param entity 业务密钥
     * @return 影响行数
     */
    int update(DeviceBusinessKey entity);
}
