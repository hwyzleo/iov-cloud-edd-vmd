package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 零件入站跨域事件动作处理器单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartInboundEventActionHandler 测试")
class PartInboundEventActionHandlerTest {

    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private PartInboundEventActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PartInboundEventActionHandler(topicRoutes);
        lenient().when(topicRoutes.topicName(VmdKafkaLogicalTopic.PART_INBOUND_CHANGED))
                .thenReturn("vmd.part-inbound.changed");
    }

    private PartPostProcessActionContext buildContext(PartInfo partInfo) {
        return PartPostProcessActionContext.builder()
                .replayId("replay-001")
                .partImportDataId(1L)
                .batchNum("B001")
                .partCode(partInfo != null ? partInfo.getPartCode() : "PN001")
                .sn(partInfo != null ? partInfo.getSn() : "SN001")
                .partInfo(partInfo)
                .operatorId("op1")
                .operatorName("操作人")
                .scope(Set.of(net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartPostProcessActionType.PART_INBOUND_EVENT))
                .build();
    }

    private PartInfo buildPartInfo() {
        return PartInfo.builder()
                .partCode("PN001")
                .sn("SN001")
                .vehicleNodeCode("TBOX_5G")
                .partType("TBOX")
                .supplierCode("SUP001")
                .hardwarePn("HW001")
                .hardwareVer("V1.0")
                .extra("{\"iccid1\":\"4600000001\"}")
                .lastInboundTime(Instant.now())
                .source(InboundSourceType.MES)
                .build();
    }

    @Test
    @DisplayName("从当前 part_info 构造当前快照事件并准备 Outbox（replay=true 新 eventId）")
    void execute_buildsOutboxWithCurrentSnapshot() {
        PartPostProcessActionContext context = buildContext(buildPartInfo());
        PartPostProcessActionResult result = handler.execute(context);

        assertEquals(PartPostProcessActionResult.Outcome.QUEUED, result.getOutcome());
        assertNotNull(result.getEventId());
        VmdOutbox outbox = result.getOutbox();
        assertNotNull(outbox);
        assertEquals("PartInboundEvent", outbox.getEventType());
        assertEquals("vmd.part-inbound.changed", outbox.getTopic());
        assertEquals("PN001:SN001", outbox.getMessageKey());
        assertEquals("PART_POST_PROCESS_REPLAY", outbox.getSourceType());
        assertEquals("replay-001", outbox.getSourceRefId());
        assertEquals("PENDING", outbox.getPublishState());

        JSONObject payload = JSONUtil.parseObj(outbox.getPayload());
        assertEquals("PartInboundEvent", payload.getStr("eventType"));
        assertEquals("PART_INSTANCE", payload.getStr("aggregateType"));
        assertEquals(Boolean.TRUE, payload.getBool("replay"));
        assertEquals("replay-001", payload.getStr("replayId"));
        assertEquals("B001", payload.getStr("batchNum"));
        assertEquals("op1", payload.getStr("replayOperator"));
        JSONObject p = payload.getJSONObject("payload");
        assertEquals("PN001", p.getStr("partCode"));
        assertEquals("SN001", p.getStr("sn"));
        assertEquals("TBOX_5G", p.getStr("vehicleNodeCode"));
        assertEquals("SUP001", p.getStr("supplierCode"));
    }

    @Test
    @DisplayName("候选实例不存在时 FAILED_FINAL(PART_NOT_FOUND)，不得重建")
    void execute_partNotFound() {
        PartPostProcessActionContext context = buildContext(null);
        PartPostProcessActionResult result = handler.execute(context);
        assertEquals(PartPostProcessActionResult.Outcome.FAILED_FINAL, result.getOutcome());
        assertEquals("PART_NOT_FOUND", result.getErrorCode());
        assertNull(result.getOutbox());
    }

    @Test
    @DisplayName("supports：候选实例存在为 true，不存在为 false")
    void supports() {
        assertTrue(handler.supports(buildContext(buildPartInfo())));
        assertFalse(handler.supports(buildContext(null)));
    }

    @Test
    @DisplayName("idempotencyKey = replayId:partCode:sn:actionType")
    void idempotencyKey() {
        assertEquals("replay-001:PN001:SN001:PART_INBOUND_EVENT",
                handler.idempotencyKey(buildContext(buildPartInfo())));
    }
}
