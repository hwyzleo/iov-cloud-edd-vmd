package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * VMD 事件专用 ISO 序列化 codec（VMD-DSN-CR-070 既有边界例外）
 * <p>
 * 为 {@code vmd.business-key.changed}（BusinessKeyChangedEvent）与
 * {@code vmd.tbox-v2c-key-provisioned}（TboxV2cKeyProvisionedEvent）两个事件提供
 * 受控、专用的 ISO-8601（UTC）写出能力——仅固定这两个事件 payload 的时间字段
 * {@code occurredAt}（Instant）为 ISO 字符串（如 {@code 2026-10-27T14:57:34.340Z}），
 * 与消费端 TSP（TSP-DSN-CR-013 已双读：ISO / 10 位秒 / 13 位毫秒等值）契约一致，
 * 避免 hutool 默认 epoch 毫秒数字被按 epoch 秒解析导致的单位歧义（OCCURRED_AT_FUTURE）。
 * <p>
 * 边界（CR-070 §5.10.2 / FW-WEB-DSN-CR-001 RD-001-5 / §5.1）：
 * <ul>
 *   <li>本 codec 内部持有独立 mapper，但<b>不注册为 Spring ObjectMapper bean</b>——
 *       避免触发 Boot {@code @ConditionalOnMissingBean(ObjectMapper)} 让位导致
 *       {@code vmdEventJsonMapper} 变成全局唯一 mapper、破坏共享装配（FW-WEB CR-001 §5.1
 *       “不创建绕过 Boot 的唯一 ObjectMapper”）；</li>
 *   <li>不修改共享 Spring ObjectMapper，不影响其他受管字段的 epoch-millis 旧行为；</li>
 *   <li>只序列化这两个事件的 payload（写出路径），不做任何读取；事件对象除 occurredAt
 *       外无其他 java.time 字段；</li>
 *   <li>Instant.toString() 规范化算法不变（ISO-8601 UTC，精度按实际值），不改变
 *       任何 digest / canonicalization 输入；</li>
 *   <li>历史 Outbox payload 由 Relay 原样重发，本 codec 不对其重编码。</li>
 * </ul>
 * 例外登记：本 codec 属于 CR-070「兼容阶段保持旧输出」通用条款的显式例外，已回写
 * VMD-DSN-CR-070（Notion 实现与验证记录）与字段清单；
 * 待办 P0：线上 Topic 实际格式核验 + 全部真实消费方签收证据（生产事实，由部署方提供）。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
public class VmdEventJsonCodec {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 序列化为 JSON 字符串（ISO-8601 UTC 时间输出）。
     */
    public String writeAsString(Object value) throws JsonProcessingException {
        return mapper.writeValueAsString(value);
    }

    /**
     * 暴露内部 mapper（测试与受控扩展用；非 Spring bean）。
     */
    public ObjectMapper mapper() {
        return mapper;
    }
}
