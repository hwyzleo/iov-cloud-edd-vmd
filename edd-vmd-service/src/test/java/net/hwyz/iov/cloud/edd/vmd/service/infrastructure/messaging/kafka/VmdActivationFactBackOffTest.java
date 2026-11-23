package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.BackOffExecution;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 激活事实退避策略单元测试（VMD-DSN-CR-077 / D-VMD-077-07 / TC-077-09）
 * <p>
 * 默认「3 次投递 / 100ms→1s 指数退避」：退避序列应为 100、200，第三次调用返回 STOP
 * （即退避耗尽后进入 FAILED 终态）。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdActivationFactBackOff 测试")
class VmdActivationFactBackOffTest {

    @Test
    @DisplayName("默认参数：3 次投递，退避 100ms → 200ms → STOP")
    void defaultBackoff_threeAttempts() {
        VmdActivationFactBackOff backOff = new VmdActivationFactBackOff(3, 100L, 2.0d, 1_000L);
        BackOffExecution execution = backOff.start();

        assertEquals(100L, execution.nextBackOff());
        assertEquals(200L, execution.nextBackOff());
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
        assertEquals(3, backOff.maxAttempts());
    }

    @Test
    @DisplayName("退避间隔以 maxInterval 封顶")
    void backoffCappedByMaxInterval() {
        VmdActivationFactBackOff backOff = new VmdActivationFactBackOff(5, 400L, 4.0d, 1_000L);
        BackOffExecution execution = backOff.start();

        assertEquals(400L, execution.nextBackOff());
        assertEquals(1_000L, execution.nextBackOff(), "1600ms 应封顶为 1000ms");
        assertEquals(1_000L, execution.nextBackOff());
        assertEquals(1_000L, execution.nextBackOff());
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
    }

    @Test
    @DisplayName("maxAttempts=1：首次失败即终止（不重试）")
    void singleAttempt_noRetry() {
        BackOffExecution execution = new VmdActivationFactBackOff(1, 100L, 2.0d, 1_000L).start();
        assertEquals(BackOffExecution.STOP, execution.nextBackOff());
    }

}
