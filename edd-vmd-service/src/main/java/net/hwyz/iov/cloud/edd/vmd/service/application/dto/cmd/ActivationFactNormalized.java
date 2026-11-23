package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.UserVehicleActivationFactEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactContractException;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationDigestSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * 已校验并规范化的首次激活事实（VMD-DSN-CR-077 §4.1 / §6.1 ①②，终审修复 B-4 残留）
 * <p>
 * 契约校验规则（不满足即 {@link ActivationFactContractException} → 隔离，不重试）：
 * <ul>
 *   <li>必填：{@code eventId}（≤64）、{@code schemaVersion}=1、{@code vin}（≤20）、
 *       {@code bindingOperationId}（≤64）、{@code occurredAt} 可解析</li>
 *   <li>可空：{@code sourceFactRef}（≤512）、{@code payloadDigest}（≤64）</li>
 *   <li>{@code vin} 做 {@code trim + 大写(Locale.ROOT)} 规范化；超长<strong>不截断</strong>直接隔离</li>
 *   <li>{@code eventId}/{@code bindingOperationId} 按原值精确匹配（不去空格、不改大小写）</li>
 *   <li>Kafka Key 非空时必须与规范化 VIN 一致</li>
 * </ul>
 * <h3>摘要口径（终审修复 B-4 残留，定稿）</h3>
 * <ul>
 *   <li><strong>生产端摘要为权威</strong>：事件携带 {@code payloadDigest} 时<strong>一律以事件值落库</strong>，
 *       {@code digest_source=EVENT}；本地复算（{@link #localDigest()}）仅作完整性校验</li>
 *   <li>校验不一致时置 {@link #digestProvidedButMismatch()}：<strong>只</strong>打 WARN + 指标
 *       {@code digest_mismatch}，<strong>不改变 status、不判冲突、不影响幂等</strong></li>
 *   <li>事件未携带 {@code payloadDigest}：本地按 {@code occurredAt} 原文复算并 {@code digest_source=LOCAL}</li>
 *   <li>本地复算一律使用事件 {@code occurredAt} <strong>字符串原文</strong>参与哈希
 *       （见 {@link ActivationPayloadDigest}），避免「解析再序列化」造成两侧口径漂移</li>
 * </ul>
 *
 * @param eventId                    事件 ID（幂等键 1）
 * @param schemaVersion              契约版本
 * @param vin                        规范化 VIN
 * @param bindingOperationId         绑定操作 ID（幂等键 2）
 * @param sourceFactRef              来源事实引用（可空）
 * @param occurredAt                 事实发生时刻（解析后，UTC）
 * @param occurredAtText             事实发生时刻的事件字符串原文（参与摘要哈希）
 * @param payloadDigest              <strong>落库权威摘要</strong>：事件提供优先，缺失为本地复算值
 * @param localDigest                本地按事件原文复算的摘要（完整性校验/观测，恒非空）
 * @param digestSource               摘要来源：EVENT=事件提供 / LOCAL=本地复算
 * @param digestProvidedButMismatch  事件提供了摘要但本地校验不一致（观测标记，不改变判定）
 * @author hwyz_leo
 */
@Builder
public record ActivationFactNormalized(
        String eventId,
        Integer schemaVersion,
        String vin,
        String bindingOperationId,
        String sourceFactRef,
        Instant occurredAt,
        String occurredAtText,
        String payloadDigest,
        String localDigest,
        ActivationDigestSource digestSource,
        boolean digestProvidedButMismatch) {

    /**
     * VIN 存储宽度（与 VMD 既有全表一致，R-12）
     */
    public static final int VIN_MAX_LENGTH = 20;

    /**
     * 事件 ID / 绑定操作 ID 最大长度
     */
    public static final int ID_MAX_LENGTH = 64;

    /**
     * 来源事实引用最大长度
     */
    public static final int SOURCE_FACT_REF_MAX_LENGTH = 512;

    /**
     * 支持的契约版本
     */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /**
     * 校验 + 规范化事件
     *
     * @param event    原始事件
     * @param kafkaKey Kafka Key（可空）
     * @return 规范化事实
     * @throws ActivationFactContractException 契约错误（应隔离）
     */
    public static ActivationFactNormalized of(UserVehicleActivationFactEvent event, String kafkaKey) {
        if (event == null) {
            throw new ActivationFactContractException("事件载荷为空");
        }
        String eventId = event.getEventId();
        if (StrUtil.isBlank(eventId)) {
            throw new ActivationFactContractException("缺失必填字段 eventId");
        }
        if (eventId.length() > ID_MAX_LENGTH) {
            throw new ActivationFactContractException("eventId 超长（>" + ID_MAX_LENGTH + "）");
        }
        if (event.getSchemaVersion() == null) {
            throw new ActivationFactContractException("缺失必填字段 schemaVersion");
        }
        if (event.getSchemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new ActivationFactContractException("不支持的 schemaVersion=" + event.getSchemaVersion());
        }
        if (StrUtil.isBlank(event.getVin())) {
            throw new ActivationFactContractException("缺失必填字段 vin");
        }
        String vin = event.getVin().trim().toUpperCase(Locale.ROOT);
        if (vin.length() > VIN_MAX_LENGTH) {
            throw new ActivationFactContractException("vin 超长（>" + VIN_MAX_LENGTH + "，不截断）");
        }
        if (StrUtil.isNotBlank(kafkaKey) && !kafkaKey.trim().toUpperCase(Locale.ROOT).equals(vin)) {
            // Key 与 payload 不一致属生产端契约错误；脱敏日志不输出原文
            throw new ActivationFactContractException("Kafka Key 与 payload.vin 不一致");
        }
        String bindingOperationId = event.getBindingOperationId();
        if (StrUtil.isBlank(bindingOperationId)) {
            throw new ActivationFactContractException("缺失必填字段 bindingOperationId");
        }
        if (bindingOperationId.length() > ID_MAX_LENGTH) {
            throw new ActivationFactContractException("bindingOperationId 超长（>" + ID_MAX_LENGTH + "）");
        }
        String sourceFactRef = event.getSourceFactRef();
        if (sourceFactRef != null && sourceFactRef.length() > SOURCE_FACT_REF_MAX_LENGTH) {
            throw new ActivationFactContractException("sourceFactRef 超长（>" + SOURCE_FACT_REF_MAX_LENGTH + "）");
        }
        if (StrUtil.isBlank(event.getOccurredAt())) {
            throw new ActivationFactContractException("缺失必填字段 occurredAt");
        }
        String occurredAtText = event.getOccurredAt();
        Instant occurredAt = parseOccurredAt(occurredAtText);

        String providedDigest = StrUtil.trimToNull(event.getPayloadDigest());
        if (providedDigest != null && providedDigest.length() > ActivationPayloadDigest.DIGEST_LENGTH) {
            throw new ActivationFactContractException(
                    "payloadDigest 超长（>" + ActivationPayloadDigest.DIGEST_LENGTH + "）");
        }
        // 本地复算一律按事件 occurredAt 「字符串原文」参与哈希（口径见 ActivationPayloadDigest 类注释）
        String localDigest = ActivationPayloadDigest.compute(vin, bindingOperationId, sourceFactRef, occurredAtText);
        boolean provided = providedDigest != null;
        boolean providedMatches = provided && ActivationPayloadDigest.verify(providedDigest, vin, bindingOperationId,
                sourceFactRef, occurredAtText, occurredAt);
        return ActivationFactNormalized.builder()
                .eventId(eventId)
                .schemaVersion(event.getSchemaVersion())
                .vin(vin)
                .bindingOperationId(bindingOperationId)
                .sourceFactRef(sourceFactRef)
                .occurredAt(occurredAt)
                .occurredAtText(occurredAtText)
                // 事件摘要为权威：提供即落库事件值；缺失才回退本地复算
                .payloadDigest(provided ? providedDigest : localDigest)
                .localDigest(localDigest)
                .digestSource(provided ? ActivationDigestSource.EVENT : ActivationDigestSource.LOCAL)
                .digestProvidedButMismatch(provided && !providedMatches)
                .build();
    }

    /**
     * 解析事实发生时刻（ISO-8601 带 Z 或带偏移）
     *
     * @param text 时间文本
     * @return UTC 时刻
     * @throws ActivationFactContractException 不可解析
     */
    private static Instant parseOccurredAt(String text) {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // 兼容带偏移量的 ISO-8601
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            throw new ActivationFactContractException("occurredAt 不可解析");
        }
    }

    /**
     * 摘要来源编码（持久化用）
     *
     * @return {@code EVENT} / {@code LOCAL}
     */
    public String digestSourceCode() {
        return digestSource == null ? ActivationDigestSource.LOCAL.name() : digestSource.name();
    }

    /**
     * 事件是否携带摘要（落库摘要是否为生产端权威值）
     *
     * @return true=事件提供了 {@code payloadDigest}
     */
    public boolean digestProvided() {
        return digestSource == ActivationDigestSource.EVENT;
    }

}
