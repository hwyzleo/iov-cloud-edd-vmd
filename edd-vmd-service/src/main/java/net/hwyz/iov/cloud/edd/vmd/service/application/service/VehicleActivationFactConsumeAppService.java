package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactNormalized;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.UserVehicleActivationFactEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationConsumeResult;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationDigestSource;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationFactConsumeStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 车辆首次激活候选事实消费应用服务（VMD-DSN-CR-077 §6.1）
 * <p>
 * 编排「事件级 + 业务级双重幂等 → 台账归档 → 投影条件 upsert」：
 * <ol>
 *   <li><strong>事件级</strong>：{@code event_id} 命中且 {@code vin}/{@code bindingOperationId} 一致 → {@code DUPLICATE}；
 *       不一致 → 真冲突 {@code QUARANTINED}（不覆盖既有证据与投影）</li>
 *   <li><strong>业务级</strong>：{@code (vin, binding_operation_id)} 命中 → {@code DUPLICATE}；
 *       摘要不同仅记 {@code DIGEST_MISMATCH}，<strong>绝不</strong>判冲突终态（R-10）</li>
 *   <li><strong>摘要口径（终审修复 B-4 残留）</strong>：事件携带 {@code payloadDigest} 时<strong>以其为权威</strong>落库
 *       （{@code digest_source=EVENT}），本地复算仅作完整性校验；校验不一致仅在投影备注
 *       {@code DIGEST_VERIFY_MISMATCH} + WARN + 指标，<strong>不改变 status、不判冲突、不影响幂等</strong></li>
 *   <li><strong>投影</strong>：{@code occurred_at} 最小值单调收敛，更早则向前纠正（{@code CORRECTED}），
 *       否则保持最早（台账仍 {@code RECORDED}，{@code applied=0}）</li>
 * </ol>
 * 事务边界：台账与投影同一短事务，投影失败整体回滚（不留「台账成功而投影缺失」半成品），
 * 由消费者错误处理器退避重试，耗尽后由 {@link #markFailed} 落 {@code FAILED} 证据。
 * <p>
 * 纯终端：不写 {@code vmd_outbox}、不发二次事件、不写 {@code tb_veh_lifecycle}、
 * 不因 VIN 未建档而伪造车辆主档（D-VMD-077-08 / D-VMD-077-10）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleActivationFactConsumeAppService {

    private static final String DESCRIPTION_CORRECTED = "CORRECTED";
    private static final String DESCRIPTION_DIGEST_MISMATCH = "DIGEST_MISMATCH";
    private static final String DESCRIPTION_DIGEST_VERIFY_MISMATCH = "DIGEST_VERIFY_MISMATCH";
    private static final String DESCRIPTION_EVENT_ID_CONFLICT = "EVENT_ID_BUSINESS_KEY_MISMATCH";
    private static final int LAST_ERROR_MAX_LENGTH = 512;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final VehicleActivationFactConsumeRepository consumeRepository;
    private final VehicleActivationNodeRepository nodeRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final ObjectMapper objectMapper;

    /**
     * 消费一条已规范化的激活事实
     *
     * @param fact 已校验事实
     * @param meta 来源位点
     * @return 消费结论
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleActivationConsumeResult consume(ActivationFactNormalized fact, ActivationFactRecordMeta meta) {
        // ② 事件级去重
        VehicleActivationFactConsume existingByEvent = consumeRepository.selectByEventId(fact.eventId());
        if (existingByEvent != null) {
            if (!sameEventIdentity(existingByEvent, fact)) {
                // 真冲突：同 eventId 但 vin/bindingOperationId 不同 → 隔离，不覆盖既有投影
                return quarantineConflict(existingByEvent, fact, meta);
            }
            return markDuplicate(existingByEvent, fact.payloadDigest(), fact.digestProvidedButMismatch());
        }

        // ③ 业务级去重（同一次绑定操作只允许一条有效事实）
        VehicleActivationFactConsume existingByBusiness = consumeRepository.selectByBusinessKey(
                fact.vin(), fact.bindingOperationId());
        if (existingByBusiness != null) {
            if (!fact.eventId().equals(existingByBusiness.getEventId())) {
                // 合法重投：不同 eventId 同业务键；摘要差异不改变终态（R-10）
                return markDuplicate(existingByBusiness, fact.payloadDigest(), fact.digestProvidedButMismatch());
            }
            return markDuplicate(existingByBusiness, fact.payloadDigest(), fact.digestProvidedButMismatch());
        }

        // ④ 首次见到的合法事实：台账 + 投影同事务
        boolean vinUnresolved = !vinResolved(fact.vin());
        VehicleActivationNode candidate = buildNodeCandidate(fact, meta, vinUnresolved);
        VehicleActivationNode existingNode = nodeRepository.selectByVin(fact.vin());
        boolean projectionExisted = existingNode != null;
        boolean applied = upsertProjection(existingNode, candidate);
        boolean corrected = applied && projectionExisted;

        VehicleActivationFactConsume audit = buildAudit(fact, meta);
        audit.setStatus(ActivationFactConsumeStatus.RECORDED.name());
        audit.setApplied(applied ? 1 : 0);
        audit.setDescription(corrected ? DESCRIPTION_CORRECTED : null);
        int inserted = consumeRepository.insert(audit);
        if (inserted == 0) {
            // 并发兜底：唯一键命中说明同事件/同业务键已被其它线程处理，转重复判定（投影单调安全）
            VehicleActivationFactConsume raceWinner =
                    consumeRepository.selectByBusinessKey(fact.vin(), fact.bindingOperationId());
            if (raceWinner == null) {
                raceWinner = consumeRepository.selectByEventId(fact.eventId());
            }
            if (raceWinner != null) {
                return markDuplicate(raceWinner, fact.payloadDigest(), fact.digestProvidedButMismatch());
            }
            log.warn("激活事实台账写入返回0行且回读为空，按失败处理: eventId={}", fact.eventId());
            return VehicleActivationConsumeResult.failed();
        }

        if (vinUnresolved) {
            // D-VMD-077-08：仅标记 + 指标 + WARN，不建主档、不隔离
            log.warn("激活事实对应VIN未在VMD建档（仅事实投影，不建车）: vin={}, eventId={}",
                    maskVin(fact.vin()), fact.eventId());
        }
        log.info("激活事实已记录: eventId={}, vin={}, occurredAt={}, applied={}, corrected={}, digestSource={}",
                fact.eventId(), maskVin(fact.vin()), fact.occurredAt(), applied, corrected, fact.digestSourceCode());
        return applied
                ? VehicleActivationConsumeResult.recorded(corrected, vinUnresolved)
                : VehicleActivationConsumeResult.recordedWithoutEffect(vinUnresolved);
    }

    /**
     * 契约错误隔离（不重试、不阻塞分区）
     *
     * @param raw      原始事件（可能为 null 或字段不全）
     * @param meta     来源位点
     * @param reason   隔离原因（脱敏、截断）
     * @param rawValue 原始载荷（仅用于派生占位幂等键，不落库）
     * @return 消费结论
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleActivationConsumeResult quarantine(UserVehicleActivationFactEvent raw,
                                                     ActivationFactRecordMeta meta,
                                                     String reason,
                                                     String rawValue) {
        VehicleActivationFactConsume audit = buildQuarantineAudit(raw, meta, reason, rawValue);
        audit.setStatus(ActivationFactConsumeStatus.QUARANTINED.name());
        audit.setApplied(0);
        audit.setLastError(truncate(reason, LAST_ERROR_MAX_LENGTH));
        archive(audit);
        log.warn("激活事实已隔离: location={}, reason={}", meta, reason);
        return VehicleActivationConsumeResult.quarantined();
    }

    /**
     * 技术失败终态归档（退避重试耗尽后由错误处理器调用）
     *
     * @param rawValue  原始载荷（可能不可解析）
     * @param meta      来源位点
     * @param error     错误摘要（脱敏、截断）
     * @param retryCount 已重试次数
     * @return 消费结论
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleActivationConsumeResult markFailed(String rawValue, ActivationFactRecordMeta meta,
                                                     String error, int retryCount) {
        UserVehicleActivationFactEvent raw = parseLeniently(rawValue);
        VehicleActivationFactConsume existing = raw == null || StrUtil.isBlank(raw.getEventId())
                ? null : consumeRepository.selectByEventId(raw.getEventId());
        if (existing != null) {
            existing.setStatus(ActivationFactConsumeStatus.FAILED.name());
            existing.setApplied(0);
            existing.setRetryCount(retryCount);
            existing.setLastError(truncate(error, LAST_ERROR_MAX_LENGTH));
            existing.setModifyTime(LocalDateTime.now());
            consumeRepository.update(existing);
        } else {
            VehicleActivationFactConsume audit = buildQuarantineAudit(raw, meta, error, rawValue);
            audit.setStatus(ActivationFactConsumeStatus.FAILED.name());
            audit.setApplied(0);
            audit.setRetryCount(retryCount);
            audit.setLastError(truncate(error, LAST_ERROR_MAX_LENGTH));
            archive(audit);
        }
        log.error("激活事实技术失败（重试耗尽，保留位点待重放）: location={}, retryCount={}, error={}",
                meta, retryCount, truncate(error, 200));
        return VehicleActivationConsumeResult.failed();
    }

    /**
     * 判定 VIN 是否已在 VMD 建档（不写主档、不反向触发导入）
     *
     * @param vin 车架号
     * @return true=已建档
     */
    private boolean vinResolved(String vin) {
        return vehBasicInfoRepository.selectByVin(vin) != null;
    }

    /**
     * 投影条件 upsert：仅在候选事实更早时插入或向前纠正（D-VMD-077-04）
     * <p>
     * 「首次」以事实时刻而非到达顺序判定：更早则向前纠正，更晚或相等则保持最早，
     * 数据库层条件更新 {@code WHERE occurred_at > ?} 再做一次单调保护。
     *
     * @param existing  既有投影（可空）
     * @param candidate 候选投影（occurredAt=候选事实时刻）
     * @return true=投影已反映本事实
     */
    private boolean upsertProjection(VehicleActivationNode existing, VehicleActivationNode candidate) {
        candidate.setFirstActivatedAt(candidate.getOccurredAt());
        if (existing == null) {
            int inserted = nodeRepository.insert(candidate);
            if (inserted > 0) {
                return true;
            }
            // 并发插入冲突：唯一键兜底后重读重判（更早则纠正，否则忽略）
            VehicleActivationNode reRead = nodeRepository.selectByVin(candidate.getVin());
            if (reRead == null || !reRead.shouldCorrect(candidate.getOccurredAt())) {
                return false;
            }
            candidate.setDescription(DESCRIPTION_CORRECTED);
            return nodeRepository.correctIfEarlier(candidate) > 0;
        }
        if (!existing.shouldCorrect(candidate.getOccurredAt())) {
            // 晚到/相等事实：投影保持最早，仅台账留证
            return false;
        }
        candidate.setDescription(DESCRIPTION_CORRECTED);
        return nodeRepository.correctIfEarlier(candidate) > 0;
    }

    /**
     * 同 eventId 身份一致性判定：{@code vin} 与 {@code bindingOperationId} 均需一致
     *
     * @param existing 既有台账
     * @param fact     当前事实
     * @return true=身份一致（属重复投递）
     */
    private boolean sameEventIdentity(VehicleActivationFactConsume existing, ActivationFactNormalized fact) {
        return eq(existing.getVin(), fact.vin())
                && eq(existing.getBindingOperationId(), fact.bindingOperationId());
    }

    /**
     * 真冲突隔离：同 eventId 不同业务身份，不覆盖既有投影与台账终态，仅附冲突备注
     *
     * @param existing 既有台账
     * @param fact     当前事实
     * @param meta     来源位点
     * @return 消费结论
     */
    private VehicleActivationConsumeResult quarantineConflict(VehicleActivationFactConsume existing,
                                                              ActivationFactNormalized fact,
                                                              ActivationFactRecordMeta meta) {
        existing.setDescription(appendDescription(existing.getDescription(), DESCRIPTION_EVENT_ID_CONFLICT));
        existing.setModifyTime(LocalDateTime.now());
        consumeRepository.update(existing);
        log.warn("激活事实真冲突（同eventId不同业务键），已隔离不覆盖: eventId={}, location={}, existingVin={}, incomingVin={}",
                fact.eventId(), meta, maskVin(existing.getVin()), maskVin(fact.vin()));
        return VehicleActivationConsumeResult.quarantined();
    }

    /**
     * 重复投递归档：更新既有台账终态为 {@code DUPLICATE}（保留首次投影不动）
     *
     * @param existing       既有台账
     * @param incomingDigest 本次事实的权威摘要（事件提供优先，缺失为本地复算）
     * @param verifyMismatch 事件摘要与本地复算不一致（观测，不改变判定）
     * @return 消费结论
     */
    private VehicleActivationConsumeResult markDuplicate(VehicleActivationFactConsume existing,
                                                         String incomingDigest,
                                                         boolean verifyMismatch) {
        boolean digestMismatch = !eq(existing.getPayloadDigest(), incomingDigest);
        existing.setStatus(ActivationFactConsumeStatus.DUPLICATE.name());
        if (digestMismatch) {
            existing.setDescription(appendDescription(existing.getDescription(), DESCRIPTION_DIGEST_MISMATCH));
        }
        if (verifyMismatch) {
            existing.setDescription(appendDescription(existing.getDescription(), DESCRIPTION_DIGEST_VERIFY_MISMATCH));
        }
        existing.setModifyTime(LocalDateTime.now());
        consumeRepository.update(existing);
        log.info("激活事实重复投递（保留首次，不覆盖投影）: eventId={}, digestMismatch={}", existing.getEventId(), digestMismatch);
        return VehicleActivationConsumeResult.duplicate(digestMismatch);
    }

    /**
     * 归档台账：先插入，命中唯一键则更新既有证据（幂等）
     *
     * @param audit 台账
     */
    private void archive(VehicleActivationFactConsume audit) {
        int inserted = consumeRepository.insert(audit);
        if (inserted > 0) {
            return;
        }
        VehicleActivationFactConsume existing = consumeRepository.selectByEventId(audit.getEventId());
        if (existing == null) {
            existing = consumeRepository.selectByBusinessKey(audit.getVin(), audit.getBindingOperationId());
        }
        if (existing == null) {
            log.warn("激活事实台账归档未生效且回读为空: eventId={}, status={}", audit.getEventId(), audit.getStatus());
            return;
        }
        existing.setStatus(audit.getStatus());
        existing.setApplied(audit.getApplied());
        existing.setLastError(audit.getLastError());
        existing.setRetryCount(audit.getRetryCount());
        existing.setDescription(appendDescription(existing.getDescription(), audit.getDescription()));
        existing.setModifyTime(LocalDateTime.now());
        consumeRepository.update(existing);
    }

    /**
     * 构建合法事实台账
     *
     * @param fact 事实
     * @param meta 位点
     * @return 台账
     */
    private VehicleActivationFactConsume buildAudit(
            ActivationFactNormalized fact, ActivationFactRecordMeta meta) {
        LocalDateTime receivedAt = LocalDateTime.now(ZoneOffset.UTC);
        return VehicleActivationFactConsume.builder()
                .eventId(fact.eventId())
                .schemaVersion(fact.schemaVersion())
                .vin(fact.vin())
                .bindingOperationId(fact.bindingOperationId())
                .sourceFactRef(fact.sourceFactRef())
                .payloadDigest(fact.payloadDigest())
                .digestSource(fact.digestSourceCode())
                .occurredAt(toLocalDateTime(fact.occurredAt()))
                .receivedAt(receivedAt)
                .retryCount(0)
                .topic(meta.topic())
                .partitionNo(meta.partition())
                .recordOffset(meta.offset())
                .createTime(receivedAt)
                .build();
    }

    /**
     * 构建隔离/失败台账（字段可能不全，占位值保证 NOT NULL 约束）
     * <p>
     * 摘要仍遵循「事件提供为权威」：载荷携带合法 {@code payloadDigest} 时落事件值并记 {@code EVENT}，
     * 否则按 {@code occurredAt} 原文本地复算并记 {@code LOCAL}。
     *
     * @param raw      原始事件（可空）
     * @param meta     位点
     * @param reason   原因
     * @param rawValue 原始载荷（仅用于派生占位幂等键）
     * @return 台账
     */
    private VehicleActivationFactConsume buildQuarantineAudit(
            UserVehicleActivationFactEvent raw, ActivationFactRecordMeta meta, String reason, String rawValue) {
        LocalDateTime receivedAt = LocalDateTime.now(ZoneOffset.UTC);
        String vin = normalizePlaceholderVin(raw == null ? null : raw.getVin());
        String bindingOperationId = placeholder(raw == null ? null : raw.getBindingOperationId());
        String sourceFactRef = raw == null ? null : raw.getSourceFactRef();
        String occurredAtText = raw == null ? null : raw.getOccurredAt();
        Instant occurredAt = safeInstant(occurredAtText, receivedAt);
        String eventId = placeholderEventId(raw == null ? null : raw.getEventId(), meta, rawValue);
        String localDigest = ActivationPayloadDigest.compute(vin, bindingOperationId, sourceFactRef, occurredAtText);
        String providedDigest = raw == null ? null : StrUtil.trimToNull(raw.getPayloadDigest());
        boolean provided = providedDigest != null
                && providedDigest.length() <= ActivationPayloadDigest.DIGEST_LENGTH;
        return VehicleActivationFactConsume.builder()
                .eventId(eventId)
                .schemaVersion(raw == null || raw.getSchemaVersion() == null ? 1 : raw.getSchemaVersion())
                .vin(vin)
                .bindingOperationId(bindingOperationId)
                .sourceFactRef(sourceFactRef)
                .payloadDigest(provided ? providedDigest : localDigest)
                .digestSource(provided ? ActivationDigestSource.EVENT.name() : ActivationDigestSource.LOCAL.name())
                .occurredAt(toLocalDateTime(occurredAt))
                .receivedAt(receivedAt)
                .retryCount(0)
                .topic(meta.topic())
                .partitionNo(meta.partition())
                .recordOffset(meta.offset())
                .description(truncate(reason, DESCRIPTION_MAX_LENGTH))
                .createTime(receivedAt)
                .build();
    }

    /**
     * 构建投影候选
     *
     * @param fact          事实
     * @param meta          位点
     * @param vinUnresolved VIN 是否未建档
     * @return 投影候选
     */
    private VehicleActivationNode buildNodeCandidate(ActivationFactNormalized fact, ActivationFactRecordMeta meta,
                                                     boolean vinUnresolved) {
        LocalDateTime receivedAt = LocalDateTime.now(ZoneOffset.UTC);
        String description = fact.digestProvidedButMismatch() ? DESCRIPTION_DIGEST_VERIFY_MISMATCH : null;
        return VehicleActivationNode.builder()
                .vin(fact.vin())
                .firstActivatedAt(toLocalDateTime(fact.occurredAt()))
                .bindingOperationId(fact.bindingOperationId())
                .sourceFactRef(fact.sourceFactRef())
                .payloadDigest(fact.payloadDigest())
                .eventId(fact.eventId())
                .occurredAt(toLocalDateTime(fact.occurredAt()))
                .receivedAt(receivedAt)
                .schemaVersion(fact.schemaVersion())
                .digestSource(fact.digestSourceCode())
                .vinResolved(vinUnresolved ? 0 : 1)
                .description(description)
                .createTime(receivedAt)
                .build();
    }

    /**
     * 宽松解析原始载荷（失败返回 null，不抛异常）
     *
     * @param rawValue 原始载荷
     * @return 事件或 null
     */
    private UserVehicleActivationFactEvent parseLeniently(String rawValue) {
        if (StrUtil.isBlank(rawValue)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValue, UserVehicleActivationFactEvent.class);
        } catch (Exception e) {
            log.debug("原始载荷无法解析（按不可解析载荷归档失败证据）: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 缺失 eventId 时派生稳定的占位幂等键（不使用随机值，保证重放幂等）
     *
     * @param eventId  原始 eventId
     * @param meta     位点
     * @param rawValue 原始载荷
     * @return 幂等键（≤64）
     */
    private String placeholderEventId(String eventId, ActivationFactRecordMeta meta, String rawValue) {
        if (StrUtil.isNotBlank(eventId) && eventId.length() <= ActivationFactNormalized.ID_MAX_LENGTH) {
            return eventId;
        }
        String seed = meta.toString() + "|" + StrUtil.nullToEmpty(rawValue);
        return "MISSING-" + DigestUtil.sha256Hex(seed).substring(0, 32);
    }

    /**
     * VIN 占位：合法宽度保留（隔离证据仍需可比对），超长/缺失用固定占位
     *
     * @param vin 原始 VIN
     * @return 存储用 VIN（≤20）
     */
    private String normalizePlaceholderVin(String vin) {
        if (StrUtil.isBlank(vin)) {
            return "-";
        }
        String normalized = vin.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.length() > ActivationFactNormalized.VIN_MAX_LENGTH ? "-" : normalized;
    }

    /**
     * 字段占位
     *
     * @param value 原值
     * @return 原值或占位符
     */
    private String placeholder(String value) {
        if (StrUtil.isBlank(value)) {
            return "-";
        }
        return value.length() > ActivationFactNormalized.ID_MAX_LENGTH
                ? value.substring(0, ActivationFactNormalized.ID_MAX_LENGTH) : value;
    }

    /**
     * 安全解析时刻（失败回退到接收时刻）
     *
     * @param text     时间文本
     * @param fallback 回退时刻
     * @return 时刻
     */
    private Instant safeInstant(String text, LocalDateTime fallback) {
        if (StrUtil.isNotBlank(text)) {
            try {
                return Instant.parse(text);
            } catch (Exception ignored) {
                try {
                    return java.time.OffsetDateTime.parse(text).toInstant();
                } catch (Exception alsoIgnored) {
                    log.debug("occurredAt 不可解析，隔离证据使用接收时刻占位");
                }
            }
        }
        return fallback.toInstant(ZoneOffset.UTC);
    }

    /**
     * LocalDateTime(UTC) 转换
     *
     * @param instant 时刻
     * @return UTC LocalDateTime
     */
    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * 备注追加（去重、限长）
     *
     * @param existing 现有备注
     * @param append   追加备注
     * @return 合并后备注
     */
    private static String appendDescription(String existing, String append) {
        if (StrUtil.isBlank(append)) {
            return existing;
        }
        if (StrUtil.isBlank(existing)) {
            return truncate(append, DESCRIPTION_MAX_LENGTH);
        }
        if (existing.contains(append)) {
            return existing;
        }
        return truncate(existing + "," + append, DESCRIPTION_MAX_LENGTH);
    }

    /**
     * 安全等值比较
     *
     * @param left  左值
     * @param right 右值
     * @return true=相等（忽略大小写）
     */
    private static boolean eq(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    /**
     * 截断
     *
     * @param text      原文
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    /**
     * VIN 脱敏（保留前 3 后 4）
     *
     * @param vin 车架号
     * @return 脱敏后的 VIN
     */
    private static String maskVin(String vin) {
        if (StrUtil.isBlank(vin) || vin.length() <= 7) {
            return "****";
        }
        return vin.substring(0, 3) + "****" + vin.substring(vin.length() - 4);
    }

}
