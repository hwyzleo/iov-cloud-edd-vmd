package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 绑定事实事件动作处理器单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BindingFactEventActionHandler 测试")
class BindingFactEventActionHandlerTest {

    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private BindingFactEventActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BindingFactEventActionHandler(topicRoutes);
        lenient().when(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_BINDING_CHANGED))
                .thenReturn("vmd.vehicle-part-binding.changed");
    }

    private PartPostProcessActionContext buildContext(PartInfo partInfo, VehiclePart binding, VehicleNode vehicleNode) {
        return PartPostProcessActionContext.builder()
                .replayId("replay-002")
                .partImportDataId(1L)
                .batchNum("B001")
                .partCode("PN001")
                .sn("SN001")
                .partInfo(partInfo)
                .activeBinding(binding)
                .vehicleNode(vehicleNode)
                .operatorId("op1")
                .scope(Set.of(PartPostProcessActionType.BINDING_FACT_EVENT))
                .build();
    }

    @Test
    @DisplayName("存在 active 绑定时按当前快照重述绑定事实事件（replay=true 当前 bindingId/seq）")
    void execute_withActiveBinding() {
        PartInfo partInfo = PartInfo.builder().partCode("PN001").sn("SN001")
                .extra("{\"iccid1\":\"4600000001\"}").build();
        VehiclePart binding = VehiclePart.builder().id(88L).vin("VIN0001").partId(1L)
                .vehicleNodeCode("TBOX_5G").bindTime(Instant.parse("2026-01-01T00:00:00Z")).build();
        VehicleNode vehicleNode = VehicleNode.builder().code("TBOX_5G").deviceCategory("TBOX").build();

        PartPostProcessActionResult result = handler.execute(buildContext(partInfo, binding, vehicleNode));

        assertEquals(PartPostProcessActionResult.Outcome.QUEUED, result.getOutcome());
        assertNotNull(result.getEventId());
        VmdOutbox outbox = result.getOutbox();
        assertEquals("VehiclePartBindingChangedEvent", outbox.getEventType());
        assertEquals("vmd.vehicle-part-binding.changed", outbox.getTopic());
        assertEquals("VIN0001", outbox.getMessageKey());

        JSONObject payload = JSONUtil.parseObj(outbox.getPayload());
        assertEquals(Boolean.TRUE, payload.getBool("replay"));
        assertEquals("replay-002", payload.getStr("replayId"));
        assertEquals(88L, payload.getLong("bindingId"));
        assertEquals("BIND", payload.getStr("changeType"));
        assertEquals("VIN0001", payload.getStr("vin"));
        assertEquals("TBOX", payload.getStr("deviceCategory"));
        assertEquals("4600000001", payload.getStr("iccid1"));
        assertNotNull(payload.getLong("seq"));
    }

    @Test
    @DisplayName("无 active 绑定时 SKIPPED(NO_ACTIVE_BINDING)，不产生事件")
    void execute_noActiveBinding() {
        PartInfo partInfo = PartInfo.builder().partCode("PN001").sn("SN001").build();
        PartPostProcessActionResult result = handler.execute(buildContext(partInfo, null, null));
        assertEquals(PartPostProcessActionResult.Outcome.SKIPPED, result.getOutcome());
        assertEquals("NO_ACTIVE_BINDING", result.getSkipReason());
        assertNull(result.getOutbox());
    }

    @Test
    @DisplayName("候选不存在时 FAILED_FINAL(PART_NOT_FOUND)")
    void execute_partNotFound() {
        PartPostProcessActionResult result = handler.execute(buildContext(null, null, null));
        assertEquals(PartPostProcessActionResult.Outcome.FAILED_FINAL, result.getOutcome());
        assertEquals("PART_NOT_FOUND", result.getErrorCode());
    }

    @Test
    @DisplayName("supports：候选存在且有 active 绑定为 true")
    void supports() {
        PartInfo partInfo = PartInfo.builder().partCode("PN001").sn("SN001").build();
        VehiclePart binding = VehiclePart.builder().id(1L).vin("VIN0001").build();
        assertTrue(handler.supports(buildContext(partInfo, binding, null)));
        assertFalse(handler.supports(buildContext(partInfo, null, null)));
        assertFalse(handler.supports(buildContext(null, binding, null)));
    }
}
