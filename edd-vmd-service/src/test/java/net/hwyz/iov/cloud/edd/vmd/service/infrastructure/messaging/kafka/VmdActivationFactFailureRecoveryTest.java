package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ActivationFactRecordMeta;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationConsumeResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleActivationFactConsumeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 激活事实技术失败恢复器单元测试（VMD-DSN-CR-077 / D-VMD-077-07 / TC-077-09）
 * <p>
 * 退避耗尽后：台账落 FAILED（retry_count=attempts-1）并提交位点；
 * 归档本身失败时仍不得抛异常导致分区被毒化。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VmdActivationFactFailureRecovery 测试")
class VmdActivationFactFailureRecoveryTest {

    @Mock
    private VehicleActivationFactConsumeAppService consumeAppService;

    private VmdActivationFactConsumerProperties properties;
    private VmdActivationFactFailureRecovery recovery;

    @BeforeEach
    void setUp() {
        properties = new VmdActivationFactConsumerProperties();
        recovery = new VmdActivationFactFailureRecovery(consumeAppService, properties);
    }

    @Test
    @DisplayName("退避耗尽 → 归档 FAILED（retry_count=2）并提交位点，不抛异常")
    void recover_archivesFailed() {
        when(consumeAppService.markFailed(anyString(), any(ActivationFactRecordMeta.class), anyString(), anyInt()))
                .thenReturn(VehicleActivationConsumeResult.failed());
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("tsp.user-vehicle-activation-fact", 1, 99L, "VIN", "{\"eventId\":\"E1\"}");

        assertDoesNotThrow(() -> recovery.recover(record, new IllegalStateException("db down")));

        ArgumentCaptor<ActivationFactRecordMeta> metaCaptor =
                ArgumentCaptor.forClass(ActivationFactRecordMeta.class);
        verify(consumeAppService).markFailed(anyString(), metaCaptor.capture(), anyString(), anyInt());
        assertEquals("tsp.user-vehicle-activation-fact", metaCaptor.getValue().topic());
        assertEquals(1, metaCaptor.getValue().partition());
        assertEquals(99L, metaCaptor.getValue().offset());
    }

    @Test
    @DisplayName("归档失败（存储不可用）→ 仅记日志，不抛异常（避免毒化分区）")
    void recover_persistFailure_doesNotThrow() {
        when(consumeAppService.markFailed(anyString(), any(ActivationFactRecordMeta.class), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("db down"));
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("tsp.user-vehicle-activation-fact", 0, 1L, null, "{}");

        assertDoesNotThrow(() -> recovery.recover(record, new IllegalStateException("boom")));
    }

    @Test
    @DisplayName("错误摘要截断到 512 字符内（脱敏证据长度控制）")
    void truncate_limitsLength() {
        String longText = "x".repeat(600);
        String truncated = VmdActivationFactFailureRecovery.truncate(longText);
        assertTrue(truncated.length() <= 512);
        assertTrue(truncated.endsWith("..."));
    }

}
