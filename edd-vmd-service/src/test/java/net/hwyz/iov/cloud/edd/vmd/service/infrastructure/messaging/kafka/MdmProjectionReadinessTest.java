package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MDM 投影 Readiness 状态单元测试（VMD-DSN-CR-052 §5 / §8.2）
 * <p>
 * 验证逐投影状态（topicReachable / aclReady / bootstrapStatus / consumerRunning /
 * lag / lastEventAt）分开暴露、汇总与逐投影状态不互相掩蔽。
 *
 * @author hwyz_leo
 */
@DisplayName("MdmProjectionReadiness 测试")
class MdmProjectionReadinessTest {

    @Test
    @DisplayName("初始状态：全部 INIT，不视为失败")
    void initialState_allInit() {
        MdmProjectionReadiness readiness = new MdmProjectionReadiness();
        assertTrue(readiness.allInit());
        assertFalse(readiness.allReady());
        assertFalse(readiness.anyFailure());
        assertEquals(MdmProjectionType.values().length, readiness.statuses().size());
    }

    @Test
    @DisplayName("预检通过：UP + topicReachable + aclReady，Bootstrap/消费状态仍分开暴露")
    void preflightPassed_separateBootstrapState() {
        MdmProjectionReadiness readiness = new MdmProjectionReadiness();
        readiness.markPreflightPassed(MdmProjectionType.BRAND, "mdm.brand");
        MdmProjectionReadiness.ProjectionStatus status = readiness.status(MdmProjectionType.BRAND);
        assertEquals(MdmProjectionReadiness.State.UP, status.state());
        assertTrue(status.topicReachable());
        assertTrue(status.aclReady());
        assertEquals(MdmProjectionReadiness.BootstrapStatus.NOT_RUN, status.bootstrapStatus(),
                "预检通过不代表投影已同步，Bootstrap 状态必须分开暴露");
        assertFalse(status.consumerRunning());
        assertEquals(-1, status.lag());
    }

    @Test
    @DisplayName("全投影通过才 allReady；单投影失败 anyFailure")
    void singleFailure_notAllReady() {
        MdmProjectionReadiness readiness = new MdmProjectionReadiness();
        for (MdmProjectionType p : MdmProjectionType.values()) {
            if (p == MdmProjectionType.PART) {
                readiness.markPreflightFailed(p, "mdm.part", "missing: Topic 不存在");
            } else {
                readiness.markPreflightPassed(p, p.directoryTopic());
            }
        }
        assertFalse(readiness.allReady());
        assertTrue(readiness.anyFailure());
        assertEquals(MdmProjectionReadiness.State.DOWN, readiness.status(MdmProjectionType.PART).state());
        assertEquals(MdmProjectionReadiness.State.UP, readiness.status(MdmProjectionType.BRAND).state());
    }

    @Test
    @DisplayName("降级启动：DEGRADED 计入 anyFailure（健康不得掩蔽）")
    void degraded_countsAsFailure() {
        MdmProjectionReadiness readiness = new MdmProjectionReadiness();
        readiness.markDegraded(MdmProjectionType.PLATFORM, "mdm.platform", "unauthorized: ACL 不足");
        assertTrue(readiness.anyFailure());
        assertEquals(MdmProjectionReadiness.State.DEGRADED, readiness.status(MdmProjectionType.PLATFORM).state());
        assertFalse(readiness.status(MdmProjectionType.PLATFORM).topicReachable());
    }

    @Test
    @DisplayName("Bootstrap / consumer / lag / lastEventAt 记录与暴露")
    void runtimeStateExposure() {
        MdmProjectionReadiness readiness = new MdmProjectionReadiness();
        readiness.markPreflightPassed(MdmProjectionType.MODEL, "mdm.model");
        readiness.markBootstrap(MdmProjectionType.MODEL, MdmProjectionReadiness.BootstrapStatus.COMPLETED);
        readiness.markConsumerRunning(MdmProjectionType.MODEL, true);
        readiness.recordLag(MdmProjectionType.MODEL, 5);
        Instant now = Instant.now();
        readiness.recordEvent(MdmProjectionType.MODEL, now);

        MdmProjectionReadiness.ProjectionStatus status = readiness.status(MdmProjectionType.MODEL);
        assertEquals(MdmProjectionReadiness.BootstrapStatus.COMPLETED, status.bootstrapStatus());
        assertTrue(status.consumerRunning());
        assertEquals(5, status.lag());
        assertEquals(now, status.lastEventAt());
    }
}
