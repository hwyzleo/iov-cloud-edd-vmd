package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.VehicleActivationLifecycleWriter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdActivationFactMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活事实消费应用服务单元测试（VMD-DSN-CR-077 §6.1 / §6.2）
 * <p>
 * 覆盖 TC-VMD-077-02/03/04/05/06/07/08/17/18：
 * 双重幂等、摘要不互斥（合法重投不判冲突终态）、occurredAt 最小值单调收敛、
 * 真冲突隔离、契约错误隔离、技术失败终态。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleActivationFactConsumeAppService 测试")
class VehicleActivationFactConsumeAppServiceTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String EVENT_ID = "UVA20261122094501123456";
    private static final ActivationFactRecordMeta META =
            new ActivationFactRecordMeta("tsp.user-vehicle-activation-fact", 3, 1024L);

    @Mock
    private VehicleActivationFactConsumeRepository consumeRepository;
    @Mock
    private VehicleActivationNodeRepository nodeRepository;
    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;
    @Mock
    private VehicleActivationLifecycleWriter lifecycleWriter;
    @Mock
    private VmdActivationFactMetrics activationFactMetrics;

    private VehicleActivationFactConsumeAppService appService;

    @BeforeEach
    void setUp() {
        appService = new VehicleActivationFactConsumeAppService(consumeRepository, nodeRepository,
                vehBasicInfoRepository, lifecycleWriter, activationFactMetrics, new ObjectMapper());
    }

    private ActivationFactNormalized fact(String eventId, String op, String occurredAt) {
        String digest = ActivationPayloadDigest.compute(VIN, op,
                "vso:order:SO20261120001#delivery", occurredAt);
        return ActivationFactNormalized.builder()
                .eventId(eventId)
                .schemaVersion(1)
                .vin(VIN)
                .bindingOperationId(op)
                .sourceFactRef("vso:order:SO20261120001#delivery")
                .occurredAt(Instant.parse(occurredAt))
                .occurredAtText(occurredAt)
                .payloadDigest(digest)
                .localDigest(digest)
                .digestSource(ActivationDigestSource.LOCAL)
                .digestProvidedButMismatch(false)
                .build();
    }

    private VehicleActivationFactConsume audit(String eventId, String op, String digest) {
        return VehicleActivationFactConsume.builder()
                .id(1L)
                .eventId(eventId)
                .vin(VIN)
                .bindingOperationId(op)
                .payloadDigest(digest)
                .status(ActivationFactConsumeStatus.RECORDED.name())
                .applied(1)
                .build();
    }

    private VehicleActivationNode node(String occurredAt) {
        return VehicleActivationNode.builder()
                .id(9L)
                .vin(VIN)
                .firstActivatedAt(toLocal(occurredAt))
                .occurredAt(toLocal(occurredAt))
                .eventId(EVENT_ID)
                .build();
    }

    private LocalDateTime toLocal(String instant) {
        return LocalDateTime.ofInstant(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("TC-077-02：首次合法事实 → 投影插入 + 台账 RECORDED(applied=1) + 未建档标记")
    void firstLegalFact_recordsAndApplies() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(
                fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        assertEquals(ActivationFactConsumeStatus.RECORDED, result.status());
        assertTrue(result.applied());
        assertFalse(result.corrected());
        assertTrue(result.vinUnresolved(), "未建档 VIN 应标记 vin_resolved=0 而不建主档");

        ArgumentCaptor<VehicleActivationNode> nodeCaptor = ArgumentCaptor.forClass(VehicleActivationNode.class);
        verify(nodeRepository).insert(nodeCaptor.capture());
        assertEquals(toLocal("2026-11-22T09:45:01.234Z"), nodeCaptor.getValue().getFirstActivatedAt());
        assertEquals(0, nodeCaptor.getValue().getVinResolved());
        assertEquals("LOCAL", nodeCaptor.getValue().getDigestSource());

        ArgumentCaptor<VehicleActivationFactConsume> auditCaptor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(auditCaptor.capture());
        assertEquals(ActivationFactConsumeStatus.RECORDED.name(), auditCaptor.getValue().getStatus());
        assertEquals(1, auditCaptor.getValue().getApplied());
        assertEquals(3, auditCaptor.getValue().getPartitionNo());
        assertEquals(1024L, auditCaptor.getValue().getRecordOffset());
        verify(vehBasicInfoRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-03：同 eventId 重投 → DUPLICATE，投影不动")
    void sameEventId_duplicate() {
        when(consumeRepository.selectByEventId(EVENT_ID))
                .thenReturn(audit(EVENT_ID, OP, fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z").payloadDigest()));

        VehicleActivationConsumeResult result = appService.consume(
                fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        assertEquals(ActivationFactConsumeStatus.DUPLICATE, result.status());
        assertFalse(result.digestMismatch());
        verify(nodeRepository, never()).insert(any());
        verify(consumeRepository, never()).insert(any());
        verify(consumeRepository).update(any(VehicleActivationFactConsume.class));
    }

    @Test
    @DisplayName("TC-077-04：同业务键不同 eventId、摘要一致 → DUPLICATE，无摘要差异")
    void sameBusinessKey_duplicateWithoutDigestMismatch() {
        ActivationFactNormalized fact = fact("UVA-NEW", OP, "2026-11-22T09:45:01.234Z");
        when(consumeRepository.selectByEventId("UVA-NEW")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP))
                .thenReturn(audit(EVENT_ID, OP, fact.payloadDigest()));

        VehicleActivationConsumeResult result = appService.consume(fact, META);

        assertEquals(ActivationFactConsumeStatus.DUPLICATE, result.status());
        assertFalse(result.digestMismatch());
        verify(nodeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-05：同业务键摘要不一致（合法重投）→ 仍 DUPLICATE + DIGEST_MISMATCH，不判冲突终态")
    void sameBusinessKey_digestMismatch_notConflict() {
        ActivationFactNormalized fact = fact("UVA-NEW", OP, "2026-11-24T09:45:01.234Z");
        VehicleActivationFactConsume existing = audit(EVENT_ID, OP, "different-digest");        when(consumeRepository.selectByEventId("UVA-NEW")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(existing);

        VehicleActivationConsumeResult result = appService.consume(fact, META);

        assertEquals(ActivationFactConsumeStatus.DUPLICATE, result.status(),
                "摘要差异不允许升级为冲突终态（R-10）");
        assertTrue(result.digestMismatch());
        assertEquals("DIGEST_MISMATCH", existing.getDescription());
        verify(consumeRepository, never()).insert(any());
        verify(nodeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-06：乱序向前纠正 → 投影改为更早时刻 + CORRECTED")
    void earlierFact_corrects() {
        when(consumeRepository.selectByEventId("UVA-EARLY")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, "BOP-EARLY")).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(node("2026-11-22T09:45:01.234Z"));
        when(nodeRepository.correctIfEarlier(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(
                fact("UVA-EARLY", "BOP-EARLY", "2026-11-20T01:00:00.000Z"), META);

        assertEquals(ActivationFactConsumeStatus.RECORDED, result.status());
        assertTrue(result.corrected(), "更早事实应触发向前纠正");
        ArgumentCaptor<VehicleActivationNode> captor = ArgumentCaptor.forClass(VehicleActivationNode.class);
        verify(nodeRepository).correctIfEarlier(captor.capture());
        assertEquals(toLocal("2026-11-20T01:00:00.000Z"), captor.getValue().getOccurredAt());
        assertEquals("CORRECTED", captor.getValue().getDescription());
        verify(nodeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-07：晚到事实不覆盖 → 投影保持最早，台账 RECORDED(applied=0)")
    void laterFact_doesNotOverride() {
        when(consumeRepository.selectByEventId("UVA-LATE")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, "BOP-LATE")).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(node("2026-11-20T01:00:00.000Z"));
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(
                fact("UVA-LATE", "BOP-LATE", "2026-11-25T01:00:00.000Z"), META);

        assertEquals(ActivationFactConsumeStatus.RECORDED, result.status());
        assertFalse(result.applied(), "晚到事实不得覆盖首次激活时刻");
        verify(nodeRepository, never()).correctIfEarlier(any());
        verify(nodeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-17：同 eventId 但业务键不同 → QUARANTINED，投影与既有终态不被覆盖")
    void trueConflict_quarantined() {
        VehicleActivationFactConsume existing = audit(EVENT_ID, "BOP-OTHER", "digest-1");
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(existing);

        VehicleActivationConsumeResult result = appService.consume(
                fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        assertEquals(ActivationFactConsumeStatus.QUARANTINED, result.status());
        assertEquals("EVENT_ID_BUSINESS_KEY_MISMATCH", existing.getDescription());
        assertEquals(ActivationFactConsumeStatus.RECORDED.name(), existing.getStatus(), "既有终态不得被覆盖");
        verify(nodeRepository, never()).insert(any());
        verify(nodeRepository, never()).correctIfEarlier(any());
    }

    @Test
    @DisplayName("TC-077-08：契约错误隔离 → 台账 QUARANTINED + 位点证据")
    void quarantine_archivesEvidence() {
        UserVehicleActivationFactEvent raw = UserVehicleActivationFactEvent.builder()
                .eventId(EVENT_ID)
                .schemaVersion(2)
                .vin(VIN)
                .bindingOperationId(OP)
                .occurredAt("not-a-time")
                .build();
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result =
                appService.quarantine(raw, META, "不支持的 schemaVersion=2", "{}");

        assertEquals(ActivationFactConsumeStatus.QUARANTINED, result.status());
        ArgumentCaptor<VehicleActivationFactConsume> captor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(captor.capture());
        VehicleActivationFactConsume archived = captor.getValue();
        assertEquals(EVENT_ID, archived.getEventId());
        assertEquals(ActivationFactConsumeStatus.QUARANTINED.name(), archived.getStatus());
        assertEquals(0, archived.getApplied());
        assertEquals(3, archived.getPartitionNo());
        assertEquals(1024L, archived.getRecordOffset());
        assertEquals("tsp.user-vehicle-activation-fact", archived.getTopic());
        assertTrue(archived.getLastError().contains("schemaVersion"));
        verify(nodeRepository, never()).insert(any());
    }

    @Test
    @DisplayName("TC-077-08：缺失 eventId/vin 的载荷仍可归档隔离（占位幂等键，重放稳定）")
    void quarantine_missingIds_usesStablePlaceholder() {
        UserVehicleActivationFactEvent raw = UserVehicleActivationFactEvent.builder().build();
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        appService.quarantine(raw, META, "缺失必填字段 eventId", "{\"foo\":1}");

        ArgumentCaptor<VehicleActivationFactConsume> captor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(captor.capture());
        assertTrue(captor.getValue().getEventId().startsWith("MISSING-"));
        assertEquals("-", captor.getValue().getVin());
        assertEquals("-", captor.getValue().getBindingOperationId());
        assertEquals(64, captor.getValue().getPayloadDigest().length());
    }

    @Test
    @DisplayName("TC-077-09：技术失败 → 既有台账置 FAILED（保留位点待重放）")
    void markFailed_updatesExistingAudit() {
        VehicleActivationFactConsume existing = audit(EVENT_ID, OP, "digest-1");
        String payload = "{\"eventId\":\"" + EVENT_ID + "\",\"schemaVersion\":1,\"vin\":\"" + VIN
                + "\",\"bindingOperationId\":\"" + OP + "\",\"occurredAt\":\"2026-11-22T09:45:01.234Z\"}";
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(existing);

        VehicleActivationConsumeResult result = appService.markFailed(payload, META, "DataAccessException: down", 2);

        assertEquals(ActivationFactConsumeStatus.FAILED, result.status());
        assertEquals(ActivationFactConsumeStatus.FAILED.name(), existing.getStatus());
        assertEquals(0, existing.getApplied());
        assertEquals(2, existing.getRetryCount());
        assertTrue(existing.getLastError().contains("DataAccessException"));
    }

    @Test
    @DisplayName("并发唯一键兜底：台账插入返回 0 行 → 重读后判 DUPLICATE（无重复投影）")
    void concurrentInsert_duplicateFallback() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP))
                .thenReturn(null, audit(EVENT_ID, OP, "digest-x"));
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(0);

        VehicleActivationConsumeResult result = appService.consume(
                fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        assertEquals(ActivationFactConsumeStatus.DUPLICATE, result.status());
    }

    @Test
    @DisplayName("B-4 残留②：事件摘要与本地复算不一致 → 仍 RECORDED + 事件摘要权威落库 + DIGEST_VERIFY_MISMATCH 观测备注")
    void digestVerifyMismatch_recordedWithEventDigest() {
        String eventDigest = "b8ec3fba4ecb32e1455b1da0dba641446a0ac8e89a41f0bf54f17a7cc1680574";
        ActivationFactNormalized mismatch = ActivationFactNormalized.builder()
                .eventId(EVENT_ID)
                .schemaVersion(1)
                .vin(VIN)
                .bindingOperationId(OP)
                .sourceFactRef("vso:order:SO20261120001#delivery")
                .occurredAt(Instant.parse("2026-11-22T09:45:01.234Z"))
                .occurredAtText("2026-11-22T09:45:01.234Z")
                .payloadDigest(eventDigest)
                .localDigest("0ce3b860eb5d9fa1f7c6169af610af6ab653d06ab58ccd3750ec98c3feb8fa33")
                .digestSource(ActivationDigestSource.EVENT)
                .digestProvidedButMismatch(true)
                .build();
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        lenient().when(vehBasicInfoRepository.selectByVin(anyString())).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(mismatch, META);

        assertEquals(ActivationFactConsumeStatus.RECORDED, result.status(),
                "摘要校验不一致不得改变终态（不判冲突、不影响幂等）");
        ArgumentCaptor<VehicleActivationNode> nodeCaptor = ArgumentCaptor.forClass(VehicleActivationNode.class);
        verify(nodeRepository).insert(nodeCaptor.capture());
        assertEquals("DIGEST_VERIFY_MISMATCH", nodeCaptor.getValue().getDescription());
        assertEquals("EVENT", nodeCaptor.getValue().getDigestSource(), "来源必须仍为 EVENT");
        assertEquals(eventDigest, nodeCaptor.getValue().getPayloadDigest(), "投影摘要取事件权威值");

        ArgumentCaptor<VehicleActivationFactConsume> auditCaptor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(auditCaptor.capture());
        assertEquals(ActivationFactConsumeStatus.RECORDED.name(), auditCaptor.getValue().getStatus());
        assertEquals(1, auditCaptor.getValue().getApplied(), "校验不一致不得影响投影生效");
        assertEquals(eventDigest, auditCaptor.getValue().getPayloadDigest(), "台账摘要取事件权威值");
        assertEquals("EVENT", auditCaptor.getValue().getDigestSource());
    }

    @Test
    @DisplayName("B-4 残留①：事件带摘要且与本地一致 → 台账/投影 digest_source=EVENT 且与事件值一致")
    void eventDigestAuthoritative_recorded() {
        ActivationFactNormalized eventDigested = fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z");
        ActivationFactNormalized withEventDigest = ActivationFactNormalized.builder()
                .eventId(eventDigested.eventId())
                .schemaVersion(1)
                .vin(VIN)
                .bindingOperationId(OP)
                .sourceFactRef(eventDigested.sourceFactRef())
                .occurredAt(eventDigested.occurredAt())
                .occurredAtText(eventDigested.occurredAtText())
                .payloadDigest(eventDigested.localDigest())
                .localDigest(eventDigested.localDigest())
                .digestSource(ActivationDigestSource.EVENT)
                .digestProvidedButMismatch(false)
                .build();
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        lenient().when(vehBasicInfoRepository.selectByVin(anyString())).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(withEventDigest, META);

        assertEquals(ActivationFactConsumeStatus.RECORDED, result.status());
        ArgumentCaptor<VehicleActivationFactConsume> captor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(captor.capture());
        assertEquals("EVENT", captor.getValue().getDigestSource());
        assertEquals(withEventDigest.payloadDigest(), captor.getValue().getPayloadDigest());
        assertNull(captor.getValue().getDescription(), "一致时不应留下校验不一致备注");
    }

    @Test
    @DisplayName("已建档 VIN → vin_resolved=1，不额外写主档")
    void resolvedVin_marked() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN).build());
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .applied(toLocal("2026-11-22T09:45:01.234Z").toInstant(ZoneOffset.UTC)));
        lenient().when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        VehicleActivationConsumeResult result = appService.consume(
                fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        assertFalse(result.vinUnresolved());
        ArgumentCaptor<VehicleActivationNode> captor = ArgumentCaptor.forClass(VehicleActivationNode.class);
        verify(nodeRepository).insert(captor.capture());
        assertEquals(1, captor.getValue().getVinResolved());
    }

    @Test
    @DisplayName("TC079-01：已建档合法事实 → 同事务写 VEHICLE_ACTIVE，时间取事实 occurredAt")
    void cr079_resolvedVin_writesLifecycleWithOccurredAt() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN).build());
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .applied(toLocal("2026-11-22T09:45:01.234Z").toInstant(ZoneOffset.UTC)));
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        appService.consume(fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        ArgumentCaptor<LocalDateTime> reachTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(lifecycleWriter).write(eq(VIN), reachTimeCaptor.capture());
        assertEquals(toLocal("2026-11-22T09:45:01.234Z"), reachTimeCaptor.getValue(),
                "生命周期时间必须取事实 occurredAt，不得使用 receivedAt/当前时间");
        verify(activationFactMetrics).recordLifecycle(META.topic(), "APPLIED");
    }

    @Test
    @DisplayName("TC079-02：同 eventId 重投 → 不写生命周期、不新增节点")
    void cr079_duplicate_doesNotWriteLifecycle() {
        when(consumeRepository.selectByEventId(EVENT_ID))
                .thenReturn(audit(EVENT_ID, OP, fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z").payloadDigest()));

        appService.consume(fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        verify(lifecycleWriter, never()).write(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TC079-03：晚到事实 → 生命周期取已存投影的最早时间（不前移）")
    void cr079_laterFact_usesExistingEarliestProjection() {
        when(consumeRepository.selectByEventId("UVA-LATE")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, "BOP-LATE")).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN).build());
        when(nodeRepository.selectByVin(VIN)).thenReturn(node("2026-11-20T01:00:00.000Z"));
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome
                        .duplicate(Instant.parse("2026-11-20T01:00:00.000Z")));
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        appService.consume(fact("UVA-LATE", "BOP-LATE", "2026-11-25T01:00:00.000Z"), META);

        ArgumentCaptor<LocalDateTime> reachTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(lifecycleWriter).write(eq(VIN), reachTimeCaptor.capture());
        assertEquals(toLocal("2026-11-20T01:00:00.000Z"), reachTimeCaptor.getValue());
        verify(activationFactMetrics).recordLifecycle(META.topic(), "DUPLICATE");
    }

    @Test
    @DisplayName("TC079-03：更早事实 → 生命周期纠正证据 LIFECYCLE_CORRECTED")
    void cr079_earlierFact_recordsCorrectionEvidence() {
        when(consumeRepository.selectByEventId("UVA-EARLY")).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, "BOP-EARLY")).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN).build());
        when(nodeRepository.selectByVin(VIN)).thenReturn(node("2026-11-22T09:45:01.234Z"));
        when(nodeRepository.correctIfEarlier(any(VehicleActivationNode.class))).thenReturn(1);
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenReturn(VehicleActivationLifecycleWriter.LifecycleWriteOutcome.corrected(
                        Instant.parse("2026-11-22T09:45:01.234Z"),
                        Instant.parse("2026-11-20T01:00:00.000Z")));
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        appService.consume(fact("UVA-EARLY", "BOP-EARLY", "2026-11-20T01:00:00.000Z"), META);

        ArgumentCaptor<VehicleActivationFactConsume> auditCaptor =
                ArgumentCaptor.forClass(VehicleActivationFactConsume.class);
        verify(consumeRepository).insert(auditCaptor.capture());
        String description = auditCaptor.getValue().getDescription();
        assertTrue(description.contains("LIFECYCLE_CORRECTED"), "应留下生命周期纠正证据");
        assertTrue(description.contains("event=UVA-EARLY"));
        verify(activationFactMetrics).recordLifecycle(META.topic(), "CORRECTED");
    }

    @Test
    @DisplayName("TC079-07：未建档 VIN → 不写生命周期，只记 PENDING_VIN")
    void cr079_unresolvedVin_skipsLifecycle() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(consumeRepository.insert(any(VehicleActivationFactConsume.class))).thenReturn(1);

        appService.consume(fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META);

        verify(lifecycleWriter, never()).write(anyString(), any(LocalDateTime.class));
        verify(activationFactMetrics).recordLifecycle(META.topic(), "PENDING_VIN");
    }

    @Test
    @DisplayName("TC079-05：生命周期写入异常 → 异常抛出（事务回滚）+ FAILED 指标")
    void cr079_lifecycleFailure_propagatesAndMarksFailed() {
        when(consumeRepository.selectByEventId(EVENT_ID)).thenReturn(null);
        when(consumeRepository.selectByBusinessKey(VIN, OP)).thenReturn(null);
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN).build());
        when(nodeRepository.selectByVin(VIN)).thenReturn(null);
        when(nodeRepository.insert(any(VehicleActivationNode.class))).thenReturn(1);
        when(lifecycleWriter.write(eq(VIN), any(LocalDateTime.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("db down"));

        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
                () -> appService.consume(fact(EVENT_ID, OP, "2026-11-22T09:45:01.234Z"), META));
        verify(activationFactMetrics).recordLifecycle(META.topic(), "FAILED");
    }

}
