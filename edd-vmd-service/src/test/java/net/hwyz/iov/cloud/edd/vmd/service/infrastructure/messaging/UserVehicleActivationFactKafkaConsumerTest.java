package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactNormalized;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.UserVehicleActivationFactEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationConsumeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 车辆首次激活候选事实消费者单元测试（VMD-DSN-CR-077 §6.1 / TC-077-02/03/05/08/09）
 * <p>
 * 契约错误隔离不重试不阻塞；技术错误上抛触发退避（由错误处理器归档 FAILED）；
 * 摘要差异仅计观测指标、不判冲突终态。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserVehicleActivationFactKafkaConsumer 测试")
class UserVehicleActivationFactKafkaConsumerTest {

    private static final String TOPIC = "tsp.user-vehicle-activation-fact";
    private static final String VIN = "LSVAA1234567890AB";
    private static final String REF = "vso:order:SO1#delivery";
    private static final String OCCURRED_AT = "2026-11-22T09:45:01.234Z";
    private static final String OP = "BOP20261122094501000123";
    private static final String VALID_PAYLOAD = "{\"eventId\":\"UVA20261122094501123456\","
            + "\"schemaVersion\":1,\"vin\":\"" + VIN + "\","
            + "\"bindingOperationId\":\"" + OP + "\","
            + "\"sourceFactRef\":\"" + REF + "\","
            + "\"occurredAt\":\"" + OCCURRED_AT + "\"}";

    @Mock
    private VehicleActivationFactConsumeAppService consumeAppService;
    @Mock
    private VmdActivationFactMetrics metrics;

    private UserVehicleActivationFactKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserVehicleActivationFactKafkaConsumer(consumeAppService, metrics, new ObjectMapper());
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>(TOPIC, 2, 42L, VIN, payload);
    }

    /**
     * 给合法载荷追加 payloadDigest 字段
     *
     * @param digest 摘要
     * @return 载荷 JSON
     */
    private String payloadWithDigest(String digest) {
        return VALID_PAYLOAD.replaceFirst("\\}$", ",\"payloadDigest\":\"" + digest + "\"}");
    }

    @Test
    @DisplayName("TC-077-02：合法载荷 → 应用服务消费并记 success 指标")
    void validPayload_consumed() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, true));

        consumer.onActivationFact(record(VALID_PAYLOAD));

        ArgumentCaptor<ActivationFactNormalized> captor = ArgumentCaptor.forClass(ActivationFactNormalized.class);
        verify(consumeAppService).consume(captor.capture(), any(ActivationFactRecordMeta.class));
        assertEquals(VIN, captor.getValue().vin());
        verify(metrics).recordConsume(TOPIC, "success");
        verify(metrics).recordDigestSource("LOCAL");
        verify(metrics).recordVinUnresolved(TOPIC);
    }

    @Test
    @DisplayName("B-4 残留①：事件带摘要且与本地一致 → 消费正常、digest_source=EVENT、不算 mismatch")
    void digestProvidedAndMatching_sourceEvent() {
        String digest = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, true));

        consumer.onActivationFact(record(payloadWithDigest(digest)));

        ArgumentCaptor<ActivationFactNormalized> captor = ArgumentCaptor.forClass(ActivationFactNormalized.class);
        verify(consumeAppService).consume(captor.capture(), any(ActivationFactRecordMeta.class));
        assertEquals("EVENT", captor.getValue().digestSourceCode());
        assertEquals(digest, captor.getValue().payloadDigest(), "事件摘要为权威落库值");
        assertFalse(captor.getValue().digestProvidedButMismatch());
        verify(metrics).recordDigestSource("EVENT");
        verify(metrics, never()).recordDigestMismatch(TOPIC);
        verify(metrics).recordConsume(TOPIC, "success");
    }

    @Test
    @DisplayName("B-4 残留②：事件带摘要但与本地不一致 → 仍 EVENT（不降级）+ digest_mismatch 指标 + 消费正常")
    void digestProvidedButMismatch_stillEventWithMismatchMetric() {
        String foreignDigest = "0".repeat(ActivationPayloadDigest.DIGEST_LENGTH);
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, true));

        consumer.onActivationFact(record(payloadWithDigest(foreignDigest)));

        ArgumentCaptor<ActivationFactNormalized> captor = ArgumentCaptor.forClass(ActivationFactNormalized.class);
        verify(consumeAppService).consume(captor.capture(), any(ActivationFactRecordMeta.class));
        assertEquals("EVENT", captor.getValue().digestSourceCode(), "事件摘要为权威，不得降级为 LOCAL");
        assertEquals(foreignDigest, captor.getValue().payloadDigest());
        assertTrue(captor.getValue().digestProvidedButMismatch());
        verify(metrics).recordDigestSource("EVENT");
        verify(metrics).recordDigestMismatch(TOPIC);
        verify(metrics).recordConsume(TOPIC, "success");
    }

    @Test
    @DisplayName("B-4 残留③：事件不带摘要 → digest_source=LOCAL、无 mismatch 指标")
    void digestAbsent_sourceLocal() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, true));

        consumer.onActivationFact(record(VALID_PAYLOAD));

        ArgumentCaptor<ActivationFactNormalized> captor = ArgumentCaptor.forClass(ActivationFactNormalized.class);
        verify(consumeAppService).consume(captor.capture(), any(ActivationFactRecordMeta.class));
        assertEquals("LOCAL", captor.getValue().digestSourceCode());
        assertEquals(captor.getValue().localDigest(), captor.getValue().payloadDigest());
        assertFalse(captor.getValue().digestProvidedButMismatch());
        verify(metrics).recordDigestSource("LOCAL");
        verify(metrics, never()).recordDigestMismatch(TOPIC);
    }

    @Test
    @DisplayName("TC-077-05：摘要差异 → duplicate + digest_mismatch 指标，无异常上抛")
    void digestMismatch_metricsOnly() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.duplicate(true));

        consumer.onActivationFact(record(VALID_PAYLOAD));

        verify(metrics).recordConsume(TOPIC, "duplicate");
        verify(metrics).recordDigestMismatch(TOPIC);
    }

    @Test
    @DisplayName("TC-077-08：schemaVersion=2 → 立即隔离（不重试、不抛异常）")
    void contractError_quarantinedWithoutRetry() {
        String payload = VALID_PAYLOAD.replace("\"schemaVersion\":1", "\"schemaVersion\":2");

        consumer.onActivationFact(record(payload));

        ArgumentCaptor<ActivationFactRecordMeta> metaCaptor =
                ArgumentCaptor.forClass(ActivationFactRecordMeta.class);
        verify(consumeAppService).quarantine(any(UserVehicleActivationFactEvent.class), metaCaptor.capture(),
                eq("不支持的 schemaVersion=2"), eq(payload));
        assertEquals(TOPIC, metaCaptor.getValue().topic());
        assertEquals(42L, metaCaptor.getValue().offset());
        verify(metrics).recordConsume(TOPIC, "quarantined");
        verify(consumeAppService, never()).consume(any(), any());
    }

    @Test
    @DisplayName("TC-077-08：VIN 超长 → 隔离（不截断），同分区后续消息不受影响")
    void vinTooLong_quarantined() {
        String payload = VALID_PAYLOAD.replace(VIN, "LSVAA1234567890ABCDEF");

        consumer.onActivationFact(new ConsumerRecord<>(TOPIC, 2, 43L, null, payload));

        verify(consumeAppService).quarantine(any(UserVehicleActivationFactEvent.class), any(),
                eq("vin 超长（>20，不截断）"), anyString());
        verify(metrics).recordConsume(TOPIC, "quarantined");
        // 后续合法消息可继续消费（消费者未抛出、未阻塞）
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any()))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, false));
        consumer.onActivationFact(record(VALID_PAYLOAD));
        verify(metrics).recordConsume(TOPIC, "success");
    }

    @Test
    @DisplayName("载荷不可解析 → 隔离且不抛异常")
    void malformedPayload_quarantined() {
        consumer.onActivationFact(record("not-json"));

        verify(consumeAppService).quarantine(isNull(), any(ActivationFactRecordMeta.class),
                eq("载荷不可解析: JsonParseException"), eq("not-json"));
        verify(metrics).recordConsume(TOPIC, "quarantined");
    }

    @Test
    @DisplayName("空载荷 → 隔离且不抛异常")
    void blankPayload_quarantined() {
        consumer.onActivationFact(record("  "));

        verify(consumeAppService).quarantine(isNull(), any(ActivationFactRecordMeta.class),
                eq("载荷不可解析: IllegalArgumentException"), eq("  "));
    }

    @Test
    @DisplayName("TC-077-09：技术错误上抛，交由错误处理器退避重试（不静默确认）")
    void technicalError_propagates() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        DataAccessResourceFailureException ex = assertThrows(DataAccessResourceFailureException.class,
                () -> consumer.onActivationFact(record(VALID_PAYLOAD)));
        assertTrue(ex.getMessage().contains("db down"));
        verify(consumeAppService, never()).quarantine(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("TC-077-06：向前纠正 → 计 success + corrected 指标")
    void corrected_metrics() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(true, false));

        consumer.onActivationFact(record(VALID_PAYLOAD));

        verify(metrics).recordConsume(TOPIC, "success");
        verify(metrics).recordConsume(TOPIC, "corrected");
    }

    @Test
    @DisplayName("指标记录失败不影响消费")
    void metricFailure_doesNotBreakConsume() {
        when(consumeAppService.consume(any(ActivationFactNormalized.class), any(ActivationFactRecordMeta.class)))
                .thenReturn(VehicleActivationConsumeResult.recorded(false, false));
        org.mockito.Mockito.doThrow(new IllegalStateException("meter down"))
                .when(metrics).recordConsume(anyString(), anyString());

        consumer.onActivationFact(record(VALID_PAYLOAD));

        verify(consumeAppService).consume(any(), any());
    }

}
